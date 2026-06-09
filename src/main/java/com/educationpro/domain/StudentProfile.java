package com.educationpro.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "student_profiles")
@Getter @Setter @NoArgsConstructor
public class StudentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_user_id")
    private User parentUser;

    // 1. Student Information
    @Column(name = "student_id",   length = 50)  private String studentId;
    @Column(name = "first_name",   nullable = false, length = 100) private String firstName;
    @Column(name = "middle_name",  length = 100) private String middleName;
    @Column(name = "last_name",    nullable = false, length = 100) private String lastName;
    @Column(name = "preferred_name", length = 100) private String preferredName;
    @Column(length = 20)           private String gender;
    @Column(name = "date_of_birth") private LocalDate dateOfBirth;
    @Column(length = 100)          private String nationality;
    @Column(name = "profile_photo_path", length = 500) private String profilePhotoPath;

    // 2. Contact Information
    @Column(name = "student_email") private String studentEmail;
    @Column(name = "mobile_number", length = 30) private String mobileNumber;
    @Column(name = "address_line1") private String addressLine1;
    @Column(name = "address_line2") private String addressLine2;
    @Column(length = 100)           private String city;
    @Column(name = "state_county",  length = 100) private String stateCounty;
    @Column(name = "postal_code",   length = 20)  private String postalCode;
    @Column(length = 100)           private String country;

    // 3. Parent / Guardian Information
    @Column(name = "guardian_name", length = 150) private String guardianName;
    @Column(length = 50)            private String relationship;
    @Column(name = "parent_email")  private String parentEmail;
    @Column(name = "parent_mobile", length = 30)  private String parentMobile;
    @Column(name = "alternate_contact", length = 30) private String alternateContact;
    @Column(length = 150)           private String occupation;
    @Column(name = "emergency_contact_number", length = 30) private String emergencyContactNumber;

    // 4. Admission Information
    @Column(name = "admission_number", length = 50) private String admissionNumber;
    @Column(name = "admission_date")   private LocalDate admissionDate;
    @Column(name = "academic_year",    length = 20) private String academicYear;
    @Column(length = 150)              private String campus;
    @Column(name = "program_course")   private String programCourse;
    @Column(name = "grade_year",       length = 50) private String gradeYear;
    @Column(name = "class_name",       length = 50) private String className;
    @Column(length = 20)               private String section;
    @Column(name = "student_status",   nullable = false, length = 20)
    private String studentStatus = "ACTIVE";

    // 5. Academic Information
    @Column(name = "previous_school")         private String previousSchool;
    @Column(name = "previous_qualification")  private String previousQualification;
    @Column(name = "subjects_selected", columnDefinition = "TEXT") private String subjectsSelected;
    @Column(name = "medium_of_instruction", length = 100) private String mediumOfInstruction;
    @Column(name = "enrollment_type",       length = 20)  private String enrollmentType;

    // 7. Medical Information
    @Column(name = "blood_group",  length = 10)  private String bloodGroup;
    @Column(columnDefinition = "TEXT")            private String allergies;
    @Column(name = "medical_conditions", columnDefinition = "TEXT") private String medicalConditions;
    @Column(name = "emergency_medical_notes", columnDefinition = "TEXT") private String emergencyMedicalNotes;
    @Column(name = "doctor_contact")             private String doctorContact;

    // 8. Fee & Financial Information
    @Column(name = "fee_category",     length = 100) private String feeCategory;
    @Column(name = "scholarship_status", length = 100) private String scholarshipStatus;
    @Column(name = "discount_waiver",  length = 100) private String discountWaiver;
    @Column(name = "sponsor_info", columnDefinition = "TEXT") private String sponsorInfo;

    // 9. Documents
    @Column(name = "doc_birth_certificate", length = 500) private String docBirthCertificate;
    @Column(name = "doc_passport_id",       length = 500) private String docPassportId;
    @Column(name = "doc_academic_records",  length = 500) private String docAcademicRecords;
    @Column(name = "doc_address_proof",     length = 500) private String docAddressProof;
    @Column(name = "doc_photograph",        length = 500) private String docPhotograph;
    @Column(name = "doc_guardian_id",       length = 500) private String docGuardianId;

    // 10. Audit
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_updated_by_id")
    private User lastUpdatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
