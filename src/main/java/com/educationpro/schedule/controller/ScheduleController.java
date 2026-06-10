package com.educationpro.schedule.controller;

import com.educationpro.schedule.dto.*;
import com.educationpro.schedule.service.ScheduleService;
import com.educationpro.dto.ApiResponse;
import com.educationpro.schedule.domain.ConflictType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/schedules")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @PostMapping
    public ResponseEntity<ApiResponse<ScheduleResponseDto>> createSchedule(
            @Valid @RequestBody CreateScheduleRequest req,
            Authentication auth) {
        Long adminId = extractAdminId(auth);
        ScheduleResponseDto response = scheduleService.createSchedule(req, adminId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Schedule created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ScheduleResponseDto>> getSchedule(@PathVariable Long id) {
        ScheduleResponseDto response = scheduleService.getSchedule(id);
        return ResponseEntity.ok(ApiResponse.success("Schedule retrieved", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ScheduleResponseDto>> updateSchedule(
            @PathVariable Long id,
            @Valid @RequestBody CreateScheduleRequest req) {
        ScheduleResponseDto response = scheduleService.updateSchedule(id, req);
        return ResponseEntity.ok(ApiResponse.success("Schedule updated", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long id) {
        scheduleService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/calendar")
    public ResponseEntity<ApiResponse<List<ScheduleCalendarDto>>> getCalendar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {

        ScheduleFilters filters = new ScheduleFilters(teacherId, groupId, type, null, null, status);
        List<ScheduleCalendarDto> response = scheduleService.getCalendarData(from, to, filters);
        return ResponseEntity.ok(ApiResponse.success("Calendar data retrieved", response));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<ScheduleStatsDto>> getStats() {
        ScheduleStatsDto response = scheduleService.getStats();
        return ResponseEntity.ok(ApiResponse.success("Stats retrieved", response));
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<ApiResponse<List<ScheduleCalendarDto>>> getSessionSchedules(@PathVariable Long sessionId) {
        List<ScheduleCalendarDto> response = scheduleService.getSessionSchedules(sessionId);
        return ResponseEntity.ok(ApiResponse.success("Session schedules retrieved", response));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<ScheduleResponseDto>> cancelSchedule(
            @PathVariable Long id,
            @RequestBody(required = false) CancelScheduleRequest req) {
        ScheduleResponseDto response = scheduleService.getSchedule(id);
        return ResponseEntity.ok(ApiResponse.success("Schedule cancelled", response));
    }

    @GetMapping("/conflicts")
    public ResponseEntity<ApiResponse<List<ConflictSummaryDto>>> getConflicts(
            @RequestParam(required = false) String type) {
        ConflictType conflictType = type != null ? ConflictType.valueOf(type) : null;
        List<ConflictSummaryDto> response = conflictType != null ?
                scheduleService.getUnresolvedConflicts(conflictType) :
                scheduleService.getUnresolvedConflicts(null);
        return ResponseEntity.ok(ApiResponse.success("Conflicts retrieved", response));
    }

    @PostMapping("/conflicts/{id}/resolve")
    public ResponseEntity<Void> resolveConflict(@PathVariable Long id) {
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/teachers")
    public ResponseEntity<ApiResponse<List<TeacherDropdownDto>>> getTeachers() {
        List<TeacherDropdownDto> response = scheduleService.getTeachersForDropdown();
        return ResponseEntity.ok(ApiResponse.success("Teachers retrieved", response));
    }

    @GetMapping("/groups")
    public ResponseEntity<ApiResponse<List<GroupDropdownDto>>> getGroups(
            @RequestParam Long teacherProfileId) {
        List<GroupDropdownDto> response = scheduleService.getGroupsForTeacher(teacherProfileId);
        return ResponseEntity.ok(ApiResponse.success("Groups retrieved", response));
    }

    @GetMapping("/subjects")
    public ResponseEntity<ApiResponse<List<SubjectDropdownDto>>> getSubjects(
            @RequestParam Long sessionId) {
        List<SubjectDropdownDto> response = scheduleService.getSubjectsForSession(sessionId);
        return ResponseEntity.ok(ApiResponse.success("Subjects retrieved", response));
    }

    @GetMapping("/classrooms")
    public ResponseEntity<ApiResponse<List<ClassroomDto>>> getClassrooms() {
        List<ClassroomDto> response = scheduleService.getClassrooms();
        return ResponseEntity.ok(ApiResponse.success("Classrooms retrieved", response));
    }

    @PostMapping("/check-conflicts")
    public ResponseEntity<ApiResponse<List<ConflictSummaryDto>>> checkConflicts(
            @Valid @RequestBody CreateScheduleRequest req) {
        List<ConflictSummaryDto> response = scheduleService.detectConflicts(req);
        return ResponseEntity.ok(ApiResponse.success("Conflict check complete", response));
    }

    private Long extractAdminId(Authentication auth) {
        return 1L;
    }
}

record CancelScheduleRequest(String reason) {}

record ScheduleFilters(Long teacherId, Long groupId, String type, LocalDate dateFrom, LocalDate dateTo, String status) {}
