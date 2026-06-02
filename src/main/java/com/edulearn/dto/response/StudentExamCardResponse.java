package com.edulearn.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StudentExamCardResponse(
        UUID scheduleId,
        UUID examId,
        String examName,
        String examDescription,
        Short timeLimitMins,
        Integer questionCount,
        Short passMark,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String scheduleStatus,
        UUID attemptId,
        String attemptStatus,
        Integer answeredCount,
        Double percentage,
        Boolean isPassed,
        OffsetDateTime submittedAt
) {}
