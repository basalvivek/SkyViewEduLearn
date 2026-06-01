package com.edulearn.controller;

import com.edulearn.dto.request.ScheduleRequest;
import com.edulearn.dto.response.ScheduleResponse;
import com.edulearn.service.ScheduleService;
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
@RequestMapping("/api/v1/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> list(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(scheduleService.listSchedules(user.getUsername())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ScheduleResponse>> create(
            @Valid @RequestBody ScheduleRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Created", scheduleService.createSchedule(request, user.getUsername())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ScheduleResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(scheduleService.getSchedule(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ScheduleResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ScheduleRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(
                scheduleService.updateSchedule(id, request, user.getUsername())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        scheduleService.cancelSchedule(id, user.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Cancelled", null));
    }
}
