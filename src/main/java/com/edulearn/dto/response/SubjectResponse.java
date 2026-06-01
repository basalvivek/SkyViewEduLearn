package com.edulearn.dto.response;

import com.edulearn.enums.ContentStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SubjectResponse(
    UUID id,
    UUID categoryId,
    String name,
    String curriculumLevel,
    String description,
    ContentStatus status,
    String rejectionReason,
    String createdByName,
    UUID createdById,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
