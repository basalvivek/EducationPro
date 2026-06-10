package com.educationpro.repository;

import com.educationpro.domain.AssignmentSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssignmentSessionRepository extends JpaRepository<AssignmentSession, Long> {
    Optional<AssignmentSession> findTopByOrderByIdDesc();
}
