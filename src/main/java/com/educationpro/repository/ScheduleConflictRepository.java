package com.educationpro.repository;

import com.educationpro.domain.ScheduleConflict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleConflictRepository extends JpaRepository<ScheduleConflict, Long> {

    List<ScheduleConflict> findByClassScheduleId(Long classScheduleId);

    List<ScheduleConflict> findByIsResolvedFalse();
}
