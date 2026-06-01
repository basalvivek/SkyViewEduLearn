package com.edulearn.dto.response;

import com.edulearn.enums.UserRole;

import java.util.UUID;

public record UserResponse(UUID id, String fullName, String email, UserRole role) {}
