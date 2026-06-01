package com.edulearn.controller;

import com.edulearn.dto.request.PasswordChangeRequest;
import com.edulearn.dto.request.ProfileUpdateRequest;
import com.edulearn.dto.response.ProfileResponse;
import com.edulearn.service.ProfileService;
import com.edulearn.util.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ProfileController {

    private final ProfileService service;

    public ProfileController(ProfileService service) {
        this.service = service;
    }

    @GetMapping("/my/profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> getMyProfile(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getProfile(user.getUsername())));
    }

    @PutMapping("/my/profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateMyProfile(
            @Valid @RequestBody ProfileUpdateRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success("Profile updated",
                service.updateProfile(user.getUsername(), req)));
    }

    @PostMapping("/my/profile/avatar")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails user) throws IOException {
        String url = service.uploadAvatar(user.getUsername(), file);
        return ResponseEntity.ok(ApiResponse.success("Avatar uploaded", Map.of("avatarUrl", url)));
    }

    @PutMapping("/my/profile/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody PasswordChangeRequest req,
            @AuthenticationPrincipal UserDetails user) {
        service.changePassword(user.getUsername(), req);
        return ResponseEntity.ok(ApiResponse.success("Password changed", null));
    }

    @GetMapping("/users/{id}/profile")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProfileResponse>> getUserProfile(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getProfileById(id)));
    }
}
