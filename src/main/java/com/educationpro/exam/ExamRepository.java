package com.educationpro.exam;

import com.educationpro.domain.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    @Query("SELECT e FROM Exam e ORDER BY e.createdAt DESC")
    List<Exam> findAllOrderByCreatedAtDesc();
}
