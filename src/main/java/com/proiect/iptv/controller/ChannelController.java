package com.proiect.iptv.controller;

import com.proiect.iptv.entity.Channel;
import com.proiect.iptv.entity.Playlist;
import com.proiect.iptv.entity.User;
import com.proiect.iptv.repository.ChannelRepository;
import com.proiect.iptv.repository.PlaylistRepository;
import com.proiect.iptv.repository.UserRepository;
import com.proiect.iptv.service.M3UParserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
public class ChannelController {

    private final M3UParserService m3uParserService;
    private final ChannelRepository channelRepository;
    private final PlaylistRepository playlistRepository;
    private final UserRepository userRepository;

    public ChannelController(M3UParserService m3uParserService,
                             ChannelRepository channelRepository,
                             PlaylistRepository playlistRepository,
                             UserRepository userRepository) {
        this.m3uParserService = m3uParserService;
        this.channelRepository = channelRepository;
        this.playlistRepository = playlistRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/upload")
    public String uploadPage() {
        return "upload";
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file,
                             @RequestParam("playlistName") String playlistName,
                             Principal principal,
                             RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findByUsername(principal.getName()).orElseThrow();

            Playlist playlist = new Playlist();
            playlist.setName(playlistName != null && !playlistName.isBlank()
                    ? playlistName : "Untitled");
            playlist.setUser(user);
            playlistRepository.save(playlist);

            List<Channel> channels = m3uParserService.parse(file, playlist);
            redirectAttributes.addFlashAttribute("message",
                    channels.size() + " channels imported into \"" + playlist.getName() + "\"!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Parsing error: " + e.getMessage());
        }
        return "redirect:/playlists";
    }

    @GetMapping("/channels")
    public String channelsRedirect() {
        return "redirect:/playlists";
    }

    @GetMapping("/watch/{id}")
    public String watchChannel(@PathVariable Long id, Principal principal, Model model) {
        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Channel doesn't exist!"));

        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        if (!channel.getPlaylist().getUser().getId().equals(user.getId())) {
            return "redirect:/playlists";
        }

        model.addAttribute("channel", channel);
        return "watch";
    }
}