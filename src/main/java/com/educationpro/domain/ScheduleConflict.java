package com.educationpro.domain;

import com.educationpro.schedule.domain.ConflictType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

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

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "class_schedule_id", nullable = false)
    private ClassSchedule schedule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ConflictType conflictType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String conflictDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conflicting_schedule_id")
    private ClassSchedule conflictingSchedule;

    @Column(length = 20)
    private String severity = "WARNING";

    @Column(nullable = false)
    private Boolean isResolved = false;

    @Column(columnDefinition = "TEXT")
    private String resolutionNote;

    @Column(nullable = false, updatable = false)
    private Instant detectedAt = Instant.now();

    private Instant resolvedAt;
}
