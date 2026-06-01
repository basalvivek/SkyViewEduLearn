package com.edulearn.controller;

import com.edulearn.dto.response.SubmissionResponse;
import com.edulearn.dto.response.SubmissionSummaryResponse;
import com.edulearn.enums.ContentStatus;
import com.edulearn.service.SubmissionService;
import com.edulearn.util.ApiResponse;
import com.edulearn.util.PageMeta;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/my")
public class SubmissionController {

    private final SubmissionService service;

    public SubmissionController(SubmissionService service) {
        this.service = service;
    }

    @GetMapping("/submissions")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails user) {

        ContentStatus cs = null;
        if (status != null && !status.isBlank()) {
            try { cs = ContentStatus.valueOf(status.toUpperCase()); } catch (IllegalArgumentException ignored) {}
        }
        List<SubmissionResponse> data = service.getSubmissions(user.getUsername(), cs, type, page, size);
        long total = service.countSubmissions(user.getUsername(), cs, type);
        int totalPages = (int) Math.ceil((double) total / size);
        PageMeta meta = new PageMeta(page, size, total, totalPages);
        return ResponseEntity.ok(new ApiResponse<>("success", "OK", data, meta));
    }

    @GetMapping("/submissions/summary")
    public ResponseEntity<ApiResponse<SubmissionSummaryResponse>> summary(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getSummary(user.getUsername())));
    }
}
