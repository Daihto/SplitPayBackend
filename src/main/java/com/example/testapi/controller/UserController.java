package com.example.testapi.controller;

import com.example.testapi.dto.AvatarUploadResponse;
import com.example.testapi.dto.BalanceResponse;
import com.example.testapi.dto.ChangePasswordRequest;
import com.example.testapi.dto.UpdateUserProfileRequest;
import com.example.testapi.dto.UserProfileResponse;
import com.example.testapi.security.CustomUserDetails;
import com.example.testapi.service.ExpenseService;
import com.example.testapi.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final ExpenseService expenseService;
    private final UserService userService;

    public UserController(ExpenseService expenseService, UserService userService) {
        this.expenseService = expenseService;
        this.userService = userService;
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserProfileResponse> updateProfile(@PathVariable Long id,
                                                             @Valid @RequestBody UpdateUserProfileRequest request,
                                                             Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return ResponseEntity.ok(userService.updateProfile(id, userDetails.getId(), request));
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<Map<String, String>> changePassword(@PathVariable Long id,
                                                               @Valid @RequestBody ChangePasswordRequest request,
                                                               Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        userService.changePassword(id, userDetails.getId(), request);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

    @PostMapping("/{id}/avatar")
    public ResponseEntity<AvatarUploadResponse> uploadAvatar(@PathVariable Long id,
                                                              @RequestParam("avatar") MultipartFile avatar,
                                                              Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return ResponseEntity.ok(userService.uploadAvatar(id, userDetails.getId(), avatar));
    }

    @GetMapping("/{id}/balances")
    public ResponseEntity<List<BalanceResponse>> getUserBalances(@PathVariable Long id, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return ResponseEntity.ok(expenseService.getBalancesForUser(id, userDetails.getId()));
    }
}
