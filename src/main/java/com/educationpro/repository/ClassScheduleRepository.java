package com.educationpro.repository;

import com.educationpro.domain.ClassSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, Long> {

    List<ClassSchedule> findByAssignmentSessionId(Long sessionId);

    List<ClassSchedule> findByTeacherId(Long teacherId);

    List<ClassSchedule> findByAssignmentGroupId(Long groupId);

    @Query("SELECT cs FROM ClassSchedule cs WHERE cs.teacher.id = :teacherId " +
           "AND cs.scheduleDate >= :startDate AND cs.scheduleDate <= :endDate")
    List<ClassSchedule> findByTeacherAndDateRange(@Param("teacherId") Long teacherId,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);
}
