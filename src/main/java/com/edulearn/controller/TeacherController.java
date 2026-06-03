package com.edulearn.controller;

import com.edulearn.dto.request.TeacherRequest;
import com.edulearn.dto.response.TeacherResponse;
import com.edulearn.service.TeacherService;
import com.edulearn.util.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teachers")
@PreAuthorize("hasRole('ADMIN')")
public class TeacherController {

    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TeacherResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(teacherService.listTeachers()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TeacherResponse>> create(
            @Valid @RequestBody TeacherRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Created", teacherService.createTeacher(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TeacherResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody TeacherRequest request) {
        return ResponseEntity.ok(ApiResponse.success(teacherService.updateTeacher(id, request)));
    }

    @PutMapping("/{id}/reset-password")
    public ResponseEntity<ApiResponse<Map<String, String>>> resetPassword(@PathVariable UUID id) {
        String newPassword = teacherService.resetPassword(id);
        return ResponseEntity.ok(ApiResponse.success("Password reset", Map.of("temporaryPassword", newPassword)));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable UUID id) {
        teacherService.deactivateTeacher(id);
        return ResponseEntity.ok(ApiResponse.success("Deactivated", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        teacherService.deleteTeacher(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }
}
