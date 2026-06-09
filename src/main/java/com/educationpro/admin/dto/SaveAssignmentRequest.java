package com.educationpro.admin.dto;

import lombok.Data;

import java.util.List;

@Data
public class SaveAssignmentRequest {

    private Long courseNodeId;
    private Long scopeNodeId;
    private String scopeLevel;
    private int maxPerGroup;
    private String status;
    private List<GroupRequest> groups;
    private List<TeacherAssignmentRequest> teacherAssignments;

    @Data
    public static class GroupRequest {
        private Integer localId;
        private String name;
        private String description;
        private String period;
        private List<Long> studentIds;
    }

    @Data
    public static class TeacherAssignmentRequest {
        private Long teacherId;
        private Integer groupLocalId;
    }
}
