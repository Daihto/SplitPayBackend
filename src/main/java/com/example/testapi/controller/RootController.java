package com.example.testapi.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, Object> root() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "SplitPay backend is running");
        response.put("timestamp", LocalDateTime.now());
        return response;
    }
}
