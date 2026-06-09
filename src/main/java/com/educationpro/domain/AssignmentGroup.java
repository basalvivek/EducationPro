package com.educationpro.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "assignment_groups")
@Getter @Setter @NoArgsConstructor
public class AssignmentGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private AssignmentSession session;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String period;

    @ElementCollection
    @CollectionTable(
        name = "assignment_group_students",
        joinColumns = @JoinColumn(name = "group_id")
    )
    @Column(name = "student_profile_id")
    private List<Long> studentProfileIds = new ArrayList<>();
}
