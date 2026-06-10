package com.educationpro.schedule.dto;

import com.educationpro.schedule.domain.DateMode;
import com.educationpro.schedule.domain.ScheduleTab;
import com.educationpro.schedule.domain.ScheduleType;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record CreateScheduleRequest(
    @NotNull ScheduleTab scheduleTab,
    Long teacherProfileId,
    Long groupId,
    Long subjectNodeId,
    ScheduleType scheduleType,
    Long assignmentSessionId,
    @NotNull DateMode dateMode,
    @NotNull LocalDate startDate,
    LocalDate endDate,
    List<LocalDate> multipleDates,
    @NotNull LocalTime startTime,
    @NotNull LocalTime endTime,
    @Size(max = 200) String topic,
    Long classroomId,
    @Size(max = 5000) String description,
    List<String> learningObjectives,
    Boolean attendanceRequired,
    Integer reminderMinutes,
    @Size(max = 200) String eventTitle,
    @Size(max = 200) String location,
    String audience,
    RecurrenceRequest recurrence,
    List<Long> equipmentIds
) {}
