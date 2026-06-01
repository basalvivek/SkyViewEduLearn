package com.edulearn.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubjectRequest(
    @NotBlank @Size(max = 120) String name,
    @Size(max = 50) String curriculumLevel,
    String description
) {}
