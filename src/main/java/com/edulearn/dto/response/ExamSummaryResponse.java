package com.edulearn.dto.response;

import com.edulearn.enums.ExamStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ExamSummaryResponse(
    UUID id,
    String name,
    ExamStatus status,
    int timeLimitMins,
    int totalMarks,
    int questionCount,
    String createdByName,
    OffsetDateTime createdAt
) {}
