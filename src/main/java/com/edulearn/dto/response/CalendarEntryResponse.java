package com.edulearn.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CalendarEntryResponse(
    UUID id,
    String scheduleType,
    String title,
    UUID classId,
    String className,
    OffsetDateTime startAt,
    OffsetDateTime endAt,
    String status,
    UUID seriesId,
    String recurrenceType,
    String createdByName,
    UUID examId,
    String examName,
    UUID assignedToId,
    String assignedToName
) {}
