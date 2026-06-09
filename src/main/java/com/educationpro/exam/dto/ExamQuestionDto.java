package com.educationpro.exam.dto;

public record ExamQuestionDto(
    Long   examQuestionId,
    Long   questionId,
    String title,
    String questionText,
    String questionType,
    String complexity,
    int    marks,
    int    position
) {}
