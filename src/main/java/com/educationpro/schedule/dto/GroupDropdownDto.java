package com.educationpro.schedule.dto;

public record GroupDropdownDto(
    Long id,
    String groupName,
    int studentCount,
    String academicYear,
    Long sessionId
) {}
