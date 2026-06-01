package com.edulearn.dto.response;

import java.util.UUID;

public record QuestionOptionResponse(
    UUID id,
    String optionText,
    boolean isCorrect,
    int displayOrder,
    Short partialMarks
) {}
