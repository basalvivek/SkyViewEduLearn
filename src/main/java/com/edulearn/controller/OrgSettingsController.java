package com.edulearn.controller;

import com.edulearn.dto.request.*;
import com.edulearn.dto.response.OrgSettingsResponse;
import com.edulearn.service.OrgSettingsService;
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

@RestController
@RequestMapping("/api/v1/org/settings")
public class OrgSettingsController {

    private final OrgSettingsService service;

    public OrgSettingsController(OrgSettingsService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<OrgSettingsResponse>> get() {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getSettings()));
    }

    @PutMapping("/branding")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrgSettingsResponse>> updateBranding(
            @Valid @RequestBody OrgBrandingRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success("Branding updated",
                service.updateBranding(req, user.getUsername())));
    }

    @PutMapping("/legal")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrgSettingsResponse>> updateLegal(
            @Valid @RequestBody OrgLegalRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success("Legal details updated",
                service.updateLegal(req, user.getUsername())));
    }

    @PutMapping("/address")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrgSettingsResponse>> updateAddress(
            @Valid @RequestBody OrgAddressRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success("Address updated",
                service.updateAddress(req, user.getUsername())));
    }

    @PutMapping("/contacts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrgSettingsResponse>> updateContacts(
            @Valid @RequestBody OrgContactsRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success("Contacts updated",
                service.updateContacts(req, user.getUsername())));
    }

    @PutMapping("/academic")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrgSettingsResponse>> updateAcademic(
            @Valid @RequestBody OrgAcademicRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success("Academic info updated",
                service.updateAcademic(req, user.getUsername())));
    }

    @PostMapping("/logo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadLogo(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails user) throws IOException {
        String logoUrl = service.uploadLogo(file, user.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Logo uploaded", Map.of("logoUrl", logoUrl)));
    }
}
