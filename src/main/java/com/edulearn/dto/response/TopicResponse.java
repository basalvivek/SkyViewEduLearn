package com.edulearn.dto.response;

import com.edulearn.enums.ContentStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TopicResponse(
    UUID id,
    UUID subjectId,
    String name,
    String learningObjective,
    String difficulty,
    ContentStatus status,
    String rejectionReason,
    String createdByName,
    UUID createdById,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
