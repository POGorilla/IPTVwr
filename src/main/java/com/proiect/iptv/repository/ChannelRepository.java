package com.proiect.iptv.repository;

import com.proiect.iptv.entity.Channel;
import com.proiect.iptv.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChannelRepository extends JpaRepository<Channel, Long> {
    Optional<Channel> findByName(String name);
    List<Channel> findByUser(User user);
}
