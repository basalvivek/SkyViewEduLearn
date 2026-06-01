package com.edulearn.dto.request;

import java.util.List;
import java.util.UUID;

public record SaveAnswersRequest(
    List<AnswerItem> answers
) {
    public record AnswerItem(
        UUID questionId,
        List<UUID> selectedOptionIds,
        Boolean booleanAnswer,
        String textAnswer,
        Boolean isFlagged
    ) {}
}
