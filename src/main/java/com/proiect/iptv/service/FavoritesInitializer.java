package com.proiect.iptv.service;

import com.proiect.iptv.entity.Playlist;
import com.proiect.iptv.entity.User;
import com.proiect.iptv.repository.PlaylistRepository;
import com.proiect.iptv.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class FavoritesInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PlaylistRepository playlistRepository;

    public FavoritesInitializer(UserRepository userRepository,
                                PlaylistRepository playlistRepository) {
        this.userRepository = userRepository;
        this.playlistRepository = playlistRepository;
    }

    @Override
    public void run(String... args) {
        for (User user : userRepository.findAll()) {
            if (playlistRepository.findByUserAndLockedTrue(user).isEmpty()) {
                Playlist favorites = new Playlist();
                favorites.setName("Favorites");
                favorites.setLocked(true);
                favorites.setUser(user);
                playlistRepository.save(favorites);
            }
        }
    }
}