package com.educationpro.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "class_schedules")
public class ClassSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private AssignmentSession assignmentSession;

    @ManyToOne(optional = false)
    @JoinColumn(name = "teacher_profile_id", nullable = false)
    private TeacherProfile teacher;

    @ManyToOne(optional = false)
    @JoinColumn(name = "assignment_group_id", nullable = false)
    private AssignmentGroup assignmentGroup;

    @ManyToOne
    @JoinColumn(name = "subject_id")
    private CourseNode subject;

    @Column(nullable = false, length = 50)
    private String scheduleType;  // REGULAR_CLASS, REVISION_SESSION, etc.

    @Column(nullable = false, length = 20)
    private String dateMode;  // SINGLE_DAY, MULTIPLE_DAYS, RECURRING

    @Column(nullable = false)
    private LocalDate scheduleDate;

    private LocalDate endDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    private Integer durationMinutes;

    @Column(nullable = false, length = 255)
    private String topic;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String learningObjectives;  // JSON array

    @ManyToOne
    @JoinColumn(name = "classroom_id")
    private Classroom classroom;

    @Column(nullable = false)
    private Boolean enableAttendance = false;

    @Column(nullable = false)
    private Boolean notifyStudents = false;

    @Column(nullable = false)
    private Boolean notifyParents = false;

    @Column(nullable = false)
    private Boolean sendReminder = false;

    @Column(length = 20)
    private String eventColor = "blue";

    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(nullable = false)
    private Integer version = 1;

    @ManyToMany
    @JoinTable(
        name = "schedule_equipment",
        joinColumns = @JoinColumn(name = "class_schedule_id"),
        inverseJoinColumns = @JoinColumn(name = "equipment_id")
    )
    private Set<Equipment> requiredEquipment = new HashSet<>();

    @OneToOne(mappedBy = "classSchedule", cascade = CascadeType.ALL, orphanRemoval = true)
    private ScheduleRecurrence recurrence;
}
