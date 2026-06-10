package com.educationpro.schedule.dto;

import com.educationpro.schedule.domain.DateMode;
import com.educationpro.schedule.domain.ScheduleStatus;
import com.educationpro.schedule.domain.ScheduleTab;
import com.educationpro.schedule.domain.ScheduleType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record ScheduleResponseDto(
    Long id,
    ScheduleTab scheduleTab,
    String teacherName,
    String groupName,
    Integer studentCount,
    String subjectName,
    ScheduleType scheduleType,
    String scheduleTypeName,
    DateMode dateMode,
    LocalDate startDate,
    LocalDate endDate,
    LocalTime startTime,
    LocalTime endTime,
    String durationLabel,
    String topic,
    String classroomName,
    String description,
    List<String> learningObjectives,
    Boolean attendanceRequired,
    ScheduleStatus status,
    Boolean isRecurring,
    RecurrenceDto recurrence,
    Integer occurrenceCount,
    Boolean hasConflict,
    List<ConflictSummaryDto> conflicts,
    Instant createdAt
) {}
