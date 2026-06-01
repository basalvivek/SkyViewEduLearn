package com.edulearn.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record StudentCreateRequest(
    @NotBlank String fullName,
    @Email @NotBlank String email,
    UUID classId,
    String yearGroup,
    String temporaryPassword
) {}
