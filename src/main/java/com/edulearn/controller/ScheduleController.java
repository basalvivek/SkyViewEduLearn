package com.edulearn.controller;

import com.edulearn.dto.request.ClassEventRequest;
import com.edulearn.dto.request.MoveScheduleRequest;
import com.edulearn.dto.request.RangeScheduleRequest;
import com.edulearn.dto.request.RecurringScheduleRequest;
import com.edulearn.dto.request.ScheduleRequest;
import com.edulearn.dto.response.CalendarEntryResponse;
import com.edulearn.dto.response.ScheduleResponse;
import com.edulearn.dto.response.SeriesCreatedResponse;
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
    public ResponseEntity<ApiResponse<Void>> cancel(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        scheduleService.cancelSchedule(id, user.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Cancelled", null));
    }

    // ── Teachers list (for assignment dropdown) ────────────────

    @GetMapping("/teachers")
    public ResponseEntity<ApiResponse<List<java.util.Map<String, Object>>>> listTeachers() {
        return ResponseEntity.ok(ApiResponse.success(scheduleService.listTeachers()));
    }

    // ── Calendar feed ──────────────────────────────────────────

    @GetMapping("/calendar")
    public ResponseEntity<ApiResponse<List<CalendarEntryResponse>>> calendar(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) UUID classId,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(
                scheduleService.getCalendar(year, month, classId, user.getUsername())));
    }

    // ── Class / Event / Holiday (single, no exam) ──────────────

    @PutMapping("/{id}/class-event")
    public ResponseEntity<ApiResponse<ScheduleResponse>> updateClassEvent(
            @PathVariable UUID id,
            @Valid @RequestBody ClassEventRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(
                scheduleService.updateClassEvent(id, request, user.getUsername())));
    }

    @PostMapping("/class-event")
    public ResponseEntity<ApiResponse<ScheduleResponse>> createClassEvent(
            @Valid @RequestBody ClassEventRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Created", scheduleService.createClassEvent(request, user.getUsername())));
    }

    // ── Date-range booking ─────────────────────────────────────

    @PostMapping("/range")
    public ResponseEntity<ApiResponse<SeriesCreatedResponse>> createRange(
            @Valid @RequestBody RangeScheduleRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Created", scheduleService.createRange(request, user.getUsername())));
    }

    // ── Recurring series booking ───────────────────────────────

    @PostMapping("/recurring")
    public ResponseEntity<ApiResponse<SeriesCreatedResponse>> createRecurring(
            @Valid @RequestBody RecurringScheduleRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Created", scheduleService.createRecurring(request, user.getUsername())));
    }

    // ── Drag-drop move ─────────────────────────────────────────

    @PutMapping("/{id}/move")
    public ResponseEntity<ApiResponse<Void>> moveSchedule(
            @PathVariable UUID id,
            @Valid @RequestBody MoveScheduleRequest request,
            @AuthenticationPrincipal UserDetails user) {
        scheduleService.moveSchedule(id, request, user.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Moved", null));
    }

    // ── Series endpoints ───────────────────────────────────────

    @GetMapping("/series/{seriesId}")
    public ResponseEntity<ApiResponse<List<CalendarEntryResponse>>> getSeries(@PathVariable UUID seriesId) {
        return ResponseEntity.ok(ApiResponse.success(scheduleService.getSeries(seriesId)));
    }

    @DeleteMapping("/series/{seriesId}")
    public ResponseEntity<ApiResponse<Void>> deleteSeries(
            @PathVariable UUID seriesId,
            @AuthenticationPrincipal UserDetails user) {
        scheduleService.deleteSeries(seriesId, user.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Series deleted", null));
    }
}
