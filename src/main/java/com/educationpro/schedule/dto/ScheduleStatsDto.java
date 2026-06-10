package com.educationpro.schedule.dto;

public record ScheduleStatsDto(
    long totalSchedules,
    long thisWeekSchedules,
    long activeConflicts,
    long completedSchedules
) {}
