package com.proiect.iptv.service;

import com.proiect.iptv.entity.Channel;
import com.proiect.iptv.entity.User;
import com.proiect.iptv.repository.ChannelRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class M3UParserService {
    private final ChannelRepository channelRepository;

    public M3UParserService(ChannelRepository channelRepository) {
        this.channelRepository = channelRepository;
    }

    public List<Channel> parse(MultipartFile file, User user) throws IOException {
        List<Channel> channels = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
        String line;
        String name = null;
        String group = null;

        while ((line = reader.readLine()) != null) {
            if (line.startsWith("#EXTINF:")) {
                name = line.substring(line.lastIndexOf(",") + 1).trim();

                if (line.contains("group-title=\"")) {
                    int start = line.indexOf("group-title=\"") + 13;
                    int end = line.indexOf("\"", start);
                    group = line.substring(start, end);
                }
            } else if (line.startsWith("http")) {
                Channel channel = new Channel();
                channel.setName(name != null ? name : "Unknown");
                channel.setGroupTitle(group != null ? group : "Uncategorized");
                channel.setStreamUrl(line.trim());
                channel.setUser(user);
                channels.add(channel);
                name = null;
                group = null;
            }
        }

        channelRepository.saveAll(channels);
        return channels;
    }
}
