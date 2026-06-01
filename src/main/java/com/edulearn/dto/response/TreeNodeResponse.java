package com.edulearn.dto.response;

import com.edulearn.enums.ContentStatus;

import java.util.List;
import java.util.UUID;

public record TreeNodeResponse(
    UUID id,
    String name,
    String type,
    ContentStatus status,
    String rejectionReason,
    String createdByName,
    UUID createdById,
    List<TreeNodeResponse> children
) {}
