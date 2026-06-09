package com.educationpro.admin;

import com.educationpro.admin.dto.StudentRegistrationRequest;
import com.educationpro.admin.dto.StudentSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/students")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class StudentManagementController {

    private final StudentManagementService service;

    @GetMapping
    public ResponseEntity<List<StudentSummaryDto>> list() {
        return ResponseEntity.ok(service.findAll());
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<StudentSummaryDto> register(
            @ModelAttribute StudentRegistrationRequest req,
            Authentication auth,
            @RequestPart(required = false) MultipartFile profilePhoto,
            @RequestPart(required = false) MultipartFile docBirth,
            @RequestPart(required = false) MultipartFile docPassport,
            @RequestPart(required = false) MultipartFile docAcademic,
            @RequestPart(required = false) MultipartFile docAddress,
            @RequestPart(required = false) MultipartFile docPhoto,
            @RequestPart(required = false) MultipartFile docGuardian) {

        return ResponseEntity.status(201).body(
            service.register(req, auth, profilePhoto, docBirth, docPassport,
                             docAcademic, docAddress, docPhoto, docGuardian));
    }
}
