package com.educationpro.repository;

import com.educationpro.domain.ClassSchedule;
import com.educationpro.schedule.domain.ScheduleStatus;
import com.educationpro.schedule.domain.ScheduleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
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

    @Query("SELECT cs FROM ClassSchedule cs WHERE cs.scheduleDate >= :from AND cs.scheduleDate <= :to " +
           "AND (:teacherId IS NULL OR cs.teacher.id = :teacherId) " +
           "AND (:groupId IS NULL OR cs.assignmentGroup.id = :groupId) " +
           "AND (:type IS NULL OR cs.scheduleType = :type) " +
           "AND (:status IS NULL OR cs.status = :status) " +
           "AND cs.status != com.educationpro.schedule.domain.ScheduleStatus.CANCELLED")
    List<ClassSchedule> findByDateRangeAndFilters(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("teacherId") Long teacherId,
            @Param("groupId") Long groupId,
            @Param("type") ScheduleType type,
            @Param("status") ScheduleStatus status);

    @Query("SELECT cs FROM ClassSchedule cs WHERE cs.teacher.id = :teacherId " +
           "AND cs.scheduleDate = :date " +
           "AND ((:startTime < cs.endTime AND :endTime > cs.startTime)) " +
           "AND cs.status != com.educationpro.schedule.domain.ScheduleStatus.CANCELLED")
    List<ClassSchedule> findTeacherConflicts(@Param("teacherId") Long teacherId,
                                            @Param("date") LocalDate date,
                                            @Param("startTime") LocalTime startTime,
                                            @Param("endTime") LocalTime endTime);

    @Query("SELECT cs FROM ClassSchedule cs WHERE cs.assignmentGroup.id = :groupId " +
           "AND cs.scheduleDate = :date " +
           "AND ((:startTime < cs.endTime AND :endTime > cs.startTime)) " +
           "AND cs.status != com.educationpro.schedule.domain.ScheduleStatus.CANCELLED")
    List<ClassSchedule> findGroupConflicts(@Param("groupId") Long groupId,
                                          @Param("date") LocalDate date,
                                          @Param("startTime") LocalTime startTime,
                                          @Param("endTime") LocalTime endTime);

    @Query("SELECT cs FROM ClassSchedule cs WHERE cs.classroom.id = :classroomId " +
           "AND cs.scheduleDate = :date " +
           "AND ((:startTime < cs.endTime AND :endTime > cs.startTime)) " +
           "AND cs.status != com.educationpro.schedule.domain.ScheduleStatus.CANCELLED")
    List<ClassSchedule> findClassroomConflicts(@Param("classroomId") Long classroomId,
                                              @Param("date") LocalDate date,
                                              @Param("startTime") LocalTime startTime,
                                              @Param("endTime") LocalTime endTime);

    @Query("SELECT COUNT(cs) FROM ClassSchedule cs WHERE cs.scheduleDate >= :from AND cs.scheduleDate <= :to")
    long countByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    long countByStatus(ScheduleStatus status);
}
