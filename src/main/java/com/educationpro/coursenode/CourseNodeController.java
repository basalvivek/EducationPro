package com.educationpro.coursenode;

import com.educationpro.coursenode.dto.CourseNodeDto;
import com.educationpro.coursenode.dto.CreateNodeRequest;
import com.educationpro.coursenode.dto.UpdateNodeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/course-nodes")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class CourseNodeController {

    private final CourseNodeService service;

    @GetMapping
    public ResponseEntity<List<CourseNodeDto>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @PostMapping
    public ResponseEntity<CourseNodeDto> create(@Valid @RequestBody CreateNodeRequest req,
                                                 Authentication auth) {
        return ResponseEntity.status(201).body(service.addNode(req, auth.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseNodeDto> update(@PathVariable Long id,
                                                 @Valid @RequestBody UpdateNodeRequest req) {
        return ResponseEntity.ok(service.updateNode(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteNode(id);
        return ResponseEntity.noContent().build();
    }
}
