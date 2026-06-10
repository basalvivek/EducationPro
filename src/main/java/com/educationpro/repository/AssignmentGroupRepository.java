package com.educationpro.repository;

import com.educationpro.domain.AssignmentGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AssignmentGroupRepository extends JpaRepository<AssignmentGroup, Long> {
    List<AssignmentGroup> findBySession_Id(Long sessionId);

    @Query("SELECT DISTINCT ag FROM AssignmentGroup ag " +
           "JOIN AssignmentTeacherMapping atm ON atm.groupId = ag.id " +
           "WHERE atm.teacherProfile.id = :teacherId AND ag.session.status = 'SAVED'")
    List<AssignmentGroup> findGroupsForTeacher(@Param("teacherId") Long teacherId);
}
