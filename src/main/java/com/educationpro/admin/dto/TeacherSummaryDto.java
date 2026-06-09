package com.educationpro.admin.dto;

public record TeacherSummaryDto(
    Long   id,
    Long   userId,
    String teacherId,
    String firstName,
    String lastName,
    String email,
    String designation,
    String department,
    String employmentStatus,
    String employmentType,
    String profilePhotoPath,
    String joiningDate
) {}
