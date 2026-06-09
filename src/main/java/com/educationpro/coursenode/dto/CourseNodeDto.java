package com.educationpro.coursenode.dto;

public record CourseNodeDto(
        Long id,
        Long parentId,
        String type,
        String title,
        String description,
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
        String subTopic,

        // Audit/ordering
        int sortOrder
) {}
