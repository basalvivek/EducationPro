package com.educationpro.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "schedule_recurrence")
public class ScheduleRecurrence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "class_schedule_id", nullable = false, unique = true)
    private ClassSchedule classSchedule;

    @Column(nullable = false, length = 20)
    private String frequency;  // DAILY, WEEKLY, MONTHLY, CUSTOM

    // Weekly recurrence
    @Column(nullable = false)
    private Boolean monday = false;

    @Column(nullable = false)
    private Boolean tuesday = false;

    @Column(nullable = false)
    private Boolean wednesday = false;

    @Column(nullable = false)
    private Boolean thursday = false;

    @Column(nullable = false)
    private Boolean friday = false;

    @Column(nullable = false)
    private Boolean saturday = false;

    @Column(nullable = false)
    private Boolean sunday = false;

    // End condition
    @Column(length = 20)
    private String endCondition;  // NEVER_ENDS, UNTIL_DATE, NUM_OCCURRENCES

    private LocalDate endDate;

    private Integer numOccurrences;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
