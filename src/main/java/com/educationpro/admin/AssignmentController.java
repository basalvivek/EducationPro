package com.educationpro.admin;

import com.educationpro.admin.dto.AssignmentResultDto;
import com.educationpro.admin.dto.SaveAssignmentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/assignments")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService service;

    @PostMapping
    public ResponseEntity<AssignmentResultDto> save(
            @RequestBody SaveAssignmentRequest req,
            Authentication auth) {
        return ResponseEntity.status(201).body(service.save(req, auth.getName()));
    }
}
