package com.edulearn.controller;

import com.edulearn.dto.response.NotificationResponse;
import com.edulearn.service.NotificationService;
import com.edulearn.util.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails user) {
        Page<NotificationResponse> result = service.getForUser(user.getUsername(), PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.paged(result.getContent(), result));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> unreadCount(
            @AuthenticationPrincipal UserDetails user) {
        long count = service.getUnreadCount(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success("OK", Map.of("count", count)));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        service.markRead(id, user.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Marked as read", null));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead(
            @AuthenticationPrincipal UserDetails user) {
        service.markAllRead(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success("All marked as read", null));
    }
}
