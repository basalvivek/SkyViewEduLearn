package com.edulearn.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record TeacherRequest(
    @NotBlank String fullName,
    @Email @NotBlank String email,
    String temporaryPassword,
    String jobTitle,
    String department
) {}
