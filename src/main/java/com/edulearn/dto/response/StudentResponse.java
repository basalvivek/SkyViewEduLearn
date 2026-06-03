package com.edulearn.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record StudentResponse(
    UUID id,
    String fullName,
    String email,
    String displayName,
    boolean isActive,
    OffsetDateTime createdAt,
    String yearGroup,
    List<ClassInfo> classes
) {
    public record ClassInfo(UUID id, String name) {}
}
