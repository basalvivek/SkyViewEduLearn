package com.edulearn.dto.request;

import jakarta.validation.constraints.NotBlank;

public record OptionRequest(
    @NotBlank String optionText,
    boolean isCorrect,
    int displayOrder,
    Short partialMarks
) {}
