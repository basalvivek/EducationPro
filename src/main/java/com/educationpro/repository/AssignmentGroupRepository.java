package com.educationpro.repository;

import com.educationpro.domain.AssignmentGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentGroupRepository extends JpaRepository<AssignmentGroup, Long> {
    List<AssignmentGroup> findBySession_Id(Long sessionId);
}
