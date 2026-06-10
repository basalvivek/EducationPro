package com.educationpro.admin.dto;

import java.util.List;

public record AssignmentSessionDetailDto(
        Long sessionId,
        Long courseNodeId,
        Long scopeNodeId,
        String scopeLevel,
        int maxPerGroup,
        String status,
        List<GroupDetail> groups,
        List<TeacherAssignmentDetail> teacherAssignments
) {
    public record GroupDetail(Long id, String name, String description, String period, List<Long> studentIds) {}
    public record TeacherAssignmentDetail(Long teacherId, Long groupId) {}
}
