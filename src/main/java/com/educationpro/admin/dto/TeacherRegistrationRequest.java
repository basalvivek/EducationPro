package com.educationpro.admin.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TeacherRegistrationRequest {

    // Login credentials
    private String email;
    private String password;
    private boolean active = true;

    // Basic Information
    private String teacherId;
    private String firstName;
    private String middleName;
    private String lastName;
    private String gender;
    private String dateOfBirth;
    private String nationality;

    // Contact Information
    private String mobileNumber;
    private String alternatePhone;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String stateCounty;
    private String postalCode;
    private String country;

    // Employment Details
    private String employeeNumber;
    private String joiningDate;
    private String employmentType;
    private String designation;
    private String department;
    private String subjectSpecialization;
    private String reportingManager;
    private String employmentStatus;

    // Academic Qualifications
    private String highestQualification;
    private String degreeName;
    private String university;
    private String graduationYear;
    private String additionalCerts;

    // Emergency Contact
    private String emergencyContactName;
    private String emergencyRelationship;
    private String emergencyPhone;

    // Payroll
    private String bankName;
    private String accountNumber;
    private String sortCode;
    private String taxId;
    private String salaryGrade;
    private String paymentFrequency;
}
