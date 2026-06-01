package com.edulearn.controller;

import com.edulearn.dto.request.ApprovalActionRequest;
import com.edulearn.dto.request.ExamQuestionsRequest;
import com.edulearn.dto.request.ExamRequest;
import com.edulearn.dto.response.ExamResponse;
import com.edulearn.dto.response.ExamSummaryResponse;
import com.edulearn.service.ExamService;
import com.edulearn.util.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exams")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ExamSummaryResponse>>> list(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(examService.listExams(user.getUsername())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ExamResponse>> create(
            @Valid @RequestBody ExamRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Created", examService.createExam(request, user.getUsername())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExamResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(examService.getExam(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ExamResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ExamRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(examService.updateExam(id, request, user.getUsername())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        examService.deleteExam(id, user.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }

    @PutMapping("/{id}/questions")
    public ResponseEntity<ApiResponse<ExamResponse>> setQuestions(
            @PathVariable UUID id,
            @Valid @RequestBody ExamQuestionsRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(examService.setQuestions(id, request, user.getUsername())));
    }

    @PutMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<ExamResponse>> submit(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(examService.submitExam(id, user.getUsername())));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ExamResponse>> approve(
            @PathVariable UUID id,
            @RequestBody(required = false) ApprovalActionRequest req,
            @AuthenticationPrincipal UserDetails user) {
        String note = req != null ? req.note() : null;
        return ResponseEntity.ok(ApiResponse.success(examService.approveExam(id, user.getUsername(), note)));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ExamResponse>> reject(
            @PathVariable UUID id,
            @RequestBody ApprovalActionRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(
                examService.rejectExam(id, user.getUsername(), req != null ? req.note() : null)));
    }
}
