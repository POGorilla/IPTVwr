package com.proiect.iptv.repository;

import com.proiect.iptv.entity.Channel;
import com.proiect.iptv.entity.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChannelRepository extends JpaRepository<Channel, Long> {
    List<Channel> findByPlaylist(Playlist playlist);

    Optional<Channel> findByPlaylistAndNameAndGroupTitleAndCountry(
            Playlist playlist, String name, String groupTitle, String country);

    boolean existsByPlaylistAndNameAndGroupTitleAndCountry(
            Playlist playlist, String name, String groupTitle, String country);
}