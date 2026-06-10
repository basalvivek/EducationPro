package com.educationpro.repository;

import com.educationpro.domain.AssignmentTeacherMapping;
import com.educationpro.domain.TeacherProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AssignmentTeacherMappingRepository extends JpaRepository<AssignmentTeacherMapping, Long> {
    List<AssignmentTeacherMapping> findBySession_Id(Long sessionId);

    @Query("SELECT atm FROM AssignmentTeacherMapping atm " +
           "WHERE atm.teacherProfile.id = :teacherId AND atm.groupId = :groupId " +
           "AND atm.session.status = 'SAVED'")
    AssignmentTeacherMapping findByTeacherAndGroup(@Param("teacherId") Long teacherId, @Param("groupId") Long groupId);

    @Query("SELECT DISTINCT atm.teacherProfile FROM AssignmentTeacherMapping atm " +
           "WHERE atm.session.status = 'SAVED'")
    List<TeacherProfile> findDistinctTeachersByActiveSessions();
}
