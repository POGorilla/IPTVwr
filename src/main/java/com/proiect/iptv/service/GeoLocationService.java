package com.proiect.iptv.service;

import com.proiect.iptv.dto.GeoResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GeoLocationService {
    private final RestTemplate restTemplate;

    public GeoLocationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String detectCountryCode(HttpServletRequest request) {
        String ip = request.getRemoteAddr();

        String url;
        if (ip.equals("127.0.0.1") ||  ip.equals("0:0:0:0:0:0:0:1")) {
            url = "http://ip-api.com/json/";
        } else {
            url = "http://ip-api.com/json/" + ip;
        }

        GeoResponse response = restTemplate.getForObject(url, GeoResponse.class);
        return (response != null) ? response.getCountryCode() : null;
    }
}
