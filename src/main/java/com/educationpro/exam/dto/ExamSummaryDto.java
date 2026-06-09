package com.educationpro.exam.dto;

public record ExamSummaryDto(
    Long   id,
    String name,
    String status,
    int    timeLimitMinutes,
    int    totalMarks,
    int    questionCount,
    String createdByName
) {
    public ExamSummaryDto(Long id, String name, String status,
                          int timeLimitMinutes, int totalMarks, int questionCount) {
        this(id, name, status, timeLimitMinutes, totalMarks, questionCount, null);
    }
}
