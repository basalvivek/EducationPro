package com.educationpro.repository;

import com.educationpro.domain.ScheduleRecurrence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduleRecurrenceRepository extends JpaRepository<ScheduleRecurrence, Long> {
}
