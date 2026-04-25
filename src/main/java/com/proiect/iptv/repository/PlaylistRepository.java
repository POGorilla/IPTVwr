package com.proiect.iptv.repository;

import com.proiect.iptv.entity.Playlist;
import com.proiect.iptv.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {
    List<Playlist> findByUser(User user);
}