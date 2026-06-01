package com.edulearn.controller;

import com.edulearn.dto.request.LoginRequest;
import com.edulearn.dto.response.AuthResponse;
import com.edulearn.dto.response.UserResponse;
import com.edulearn.service.AuthService;
import com.edulearn.util.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(ApiResponse.success("Login successful", response));
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(401).body(ApiResponse.error(ex.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(@AuthenticationPrincipal UserDetails userDetails) {
        UserResponse response = authService.me(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
