package com.edulearn.dto.response;

import com.edulearn.enums.AttemptStatus;
import com.edulearn.enums.QuestionType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AttemptResultResponse(
    UUID id,
    String examName,
    int totalMarks,
    Integer marksObtained,
    BigDecimal percentage,
    Boolean isPassed,
    AttemptStatus status,
    int autoMarkedCount,
    int pendingManualCount,
    List<AnswerResult> answers
) {
    public record AnswerResult(
        UUID questionId,
        String questionText,
        QuestionType type,
        int marks,
        Integer marksAwarded,
        Boolean isCorrect,
        String markerNote
    ) {}
}
