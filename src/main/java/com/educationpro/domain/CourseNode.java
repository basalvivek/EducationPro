package com.educationpro.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "course_nodes")
@Getter @Setter @NoArgsConstructor
public class CourseNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_id")
    private Long parentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NodeType type;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String tagline;

    // ── Question core ────────────────────────────────────────────────────────

    @Column(name = "question_text", columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "question_type", length = 20)
    private String questionType;

    private Short marks = 1;

    @Column(length = 20)
    private String complexity;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    // ── MCQ ─────────────────────────────────────────────────────────────────

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String options;

    @Column(name = "correct_index")
    private Short correctIndex;

    @Column(name = "correct_indices", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String correctIndices;

    @Column(name = "partial_marking", length = 20)
    private String partialMarking;

    // ── TRUE_FALSE ───────────────────────────────────────────────────────────

    @Column(name = "correct_answer", length = 5)
    private String correctAnswer;

    // ── SHORT_ANSWER / ESSAY ─────────────────────────────────────────────────

    @Column(name = "model_answer", columnDefinition = "TEXT")
    private String modelAnswer;

    @Column(name = "marking_scheme", columnDefinition = "TEXT")
    private String markingScheme;

    @Column(name = "word_limit")
    private Short wordLimit = 0;

    // ── CODE ─────────────────────────────────────────────────────────────────

    @Column(name = "code_language", length = 20)
    private String codeLanguage;

    @Column(name = "starter_code", columnDefinition = "TEXT")
    private String starterCode;

    @Column(name = "expected_output", columnDefinition = "TEXT")
    private String expectedOutput;

    // ── IMAGE_BASED ──────────────────────────────────────────────────────────

    @Column(name = "image_path", length = 500)
    private String imagePath;

    @Column(name = "image_alt", length = 255)
    private String imageAlt;

    @Column(name = "image_answer_type", length = 10)
    private String imageAnswerType;

    // ── Question metadata (for exam picker filtering) ────────────────────────

    @Column(name = "class_name", length = 100)
    private String className;

    @Column(length = 100)
    private String subject;

    @Column(name = "exam_board", length = 100)
    private String examBoard;

    @Column(length = 150)
    private String topic;

    @Column(name = "sub_topic", length = 150)
    private String subTopic;

    // ── Audit ────────────────────────────────────────────────────────────────

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
