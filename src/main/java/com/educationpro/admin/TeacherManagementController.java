package com.educationpro.admin;

import com.educationpro.admin.dto.TeacherRegistrationRequest;
import com.educationpro.admin.dto.TeacherSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/teachers")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class TeacherManagementController {

    private final TeacherManagementService service;

    @GetMapping
    public ResponseEntity<List<TeacherSummaryDto>> list() {
        return ResponseEntity.ok(service.findAll());
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<TeacherSummaryDto> register(
            @ModelAttribute TeacherRegistrationRequest req,
            @RequestPart(required = false) MultipartFile profilePhoto,
            @RequestPart(required = false) MultipartFile docIdentity,
            @RequestPart(required = false) MultipartFile docAddress,
            @RequestPart(required = false) MultipartFile docQualification,
            @RequestPart(required = false) MultipartFile docLicense,
            @RequestPart(required = false) MultipartFile docContract,
            @RequestPart(required = false) MultipartFile docBackground) {

        return ResponseEntity.status(201).body(
            service.register(req, profilePhoto, docIdentity, docAddress,
                             docQualification, docLicense, docContract, docBackground));
    }
}
