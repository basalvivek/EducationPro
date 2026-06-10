package com.educationpro.repository;

import com.educationpro.domain.AssignmentTeacherMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentTeacherMappingRepository extends JpaRepository<AssignmentTeacherMapping, Long> {
    List<AssignmentTeacherMapping> findBySession_Id(Long sessionId);
}
