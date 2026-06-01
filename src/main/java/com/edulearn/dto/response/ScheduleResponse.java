package com.edulearn.dto.response;

import com.edulearn.enums.ScheduleStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ScheduleResponse(
    UUID id,
    UUID examId,
    String examName,
    UUID classId,
    String className,
    OffsetDateTime startAt,
    OffsetDateTime endAt,
    int maxAttempts,
    boolean showResultsImmediately,
    String instructions,
    ScheduleStatus status,
    int attemptCount,
    String createdByName
) {}
