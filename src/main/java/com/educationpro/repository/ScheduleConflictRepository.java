package com.educationpro.repository;

import com.educationpro.domain.ScheduleConflict;
import com.educationpro.schedule.domain.ConflictType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleConflictRepository extends JpaRepository<ScheduleConflict, Long> {

    List<ScheduleConflict> findByScheduleId(Long scheduleId);

    List<ScheduleConflict> findByIsResolvedFalse();

    List<ScheduleConflict> findByIsResolvedFalseAndConflictType(ConflictType conflictType);

    long countByIsResolvedFalse();
}
