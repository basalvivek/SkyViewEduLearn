package com.edulearn.dto.response;

import com.edulearn.enums.AttemptStatus;
import com.edulearn.enums.QuestionType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AttemptResponse(
    UUID id,
    UUID scheduleId,
    String examName,
    int timeLimitMins,
    OffsetDateTime startedAt,
    AttemptStatus status,
    List<AttemptQuestionItem> questions
) {
    public record AttemptQuestionItem(
        UUID id,
        UUID questionId,
        String questionText,
        QuestionType type,
        int marks,
        List<OptionItem> options,
        Boolean correctBoolean,
        String existingAnswer,
        List<UUID> existingSelectedOptions,
        Boolean existingBooleanAnswer,
        boolean isFlagged
    ) {}

    public record OptionItem(
        UUID id,
        String text
    ) {}
}
