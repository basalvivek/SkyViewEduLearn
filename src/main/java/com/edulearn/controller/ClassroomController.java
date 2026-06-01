package com.edulearn.controller;

import com.edulearn.dto.request.ClassroomRequest;
import com.edulearn.dto.response.ClassroomResponse;
import com.edulearn.service.ClassroomService;
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
@RequestMapping("/api/v1/classes")
public class ClassroomController {

    private final ClassroomService classroomService;

    public ClassroomController(ClassroomService classroomService) {
        this.classroomService = classroomService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ClassroomResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(classroomService.listClassrooms()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ClassroomResponse>> create(
            @Valid @RequestBody ClassroomRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Created", classroomService.createClassroom(request, user.getUsername())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ClassroomResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ClassroomRequest request) {
        return ResponseEntity.ok(ApiResponse.success(classroomService.updateClassroom(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        classroomService.deleteClassroom(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }
}
