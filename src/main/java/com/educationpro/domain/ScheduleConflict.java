package com.educationpro.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "schedule_conflicts")
public class ScheduleConflict {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "class_schedule_id", nullable = false)
    private ClassSchedule classSchedule;

    @Column(nullable = false, length = 50)
    private String conflictType;  // TEACHER_CONFLICT, GROUP_CONFLICT, CLASSROOM_CONFLICT

    @Column(nullable = false, columnDefinition = "TEXT")
    private String conflictDescription;

    @ManyToOne
    @JoinColumn(name = "conflicting_schedule_id")
    private ClassSchedule conflictingSchedule;

    @Column(length = 20)
    private String severity = "WARNING";  // INFO, WARNING, ERROR

    @Column(nullable = false)
    private Boolean isResolved = false;

    @Column(columnDefinition = "TEXT")
    private String resolutionNote;

    @Column(nullable = false, updatable = false)
    private LocalDateTime detectedAt = LocalDateTime.now();

    private LocalDateTime resolvedAt;
}
