package com.educationpro.exam;

import com.educationpro.exam.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/exams")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService service;

    @GetMapping
    public ResponseEntity<List<ExamSummaryDto>> list() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExamDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<ExamDto> create(@Valid @RequestBody CreateExamRequest req, Authentication auth) {
        return ResponseEntity.status(201).body(service.create(req, auth.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExamDto> update(@PathVariable Long id, @Valid @RequestBody UpdateExamRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/questions")
    public ResponseEntity<ExamDto> addQuestion(@PathVariable Long id, @RequestBody AddQuestionRequest req) {
        return ResponseEntity.ok(service.addQuestion(id, req));
    }

    @DeleteMapping("/{id}/questions/{questionId}")
    public ResponseEntity<ExamDto> removeQuestion(@PathVariable Long id, @PathVariable Long questionId) {
        return ResponseEntity.ok(service.removeQuestion(id, questionId));
    }

    @PutMapping("/{id}/questions/reorder")
    public ResponseEntity<ExamDto> reorder(@PathVariable Long id, @RequestBody List<Long> orderedQuestionIds) {
        return ResponseEntity.ok(service.reorder(id, orderedQuestionIds));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ExamDto> submit(@PathVariable Long id) {
        return ResponseEntity.ok(service.submit(id));
    }
}
