package com.edulearn.dto.response;

import com.edulearn.enums.ExamStatus;

import java.util.List;
import java.util.UUID;

public record ExamResponse(
    UUID id,
    String name,
    String description,
    int timeLimitMins,
    int totalMarks,
    Integer passMark,
    boolean shuffleQuestions,
    boolean shuffleOptions,
    ExamStatus status,
    String rejectionReason,
    String createdByName,
    int questionCount,
    List<ExamQuestionItem> questions
) {
    public record ExamQuestionItem(
        UUID id,
        UUID questionId,
        String questionText,
        String questionType,
        int marks,
        int displayOrder
    ) {}
}
