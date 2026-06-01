package com.edulearn.controller;

import com.edulearn.dto.request.MarkAnswerRequest;
import com.edulearn.dto.response.AttemptMarkingResponse;
import com.edulearn.dto.response.MarkingQueueResponse;
import com.edulearn.service.MarkingService;
import com.edulearn.util.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/marking")
public class MarkingController {

    private final MarkingService markingService;

    public MarkingController(MarkingService markingService) {
        this.markingService = markingService;
    }

    @GetMapping("/queue")
    public ResponseEntity<ApiResponse<List<MarkingQueueResponse>>> getQueue(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(markingService.getQueue(user.getUsername())));
    }

    @GetMapping("/attempts/{id}")
    public ResponseEntity<ApiResponse<AttemptMarkingResponse>> getAttemptForMarking(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(
                markingService.getAttemptForMarking(id, user.getUsername())));
    }

    @PutMapping("/attempts/{attemptId}/answers/{answerId}")
    public ResponseEntity<ApiResponse<Void>> markAnswer(
            @PathVariable UUID attemptId,
            @PathVariable UUID answerId,
            @Valid @RequestBody MarkAnswerRequest request,
            @AuthenticationPrincipal UserDetails user) {
        markingService.markAnswer(attemptId, answerId, request, user.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Marked", null));
    }

    @PostMapping("/attempts/{id}/finalise")
    public ResponseEntity<ApiResponse<Void>> finalise(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        markingService.finaliseMarking(id, user.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Marking finalised", null));
    }
}
