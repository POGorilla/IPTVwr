package com.proiect.iptv.controller;

import com.proiect.iptv.dto.IptvOrgChannel;
import com.proiect.iptv.dto.IptvOrgCountry;
import com.proiect.iptv.dto.IptvOrgStream;
import com.proiect.iptv.dto.WatchInfo;
import com.proiect.iptv.service.GeoLocationService;
import com.proiect.iptv.service.IptvOrgService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@Controller
public class BrowseController {
    private final GeoLocationService geoLocationService;
    private final IptvOrgService iptvOrgService;

    public BrowseController(GeoLocationService geoLocationService, IptvOrgService iptvOrgService) {
        this.geoLocationService = geoLocationService;
        this.iptvOrgService = iptvOrgService;
    }

    @GetMapping("/browse")
    public String browser(HttpServletRequest request, Model model) {
        String countryCode = geoLocationService.detectCountryCode(request);

        List<IptvOrgCountry> sorted = new ArrayList<>(iptvOrgService.getCountries());
        sorted.sort((a, b) -> {
            if (a.getCode().equals(countryCode)) return -1;
            if (b.getCode().equals(countryCode)) return 1;
            return a.getName().compareTo(b.getName());
        });

        model.addAttribute("countries", sorted);
        model.addAttribute("countryCode", countryCode);

        return "browse";
    }

    @GetMapping("/browse/{code}")
    public String byCountry(@PathVariable String code,
                            @RequestParam(required = false) String q,
                            Model model) {
        model.addAttribute("categories", iptvOrgService.getCategories());
        model.addAttribute("code", code);
        model.addAttribute("q", q);

        if (q != null && !q.isEmpty()) {
            List<IptvOrgChannel> results = iptvOrgService.getChannels().stream()
                    .filter(c -> code.equals(c.getCountry()))
                    .filter(c -> c.getName() != null && c.getName().toLowerCase().contains(q.toLowerCase()))
                    .toList();
            model.addAttribute("searchResults", results);
        }

        return "browse-country";
    }

    @GetMapping("/browse/{code}/{categoryId}")
    public String byCategory(@PathVariable String code, @PathVariable String categoryId, Model model) {
        List<IptvOrgChannel> filtered = iptvOrgService.getChannels().stream()
                .filter(c -> c.getCountry().equals(code))
                .filter(c -> c.getCategories().contains(categoryId))
                .toList();

        model.addAttribute("channels", filtered);
        model.addAttribute("code", code);
        model.addAttribute("categoryId", categoryId);

        return "browse-channels";
    }

    @GetMapping("/browse/watch/{channelId}")
    public String watchBrowse(@PathVariable String channelId, Model model) {
       IptvOrgChannel channel = iptvOrgService.getChannels().stream()
               .filter(c -> channelId.equals(c.getId()))
               .findFirst()
               .orElse(null);

       IptvOrgStream stream = iptvOrgService.getStreams().stream()
               .filter(s -> channelId.equals(s.getChannel()))
               .findFirst()
               .orElse(null);

       if (channel == null || stream == null) {
           return  "redirect:/browse";
       }

        WatchInfo info = new WatchInfo();
        info.setName(channel.getName());
        info.setGroupTitle(channel.getCategories().isEmpty() ? "" : channel.getCategories().getFirst());
        info.setStreamUrl(stream.getUrl());

        model.addAttribute("channel", info);
        return "watch";
    }
}
