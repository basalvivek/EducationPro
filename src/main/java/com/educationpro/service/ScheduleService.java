package com.educationpro.service;

import com.educationpro.domain.*;
import com.educationpro.dto.ClassScheduleCreateRequest;
import com.educationpro.dto.ClassScheduleDto;
import com.educationpro.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ScheduleService {

    private final ClassScheduleRepository scheduleRepository;
    private final ScheduleOccurrenceRepository occurrenceRepository;
    private final ScheduleRecurrenceRepository recurrenceRepository;
    private final ScheduleConflictRepository conflictRepository;
    private final ScheduleNotificationRepository notificationRepository;
    private final AssignmentSessionRepository sessionRepository;
    private final TeacherRepository teacherRepository;
    private final AssignmentGroupRepository groupRepository;
    private final CourseNodeRepository courseNodeRepository;
    private final ClassroomRepository classroomRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;

    public ClassScheduleDto createSchedule(ClassScheduleCreateRequest request, Long userId) {
        AssignmentSession session = sessionRepository.findById(request.getSessionId())
            .orElseThrow(() -> new RuntimeException("Session not found"));
        TeacherProfile teacher = teacherRepository.findById(request.getTeacherId())
            .orElseThrow(() -> new RuntimeException("Teacher not found"));
        AssignmentGroup group = groupRepository.findById(request.getGroupId())
            .orElseThrow(() -> new RuntimeException("Group not found"));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        ClassSchedule schedule = ClassSchedule.builder()
            .assignmentSession(session)
            .teacher(teacher)
            .assignmentGroup(group)
            .scheduleType(request.getScheduleType())
            .dateMode(request.getDateMode())
            .scheduleDate(request.getScheduleDate())
            .endDate(request.getEndDate())
            .startTime(request.getStartTime())
            .endTime(request.getEndTime())
            .durationMinutes((int) ChronoUnit.MINUTES.between(request.getStartTime(), request.getEndTime()))
            .topic(request.getTopic())
            .description(request.getDescription())
            .learningObjectives(request.getLearningObjectives())
            .enableAttendance(request.getEnableAttendance() != null && request.getEnableAttendance())
            .notifyStudents(request.getNotifyStudents() != null && request.getNotifyStudents())
            .notifyParents(request.getNotifyParents() != null && request.getNotifyParents())
            .sendReminder(request.getSendReminder() != null && request.getSendReminder())
            .eventColor(request.getEventColor() != null ? request.getEventColor() : "blue")
            .createdBy(user)
            .build();

        if (request.getClassroomId() != null) {
            Classroom classroom = classroomRepository.findById(request.getClassroomId())
                .orElseThrow(() -> new RuntimeException("Classroom not found"));
            schedule.setClassroom(classroom);
        }

        ClassSchedule saved = scheduleRepository.save(schedule);

        if (request.getEquipmentIds() != null && !request.getEquipmentIds().isEmpty()) {
            List<Equipment> equipment = equipmentRepository.findAllById(request.getEquipmentIds());
            saved.setRequiredEquipment(equipment.stream().collect(Collectors.toSet()));
            scheduleRepository.save(saved);
        }

        if ("RECURRING".equals(request.getDateMode()) && request.getRecurrence() != null) {
            createRecurrence(saved, request.getRecurrence());
        }

        detectConflicts(saved);

        return mapToDto(saved);
    }

    private void createRecurrence(ClassSchedule schedule, ClassScheduleCreateRequest.RecurrenceRequest recReq) {
        ScheduleRecurrence recurrence = ScheduleRecurrence.builder()
            .classSchedule(schedule)
            .frequency(recReq.getFrequency())
            .monday(recReq.getMonday() != null && recReq.getMonday())
            .tuesday(recReq.getTuesday() != null && recReq.getTuesday())
            .wednesday(recReq.getWednesday() != null && recReq.getWednesday())
            .thursday(recReq.getThursday() != null && recReq.getThursday())
            .friday(recReq.getFriday() != null && recReq.getFriday())
            .saturday(recReq.getSaturday() != null && recReq.getSaturday())
            .sunday(recReq.getSunday() != null && recReq.getSunday())
            .endCondition(recReq.getEndCondition())
            .endDate(recReq.getEndDate())
            .numOccurrences(recReq.getNumOccurrences())
            .build();
        recurrenceRepository.save(recurrence);

        generateOccurrences(schedule, recurrence);
    }

    private void generateOccurrences(ClassSchedule schedule, ScheduleRecurrence recurrence) {
        LocalDate currentDate = schedule.getScheduleDate();
        int occurrenceCount = 0;
        int maxOccurrences = recurrence.getNumOccurrences() != null ? recurrence.getNumOccurrences() : 52;

        while (occurrenceCount < maxOccurrences) {
            if (recurrence.getEndDate() != null && currentDate.isAfter(recurrence.getEndDate())) {
                break;
            }

            if (shouldGenerateOccurrence(currentDate, recurrence)) {
                ScheduleOccurrence occurrence = ScheduleOccurrence.builder()
                    .classSchedule(schedule)
                    .occurrenceDate(currentDate)
                    .startTime(schedule.getStartTime())
                    .endTime(schedule.getEndTime())
                    .status("SCHEDULED")
                    .build();
                occurrenceRepository.save(occurrence);
                occurrenceCount++;
            }

            currentDate = currentDate.plusDays(1);
        }
    }

    private boolean shouldGenerateOccurrence(LocalDate date, ScheduleRecurrence rec) {
        if ("DAILY".equals(rec.getFrequency())) return true;

        if ("WEEKLY".equals(rec.getFrequency())) {
            int dayOfWeek = date.getDayOfWeek().getValue();
            return (dayOfWeek == 1 && rec.getMonday()) ||
                   (dayOfWeek == 2 && rec.getTuesday()) ||
                   (dayOfWeek == 3 && rec.getWednesday()) ||
                   (dayOfWeek == 4 && rec.getThursday()) ||
                   (dayOfWeek == 5 && rec.getFriday()) ||
                   (dayOfWeek == 6 && rec.getSaturday()) ||
                   (dayOfWeek == 7 && rec.getSunday());
        }

        return false;
    }

    private void detectConflicts(ClassSchedule schedule) {
        List<ClassSchedule> teacherSchedules = scheduleRepository.findByTeacherAndDateRange(
            schedule.getTeacher().getId(),
            schedule.getScheduleDate(),
            schedule.getEndDate() != null ? schedule.getEndDate() : schedule.getScheduleDate()
        );

        for (ClassSchedule other : teacherSchedules) {
            if (!other.getId().equals(schedule.getId()) && timesOverlap(schedule, other)) {
                createConflict(schedule, other, "TEACHER_CONFLICT",
                    "Teacher conflict: " + schedule.getTeacher().getFirstName() + " has another class at this time");
            }
        }
    }

    private boolean timesOverlap(ClassSchedule s1, ClassSchedule s2) {
        return !s1.getEndTime().isBefore(s2.getStartTime()) &&
               !s2.getEndTime().isBefore(s1.getStartTime());
    }

    private void createConflict(ClassSchedule schedule, ClassSchedule conflicting, String type, String description) {
        ScheduleConflict conflict = ScheduleConflict.builder()
            .classSchedule(schedule)
            .conflictType(type)
            .conflictDescription(description)
            .conflictingSchedule(conflicting)
            .severity("WARNING")
            .isResolved(false)
            .build();
        conflictRepository.save(conflict);
    }

    public List<ClassScheduleDto> getSchedulesBySession(Long sessionId) {
        return scheduleRepository.findByAssignmentSessionId(sessionId).stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    private ClassScheduleDto mapToDto(ClassSchedule schedule) {
        return ClassScheduleDto.builder()
            .id(schedule.getId())
            .sessionId(schedule.getAssignmentSession().getId())
            .teacherId(schedule.getTeacher().getId())
            .teacherName(schedule.getTeacher().getFirstName() + " " + schedule.getTeacher().getLastName())
            .groupId(schedule.getAssignmentGroup().getId())
            .groupName(schedule.getAssignmentGroup().getName())
            .scheduleType(schedule.getScheduleType())
            .dateMode(schedule.getDateMode())
            .scheduleDate(schedule.getScheduleDate())
            .endDate(schedule.getEndDate())
            .startTime(schedule.getStartTime())
            .endTime(schedule.getEndTime())
            .durationMinutes(schedule.getDurationMinutes())
            .topic(schedule.getTopic())
            .description(schedule.getDescription())
            .learningObjectives(schedule.getLearningObjectives())
            .enableAttendance(schedule.getEnableAttendance())
            .notifyStudents(schedule.getNotifyStudents())
            .notifyParents(schedule.getNotifyParents())
            .sendReminder(schedule.getSendReminder())
            .eventColor(schedule.getEventColor())
            .createdAt(schedule.getCreatedAt())
            .updatedAt(schedule.getUpdatedAt())
            .version(schedule.getVersion())
            .build();
    }
}
