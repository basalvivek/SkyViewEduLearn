package com.edulearn.dto.request;

import com.edulearn.enums.RecurrenceType;
import com.edulearn.enums.ScheduleType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record RecurringScheduleRequest(
    @NotNull ScheduleType scheduleType,
    @NotNull String title,
    UUID classId,
    @NotNull RecurrenceType recurrenceType,
    String recurrenceDays,
    @NotNull LocalDate seriesStart,
    @NotNull LocalDate seriesEnd,
    @NotNull String startTime,
    int durationMinutes,
    UUID teacherId
) {
    public RecurringScheduleRequest {
        if (durationMinutes <= 0) durationMinutes = 60;
    }
}
