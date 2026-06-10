package com.educationpro.repository;

import com.educationpro.domain.ScheduleOccurrence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ScheduleOccurrenceRepository extends JpaRepository<ScheduleOccurrence, Long> {

    List<ScheduleOccurrence> findByScheduleId(Long scheduleId);

    List<ScheduleOccurrence> findByOccurrenceDateBetween(LocalDate startDate, LocalDate endDate);

    @Modifying
    @Transactional
    @Query("DELETE FROM ScheduleOccurrence so WHERE so.schedule.id = :scheduleId")
    void deleteByScheduleId(@Param("scheduleId") Long scheduleId);
}
