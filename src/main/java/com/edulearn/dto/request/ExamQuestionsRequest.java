package com.edulearn.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ExamQuestionsRequest(
    @NotNull List<UUID> questionIds
) {}
