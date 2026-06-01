package com.edulearn.controller;

import com.edulearn.dto.request.ApprovalActionRequest;
import com.edulearn.dto.request.CategoryRequest;
import com.edulearn.dto.response.CategoryResponse;
import com.edulearn.dto.response.TreeNodeResponse;
import com.edulearn.service.CategoryService;
import com.edulearn.util.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> create(
            @Valid @RequestBody CategoryRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Created", categoryService.create(request, user.getUsername())));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<java.util.List<CategoryResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails user) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        String email = user != null ? user.getUsername() : null;
        Page<CategoryResponse> result = email != null
            ? categoryService.list(email, pageable)
            : categoryService.listPublic(pageable);
        return ResponseEntity.ok(ApiResponse.paged(result.getContent(), result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> get(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.get(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody CategoryRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.update(id, request, user.getUsername())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails user) {
        categoryService.delete(id, user.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }

    @PutMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<CategoryResponse>> submit(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.submit(id, user.getUsername())));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<CategoryResponse>> approve(
            @PathVariable String id,
            @RequestBody(required = false) ApprovalActionRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.approve(id, req, user.getUsername())));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<CategoryResponse>> reject(
            @PathVariable String id,
            @RequestBody ApprovalActionRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.reject(id, req, user.getUsername())));
    }

    @GetMapping("/{id}/tree")
    public ResponseEntity<ApiResponse<TreeNodeResponse>> tree(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails user) {
        String email = user != null ? user.getUsername() : null;
        return ResponseEntity.ok(ApiResponse.success(categoryService.getTree(id, email)));
    }
}
