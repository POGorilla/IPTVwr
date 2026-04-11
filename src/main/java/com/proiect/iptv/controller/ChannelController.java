package com.proiect.iptv.controller;

import com.proiect.iptv.entity.Channel;
import com.proiect.iptv.entity.User;
import com.proiect.iptv.repository.ChannelRepository;
import com.proiect.iptv.repository.UserRepository;
import com.proiect.iptv.service.M3UParserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
public class ChannelController {

    private final M3UParserService m3uParserService;
    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;

    public ChannelController(M3UParserService m3uParserService,
                             ChannelRepository channelRepository,
                             UserRepository userRepository) {
        this.m3uParserService = m3uParserService;
        this.channelRepository = channelRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/upload")
    public String uploadPage() {
        return "upload";
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file,
                             Principal principal,
                             RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findByUsername(principal.getName())
                    .orElseThrow();
            List<Channel> channels = m3uParserService.parse(file, user);
            redirectAttributes.addFlashAttribute("message",
                    channels.size() + " canale importate cu succes!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Eroare la parsare: " + e.getMessage());
        }
        return "redirect:/channels";
    }

    @GetMapping("/channels")
    public String channelsPage(Principal principal, Model model) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow();
        List<Channel> channels = channelRepository.findByUser(user);
        model.addAttribute("channels", channels);
        return "channels";
    }

    @GetMapping("/watch/{id}")
    public String watchChannel(@PathVariable Long id, Principal principal, Model model) {
        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Canal inexistent"));

        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        if (!channel.getUser().getId().equals(user.getId())) {
            return "redirect:/channels";
        }

        model.addAttribute("channel", channel);
        return "watch";
    }
}