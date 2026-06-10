package com.educationpro.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassScheduleDto {
    private Long id;
    private Long sessionId;
    private Long teacherId;
    private String teacherName;
    private Long groupId;
    private String groupName;
    private Long subjectId;
    private String subjectName;
    private String scheduleType;
    private String dateMode;
    private LocalDate scheduleDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer durationMinutes;
    private String topic;
    private String description;
    private String learningObjectives;
    private Long classroomId;
    private String classroomName;
    private List<Long> equipmentIds;
    private Boolean enableAttendance;
    private Boolean notifyStudents;
    private Boolean notifyParents;
    private Boolean sendReminder;
    private String eventColor;
    private Long createdById;
    private LocalDateTime createdAt;
    private Long updatedById;
    private LocalDateTime updatedAt;
    private Integer version;
}
