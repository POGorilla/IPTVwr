package com.proiect.iptv.controller;

import com.proiect.iptv.dto.IptvOrgChannel;
import com.proiect.iptv.dto.IptvOrgCountry;
import com.proiect.iptv.dto.IptvOrgStream;
import com.proiect.iptv.dto.WatchInfo;
import com.proiect.iptv.service.FavoritesService;
import com.proiect.iptv.service.GeoLocationService;
import com.proiect.iptv.service.IptvOrgService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class BrowseController {

    private static final Logger log = LoggerFactory.getLogger(BrowseController.class);
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 15000;
    private static final int MAX_PLAYLIST_BYTES = 2_000_000;

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
        if (!isSafeUrl(url)) {
            log.warn("Blocked unsafe stream URL: {}", url);
            return ResponseEntity.badRequest().build();
        }
        if (url.endsWith(".m3u8")) {
            return proxyPlaylist(url);
        }
        return proxyDirect(url);
    }

    private boolean isSafeUrl(String url) {
        if (url == null || url.length() > 2048) return false;
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (scheme == null
                    || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                return false;
            }
            String host = uri.getHost();
            if (host == null || host.isBlank()) return false;

            for (InetAddress addr : InetAddress.getAllByName(host)) {
                if (addr.isLoopbackAddress()
                        || addr.isLinkLocalAddress()
                        || addr.isSiteLocalAddress()
                        || addr.isAnyLocalAddress()
                        || addr.isMulticastAddress()) {
                    return false;
                }
            }
            return true;
        } catch (URISyntaxException | UnknownHostException e) {
            return false;
        }
    }

    private ResponseEntity<StreamingResponseBody> proxyPlaylist(String url) {
        StreamingResponseBody body = outputStream -> {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URI(url).toURL().openConnection();
                conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                conn.setReadTimeout(READ_TIMEOUT_MS);
                conn.setInstanceFollowRedirects(true);
                try (InputStream is = conn.getInputStream()) {
                    String playlist = new String(is.readNBytes(MAX_PLAYLIST_BYTES), StandardCharsets.UTF_8);
                    String baseUrl = url.substring(0, url.lastIndexOf('/') + 1);

                    String rewritten = playlist.lines().map(line -> {
                        if (!line.startsWith("#") && !line.isEmpty()) {
                            String segmentUrl = line.startsWith("http") ? line : baseUrl + line;
                            return "/stream?url=" + URLEncoder.encode(segmentUrl, StandardCharsets.UTF_8);
                        }
                        return line;
                    }).collect(Collectors.joining("\n"));

                    outputStream.write(rewritten.getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception ex) {
                log.warn("Playlist proxy failed for {}: {}", url, ex.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"))
                .body(body);
    }

    private ResponseEntity<StreamingResponseBody> proxyDirect(String url) {
        StreamingResponseBody body = outputStream -> {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URI(url).toURL().openConnection();
                conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                conn.setReadTimeout(READ_TIMEOUT_MS);
                conn.setInstanceFollowRedirects(true);
                try (InputStream is = conn.getInputStream()) {
                    is.transferTo(outputStream);
                }
            } catch (Exception ex) {
                log.warn("Direct proxy failed for {}: {}", url, ex.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(body);
    }
}