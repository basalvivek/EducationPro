package com.educationpro.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "assignment_teacher_mappings")
@Getter @Setter @NoArgsConstructor
public class AssignmentTeacherMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private AssignmentSession session;

    @Column(name = "teacher_profile_id", nullable = false)
    private Long teacherProfileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private AssignmentGroup group;
}
