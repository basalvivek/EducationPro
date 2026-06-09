package com.educationpro.admin;

import com.educationpro.admin.dto.StudentRegistrationRequest;
import com.educationpro.admin.dto.StudentSummaryDto;
import com.educationpro.domain.Role;
import com.educationpro.domain.StudentProfile;
import com.educationpro.domain.User;
import com.educationpro.exception.BusinessException;
import com.educationpro.repository.StudentProfileRepository;
import com.educationpro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class StudentManagementService {

    private final UserRepository userRepo;
    private final StudentProfileRepository profileRepo;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public StudentSummaryDto register(StudentRegistrationRequest req,
                                      Authentication auth,
                                      MultipartFile profilePhoto,
                                      MultipartFile docBirth,
                                      MultipartFile docPassport,
                                      MultipartFile docAcademic,
                                      MultipartFile docAddress,
                                      MultipartFile docPhoto,
                                      MultipartFile docGuardian) {

        StudentProfile p = new StudentProfile();

        // ── Student user account ──────────────────────────────────────────────
        String email = req.getLoginEmail() != null ? req.getLoginEmail().trim()
                     : (req.getStudentEmail() != null ? req.getStudentEmail().trim() : null);
        if (email != null && !email.isBlank() && req.getLoginPassword() != null && !req.getLoginPassword().isBlank()) {
            if (userRepo.findByEmail(email).isPresent()) {
                throw new BusinessException("Student email already registered: " + email);
            }
            User student = new User();
            student.setEmail(email);
            student.setPasswordHash(passwordEncoder.encode(req.getLoginPassword()));
            student.setFullName(req.getFirstName() + (req.getLastName() != null ? " " + req.getLastName() : ""));
            student.setRole(Role.STUDENT);
            student.setActive(req.isStudentPortalAccess());
            p.setUser(userRepo.save(student));
        }

        // ── Parent user account ───────────────────────────────────────────────
        if (req.getParentEmail() != null && !req.getParentEmail().isBlank()
                && req.getParentPassword() != null && !req.getParentPassword().isBlank()) {
            if (userRepo.findByEmail(req.getParentEmail()).isEmpty()) {
                User parent = new User();
                parent.setEmail(req.getParentEmail().trim());
                parent.setPasswordHash(passwordEncoder.encode(req.getParentPassword()));
                parent.setFullName(req.getGuardianName() != null ? req.getGuardianName() : "Parent");
                parent.setRole(Role.PARENT);
                parent.setActive(req.isParentPortalAccess());
                p.setParentUser(userRepo.save(parent));
            }
        }

        // ── Audit: created by ─────────────────────────────────────────────────
        if (auth != null) {
            userRepo.findByEmail(auth.getName()).ifPresent(p::setCreatedBy);
        }

        // ── 1. Student Information ────────────────────────────────────────────
        p.setStudentId(req.getStudentId());
        p.setFirstName(req.getFirstName());
        p.setMiddleName(req.getMiddleName());
        p.setLastName(req.getLastName());
        p.setPreferredName(req.getPreferredName());
        p.setGender(req.getGender());
        p.setNationality(req.getNationality());
        parseDate(req.getDateOfBirth(), p, "dob");

        // ── 2. Contact ────────────────────────────────────────────────────────
        p.setStudentEmail(req.getStudentEmail());
        p.setMobileNumber(req.getMobileNumber());
        p.setAddressLine1(req.getAddressLine1());
        p.setAddressLine2(req.getAddressLine2());
        p.setCity(req.getCity());
        p.setStateCounty(req.getStateCounty());
        p.setPostalCode(req.getPostalCode());
        p.setCountry(req.getCountry());

        // ── 3. Parent / Guardian ──────────────────────────────────────────────
        p.setGuardianName(req.getGuardianName());
        p.setRelationship(req.getRelationship());
        p.setParentEmail(req.getParentEmail());
        p.setParentMobile(req.getParentMobile());
        p.setAlternateContact(req.getAlternateContact());
        p.setOccupation(req.getOccupation());
        p.setEmergencyContactNumber(req.getEmergencyContactNumber());

        // ── 4. Admission ──────────────────────────────────────────────────────
        p.setAdmissionNumber(req.getAdmissionNumber());
        parseDate(req.getAdmissionDate(), p, "admission");
        p.setAcademicYear(req.getAcademicYear());
        p.setCampus(req.getCampus());
        p.setProgramCourse(req.getProgramCourse());
        p.setGradeYear(req.getGradeYear());
        p.setClassName(req.getClassName());
        p.setSection(req.getSection());
        p.setStudentStatus(req.getStudentStatus() != null ? req.getStudentStatus() : "ACTIVE");

        // ── 5. Academic ───────────────────────────────────────────────────────
        p.setPreviousSchool(req.getPreviousSchool());
        p.setPreviousQualification(req.getPreviousQualification());
        p.setSubjectsSelected(req.getSubjectsSelected());
        p.setMediumOfInstruction(req.getMediumOfInstruction());
        p.setEnrollmentType(req.getEnrollmentType());

        // ── 7. Medical ────────────────────────────────────────────────────────
        p.setBloodGroup(req.getBloodGroup());
        p.setAllergies(req.getAllergies());
        p.setMedicalConditions(req.getMedicalConditions());
        p.setEmergencyMedicalNotes(req.getEmergencyMedicalNotes());
        p.setDoctorContact(req.getDoctorContact());

        // ── 8. Fee ────────────────────────────────────────────────────────────
        p.setFeeCategory(req.getFeeCategory());
        p.setScholarshipStatus(req.getScholarshipStatus());
        p.setDiscountWaiver(req.getDiscountWaiver());
        p.setSponsorInfo(req.getSponsorInfo());

        // ── Files ─────────────────────────────────────────────────────────────
        String base = uploadDir + "/students/" + req.getFirstName() + "_" + System.currentTimeMillis() + "/";
        p.setProfilePhotoPath(saveFile(profilePhoto, base, "photo"));
        p.setDocBirthCertificate(saveFile(docBirth,    base, "birth"));
        p.setDocPassportId(saveFile(docPassport,       base, "passport"));
        p.setDocAcademicRecords(saveFile(docAcademic,  base, "academic"));
        p.setDocAddressProof(saveFile(docAddress,      base, "address"));
        p.setDocPhotograph(saveFile(docPhoto,           base, "photo2"));
        p.setDocGuardianId(saveFile(docGuardian,       base, "guardian"));

        return toSummary(profileRepo.save(p));
    }

    @Transactional(readOnly = true)
    public List<StudentSummaryDto> findAll() {
        return profileRepo.findAllWithUser().stream().map(this::toSummary).toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void parseDate(String val, StudentProfile p, String field) {
        if (val == null || val.isBlank()) return;
        try {
            LocalDate d = LocalDate.parse(val);
            if ("dob".equals(field))       p.setDateOfBirth(d);
            if ("admission".equals(field)) p.setAdmissionDate(d);
        } catch (Exception ignored) {}
    }

    private String saveFile(MultipartFile file, String baseDir, String prefix) {
        if (file == null || file.isEmpty()) return null;
        try {
            Path dir = Paths.get(baseDir);
            Files.createDirectories(dir);
            String ext  = getExt(file.getOriginalFilename());
            String name = prefix + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
            file.transferTo(dir.resolve(name));
            return dir.resolve(name).toString();
        } catch (IOException e) {
            return null;
        }
    }

    private String getExt(String fn) {
        if (fn == null || !fn.contains(".")) return "";
        return fn.substring(fn.lastIndexOf('.'));
    }

    private StudentSummaryDto toSummary(StudentProfile p) {
        return new StudentSummaryDto(
            p.getId(), p.getStudentId(),
            p.getFirstName(), p.getLastName(), p.getPreferredName(),
            p.getStudentEmail(), p.getGradeYear(), p.getClassName(), p.getSection(),
            p.getStudentStatus(), p.getEnrollmentType(), p.getProgramCourse(),
            p.getProfilePhotoPath(),
            p.getAdmissionDate() != null ? p.getAdmissionDate().toString() : null
        );
    }
}
