package com.educationpro.schedule.dto;

import com.educationpro.schedule.domain.ConflictType;

import java.time.LocalDate;
import java.time.LocalTime;

public record ConflictSummaryDto(
    ConflictType conflictType,
    Long conflictingScheduleId,
    String conflictingScheduleTitle,
    LocalDate conflictDate,
    LocalTime conflictStartTime,
    LocalTime conflictEndTime,
    String message
) {}
