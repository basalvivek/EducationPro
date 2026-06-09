package com.educationpro.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateExamRequest(
    @NotBlank @Size(max = 200) String name,
    String  description,
    int     timeLimitMinutes,
    int     totalMarks,
    Integer passMark,
    boolean shuffleQuestions,
    boolean shuffleOptions
) {}
