package com.edulearn.dto.request;

import com.edulearn.enums.ScheduleType;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ClassEventRequest(
    @NotNull ScheduleType scheduleType,
    @NotNull String title,
    UUID classId,
    @NotNull OffsetDateTime startAt,
    @NotNull OffsetDateTime endAt,
    UUID teacherId
) {}
