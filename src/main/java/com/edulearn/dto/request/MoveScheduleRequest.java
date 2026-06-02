package com.edulearn.dto.request;

import com.edulearn.enums.SeriesMoveScope;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record MoveScheduleRequest(
    @NotNull LocalDate newDate,
    @NotNull SeriesMoveScope applyTo
) {}
