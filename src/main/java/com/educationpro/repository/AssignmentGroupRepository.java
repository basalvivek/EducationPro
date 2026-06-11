package com.educationpro.repository;

import com.educationpro.domain.AssignmentGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AssignmentGroupRepository extends JpaRepository<AssignmentGroup, Long> {
    List<AssignmentGroup> findBySession_Id(Long sessionId);

    @Query("SELECT DISTINCT ag FROM AssignmentGroup ag " +
           "JOIN AssignmentTeacherMapping atm ON atm.group.id = ag.id " +
           "WHERE atm.teacherProfileId = :teacherId AND ag.session.status = 'SAVED'")
    List<AssignmentGroup> findGroupsForTeacher(@Param("teacherId") Long teacherId);
}
