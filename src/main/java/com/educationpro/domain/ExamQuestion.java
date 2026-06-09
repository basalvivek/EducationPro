package com.educationpro.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "exam_questions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"exam_id", "question_id"}))
@Getter @Setter @NoArgsConstructor
public class ExamQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private CourseNode question;

    @Column(nullable = false)
    private int position = 0;

    @Column(name = "marks_override")
    private Integer marksOverride;
}
