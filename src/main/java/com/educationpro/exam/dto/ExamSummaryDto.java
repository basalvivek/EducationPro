package com.educationpro.exam.dto;

public record ExamSummaryDto(
    Long   id,
    String name,
    String status,
    int    timeLimitMinutes,
    int    totalMarks,
    int    questionCount
) {}
