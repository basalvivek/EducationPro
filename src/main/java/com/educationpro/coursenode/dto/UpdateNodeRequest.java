package com.educationpro.coursenode.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateNodeRequest(
        @NotBlank @Size(max = 150)
        String title,

        @Size(max = 500)
        String description,

        @Size(max = 100)
        String tagline,

        // Question core
        String questionText,
        String questionType,
        Short marks,
        String complexity,
        String explanation,

        // MCQ
        String options,
        Short correctIndex,
        String correctIndices,
        String partialMarking,

        // TRUE_FALSE
        String correctAnswer,

        // SHORT_ANSWER / ESSAY
        String modelAnswer,
        String markingScheme,
        Short wordLimit,

        // CODE
        String codeLanguage,
        String starterCode,
        String expectedOutput,

        // IMAGE_BASED
        String imagePath,
        String imageAlt,
        String imageAnswerType,

        // Question picker metadata
        String className,
        String subject,
        String examBoard,
        String topic,
        String subTopic
) {}
