package com.proiect.iptv.controller;

import com.proiect.iptv.entity.Channel;
import com.proiect.iptv.entity.Playlist;
import com.proiect.iptv.entity.User;
import com.proiect.iptv.repository.ChannelRepository;
import com.proiect.iptv.repository.PlaylistRepository;
import com.proiect.iptv.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.Objects;
import java.util.Optional;

@Controller
public class FavoritesController {
    private final UserRepository userRepository;
    private final PlaylistRepository playlistRepository;
    private final ChannelRepository channelRepository;

    public FavoritesController(UserRepository userRepository,
                               PlaylistRepository playlistRepository,
                               ChannelRepository channelRepository) {
        this.userRepository = userRepository;
        this.playlistRepository = playlistRepository;
        this.channelRepository = channelRepository;
    }

    @PostMapping("/favorites/toggle")
    public String toggle(@RequestParam String name,
                         @RequestParam(required = false) String category,
                         @RequestParam(required = false) String country,
                         @RequestParam String streamUrl,
                         @RequestHeader(value = "Referer", required = false) String referer,
                         Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Playlist favorites = playlistRepository.findByUserAndLockedTrue(user).orElseThrow();

        Optional<Channel> existing = channelRepository.findByPlaylist(favorites).stream()
                .filter(c -> name.equals(c.getName())
                        && Objects.equals(c.getGroupTitle(), category)
                        && Objects.equals(c.getCountry(), country))
                .findFirst();

        if (existing.isPresent()) {
            channelRepository.delete(existing.get());
        } else {
            Channel channel = new Channel();
            channel.setName(name);
            channel.setGroupTitle(category);
            channel.setCountry(country);
            channel.setStreamUrl(streamUrl);
            channel.setPlaylist(favorites);
            channelRepository.save(channel);
        }

        return "redirect:" + (referer != null ? referer : "/playlists");
    }
}