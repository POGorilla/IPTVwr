package com.proiect.iptv.service;

import com.proiect.iptv.dto.GeoResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class GeoLocationService {

    private static final Logger log = LoggerFactory.getLogger(GeoLocationService.class);

    private final RestTemplate restTemplate;

    public GeoLocationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String detectCountryCode(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        boolean isLocal = ip == null || ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1");
        String url = isLocal ? "http://ip-api.com/json/" : "http://ip-api.com/json/" + ip;

        try {
            GeoResponse response = restTemplate.getForObject(url, GeoResponse.class);
            return response != null ? response.getCountryCode() : null;
        } catch (RestClientException e) {
            log.warn("Geo lookup failed for {}: {}", ip, e.getMessage());
            return null;
        }
    }
}