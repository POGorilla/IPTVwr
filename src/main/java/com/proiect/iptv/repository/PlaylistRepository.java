package com.proiect.iptv.repository;

import com.proiect.iptv.entity.Playlist;
import com.proiect.iptv.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {
    List<Playlist> findByUser(User user);
    Optional<Playlist> findByUserAndLockedTrue(User user);
}