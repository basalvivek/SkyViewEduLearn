package com.edulearn.dto.request;

import com.edulearn.enums.ScheduleType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record RangeScheduleRequest(
    @NotNull ScheduleType scheduleType,
    @NotNull String title,
    UUID classId,
    @NotNull LocalDate fromDate,
    @NotNull LocalDate toDate,
    @NotNull String startTime,
    int durationMinutes,
    UUID teacherId
) {
    public RangeScheduleRequest {
        if (durationMinutes <= 0) durationMinutes = 60;
    }
}
