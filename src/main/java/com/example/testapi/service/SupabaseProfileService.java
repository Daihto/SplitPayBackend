package com.example.testapi.service;

import com.example.testapi.config.SupabaseProperties;
import com.example.testapi.entity.User;
import com.example.testapi.exception.BadRequestException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SupabaseProfileService {

    private final RestClient restClient;
    private final SupabaseProperties supabaseProperties;

    public SupabaseProfileService(SupabaseProperties supabaseProperties,
                                  RestClient.Builder restClientBuilder) {
        this.supabaseProperties = supabaseProperties;
        this.restClient = restClientBuilder.build();
    }

    public void syncUserProfile(User user) {
        ValidatedConfig config = validateConfiguration();

        Map<String, Object> payload = new HashMap<>();
        payload.put("id", user.getId());
        payload.put("name", user.getName());
        payload.put("email", user.getEmail());
        payload.put("avatar_url", user.getAvatarUrl());

        restClient.post()
                .uri(config.supabaseUrl() + "/rest/v1/profiles")
                .contentType(MediaType.APPLICATION_JSON)
                .header("apikey", config.serviceRoleKey())
                .header("Authorization", "Bearer " + config.serviceRoleKey())
                .header("Prefer", "resolution=merge-duplicates")
                .body(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    String body = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
                    throw new BadRequestException("Failed to sync profile with Supabase (" + response.getStatusCode().value() + "): " + body);
                })
                .toBodilessEntity();
    }

    public String uploadAvatar(Long userId, MultipartFile file) {
        ValidatedConfig config = validateConfiguration();

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new BadRequestException("Failed to read uploaded file");
        }

        String extension = resolveExtension(file.getOriginalFilename());
        String objectPath = "users/" + userId + "/" + UUID.randomUUID() + extension;

        restClient.post()
            .uri(config.supabaseUrl() + "/storage/v1/object/" + config.avatarBucket() + "/" + objectPath)
                .contentType(MediaType.parseMediaType(file.getContentType()))
            .header("apikey", config.serviceRoleKey())
            .header("Authorization", "Bearer " + config.serviceRoleKey())
                .header("x-upsert", "true")
                .body(bytes)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    String body = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
                    throw new BadRequestException("Failed to upload avatar to Supabase Storage (" + response.getStatusCode().value() + "): " + body);
                })
                .toBodilessEntity();

        return config.supabaseUrl() + "/storage/v1/object/public/" + config.avatarBucket() + "/" + objectPath;
    }

    private ValidatedConfig validateConfiguration() {
        List<String> missingSettings = new ArrayList<>();

        String supabaseUrl = trimTrailingSlash(supabaseProperties.getUrl());
        if (supabaseUrl.isBlank()) {
            missingSettings.add("app.supabase.url");
        }

        String serviceRoleKey = supabaseProperties.getServiceRoleKey();
        if (serviceRoleKey == null || serviceRoleKey.isBlank()) {
            missingSettings.add("app.supabase.service-role-key");
        }

        String avatarBucket = supabaseProperties.getStorage().getAvatarBucket();
        if (avatarBucket == null || avatarBucket.isBlank()) {
            missingSettings.add("app.supabase.storage.avatar-bucket");
        }

        if (!missingSettings.isEmpty()) {
            throw new BadRequestException("Supabase integration is not configured. Missing: " + String.join(", ", missingSettings));
        }

        return new ValidatedConfig(supabaseUrl, serviceRoleKey, avatarBucket);
    }

    private String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private String resolveExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return ".png";
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        if (extension.length() > 8) {
            return ".png";
        }

        return extension.toLowerCase();
    }

    private record ValidatedConfig(String supabaseUrl, String serviceRoleKey, String avatarBucket) {
    }
}