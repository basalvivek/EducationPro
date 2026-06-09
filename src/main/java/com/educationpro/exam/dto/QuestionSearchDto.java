package com.educationpro.exam.dto;

public record QuestionSearchDto(
    Long   id,
    String title,
    String questionText,
    String questionType,
    String complexity,
    int    marks,
    String className,
    String subject,
    String examBoard,
    String topic,
    String subTopic
) {}
