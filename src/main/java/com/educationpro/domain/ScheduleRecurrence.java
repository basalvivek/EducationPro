package com.educationpro.domain;

import com.educationpro.schedule.domain.EndCondition;
import com.educationpro.schedule.domain.RecurrencePattern;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private ClassSchedule schedule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecurrencePattern pattern;

    @Column(nullable = false)
    private int intervalValue = 1;

    @Column(columnDefinition = "integer[]")
    private List<Integer> daysOfWeek;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EndCondition endCondition = EndCondition.NEVER;

    private LocalDate endDate;

    private Integer occurrenceCount;

    @Column(nullable = false)
    private int occurrencesGenerated = 0;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
