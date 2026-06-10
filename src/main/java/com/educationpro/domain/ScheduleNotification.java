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
@Table(name = "schedule_notifications")
public class ScheduleNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "class_schedule_id", nullable = false)
    private ClassSchedule classSchedule;

    @ManyToOne
    @JoinColumn(name = "occurrence_id")
    private ScheduleOccurrence occurrence;

    @Column(nullable = false, length = 20)
    private String recipientType;  // TEACHER, STUDENT, PARENT

    private Long recipientId;

    @Column(nullable = false, length = 50)
    private String notificationType;  // SCHEDULE_CREATED, SCHEDULE_UPDATED, etc.

    @Column(length = 255)
    private String messageSubject;

    @Column(columnDefinition = "TEXT")
    private String messageBody;

    @Column(length = 20)
    private String notificationChannel = "EMAIL";  // EMAIL, SMS, IN_APP

    @Column(nullable = false)
    private Boolean isSent = false;

    private LocalDateTime sentAt;

    @Column(nullable = false)
    private Boolean isRead = false;

    private LocalDateTime readAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
