package com.edulearn.controller;

import com.edulearn.dto.request.ApprovalActionRequest;
import com.edulearn.dto.request.QuestionRequest;
import com.edulearn.dto.response.QuestionResponse;
import com.edulearn.service.QuestionService;
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
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @PostMapping("/topics/{tid}/questions")
    public ResponseEntity<ApiResponse<QuestionResponse>> create(
            @PathVariable String tid,
            @Valid @RequestBody QuestionRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Created", questionService.create(tid, request, user.getUsername())));
    }

    @GetMapping("/topics/{tid}/questions")
    public ResponseEntity<ApiResponse<List<QuestionResponse>>> list(
            @PathVariable String tid,
            @AuthenticationPrincipal UserDetails user) {
        String email = user != null ? user.getUsername() : null;
        return ResponseEntity.ok(ApiResponse.success(questionService.listByTopic(tid, email)));
    }

    @GetMapping("/questions/{id}")
    public ResponseEntity<ApiResponse<QuestionResponse>> get(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(questionService.get(id)));
    }

    @PutMapping("/questions/{id}")
    public ResponseEntity<ApiResponse<QuestionResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody QuestionRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(questionService.update(id, request, user.getUsername())));
    }

    @DeleteMapping("/questions/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails user) {
        questionService.delete(id, user.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }

    @PutMapping("/questions/{id}/submit")
    public ResponseEntity<ApiResponse<QuestionResponse>> submit(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(questionService.submit(id, user.getUsername())));
    }

    @PutMapping("/questions/{id}/approve")
    public ResponseEntity<ApiResponse<QuestionResponse>> approve(
            @PathVariable String id,
            @RequestBody(required = false) ApprovalActionRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(questionService.approve(id, req, user.getUsername())));
    }

    @PutMapping("/questions/{id}/reject")
    public ResponseEntity<ApiResponse<QuestionResponse>> reject(
            @PathVariable String id,
            @RequestBody ApprovalActionRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(questionService.reject(id, req, user.getUsername())));
    }
}
