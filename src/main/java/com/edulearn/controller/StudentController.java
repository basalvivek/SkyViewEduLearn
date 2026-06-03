package com.edulearn.controller;

import com.edulearn.dto.request.StudentCreateRequest;
import com.edulearn.dto.response.StudentResponse;
import com.edulearn.service.StudentService;
import com.edulearn.util.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/students")
@PreAuthorize("hasRole('ADMIN')")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StudentResponse>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID classId) {
        return ResponseEntity.ok(ApiResponse.success(studentService.listStudents(search, classId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StudentResponse>> create(
            @Valid @RequestBody StudentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Created", studentService.createStudent(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(studentService.getStudent(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody StudentCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(studentService.updateStudent(id, request)));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable UUID id) {
        studentService.deactivateStudent(id);
        return ResponseEntity.ok(ApiResponse.success("Deactivated", null));
    }

    @PutMapping("/{id}/reset-password")
    public ResponseEntity<ApiResponse<Map<String, String>>> resetPassword(@PathVariable UUID id) {
        String newPassword = studentService.resetPassword(id);
        return ResponseEntity.ok(ApiResponse.success("Password reset", Map.of("temporaryPassword", newPassword)));
    }

    @PostMapping("/bulk-import")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkImport(
            @RequestParam("file") MultipartFile file) {
        Map<String, Object> result = studentService.importFromCsv(file);
        return ResponseEntity.ok(ApiResponse.success("Import complete", result));
    }

    @GetMapping("/{id}/classes")
    public ResponseEntity<ApiResponse<List<String>>> getClasses(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(studentService.getStudentClasses(id)));
    }

    @PostMapping("/{id}/classes")
    public ResponseEntity<ApiResponse<Void>> assignToClass(
            @PathVariable UUID id,
            @RequestBody Map<String, UUID> body) {
        studentService.assignToClass(id, body.get("classId"));
        return ResponseEntity.ok(ApiResponse.success("Assigned", null));
    }

    @DeleteMapping("/{id}/classes/{classId}")
    public ResponseEntity<ApiResponse<Void>> removeFromClass(
            @PathVariable UUID id,
            @PathVariable UUID classId) {
        studentService.removeFromClass(id, classId);
        return ResponseEntity.ok(ApiResponse.success("Removed", null));
    }
}
