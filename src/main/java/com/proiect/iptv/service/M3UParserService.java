package com.proiect.iptv.service;

import com.proiect.iptv.entity.Channel;
import com.proiect.iptv.entity.Playlist;
import com.proiect.iptv.repository.ChannelRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class M3UParserService {

    private static final Pattern GROUP_TITLE = Pattern.compile("group-title=\"([^\"]*)\"");
    private static final Pattern TVG_COUNTRY = Pattern.compile("tvg-country=\"([^\"]*)\"");
    private static final int MAX_CHANNELS_PER_UPLOAD = 10000;

    private final ChannelRepository channelRepository;

    public M3UParserService(ChannelRepository channelRepository) {
        this.channelRepository = channelRepository;
    }

    public List<Channel> parse(MultipartFile file, Playlist playlist) throws IOException {
        List<Channel> channels = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        String name = null;
        String group = null;
        String country = null;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null && channels.size() < MAX_CHANNELS_PER_UPLOAD) {
                line = line.trim();
                if (line.startsWith("#EXTINF:")) {
                    int comma = line.lastIndexOf(',');
                    name = comma >= 0 ? line.substring(comma + 1).trim() : "Unknown";

                    Matcher gm = GROUP_TITLE.matcher(line);
                    group = gm.find() ? gm.group(1) : null;

                    Matcher cm = TVG_COUNTRY.matcher(line);
                    country = cm.find() ? cm.group(1) : null;
                } else if (line.startsWith("http://") || line.startsWith("https://")) {
                    String key = name + "|" + (group == null ? "" : group) + "|" + (country == null ? "" : country);
                    if (seen.add(key)) {
                        Channel channel = new Channel();
                        channel.setName(name != null ? name : "Unknown");
                        channel.setGroupTitle(group != null ? group : "Uncategorized");
                        channel.setCountry(country);
                        channel.setStreamUrl(line);
                        channel.setPlaylist(playlist);
                        channels.add(channel);
                    }
                    name = null;
                    group = null;
                    country = null;
                }
            }
        }

        return channelRepository.saveAll(channels);
    }
}