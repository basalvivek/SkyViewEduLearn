package com.edulearn.dto.request;

import com.edulearn.enums.CodeLanguage;
import com.edulearn.enums.ImageAnswerType;
import com.edulearn.enums.QuestionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record QuestionRequest(
    @NotBlank String questionText,
    @NotNull QuestionType questionType,
    @Min(1) Short marks,
    String answerExplanation,
    List<OptionRequest> options,
    Boolean correctBoolean,
    String modelAnswer,
    String markingScheme,
    Integer wordLimit,
    CodeLanguage codeLang,
    String starterCode,
    String expectedOutput,
    String imageUrl,
    String imageAltText,
    ImageAnswerType imageAnswerType
) {}
