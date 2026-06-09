package com.educationpro.admin.dto;

public record StudentSummaryDto(
    Long   id,
    String studentId,
    String firstName,
    String lastName,
    String preferredName,
    String studentEmail,
    String gradeYear,
    String className,
    String section,
    String studentStatus,
    String enrollmentType,
    String programCourse,
    String profilePhotoPath,
    String admissionDate
) {}
