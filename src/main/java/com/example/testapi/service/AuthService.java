package com.example.testapi.service;

import com.example.testapi.dto.AuthResponse;
import com.example.testapi.dto.LoginRequest;
import com.example.testapi.dto.RegisterRequest;
import com.example.testapi.dto.UserSummaryDto;
import com.example.testapi.entity.User;
import com.example.testapi.exception.BadRequestException;
import com.example.testapi.repository.UserRepository;
import com.example.testapi.security.CustomUserDetails;
import com.example.testapi.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserSummaryDto register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        userRepository.save(user);

        return new UserSummaryDto(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        try {
            System.out.println("LOGIN START: " + request.getEmail());

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            System.out.println("AUTH SUCCESS");

            CustomUserDetails userDetails =
                    (CustomUserDetails) authentication.getPrincipal();

            User user = userRepository.findById(userDetails.getId())
                    .orElseThrow(() ->
                            new BadRequestException("Invalid email or password")
                    );

            System.out.println("USER FETCHED");

            String token = jwtService.generateToken(userDetails);

            System.out.println("TOKEN GENERATED");

            UserSummaryDto userDto = new UserSummaryDto(
                    user.getId(),
                    user.getName(),
                    user.getEmail()
            );

            System.out.println("LOGIN END");

            return new AuthResponse(token, userDto);

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new BadRequestException("Invalid email or password");
        }
    }
}