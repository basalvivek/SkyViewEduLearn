package com.edulearn.dto.response;

public record AuthResponse(String token, String role, String name, boolean mustChangePassword) {}
