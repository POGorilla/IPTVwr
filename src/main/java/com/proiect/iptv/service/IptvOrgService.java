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

@Service
public class IptvOrgService {
    private static final String BASE_URL = "https://iptv-org.github.io/api";
    private final RestTemplate restTemplate;

    private List<IptvOrgChannel> channels;
    private List<IptvOrgCountry> countries;
    private List<IptvOrgStream> streams;
    private List<IptvOrgCategory> categories;

    @PostConstruct
    public void init() {
        this.channels = fetchChannels();
        this.countries = fetchCountries();
        this.streams = fetchStreams();
        this.categories = fetchCategories();
    }

    public IptvOrgService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private List<IptvOrgCountry> fetchCountries() {
        String url = BASE_URL + "/countries.json";

        return restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<List<IptvOrgCountry>>() {})
                .getBody();
    }

    private List<IptvOrgCategory> fetchCategories() {
        String url = BASE_URL + "/categories.json";

        return restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<List<IptvOrgCategory>>() {})
                .getBody();
    }

    private List<IptvOrgChannel> fetchChannels() {
        String url = BASE_URL + "/channels.json";

        return restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<List<IptvOrgChannel>>() {})
                .getBody();
    }

    private List<IptvOrgStream> fetchStreams() {
        String url = BASE_URL + "/streams.json";

        return restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<List<IptvOrgStream>>() {})
                .getBody();
    }

    public List<IptvOrgChannel> getChannels() {
        return channels;
    }

    public List<IptvOrgCountry> getCountries() {
        return countries;
    }

    public List<IptvOrgStream> getStreams() {
        return streams;
    }

    public List<IptvOrgCategory> getCategories() {
        return categories;
    }
}