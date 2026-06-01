package com.edulearn.dto.response;

import com.edulearn.enums.CodeLanguage;
import com.edulearn.enums.ContentStatus;
import com.edulearn.enums.ImageAnswerType;
import com.edulearn.enums.QuestionType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record QuestionResponse(
    UUID id,
    UUID topicId,
    String questionText,
    QuestionType questionType,
    Short marks,
    String answerExplanation,
    List<QuestionOptionResponse> options,
    Boolean correctBoolean,
    String modelAnswer,
    String markingScheme,
    Integer wordLimit,
    CodeLanguage codeLang,
    String starterCode,
    String expectedOutput,
    String imageUrl,
    String imageAltText,
    ImageAnswerType imageAnswerType,
    ContentStatus status,
    String rejectionReason,
    String createdByName,
    UUID createdById,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
