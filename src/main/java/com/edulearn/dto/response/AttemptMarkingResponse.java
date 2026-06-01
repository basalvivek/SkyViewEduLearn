package com.edulearn.dto.response;

import com.edulearn.enums.QuestionType;

import java.util.List;
import java.util.UUID;

public record AttemptMarkingResponse(
    UUID attemptId,
    String studentName,
    String examName,
    int totalMarks,
    int autoMarked,
    List<MarkingAnswerItem> answers
) {
    public record MarkingAnswerItem(
        UUID answerId,
        UUID questionId,
        String questionText,
        QuestionType type,
        int maxMarks,
        String textAnswer,
        List<String> selectedOptions,
        Boolean booleanAnswer,
        Short marksAwarded,
        Boolean isCorrect,
        String markerNote,
        String modelAnswer,
        String markingScheme
    ) {}
}
