package com.example.testapi.controller;

import com.example.testapi.model.LoginRequest;
import com.example.testapi.model.User;
import com.example.testapi.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000") // allow React frontend
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public String login(@RequestBody LoginRequest request) {
        boolean success = authService.login(request.getUsername(), request.getPassword());
        return success ? "Success" : "Invalid username or password";
    }

    @PostMapping("/register")
    public String register(@RequestBody LoginRequest request) {
        User user = new User(request.getUsername(), request.getPassword());
        return authService.register(user);
    }
}