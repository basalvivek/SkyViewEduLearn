package com.edulearn.dto.request;

import jakarta.validation.constraints.NotNull;

public record MarkAnswerRequest(
    @NotNull Short marksAwarded,
    String markerNote
) {}
