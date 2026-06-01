package com.edulearn.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StartAttemptRequest(
    @NotNull UUID scheduleId
) {}
