package com.edulearn.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TeacherResponse(
    UUID id,
    String fullName,
    String email,
    String jobTitle,
    String department,
    boolean isActive,
    OffsetDateTime createdAt
) {}
