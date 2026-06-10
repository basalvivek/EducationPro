package com.educationpro.repository;

import com.educationpro.domain.ScheduleNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleNotificationRepository extends JpaRepository<ScheduleNotification, Long> {

    List<ScheduleNotification> findByClassScheduleId(Long classScheduleId);

    List<ScheduleNotification> findByRecipientTypeAndRecipientId(String recipientType, Long recipientId);

    List<ScheduleNotification> findByIsSentFalse();
}
