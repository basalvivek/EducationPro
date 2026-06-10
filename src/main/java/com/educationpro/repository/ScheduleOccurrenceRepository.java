package com.educationpro.repository;

import com.educationpro.domain.ScheduleOccurrence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ScheduleOccurrenceRepository extends JpaRepository<ScheduleOccurrence, Long> {

    List<ScheduleOccurrence> findByClassScheduleId(Long classScheduleId);

    List<ScheduleOccurrence> findByOccurrenceDateBetween(LocalDate startDate, LocalDate endDate);
}
