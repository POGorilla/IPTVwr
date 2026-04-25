package com.proiect.iptv.service;

import com.proiect.iptv.dto.IptvOrgCategory;
import com.proiect.iptv.dto.IptvOrgChannel;
import com.proiect.iptv.dto.IptvOrgCountry;
import com.proiect.iptv.dto.IptvOrgStream;
import jakarta.annotation.PostConstruct;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Set;

@Service
public class IptvOrgService {
    private static final String BASE_URL = "https://iptv-org.github.io/api";
    private static final Set<String> BLOCKED_CATEGORIES = Set.of("xxx");

    private final RestTemplate restTemplate;

    private List<IptvOrgChannel> channels;
    private List<IptvOrgCountry> countries;
    private List<IptvOrgStream> streams;
    private List<IptvOrgCategory> categories;

    public IptvOrgService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    public void init() {
        this.channels = filterChannels(fetchChannels());
        this.countries = fetchCountries();
        this.streams = fetchStreams();
        this.categories = filterCategories(fetchCategories());
    }

    private List<IptvOrgCategory> filterCategories(List<IptvOrgCategory> all) {
        return all.stream()
                .filter(c -> !BLOCKED_CATEGORIES.contains(c.getId()))
                .toList();
    }

    private List<IptvOrgChannel> filterChannels(List<IptvOrgChannel> all) {
        return all.stream()
                .filter(c -> c.getCategories() == null
                        || c.getCategories().stream().noneMatch(BLOCKED_CATEGORIES::contains))
                .toList();
    }

    private List<IptvOrgCountry> fetchCountries() {
        return restTemplate.exchange(BASE_URL + "/countries.json", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<IptvOrgCountry>>() {}).getBody();
    }

    private List<IptvOrgCategory> fetchCategories() {
        return restTemplate.exchange(BASE_URL + "/categories.json", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<IptvOrgCategory>>() {}).getBody();
    }

    private List<IptvOrgChannel> fetchChannels() {
        return restTemplate.exchange(BASE_URL + "/channels.json", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<IptvOrgChannel>>() {}).getBody();
    }

    private List<IptvOrgStream> fetchStreams() {
        return restTemplate.exchange(BASE_URL + "/streams.json", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<IptvOrgStream>>() {}).getBody();
    }

    public List<IptvOrgChannel> getChannels() { return channels; }
    public List<IptvOrgCountry> getCountries() { return countries; }
    public List<IptvOrgStream> getStreams() { return streams; }
    public List<IptvOrgCategory> getCategories() { return categories; }
}