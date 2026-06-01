package com.edulearn.dto.response;

import java.util.UUID;

public record ClassroomResponse(
    UUID id,
    String name,
    String yearGroup,
    boolean isActive,
    int studentCount
) {}
