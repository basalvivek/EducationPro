package com.educationpro.admin;

import com.educationpro.admin.dto.TeacherRegistrationRequest;
import com.educationpro.admin.dto.TeacherSummaryDto;
import com.educationpro.domain.Role;
import com.educationpro.domain.TeacherProfile;
import com.educationpro.domain.User;
import com.educationpro.exception.BusinessException;
import com.educationpro.repository.TeacherProfileRepository;
import com.educationpro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
public class TeacherManagementService {

    private final UserRepository userRepo;
    private final TeacherProfileRepository profileRepo;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public TeacherSummaryDto register(TeacherRegistrationRequest req,
                                      MultipartFile profilePhoto,
                                      MultipartFile docIdentity,
                                      MultipartFile docAddress,
                                      MultipartFile docQualification,
                                      MultipartFile docLicense,
                                      MultipartFile docContract,
                                      MultipartFile docBackground) {
        if (userRepo.findByEmail(req.getEmail()).isPresent()) {
            throw new BusinessException("Email already registered: " + req.getEmail());
        }

        User user = new User();
        user.setEmail(req.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setFullName(req.getFirstName() + (req.getLastName() != null ? " " + req.getLastName() : ""));
        user.setRole(Role.TEACHER);
        user.setActive(req.isActive());
        user = userRepo.save(user);

        TeacherProfile p = new TeacherProfile();
        p.setUser(user);

        // Basic
        p.setTeacherId(req.getTeacherId());
        p.setFirstName(req.getFirstName());
        p.setMiddleName(req.getMiddleName());
        p.setLastName(req.getLastName());
        p.setGender(req.getGender());
        p.setNationality(req.getNationality());
        if (req.getDateOfBirth() != null && !req.getDateOfBirth().isBlank()) {
            p.setDateOfBirth(LocalDate.parse(req.getDateOfBirth()));
        }

        // Contact
        p.setMobileNumber(req.getMobileNumber());
        p.setAlternatePhone(req.getAlternatePhone());
        p.setAddressLine1(req.getAddressLine1());
        p.setAddressLine2(req.getAddressLine2());
        p.setCity(req.getCity());
        p.setStateCounty(req.getStateCounty());
        p.setPostalCode(req.getPostalCode());
        p.setCountry(req.getCountry());

        // Employment
        p.setEmployeeNumber(req.getEmployeeNumber());
        if (req.getJoiningDate() != null && !req.getJoiningDate().isBlank()) {
            p.setJoiningDate(LocalDate.parse(req.getJoiningDate()));
        }
        p.setEmploymentType(req.getEmploymentType());
        p.setDesignation(req.getDesignation());
        p.setDepartment(req.getDepartment());
        p.setSubjectSpecialization(req.getSubjectSpecialization());
        p.setReportingManager(req.getReportingManager());
        p.setEmploymentStatus(req.getEmploymentStatus() != null ? req.getEmploymentStatus() : "ACTIVE");

        // Academic
        p.setHighestQualification(req.getHighestQualification());
        p.setDegreeName(req.getDegreeName());
        p.setUniversity(req.getUniversity());
        if (req.getGraduationYear() != null && !req.getGraduationYear().isBlank()) {
            p.setGraduationYear(Integer.parseInt(req.getGraduationYear()));
        }
        p.setAdditionalCerts(req.getAdditionalCerts());

        // Emergency
        p.setEmergencyContactName(req.getEmergencyContactName());
        p.setEmergencyRelationship(req.getEmergencyRelationship());
        p.setEmergencyPhone(req.getEmergencyPhone());

        // Payroll
        p.setBankName(req.getBankName());
        p.setAccountNumber(req.getAccountNumber());
        p.setSortCode(req.getSortCode());
        p.setTaxId(req.getTaxId());
        p.setSalaryGrade(req.getSalaryGrade());
        p.setPaymentFrequency(req.getPaymentFrequency());

        // Files
        String base = uploadDir + "/teachers/" + user.getId() + "/";
        p.setProfilePhotoPath(saveFile(profilePhoto, base, "photo"));
        p.setDocIdentityProof(saveFile(docIdentity,     base, "doc_identity"));
        p.setDocAddressProof(saveFile(docAddress,       base, "doc_address"));
        p.setDocQualification(saveFile(docQualification, base, "doc_qualification"));
        p.setDocTeachingLicense(saveFile(docLicense,    base, "doc_license"));
        p.setDocEmploymentContract(saveFile(docContract, base, "doc_contract"));
        p.setDocBackgroundCheck(saveFile(docBackground, base, "doc_background"));

        p = profileRepo.save(p);
        return toSummary(p);
    }

    @Transactional(readOnly = true)
    public List<TeacherSummaryDto> findAll() {
        return profileRepo.findAllWithUser().stream().map(this::toSummary).toList();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String saveFile(MultipartFile file, String baseDir, String prefix) {
        if (file == null || file.isEmpty()) return null;
        try {
            Path dir = Paths.get(baseDir);
            Files.createDirectories(dir);
            String ext = getExtension(file.getOriginalFilename());
            String name = prefix + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
            Path target = dir.resolve(name);
            file.transferTo(target);
            return target.toString();
        } catch (IOException e) {
            return null;
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.'));
    }

    private TeacherSummaryDto toSummary(TeacherProfile p) {
        return new TeacherSummaryDto(
            p.getId(),
            p.getUser().getId(),
            p.getTeacherId(),
            p.getFirstName(),
            p.getLastName(),
            p.getUser().getEmail(),
            p.getDesignation(),
            p.getDepartment(),
            p.getEmploymentStatus(),
            p.getEmploymentType(),
            p.getProfilePhotoPath(),
            p.getJoiningDate() != null ? p.getJoiningDate().toString() : null
        );
    }
}
