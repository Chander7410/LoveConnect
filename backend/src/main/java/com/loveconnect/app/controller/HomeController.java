package com.loveconnect.app.controller;

import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {
    @GetMapping("/")
    public Map<String, Object> home() {
        Map<String, Object> response = new HashMap<>();
        response.put("application", "LoveConnect API");
        response.put("status", "running");
        response.put("frontend", "http://127.0.0.1:5173");
        response.put("swagger", "http://localhost:8080/swagger-ui.html");
        response.put("apiDocs", "http://localhost:8080/v3/api-docs");
        return response;
    }
}
