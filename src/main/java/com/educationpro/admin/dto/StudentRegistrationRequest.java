package com.educationpro.admin.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class StudentRegistrationRequest {

    // 1. Student Information
    private String studentId;
    private String firstName;
    private String middleName;
    private String lastName;
    private String preferredName;
    private String gender;
    private String dateOfBirth;
    private String nationality;

    // 2. Contact Information
    private String studentEmail;
    private String mobileNumber;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String stateCounty;
    private String postalCode;
    private String country;

    // 3. Parent / Guardian
    private String guardianName;
    private String relationship;
    private String parentEmail;
    private String parentPassword;
    private String parentMobile;
    private String alternateContact;
    private String occupation;
    private String emergencyContactNumber;
    private boolean parentPortalAccess;

    // 4. Admission Information
    private String admissionNumber;
    private String admissionDate;
    private String academicYear;
    private String campus;
    private String programCourse;
    private String gradeYear;
    private String className;
    private String section;
    private String studentStatus;

    // 5. Academic Information
    private String previousSchool;
    private String previousQualification;
    private String subjectsSelected;
    private String mediumOfInstruction;
    private String enrollmentType;

    // 6. Login & Portal Access
    private String loginEmail;
    private String loginPassword;
    private boolean studentPortalAccess;

    // 7. Medical Information
    private String bloodGroup;
    private String allergies;
    private String medicalConditions;
    private String emergencyMedicalNotes;
    private String doctorContact;

    // 8. Fee & Financial Information
    private String feeCategory;
    private String scholarshipStatus;
    private String discountWaiver;
    private String sponsorInfo;
}
