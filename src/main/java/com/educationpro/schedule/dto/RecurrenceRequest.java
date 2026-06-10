package com.educationpro.schedule.dto;

import com.educationpro.schedule.domain.EndCondition;
import com.educationpro.schedule.domain.RecurrencePattern;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record RecurrenceRequest(
    @NotNull RecurrencePattern pattern,
    @Min(1) Integer intervalValue,
    List<Integer> daysOfWeek,
    @NotNull EndCondition endCondition,
    LocalDate endDate,
    @Min(1) Integer occurrenceCount
) {}
