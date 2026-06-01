package com.edulearn.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TopicRequest(
    @NotBlank @Size(max = 200) String name,
    String learningObjective,
    @Pattern(regexp = "Foundation|Intermediate|Higher") String difficulty
) {}
