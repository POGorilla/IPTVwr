package com.proiect.iptv.controller;

import com.proiect.iptv.dto.IptvOrgChannel;
import com.proiect.iptv.dto.IptvOrgCountry;
import com.proiect.iptv.dto.IptvOrgStream;
import com.proiect.iptv.dto.WatchInfo;
import com.proiect.iptv.service.FavoritesService;
import com.proiect.iptv.service.GeoLocationService;
import com.proiect.iptv.service.IptvOrgService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class BrowseController {
    private final GeoLocationService geoLocationService;
    private final IptvOrgService iptvOrgService;
    private final FavoritesService favoritesService;

    public BrowseController(GeoLocationService geoLocationService,
                            IptvOrgService iptvOrgService,
                            FavoritesService favoritesService) {
        this.geoLocationService = geoLocationService;
        this.iptvOrgService = iptvOrgService;
        this.favoritesService = favoritesService;
    }

    private Set<String> getAvailableStreamIds() {
        return iptvOrgService.getStreams().stream()
                .filter(s -> s.getChannel() != null)
                .map(IptvOrgStream::getChannel)
                .collect(Collectors.toSet());
    }

    private Map<String, String> getStreamUrlMap() {
        return iptvOrgService.getStreams().stream()
                .filter(s -> s.getChannel() != null)
                .collect(Collectors.toMap(
                        IptvOrgStream::getChannel,
                        IptvOrgStream::getUrl,
                        (a, b) -> a));
    }

    @GetMapping("/browse")
    public String browse(HttpServletRequest request, Model model) {
        String countryCode = geoLocationService.detectCountryCode(request);

        List<IptvOrgCountry> sorted = new ArrayList<>(iptvOrgService.getCountries());
        sorted.sort((a, b) -> {
            if (Objects.equals(a.getCode(), countryCode)) return -1;
            if (Objects.equals(b.getCode(), countryCode)) return 1;
            return a.getName().compareTo(b.getName());
        });

        model.addAttribute("countries", sorted);
        model.addAttribute("countryCode", countryCode);
        return "browse";
    }

    @GetMapping("/browse/{code}")
    public String byCountry(@PathVariable String code,
                            @RequestParam(required = false) String q,
                            Principal principal,
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
            model.addAttribute("streamUrlMap", getStreamUrlMap());
            model.addAttribute("favoriteKeys", favoritesService.getFavoriteKeys(principal));
        }

        return "browse-country";
    }

    @GetMapping("/browse/{code}/{categoryId}")
    public String byCategory(@PathVariable String code,
                             @PathVariable String categoryId,
                             Principal principal,
                             Model model) {
        List<IptvOrgChannel> filtered = iptvOrgService.getChannels().stream()
                .filter(c -> code.equals(c.getCountry()))
                .filter(c -> c.getCategories() != null && c.getCategories().contains(categoryId))
                .toList();

        model.addAttribute("channels", filtered);
        model.addAttribute("code", code);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("availableStreams", getAvailableStreamIds());
        model.addAttribute("streamUrlMap", getStreamUrlMap());
        model.addAttribute("favoriteKeys", favoritesService.getFavoriteKeys(principal));

        return "browse-channels";
    }

    @GetMapping("/browse/watch/{channelId}")
    public String watchBrowse(@PathVariable String channelId, Principal principal, Model model) {
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

        String category = (channel.getCategories() != null && !channel.getCategories().isEmpty())
                ? channel.getCategories().getFirst() : "";

        WatchInfo info = new WatchInfo();
        info.setName(channel.getName());
        info.setGroupTitle(category);
        info.setCountry(channel.getCountry());
        info.setStreamUrl(streamUrls.getFirst());

        Set<String> favKeys = favoritesService.getFavoriteKeys(principal);
        String key = favoritesService.keyFor(channel.getName(), category, channel.getCountry());

        model.addAttribute("channel", info);
        model.addAttribute("streamUrls", streamUrls);
        model.addAttribute("isFavorited", favKeys.contains(key));
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
                System.err.println("Playlist proxy failed: " + ex.getMessage());
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
                System.err.println("Direct proxy failed: " + ex.getMessage());
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(body);
    }
}