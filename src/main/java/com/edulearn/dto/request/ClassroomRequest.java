package com.edulearn.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClassroomRequest(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 20) String yearGroup
) {}
