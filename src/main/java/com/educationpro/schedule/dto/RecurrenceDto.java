package com.educationpro.schedule.dto;

import com.educationpro.schedule.domain.EndCondition;
import com.educationpro.schedule.domain.RecurrencePattern;

import java.time.LocalDate;
import java.util.List;

public record RecurrenceDto(
    Long id,
    RecurrencePattern pattern,
    int intervalValue,
    List<Integer> daysOfWeek,
    EndCondition endCondition,
    LocalDate endDate,
    Integer occurrenceCount,
    int occurrencesGenerated
) {}
