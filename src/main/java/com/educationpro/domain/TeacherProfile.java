package com.educationpro.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "teacher_profiles")
@Getter @Setter @NoArgsConstructor
public class TeacherProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // Basic Information
    @Column(name = "teacher_id")
    private String teacherId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "middle_name", length = 100)
    private String middleName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(length = 20)
    private String gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 100)
    private String nationality;

    @Column(name = "profile_photo_path", length = 500)
    private String profilePhotoPath;

    // Contact Information
    @Column(name = "mobile_number", length = 30)
    private String mobileNumber;

    @Column(name = "alternate_phone", length = 30)
    private String alternatePhone;

    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(length = 100)
    private String city;

    @Column(name = "state_county", length = 100)
    private String stateCounty;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(length = 100)
    private String country;

    // Employment Details
    @Column(name = "employee_number", length = 50)
    private String employeeNumber;

    @Column(name = "joining_date")
    private LocalDate joiningDate;

    @Column(name = "employment_type", length = 20)
    private String employmentType;

    @Column(length = 150)
    private String designation;

    @Column(length = 150)
    private String department;

    @Column(name = "subject_specialization")
    private String subjectSpecialization;

    @Column(name = "reporting_manager", length = 150)
    private String reportingManager;

    @Column(name = "employment_status", nullable = false, length = 20)
    private String employmentStatus = "ACTIVE";

    // Academic Qualifications
    @Column(name = "highest_qualification", length = 100)
    private String highestQualification;

    @Column(name = "degree_name", length = 200)
    private String degreeName;

    @Column(length = 255)
    private String university;

    @Column(name = "graduation_year")
    private Integer graduationYear;

    @Column(name = "additional_certs", columnDefinition = "TEXT")
    private String additionalCerts;

    // Emergency Contact
    @Column(name = "emergency_contact_name", length = 150)
    private String emergencyContactName;

    @Column(name = "emergency_relationship", length = 100)
    private String emergencyRelationship;

    @Column(name = "emergency_phone", length = 30)
    private String emergencyPhone;

    // Payroll Information
    @Column(name = "bank_name", length = 150)
    private String bankName;

    @Column(name = "account_number", length = 100)
    private String accountNumber;

    @Column(name = "sort_code", length = 50)
    private String sortCode;

    @Column(name = "tax_id", length = 100)
    private String taxId;

    @Column(name = "salary_grade", length = 50)
    private String salaryGrade;

    @Column(name = "payment_frequency", length = 20)
    private String paymentFrequency;

    // Document paths
    @Column(name = "doc_identity_proof", length = 500)
    private String docIdentityProof;

    @Column(name = "doc_address_proof", length = 500)
    private String docAddressProof;

    @Column(name = "doc_qualification", length = 500)
    private String docQualification;

    @Column(name = "doc_teaching_license", length = 500)
    private String docTeachingLicense;

    @Column(name = "doc_employment_contract", length = 500)
    private String docEmploymentContract;

    @Column(name = "doc_background_check", length = 500)
    private String docBackgroundCheck;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
