package com.educationpro.exam;

import com.educationpro.domain.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    @Query("SELECT e FROM Exam e ORDER BY e.createdAt DESC")
    List<Exam> findAllOrderByCreatedAtDesc();

    @Query("SELECT e FROM Exam e JOIN FETCH e.createdBy WHERE e.status = :status ORDER BY e.createdAt ASC")
    List<Exam> findByStatusWithCreator(@Param("status") String status);
}
