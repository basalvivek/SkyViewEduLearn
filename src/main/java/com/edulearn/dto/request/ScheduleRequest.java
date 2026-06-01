package com.edulearn.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ScheduleRequest(
    @NotNull UUID examId,
    @NotNull UUID classId,
    @NotNull OffsetDateTime startAt,
    @NotNull OffsetDateTime endAt,
    int maxAttempts,
    boolean showResultsImmediately,
    String instructions
) {
    public ScheduleRequest {
        if (maxAttempts <= 0) maxAttempts = 1;
    }
}
