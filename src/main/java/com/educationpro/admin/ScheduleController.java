package com.educationpro.admin;

import com.educationpro.dto.ClassScheduleCreateRequest;
import com.educationpro.dto.ClassScheduleDto;
import com.educationpro.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.preauthorize.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/schedules")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @PostMapping
    public ResponseEntity<ClassScheduleDto> createSchedule(@RequestBody ClassScheduleCreateRequest request,
                                                           Authentication auth) {
        Long userId = extractUserIdFromAuth(auth);
        ClassScheduleDto result = scheduleService.createSchedule(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<ClassScheduleDto>> getSchedulesBySession(@PathVariable Long sessionId) {
        List<ClassScheduleDto> schedules = scheduleService.getSchedulesBySession(sessionId);
        return ResponseEntity.ok(schedules);
    }

    private Long extractUserIdFromAuth(Authentication auth) {
        // Extract from JWT claims or session - simplified version
        return 1L;  // Placeholder - implement based on your auth setup
    }
}
