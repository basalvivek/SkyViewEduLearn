package com.edulearn.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public record ExamRequest(
    @NotBlank String name,
    String description,
    int timeLimitMins,
    Integer passMark,
    boolean shuffleQuestions,
    boolean shuffleOptions,
    List<UUID> questionIds
) {
    public ExamRequest {
        if (timeLimitMins <= 0) timeLimitMins = 60;
    }
}
