package com.loveconnect.mongoapp.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {
    @GetMapping("/")
    public Map<String, String> root() {
        return Map.of(
            "application", "LoveConnect Mongo Backend",
            "status", "running",
            "health", "/api/health"
        );
    }
}
