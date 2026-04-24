package com.proiect.iptv.controller;

import com.proiect.iptv.dto.IptvOrgChannel;
import com.proiect.iptv.dto.IptvOrgCountry;
import com.proiect.iptv.dto.IptvOrgStream;
import com.proiect.iptv.dto.WatchInfo;
import com.proiect.iptv.service.GeoLocationService;
import com.proiect.iptv.service.IptvOrgService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class BrowseController {
    private final GeoLocationService geoLocationService;
    private final IptvOrgService iptvOrgService;

    public BrowseController(GeoLocationService geoLocationService, IptvOrgService iptvOrgService) {
        this.geoLocationService = geoLocationService;
        this.iptvOrgService = iptvOrgService;
    }

    private Set<String> getAvailableStreamIds() {
        return iptvOrgService.getStreams().stream()
                .filter(s -> s.getChannel() != null)
                .map(IptvOrgStream::getChannel)
                .collect(Collectors.toSet());
    }

    @GetMapping("/browse")
    public String browse(HttpServletRequest request, Model model) {
        String countryCode = geoLocationService.detectCountryCode(request);

        List<IptvOrgCountry> sorted = new ArrayList<>(iptvOrgService.getCountries());
        sorted.sort((a, b) -> {
            if (a.getCode().equals(countryCode)) return -1;
            if (b.getCode().equals(countryCode)) return 1;
            return a.getName().compareTo(b.getName());
        });

        model.addAttribute("countries", sorted);
        model.addAttribute("countryCode", countryCode);

        return "browse";
    }

    @GetMapping("/browse/{code}")
    public String byCountry(@PathVariable String code,
                            @RequestParam(required = false) String q,
                            Model model) {
        model.addAttribute("categories", iptvOrgService.getCategories());
        model.addAttribute("code", code);
        model.addAttribute("q", q);

        if (q != null && !q.isEmpty()) {
            List<IptvOrgChannel> results = iptvOrgService.getChannels().stream()
                    .filter(c -> code.equals(c.getCountry()))
                    .filter(c -> c.getName() != null && c.getName().toLowerCase().contains(q.toLowerCase()))
                    .toList();
            model.addAttribute("searchResults", results);
            model.addAttribute("availableStreams", getAvailableStreamIds());
        }

        return "browse-country";
    }

    @GetMapping("/browse/{code}/{categoryId}")
    public String byCategory(@PathVariable String code, @PathVariable String categoryId, Model model) {
        List<IptvOrgChannel> filtered = iptvOrgService.getChannels().stream()
                .filter(c -> code.equals(c.getCountry()))
                .filter(c -> c.getCategories() != null && c.getCategories().contains(categoryId))
                .toList();

        model.addAttribute("channels", filtered);
        model.addAttribute("code", code);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("availableStreams", getAvailableStreamIds());

        return "browse-channels";
    }

    @GetMapping("/browse/watch/{channelId}")
    public String watchBrowse(@PathVariable String channelId, Model model) {
        IptvOrgChannel channel = iptvOrgService.getChannels().stream()
                .filter(c -> channelId.equals(c.getId()))
                .findFirst()
                .orElse(null);

        List<String> streamUrls = iptvOrgService.getStreams().stream()
                .filter(s -> channelId.equals(s.getChannel()))
                .map(IptvOrgStream::getUrl)
                .toList();

        if (channel == null || streamUrls.isEmpty()) {
            model.addAttribute("error", "This channel has no available stream right now.");
            return "browse";
        }

        WatchInfo info = new WatchInfo();
        info.setName(channel.getName());
        info.setGroupTitle(channel.getCategories() != null && !channel.getCategories().isEmpty() ? channel.getCategories().getFirst() : "");
        info.setStreamUrl(streamUrls.getFirst());

        model.addAttribute("channel", info);
        model.addAttribute("streamUrls", streamUrls);
        return "watch";
    }

    @GetMapping("/stream")
    @ResponseBody
    public ResponseEntity<StreamingResponseBody> proxyStream(@RequestParam String url) {
        if (url.endsWith(".m3u8")) {
            return proxyPlaylist(url);
        }
        return proxyDirect(url);
    }

    private ResponseEntity<StreamingResponseBody> proxyPlaylist(String url) {
        StreamingResponseBody body = outputStream -> {
            try (InputStream is = new java.net.URI(url).toURL().openStream()) {
                String playlist = new String(is.readAllBytes());
                String baseUrl = url.substring(0, url.lastIndexOf('/') + 1);

                String rewritten = playlist.lines().map(line -> {
                    if (!line.startsWith("#") && !line.isEmpty()) {
                        String segmentUrl = line.startsWith("http") ? line : baseUrl + line;
                        return "/stream?url=" + java.net.URLEncoder.encode(segmentUrl, java.nio.charset.StandardCharsets.UTF_8);
                    }
                    return line;
                }).collect(Collectors.joining("\n"));

                outputStream.write(rewritten.getBytes());
            } catch (Exception ex) {

            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"))
                .body(body);
    }

    private ResponseEntity<StreamingResponseBody> proxyDirect(String url) {
        StreamingResponseBody body = outputStream -> {
            try (InputStream is = new java.net.URI(url).toURL().openStream()) {
                is.transferTo(outputStream);
            } catch (Exception ex) {

            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(body);
    }
}