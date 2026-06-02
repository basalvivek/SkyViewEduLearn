package com.edulearn.controller;

import com.edulearn.dto.response.StudentDashboardResponse;
import com.edulearn.dto.response.StudentExamCardResponse;
import com.edulearn.service.StudentDashboardService;
import com.edulearn.util.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/student")
public class StudentDashboardController {

    private final StudentDashboardService dashboardService;

    public StudentDashboardController(StudentDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<StudentDashboardResponse>> getDashboard(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getDashboard(user.getUsername())));
    }

    @GetMapping("/exams/active")
    public ResponseEntity<ApiResponse<List<StudentExamCardResponse>>> getActiveExams(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getActiveExams(user.getUsername())));
    }

    @GetMapping("/exams/upcoming")
    public ResponseEntity<ApiResponse<List<StudentExamCardResponse>>> getUpcomingExams(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getUpcomingExams(user.getUsername())));
    }

    @GetMapping("/exams/completed")
    public ResponseEntity<ApiResponse<List<StudentExamCardResponse>>> getCompletedExams(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getCompletedExams(user.getUsername())));
    }
}
