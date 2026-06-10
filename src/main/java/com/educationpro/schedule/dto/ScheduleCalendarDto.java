package com.educationpro.schedule.dto;

import com.educationpro.schedule.domain.ScheduleStatus;
import com.educationpro.schedule.domain.ScheduleTab;

import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleCalendarDto(
    Long id,
    ScheduleTab scheduleTab,
    String title,
    String teacherName,
    String groupName,
    LocalDate occurrenceDate,
    LocalTime startTime,
    LocalTime endTime,
    String colorClass,
    ScheduleStatus status,
    Boolean isRecurring
) {}
