package com.example.testapi.service;

import com.example.testapi.dto.AvatarUploadResponse;
import com.example.testapi.dto.ChangePasswordRequest;
import com.example.testapi.dto.UpdateUserProfileRequest;
import com.example.testapi.dto.UserProfileResponse;
import com.example.testapi.entity.User;
import com.example.testapi.exception.BadRequestException;
import com.example.testapi.exception.ResourceNotFoundException;
import com.example.testapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SupabaseProfileService supabaseProfileService;
    private final long avatarMaxSizeBytes;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       SupabaseProfileService supabaseProfileService,
                       @Value("${app.avatar.max-size-bytes:5242880}") long avatarMaxSizeBytes) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.supabaseProfileService = supabaseProfileService;
        this.avatarMaxSizeBytes = avatarMaxSizeBytes;
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, Long currentUserId, UpdateUserProfileRequest request) {
        ensureSelf(userId, currentUserId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        userRepository.findByEmail(request.getEmail())
                .filter(existingUser -> !existingUser.getId().equals(userId))
                .ifPresent(existingUser -> {
                    throw new BadRequestException("Email already exists");
                });

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setAvatarUrl(request.getAvatarUrl());

        User saved = userRepository.save(user);
        supabaseProfileService.syncUserProfile(saved);

        return toProfileResponse(saved);
    }

    @Transactional
    public void changePassword(Long userId, Long currentUserId, ChangePasswordRequest request) {
        ensureSelf(userId, currentUserId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        validateNewPassword(request.getNewPassword());

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public AvatarUploadResponse uploadAvatar(Long userId, Long currentUserId, MultipartFile file) {
        ensureSelf(userId, currentUserId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        validateAvatarFile(file);

        String avatarUrl = supabaseProfileService.uploadAvatar(userId, file);
        user.setAvatarUrl(avatarUrl);

        User saved = userRepository.save(user);
        supabaseProfileService.syncUserProfile(saved);

        return new AvatarUploadResponse(avatarUrl);
    }

    private void ensureSelf(Long userId, Long currentUserId) {
        if (!userId.equals(currentUserId)) {
            throw new AccessDeniedException("You are not allowed to access this resource");
        }
    }

    private void validateNewPassword(String newPassword) {
        if (newPassword.length() < 8) {
            throw new BadRequestException("New password must be at least 8 characters");
        }

        boolean hasLetter = newPassword.chars().anyMatch(Character::isLetter);
        boolean hasDigit = newPassword.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new BadRequestException("New password must include at least one letter and one number");
        }
    }

    private void validateAvatarFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Avatar file is required");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Avatar file must be an image");
        }

        if (file.getSize() > avatarMaxSizeBytes) {
            throw new BadRequestException("Avatar file is too large");
        }
    }

    private UserProfileResponse toProfileResponse(User user) {
        return new UserProfileResponse(user.getId(), user.getName(), user.getEmail(), user.getAvatarUrl());
    }
}