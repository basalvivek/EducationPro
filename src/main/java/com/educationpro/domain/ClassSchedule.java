package com.educationpro.domain;

import com.educationpro.schedule.domain.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import io.hypersistence.utils.hibernate.type.json.JsonType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.List;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScheduleTab scheduleTab = ScheduleTab.CLASSES;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DateMode dateMode = DateMode.SINGLE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScheduleStatus status = ScheduleStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private ScheduleType scheduleType;

    @ManyToOne
    @JoinColumn(name = "assignment_session_id")
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

    @Column(nullable = false)
    private LocalDate scheduleDate;

    private LocalDate endDate;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private List<LocalDate> multipleDates;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    private Integer durationMinutes;

    @Column(length = 200)
    private String topic;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 200)
    private String eventTitle;

    @Column(length = 200)
    private String location;

    @Column(length = 20)
    private String audience;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<String> learningObjectives = new ArrayList<>();

    @Column(nullable = false)
    private Boolean attendanceRequired = false;

    @Column
    private Integer reminderMinutes;

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

    @OneToOne(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ScheduleRecurrence recurrence;

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ScheduleOccurrence> occurrences = new ArrayList<>();

    @OneToMany(mappedBy = "schedule", fetch = FetchType.LAZY)
    private List<ScheduleConflict> conflicts = new ArrayList<>();
}
