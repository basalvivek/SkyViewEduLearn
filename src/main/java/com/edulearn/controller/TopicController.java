package com.edulearn.controller;

import com.edulearn.dto.request.ApprovalActionRequest;
import com.edulearn.dto.request.TopicRequest;
import com.edulearn.dto.response.TopicResponse;
import com.edulearn.service.TopicService;
import com.edulearn.util.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class TopicController {

    private final TopicService topicService;

    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @PostMapping("/subjects/{sid}/topics")
    public ResponseEntity<ApiResponse<TopicResponse>> create(
            @PathVariable String sid,
            @Valid @RequestBody TopicRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Created", topicService.create(sid, request, user.getUsername())));
    }

    @GetMapping("/subjects/{sid}/topics")
    public ResponseEntity<ApiResponse<List<TopicResponse>>> list(
            @PathVariable String sid,
            @AuthenticationPrincipal UserDetails user) {
        String email = user != null ? user.getUsername() : null;
        return ResponseEntity.ok(ApiResponse.success(topicService.listBySubject(sid, email)));
    }

    @GetMapping("/topics/{id}")
    public ResponseEntity<ApiResponse<TopicResponse>> get(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(topicService.get(id)));
    }

    @PutMapping("/topics/{id}")
    public ResponseEntity<ApiResponse<TopicResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody TopicRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(topicService.update(id, request, user.getUsername())));
    }

    @DeleteMapping("/topics/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails user) {
        topicService.delete(id, user.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }

    @PutMapping("/topics/{id}/submit")
    public ResponseEntity<ApiResponse<TopicResponse>> submit(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(topicService.submit(id, user.getUsername())));
    }

    @PutMapping("/topics/{id}/approve")
    public ResponseEntity<ApiResponse<TopicResponse>> approve(
            @PathVariable String id,
            @RequestBody(required = false) ApprovalActionRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(topicService.approve(id, req, user.getUsername())));
    }

    @PutMapping("/topics/{id}/reject")
    public ResponseEntity<ApiResponse<TopicResponse>> reject(
            @PathVariable String id,
            @RequestBody ApprovalActionRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(topicService.reject(id, req, user.getUsername())));
    }
}
