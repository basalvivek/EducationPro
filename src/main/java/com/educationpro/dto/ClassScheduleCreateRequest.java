package com.educationpro.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassScheduleCreateRequest {
    private Long sessionId;
    private Long teacherId;
    private Long groupId;
    private Long subjectId;
    private String scheduleType;
    private String dateMode;
    private LocalDate scheduleDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String topic;
    private String description;
    private String learningObjectives;
    private Long classroomId;
    private List<Long> equipmentIds;
    private Boolean enableAttendance;
    private Boolean notifyStudents;
    private Boolean notifyParents;
    private Boolean sendReminder;
    private String eventColor;
    private RecurrenceRequest recurrence;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecurrenceRequest {
        private String frequency;
        private Boolean monday;
        private Boolean tuesday;
        private Boolean wednesday;
        private Boolean thursday;
        private Boolean friday;
        private Boolean saturday;
        private Boolean sunday;
        private String endCondition;
        private LocalDate endDate;
        private Integer numOccurrences;
    }
}
