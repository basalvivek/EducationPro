package com.educationpro.exam.dto;

import java.util.List;

public record ExamDto(
    Long              id,
    String            name,
    String            description,
    int               timeLimitMinutes,
    int               totalMarks,
    Integer           passMark,
    boolean           shuffleQuestions,
    boolean           shuffleOptions,
    String            status,
    List<ExamQuestionDto> questions
) {}
