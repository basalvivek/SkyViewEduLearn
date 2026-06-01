package com.edulearn.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MarkingQueueResponse(
    UUID attemptId,
    String studentName,
    String className,
    String examName,
    OffsetDateTime submittedAt,
    int totalQuestions,
    int autoMarkedCount,
    int manualPendingCount,
    boolean isComplete
) {}
