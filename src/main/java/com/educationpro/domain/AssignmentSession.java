package com.educationpro.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "assignment_sessions")
@Getter @Setter @NoArgsConstructor
public class AssignmentSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_node_id", nullable = false)
    private CourseNode courseNode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scope_node_id")
    private CourseNode scopeNode;

    @Column(name = "scope_level", nullable = false, length = 20)
    private String scopeLevel;

    @Column(name = "max_per_group", nullable = false)
    private int maxPerGroup = 30;

    @Column(nullable = false, length = 20)
    private String status = "DRAFT";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
