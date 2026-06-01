package com.edulearn.controller;

import com.edulearn.dto.request.ApprovalActionRequest;
import com.edulearn.dto.request.SubjectRequest;
import com.edulearn.dto.response.SubjectResponse;
import com.edulearn.service.SubjectService;
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
public class SubjectController {

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @PostMapping("/categories/{cid}/subjects")
    public ResponseEntity<ApiResponse<SubjectResponse>> create(
            @PathVariable String cid,
            @Valid @RequestBody SubjectRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Created", subjectService.create(cid, request, user.getUsername())));
    }

    @GetMapping("/categories/{cid}/subjects")
    public ResponseEntity<ApiResponse<List<SubjectResponse>>> list(
            @PathVariable String cid,
            @AuthenticationPrincipal UserDetails user) {
        String email = user != null ? user.getUsername() : null;
        return ResponseEntity.ok(ApiResponse.success(subjectService.listByCategory(cid, email)));
    }

    @GetMapping("/subjects/{id}")
    public ResponseEntity<ApiResponse<SubjectResponse>> get(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(subjectService.get(id)));
    }

    @PutMapping("/subjects/{id}")
    public ResponseEntity<ApiResponse<SubjectResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody SubjectRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(subjectService.update(id, request, user.getUsername())));
    }

    @DeleteMapping("/subjects/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails user) {
        subjectService.delete(id, user.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }

    @PutMapping("/subjects/{id}/submit")
    public ResponseEntity<ApiResponse<SubjectResponse>> submit(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(subjectService.submit(id, user.getUsername())));
    }

    @PutMapping("/subjects/{id}/approve")
    public ResponseEntity<ApiResponse<SubjectResponse>> approve(
            @PathVariable String id,
            @RequestBody(required = false) ApprovalActionRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(subjectService.approve(id, req, user.getUsername())));
    }

    @PutMapping("/subjects/{id}/reject")
    public ResponseEntity<ApiResponse<SubjectResponse>> reject(
            @PathVariable String id,
            @RequestBody ApprovalActionRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(subjectService.reject(id, req, user.getUsername())));
    }
}
