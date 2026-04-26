package com.proiect.iptv.controller;

import com.proiect.iptv.entity.Channel;
import com.proiect.iptv.entity.Playlist;
import com.proiect.iptv.entity.User;
import com.proiect.iptv.repository.ChannelRepository;
import com.proiect.iptv.repository.PlaylistRepository;
import com.proiect.iptv.repository.UserRepository;
import com.proiect.iptv.service.FavoritesService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
@RequestMapping("/playlists")
public class PlaylistController {

    private final PlaylistRepository playlistRepository;
    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;
    private final FavoritesService favoritesService;

    public PlaylistController(PlaylistRepository playlistRepository,
                              ChannelRepository channelRepository,
                              UserRepository userRepository,
                              FavoritesService favoritesService) {
        this.playlistRepository = playlistRepository;
        this.channelRepository = channelRepository;
        this.userRepository = userRepository;
        this.favoritesService = favoritesService;
    }

    @GetMapping
    public String list(Principal principal, Model model) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        model.addAttribute("playlists", playlistRepository.findByUser(user));
        return "playlists";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Principal principal, Model model) {
        Playlist playlist = playlistRepository.findById(id).orElseThrow();
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        if (!playlist.getUser().getId().equals(user.getId())) {
            return "redirect:/playlists";
        }
        model.addAttribute("playlist", playlist);
        model.addAttribute("channels", channelRepository.findByPlaylist(playlist));
        model.addAttribute("favoriteKeys", favoritesService.getFavoriteKeys(user));
        return "playlist-view";
    }

    @PostMapping("/new")
    public String createEmpty(@RequestParam String name, Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Playlist playlist = new Playlist();
        playlist.setName(sanitizeName(name));
        playlist.setUser(user);
        playlistRepository.save(playlist);
        return "redirect:/playlists/" + playlist.getId();
    }

    @PostMapping("/{id}/channels")
    public String addChannel(@PathVariable Long id,
                             @RequestParam String name,
                             @RequestParam(required = false) String category,
                             @RequestParam(required = false) String country,
                             @RequestParam String streamUrl,
                             Principal principal) {
        Playlist playlist = playlistRepository.findById(id).orElseThrow();
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        if (!playlist.getUser().getId().equals(user.getId())) {
            return "redirect:/playlists";
        }

        Channel channel = new Channel();
        channel.setName(name);
        channel.setGroupTitle(category);
        channel.setCountry(country);
        channel.setStreamUrl(streamUrl);
        channel.setPlaylist(playlist);
        channelRepository.save(channel);
        return "redirect:/playlists/" + id;
    }

    @PostMapping("/{id}/channels/{channelId}/delete")
    public String removeChannel(@PathVariable Long id,
                                @PathVariable Long channelId,
                                Principal principal) {
        Playlist playlist = playlistRepository.findById(id).orElseThrow();
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        if (!playlist.getUser().getId().equals(user.getId())) {
            return "redirect:/playlists";
        }

        Channel channel = channelRepository.findById(channelId).orElseThrow();
        if (!channel.getPlaylist().getId().equals(id)) {
            return "redirect:/playlists/" + id;
        }
        channelRepository.delete(channel);
        return "redirect:/playlists/" + id;
    }

    @PostMapping("/{id}/rename")
    public String rename(@PathVariable Long id, @RequestParam String name, Principal principal) {
        Playlist playlist = playlistRepository.findById(id).orElseThrow();
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        if (!playlist.getUser().getId().equals(user.getId()) || playlist.isLocked()) {
            return "redirect:/playlists";
        }
        playlist.setName(sanitizeName(name));
        playlistRepository.save(playlist);
        return "redirect:/playlists";
    }

    @PostMapping("/{id}/delete")
    @Transactional
    public String delete(@PathVariable Long id, Principal principal) {
        Playlist playlist = playlistRepository.findById(id).orElseThrow();
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        if (!playlist.getUser().getId().equals(user.getId()) || playlist.isLocked()) {
            return "redirect:/playlists";
        }
        channelRepository.deleteAll(channelRepository.findByPlaylist(playlist));
        playlistRepository.delete(playlist);
        return "redirect:/playlists";
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<String> export(@PathVariable Long id, Principal principal) {
        Playlist playlist = playlistRepository.findById(id).orElseThrow();
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        if (!playlist.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        StringBuilder sb = new StringBuilder("#EXTM3U\n");
        for (Channel c : channelRepository.findByPlaylist(playlist)) {
            String group = sanitizeAttribute(c.getGroupTitle());
            String name = sanitizeLine(c.getName());
            String url = sanitizeLine(c.getStreamUrl());
            if (url.isBlank()) continue;
            sb.append("#EXTINF:-1 group-title=\"").append(group).append("\",").append(name).append("\n");
            sb.append(url).append("\n");
        }

        String filename = playlist.getName().replaceAll("[^a-zA-Z0-9._-]", "_") + ".m3u";

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .header("Content-Type", "audio/x-mpegurl")
                .body(sb.toString());
    }

    private String sanitizeName(String name) {
        if (name == null || name.isBlank()) return "Untitled";
        String cleaned = name.replaceAll("[\\r\\n]", " ").trim();
        return cleaned.length() > 100 ? cleaned.substring(0, 100) : cleaned;
    }

    private String sanitizeAttribute(String s) {
        if (s == null) return "";
        return s.replaceAll("[\\r\\n\"]", "").trim();
    }

    private String sanitizeLine(String s) {
        if (s == null) return "";
        return s.replaceAll("[\\r\\n]", "").trim();
    }
}