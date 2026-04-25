package com.proiect.iptv.service;

import com.proiect.iptv.entity.Channel;
import com.proiect.iptv.entity.User;
import com.proiect.iptv.repository.ChannelRepository;
import com.proiect.iptv.repository.PlaylistRepository;
import com.proiect.iptv.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FavoritesService {
    private final UserRepository userRepository;
    private final PlaylistRepository playlistRepository;
    private final ChannelRepository channelRepository;

    public FavoritesService(UserRepository userRepository,
                            PlaylistRepository playlistRepository,
                            ChannelRepository channelRepository) {
        this.userRepository = userRepository;
        this.playlistRepository = playlistRepository;
        this.channelRepository = channelRepository;
    }

    public Set<String> getFavoriteKeys(Principal principal) {
        if (principal == null) return Set.of();
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        if (user == null) return Set.of();
        return getFavoriteKeys(user);
    }

    public Set<String> getFavoriteKeys(User user) {
        return playlistRepository.findByUserAndLockedTrue(user)
                .map(channelRepository::findByPlaylist)
                .orElse(List.of())
                .stream()
                .map(this::keyFor)
                .collect(Collectors.toSet());
    }

    public String keyFor(Channel c) {
        return keyFor(c.getName(), c.getGroupTitle(), c.getCountry());
    }

    public String keyFor(String name, String category, String country) {
        return name + "|" + nullSafe(category) + "|" + nullSafe(country);
    }

    private static String nullSafe(String s) { return s == null ? "" : s; }
}