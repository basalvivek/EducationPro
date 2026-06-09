package com.educationpro.exam;

import com.educationpro.domain.ExamQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, Long> {

    List<ExamQuestion> findByExamIdOrderByPositionAsc(Long examId);

    boolean existsByExamIdAndQuestionId(Long examId, Long questionId);

    @Modifying
    @Query("DELETE FROM ExamQuestion eq WHERE eq.exam.id = :examId AND eq.question.id = :questionId")
    void deleteByExamIdAndQuestionId(@Param("examId") Long examId, @Param("questionId") Long questionId);
}
