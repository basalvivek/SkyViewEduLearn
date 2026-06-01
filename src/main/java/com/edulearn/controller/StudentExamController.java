package com.edulearn.controller;

import com.edulearn.dto.request.SaveAnswersRequest;
import com.edulearn.dto.request.StartAttemptRequest;
import com.edulearn.dto.response.AttemptResponse;
import com.edulearn.dto.response.AttemptResultResponse;
import com.edulearn.service.ExamAttemptService;
import com.edulearn.util.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/student")
public class StudentExamController {

    private final ExamAttemptService attemptService;

    public StudentExamController(ExamAttemptService attemptService) {
        this.attemptService = attemptService;
    }

    @GetMapping("/exams")
    public ResponseEntity<ApiResponse<List<AttemptResponse>>> getAvailableExams(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(attemptService.getAvailableExams(user.getUsername())));
    }

    @PostMapping("/attempts")
    public ResponseEntity<ApiResponse<AttemptResponse>> startAttempt(
            @Valid @RequestBody StartAttemptRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Attempt started",
                        attemptService.startAttempt(request, user.getUsername())));
    }

    @GetMapping("/attempts/{id}")
    public ResponseEntity<ApiResponse<AttemptResponse>> getAttempt(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(attemptService.getAttempt(id, user.getUsername())));
    }

    @PutMapping("/attempts/{id}/answers")
    public ResponseEntity<ApiResponse<Void>> saveAnswers(
            @PathVariable UUID id,
            @RequestBody SaveAnswersRequest request,
            @AuthenticationPrincipal UserDetails user) {
        attemptService.saveAnswers(id, request, user.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Answers saved", null));
    }

    @PostMapping("/attempts/{id}/submit")
    public ResponseEntity<ApiResponse<AttemptResultResponse>> submit(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success("Submitted",
                attemptService.submitAttempt(id, user.getUsername())));
    }

    @GetMapping("/attempts/{id}/result")
    public ResponseEntity<ApiResponse<AttemptResultResponse>> getResult(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(attemptService.getResult(id, user.getUsername())));
    }
}
