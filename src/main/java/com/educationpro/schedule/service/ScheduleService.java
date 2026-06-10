package com.educationpro.schedule.service;

import com.educationpro.domain.*;
import com.educationpro.exception.BusinessException;
import com.educationpro.repository.*;
import com.educationpro.schedule.domain.*;
import com.educationpro.schedule.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ScheduleService {

    private final ClassScheduleRepository scheduleRepository;
    private final ScheduleRecurrenceRepository recurrenceRepository;
    private final ScheduleOccurrenceRepository occurrenceRepository;
    private final ScheduleConflictRepository conflictRepository;
    private final AssignmentSessionRepository sessionRepository;
    private final AssignmentGroupRepository groupRepository;
    private final AssignmentTeacherMappingRepository teacherMappingRepository;
    private final CourseNodeRepository courseNodeRepository;
    private final TeacherProfileRepository teacherRepository;
    private final ClassroomRepository classroomRepository;

    public ScheduleResponseDto createSchedule(CreateScheduleRequest req, Long adminId) {
        validateScheduleRequest(req);

        if (ScheduleTab.CLASSES.equals(req.scheduleTab())) {
            validateAssignmentPairing(req.teacherProfileId(), req.groupId(), req.subjectNodeId());
        }

        ClassSchedule schedule = buildScheduleEntity(req);
        final ClassSchedule savedSchedule = scheduleRepository.save(schedule);

        if (DateMode.RECURRING.equals(req.dateMode()) && req.recurrence() != null) {
            ScheduleRecurrence recurrence = buildRecurrenceEntity(req.recurrence(), savedSchedule);
            recurrenceRepository.save(recurrence);
            savedSchedule.setRecurrence(recurrence);

            List<LocalDate> occurrences = generateOccurrences(savedSchedule, recurrence);
            List<ScheduleOccurrence> occurrenceEntities = occurrences.stream()
                    .map(date -> ScheduleOccurrence.builder()
                            .schedule(savedSchedule)
                            .occurrenceDate(date)
                            .startTime(req.startTime())
                            .endTime(req.endTime())
                            .status(ScheduleStatus.ACTIVE)
                            .createdAt(java.time.Instant.now())
                            .updatedAt(java.time.Instant.now())
                            .build())
                    .toList();
            occurrenceRepository.saveAll(occurrenceEntities);
            recurrence.setOccurrencesGenerated(occurrenceEntities.size());
            recurrenceRepository.save(recurrence);
        } else if (DateMode.MULTIPLE.equals(req.dateMode()) && req.multipleDates() != null) {
            List<ScheduleOccurrence> occurrenceEntities = req.multipleDates().stream()
                    .map(date -> ScheduleOccurrence.builder()
                            .schedule(savedSchedule)
                            .occurrenceDate(date)
                            .startTime(req.startTime())
                            .endTime(req.endTime())
                            .status(ScheduleStatus.ACTIVE)
                            .createdAt(java.time.Instant.now())
                            .updatedAt(java.time.Instant.now())
                            .build())
                    .toList();
            occurrenceRepository.saveAll(occurrenceEntities);
        }

        List<ConflictSummaryDto> conflicts = detectConflicts(req);
        if (!conflicts.isEmpty()) {
            savedSchedule.setStatus(ScheduleStatus.DRAFT);
            scheduleRepository.save(savedSchedule);

            for (ConflictSummaryDto conflict : conflicts) {
                ScheduleConflict conflictEntity = ScheduleConflict.builder()
                        .schedule(savedSchedule)
                        .conflictType(conflict.conflictType())
                        .conflictDescription(conflict.message())
                        .severity("WARNING")
                        .isResolved(false)
                        .detectedAt(java.time.Instant.now())
                        .build();
                conflictRepository.save(conflictEntity);
            }
        } else {
            savedSchedule.setStatus(ScheduleStatus.ACTIVE);
            scheduleRepository.save(savedSchedule);
        }

        return mapToResponseDto(savedSchedule);
    }

    public ScheduleResponseDto getSchedule(Long id) {
        ClassSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Schedule not found"));
        return mapToResponseDto(schedule);
    }

    public ScheduleResponseDto updateSchedule(Long id, CreateScheduleRequest req) {
        ClassSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Schedule not found"));

        validateScheduleRequest(req);
        if (ScheduleTab.CLASSES.equals(req.scheduleTab())) {
            validateAssignmentPairing(req.teacherProfileId(), req.groupId(), req.subjectNodeId());
        }

        updateScheduleEntity(schedule, req);
        final ClassSchedule updatedSchedule = scheduleRepository.save(schedule);

        if (DateMode.RECURRING.equals(req.dateMode())) {
            occurrenceRepository.deleteByScheduleId(updatedSchedule.getId());
            if (req.recurrence() != null) {
                ScheduleRecurrence recurrence = updatedSchedule.getRecurrence();
                if (recurrence == null) {
                    recurrence = new ScheduleRecurrence();
                    updatedSchedule.setRecurrence(recurrence);
                }
                updateRecurrenceEntity(recurrence, req.recurrence());
                recurrence.setSchedule(updatedSchedule);
                recurrenceRepository.save(recurrence);

                List<LocalDate> occurrences = generateOccurrences(updatedSchedule, recurrence);
                List<ScheduleOccurrence> occurrenceEntities = occurrences.stream()
                        .map(date -> ScheduleOccurrence.builder()
                                .schedule(updatedSchedule)
                                .occurrenceDate(date)
                                .startTime(req.startTime())
                                .endTime(req.endTime())
                                .status(ScheduleStatus.ACTIVE)
                                .createdAt(java.time.Instant.now())
                                .updatedAt(java.time.Instant.now())
                                .build())
                        .toList();
                occurrenceRepository.saveAll(occurrenceEntities);
                recurrence.setOccurrencesGenerated(occurrenceEntities.size());
                recurrenceRepository.save(recurrence);
            }
        } else {
            occurrenceRepository.deleteByScheduleId(schedule.getId());
            if (schedule.getRecurrence() != null) {
                recurrenceRepository.delete(schedule.getRecurrence());
                schedule.setRecurrence(null);
            }
        }

        return mapToResponseDto(schedule);
    }

    public void deleteSchedule(Long id) {
        ClassSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Schedule not found"));
        scheduleRepository.delete(schedule);
    }

    public List<ScheduleCalendarDto> getCalendarData(LocalDate from, LocalDate to, Long teacherId, Long groupId, String type, String status) {
        List<ClassSchedule> schedules = scheduleRepository.findByDateRangeAndFilters(from, to, teacherId, groupId, type, status);

        List<ScheduleCalendarDto> result = new ArrayList<>();
        for (ClassSchedule schedule : schedules) {
            if (DateMode.RECURRING.equals(schedule.getDateMode())) {
                List<ScheduleOccurrence> occurrences = occurrenceRepository.findByScheduleId(schedule.getId());
                for (ScheduleOccurrence occ : occurrences) {
                    if (!occ.getOccurrenceDate().isBefore(from) && !occ.getOccurrenceDate().isAfter(to)) {
                        result.add(mapToCalendarDto(schedule, occ.getOccurrenceDate()));
                    }
                }
            } else if (DateMode.MULTIPLE.equals(schedule.getDateMode())) {
                if (schedule.getMultipleDates() != null) {
                    for (LocalDate date : schedule.getMultipleDates()) {
                        if (!date.isBefore(from) && !date.isAfter(to)) {
                            result.add(mapToCalendarDto(schedule, date));
                        }
                    }
                }
            } else {
                if (!schedule.getScheduleDate().isBefore(from) && !schedule.getScheduleDate().isAfter(to)) {
                    result.add(mapToCalendarDto(schedule, schedule.getScheduleDate()));
                }
            }
        }

        return result;
    }

    public ScheduleStatsDto getStats() {
        long totalSchedules = scheduleRepository.count();
        LocalDate today = LocalDate.now();
        LocalDate weekEnd = today.plusDays(7);
        long thisWeekSchedules = scheduleRepository.countByDateRange(today, weekEnd);
        long activeConflicts = conflictRepository.countByIsResolvedFalse();
        long completedSchedules = scheduleRepository.countByStatus(ScheduleStatus.COMPLETED);

        return new ScheduleStatsDto(totalSchedules, thisWeekSchedules, activeConflicts, completedSchedules);
    }

    public List<ScheduleCalendarDto> getSessionSchedules(Long sessionId) {
        List<ClassSchedule> schedules = scheduleRepository.findByAssignmentSessionId(sessionId);
        List<ScheduleCalendarDto> result = new ArrayList<>();

        for (ClassSchedule schedule : schedules) {
            if (DateMode.RECURRING.equals(schedule.getDateMode())) {
                List<ScheduleOccurrence> occurrences = occurrenceRepository.findByScheduleId(schedule.getId());
                for (ScheduleOccurrence occ : occurrences) {
                    result.add(mapToCalendarDto(schedule, occ.getOccurrenceDate()));
                }
            } else if (DateMode.MULTIPLE.equals(schedule.getDateMode()) && schedule.getMultipleDates() != null) {
                for (LocalDate date : schedule.getMultipleDates()) {
                    result.add(mapToCalendarDto(schedule, date));
                }
            } else {
                result.add(mapToCalendarDto(schedule, schedule.getScheduleDate()));
            }
        }

        return result;
    }

    public List<ConflictSummaryDto> detectConflicts(CreateScheduleRequest req) {
        List<ConflictSummaryDto> conflicts = new ArrayList<>();

        Set<LocalDate> datesToCheck = getDateRangeToCheck(req);

        for (LocalDate date : datesToCheck) {
            if (req.teacherProfileId() != null) {
                List<ClassSchedule> teacherConflicts = scheduleRepository.findTeacherConflicts(
                        req.teacherProfileId(), date, req.startTime(), req.endTime());
                for (ClassSchedule existing : teacherConflicts) {
                    conflicts.add(new ConflictSummaryDto(
                            ConflictType.TEACHER_CONFLICT,
                            existing.getId(),
                            buildScheduleTitle(existing),
                            date,
                            existing.getStartTime(),
                            existing.getEndTime(),
                            "Teacher is already scheduled at this time"
                    ));
                }
            }

            if (req.groupId() != null) {
                List<ClassSchedule> groupConflicts = scheduleRepository.findGroupConflicts(
                        req.groupId(), date, req.startTime(), req.endTime());
                for (ClassSchedule existing : groupConflicts) {
                    conflicts.add(new ConflictSummaryDto(
                            ConflictType.GROUP_CONFLICT,
                            existing.getId(),
                            buildScheduleTitle(existing),
                            date,
                            existing.getStartTime(),
                            existing.getEndTime(),
                            "Group is already scheduled at this time"
                    ));
                }
            }

            if (req.classroomId() != null) {
                List<ClassSchedule> classroomConflicts = scheduleRepository.findClassroomConflicts(
                        req.classroomId(), date, req.startTime(), req.endTime());
                for (ClassSchedule existing : classroomConflicts) {
                    conflicts.add(new ConflictSummaryDto(
                            ConflictType.CLASSROOM_CONFLICT,
                            existing.getId(),
                            buildScheduleTitle(existing),
                            date,
                            existing.getStartTime(),
                            existing.getEndTime(),
                            "Classroom is already booked at this time"
                    ));
                }
            }
        }

        return conflicts;
    }

    public List<ConflictSummaryDto> getUnresolvedConflicts(ConflictType type) {
        List<ScheduleConflict> conflicts = conflictRepository.findByIsResolvedFalseAndConflictType(type);
        return conflicts.stream()
                .map(c -> new ConflictSummaryDto(
                        c.getConflictType(),
                        c.getConflictingSchedule() != null ? c.getConflictingSchedule().getId() : null,
                        c.getConflictingSchedule() != null ? buildScheduleTitle(c.getConflictingSchedule()) : null,
                        c.getSchedule().getScheduleDate(),
                        c.getSchedule().getStartTime(),
                        c.getSchedule().getEndTime(),
                        c.getConflictDescription()
                ))
                .toList();
    }

    public List<TeacherDropdownDto> getTeachersForDropdown() {
        return teacherMappingRepository.findDistinctTeachersByActiveSessions()
                .stream()
                .map(tp -> new TeacherDropdownDto(
                        tp.getId(),
                        tp.getUser() != null ? tp.getUser().getFullName() : "",
                        ""
                ))
                .toList();
    }

    public List<GroupDropdownDto> getGroupsForTeacher(Long teacherProfileId) {
        return groupRepository.findGroupsForTeacher(teacherProfileId)
                .stream()
                .map(ag -> new GroupDropdownDto(
                        ag.getId(),
                        ag.getName(),
                        ag.getStudentProfileIds() != null ? ag.getStudentProfileIds().size() : 0,
                        "",
                        ag.getSession() != null ? ag.getSession().getId() : null
                ))
                .toList();
    }

    public List<SubjectDropdownDto> getSubjectsForSession(Long sessionId) {
        AssignmentSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException("Session not found"));

        CourseNode scopeNode = session.getScopeNode();
        if (scopeNode == null) {
            return List.of();
        }

        return courseNodeRepository.findAll().stream()
                .filter(node -> scopeNode.getId().equals(node.getParentId()))
                .filter(node -> !NodeType.QUESTION.equals(node.getType()))
                .map(node -> new SubjectDropdownDto(node.getId(), node.getTitle()))
                .toList();
    }

    public List<ClassroomDto> getClassrooms() {
        return classroomRepository.findAll().stream()
                .filter(c -> c.getIsActive() != null && c.getIsActive())
                .map(c -> new ClassroomDto(c.getId(), c.getName(), c.getRoomCode(), c.getCapacity()))
                .toList();
    }

    public List<LocalDate> generateOccurrences(ClassSchedule schedule, ScheduleRecurrence recurrence) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate current = schedule.getScheduleDate();
        int count = 0;
        final int MAX_OCCURRENCES = 365;

        while (count < MAX_OCCURRENCES && dates.size() < MAX_OCCURRENCES) {
            if (shouldIncludeDate(current, recurrence)) {
                dates.add(current);
                count++;
            }

            if (shouldStopRecurrence(current, recurrence, dates.size())) {
                break;
            }

            current = advanceDate(current, recurrence);
        }

        return dates;
    }

    private void validateScheduleRequest(CreateScheduleRequest req) {
        if (req.startTime().isAfter(req.endTime())) {
            throw new BusinessException("End time must be after start time");
        }

        if (DateMode.MULTIPLE.equals(req.dateMode())) {
            if (req.multipleDates() == null || req.multipleDates().size() < 2) {
                throw new BusinessException("Multiple dates mode requires at least 2 dates");
            }
        }

        if (DateMode.RECURRING.equals(req.dateMode())) {
            if (req.recurrence() == null) {
                throw new BusinessException("Recurring schedule requires recurrence configuration");
            }
            if (RecurrencePattern.WEEKLY.equals(req.recurrence().pattern()) &&
                (req.recurrence().daysOfWeek() == null || req.recurrence().daysOfWeek().isEmpty())) {
                throw new BusinessException("Weekly recurrence requires at least one day of week selected");
            }
            if (EndCondition.UNTIL_DATE.equals(req.recurrence().endCondition())) {
                if (req.recurrence().endDate() == null || !req.recurrence().endDate().isAfter(req.startDate())) {
                    throw new BusinessException("Until-date end condition requires an end date after the start date");
                }
            }
            if (EndCondition.COUNT.equals(req.recurrence().endCondition())) {
                if (req.recurrence().occurrenceCount() == null || req.recurrence().occurrenceCount() < 1 || req.recurrence().occurrenceCount() > 365) {
                    throw new BusinessException("Occurrence count must be between 1 and 365");
                }
            }
        }
    }

    private void validateAssignmentPairing(Long teacherProfileId, Long groupId, Long subjectNodeId) {
        AssignmentTeacherMapping mapping = teacherMappingRepository.findByTeacherAndGroup(teacherProfileId, groupId);
        if (mapping == null) {
            throw new BusinessException("Teacher is not assigned to this group in any active session");
        }

        AssignmentSession session = mapping.getSession();
        CourseNode scopeNode = session.getScopeNode();
        CourseNode subjectNode = courseNodeRepository.findById(subjectNodeId).orElse(null);

        if (subjectNode == null || !isAncestorOrEqual(scopeNode, subjectNode)) {
            throw new BusinessException("Subject node is not in the scope of the assignment session");
        }
    }

    private boolean isAncestorOrEqual(CourseNode ancestor, CourseNode node) {
        CourseNode current = node;
        while (current != null) {
            if (current.getId().equals(ancestor.getId())) {
                return true;
            }
            if (current.getParentId() == null) break;
            current = courseNodeRepository.findById(current.getParentId()).orElse(null);
        }
        return false;
    }

    private ClassSchedule buildScheduleEntity(CreateScheduleRequest req) {
        ClassSchedule schedule = new ClassSchedule();
        schedule.setScheduleTab(req.scheduleTab());
        schedule.setDateMode(req.dateMode());
        schedule.setStatus(ScheduleStatus.DRAFT);
        schedule.setScheduleType(req.scheduleType());
        schedule.setScheduleDate(req.startDate());
        schedule.setEndDate(req.endDate());
        schedule.setMultipleDates(req.multipleDates());
        schedule.setStartTime(req.startTime());
        schedule.setEndTime(req.endTime());
        schedule.setTopic(req.topic());
        schedule.setDescription(req.description());
        schedule.setEventTitle(req.eventTitle());
        schedule.setLocation(req.location());
        schedule.setAudience(req.audience());
        schedule.setLearningObjectives(req.learningObjectives() != null ? req.learningObjectives() : new ArrayList<>());
        schedule.setAttendanceRequired(req.attendanceRequired() != null ? req.attendanceRequired() : false);
        schedule.setReminderMinutes(req.reminderMinutes());

        if (req.teacherProfileId() != null) {
            schedule.setTeacher(teacherRepository.findById(req.teacherProfileId()).orElse(null));
        }
        if (req.groupId() != null) {
            schedule.setAssignmentGroup(groupRepository.findById(req.groupId()).orElse(null));
        }
        if (req.subjectNodeId() != null) {
            schedule.setSubject(courseNodeRepository.findById(req.subjectNodeId()).orElse(null));
        }
        if (req.classroomId() != null) {
            schedule.setClassroom(classroomRepository.findById(req.classroomId()).orElse(null));
        }
        if (req.assignmentSessionId() != null) {
            schedule.setAssignmentSession(sessionRepository.findById(req.assignmentSessionId()).orElse(null));
        }

        return schedule;
    }

    private void updateScheduleEntity(ClassSchedule schedule, CreateScheduleRequest req) {
        schedule.setScheduleTab(req.scheduleTab());
        schedule.setDateMode(req.dateMode());
        schedule.setScheduleType(req.scheduleType());
        schedule.setScheduleDate(req.startDate());
        schedule.setEndDate(req.endDate());
        schedule.setMultipleDates(req.multipleDates());
        schedule.setStartTime(req.startTime());
        schedule.setEndTime(req.endTime());
        schedule.setTopic(req.topic());
        schedule.setDescription(req.description());
        schedule.setEventTitle(req.eventTitle());
        schedule.setLocation(req.location());
        schedule.setAudience(req.audience());
        schedule.setLearningObjectives(req.learningObjectives() != null ? req.learningObjectives() : new ArrayList<>());
        schedule.setAttendanceRequired(req.attendanceRequired() != null ? req.attendanceRequired() : false);
        schedule.setReminderMinutes(req.reminderMinutes());
    }

    private ScheduleRecurrence buildRecurrenceEntity(RecurrenceRequest req, ClassSchedule schedule) {
        ScheduleRecurrence recurrence = new ScheduleRecurrence();
        recurrence.setSchedule(schedule);
        recurrence.setPattern(req.pattern());
        recurrence.setIntervalValue(req.intervalValue() != null ? req.intervalValue() : 1);
        recurrence.setDaysOfWeek(req.daysOfWeek());
        recurrence.setEndCondition(req.endCondition());
        recurrence.setEndDate(req.endDate());
        recurrence.setOccurrenceCount(req.occurrenceCount());
        return recurrence;
    }

    private void updateRecurrenceEntity(ScheduleRecurrence recurrence, RecurrenceRequest req) {
        recurrence.setPattern(req.pattern());
        recurrence.setIntervalValue(req.intervalValue() != null ? req.intervalValue() : 1);
        recurrence.setDaysOfWeek(req.daysOfWeek());
        recurrence.setEndCondition(req.endCondition());
        recurrence.setEndDate(req.endDate());
        recurrence.setOccurrenceCount(req.occurrenceCount());
    }

    private ScheduleResponseDto mapToResponseDto(ClassSchedule schedule) {
        String teacherName = schedule.getTeacher() != null && schedule.getTeacher().getUser() != null ?
                schedule.getTeacher().getUser().getFullName() : "";
        String groupName = schedule.getAssignmentGroup() != null ? schedule.getAssignmentGroup().getName() : "";
        int studentCount = schedule.getAssignmentGroup() != null && schedule.getAssignmentGroup().getStudentProfileIds() != null ?
                schedule.getAssignmentGroup().getStudentProfileIds().size() : 0;
        String subjectName = schedule.getSubject() != null ? schedule.getSubject().getTitle() : "";
        String classroomName = schedule.getClassroom() != null ? schedule.getClassroom().getName() : "";

        String durationLabel = getDurationLabel(schedule.getStartTime(), schedule.getEndTime());
        boolean isRecurring = schedule.getRecurrence() != null;

        RecurrenceDto recurrenceDto = null;
        int occurrenceCount = 0;
        if (schedule.getRecurrence() != null) {
            ScheduleRecurrence rec = schedule.getRecurrence();
            recurrenceDto = new RecurrenceDto(
                    rec.getId(),
                    rec.getPattern(),
                    rec.getIntervalValue(),
                    rec.getDaysOfWeek(),
                    rec.getEndCondition(),
                    rec.getEndDate(),
                    rec.getOccurrenceCount(),
                    rec.getOccurrencesGenerated()
            );
            occurrenceCount = rec.getOccurrencesGenerated();
        } else if (DateMode.MULTIPLE.equals(schedule.getDateMode())) {
            occurrenceCount = schedule.getMultipleDates() != null ? schedule.getMultipleDates().size() : 1;
        } else {
            occurrenceCount = 1;
        }

        boolean hasConflict = !schedule.getConflicts().isEmpty();
        List<ConflictSummaryDto> conflictDtos = schedule.getConflicts().stream()
                .map(c -> new ConflictSummaryDto(
                        c.getConflictType(),
                        c.getConflictingSchedule() != null ? c.getConflictingSchedule().getId() : null,
                        c.getConflictingSchedule() != null ? buildScheduleTitle(c.getConflictingSchedule()) : null,
                        c.getSchedule().getScheduleDate(),
                        c.getSchedule().getStartTime(),
                        c.getSchedule().getEndTime(),
                        c.getConflictDescription()
                ))
                .toList();

        return new ScheduleResponseDto(
                schedule.getId(),
                schedule.getScheduleTab(),
                teacherName,
                groupName,
                studentCount,
                subjectName,
                schedule.getScheduleType(),
                schedule.getScheduleType() != null ? schedule.getScheduleType().toString() : "",
                schedule.getDateMode(),
                schedule.getScheduleDate(),
                schedule.getEndDate(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                durationLabel,
                schedule.getTopic(),
                classroomName,
                schedule.getDescription(),
                schedule.getLearningObjectives(),
                schedule.getAttendanceRequired(),
                schedule.getStatus(),
                isRecurring,
                recurrenceDto,
                occurrenceCount,
                hasConflict,
                conflictDtos,
                java.time.Instant.now()
        );
    }

    private ScheduleCalendarDto mapToCalendarDto(ClassSchedule schedule, LocalDate date) {
        String title = ScheduleTab.CLASSES.equals(schedule.getScheduleTab()) ?
                (schedule.getSubject() != null ? schedule.getSubject().getTitle() : "") :
                schedule.getEventTitle();

        String teacherName = schedule.getTeacher() != null && schedule.getTeacher().getUser() != null ?
                schedule.getTeacher().getUser().getFullName() : "";
        String groupName = schedule.getAssignmentGroup() != null ? schedule.getAssignmentGroup().getName() : "";

        String colorClass = switch (schedule.getScheduleTab()) {
            case CLASSES -> "schedule-card--classes";
            case EVENTS -> "schedule-card--events";
            case HOLIDAYS -> "schedule-card--holidays";
            case OTHERS -> "schedule-card--others";
        };

        if (ScheduleStatus.CANCELLED.equals(schedule.getStatus())) {
            colorClass = "schedule-card--cancelled";
        }

        boolean isRecurring = schedule.getRecurrence() != null;

        return new ScheduleCalendarDto(
                schedule.getId(),
                schedule.getScheduleTab(),
                title,
                teacherName,
                groupName,
                date,
                schedule.getStartTime(),
                schedule.getEndTime(),
                colorClass,
                schedule.getStatus(),
                isRecurring
        );
    }

    private String getDurationLabel(LocalTime startTime, LocalTime endTime) {
        long minutes = java.time.temporal.ChronoUnit.MINUTES.between(startTime, endTime);
        long hours = minutes / 60;
        long mins = minutes % 60;

        if (hours > 0 && mins > 0) {
            return hours + " Hour" + (hours > 1 ? "s" : "") + " " + mins + " Minutes";
        } else if (hours > 0) {
            return hours + " Hour" + (hours > 1 ? "s" : "");
        } else {
            return mins + " Minutes";
        }
    }

    private String buildScheduleTitle(ClassSchedule schedule) {
        if (ScheduleTab.CLASSES.equals(schedule.getScheduleTab())) {
            return schedule.getSubject() != null ? schedule.getSubject().getTitle() : "Class";
        }
        return schedule.getEventTitle() != null ? schedule.getEventTitle() : "Event";
    }

    private Set<LocalDate> getDateRangeToCheck(CreateScheduleRequest req) {
        Set<LocalDate> dates = new HashSet<>();

        if (DateMode.RECURRING.equals(req.dateMode()) && req.recurrence() != null) {
            ScheduleRecurrence tempRecurrence = new ScheduleRecurrence();
            tempRecurrence.setPattern(req.recurrence().pattern());
            tempRecurrence.setIntervalValue(req.recurrence().intervalValue() != null ? req.recurrence().intervalValue() : 1);
            tempRecurrence.setDaysOfWeek(req.recurrence().daysOfWeek());
            tempRecurrence.setEndCondition(req.recurrence().endCondition());
            tempRecurrence.setEndDate(req.recurrence().endDate());
            tempRecurrence.setOccurrenceCount(req.recurrence().occurrenceCount());

            LocalDate current = req.startDate();
            int count = 0;
            while (count < 365) {
                if (shouldIncludeDate(current, tempRecurrence)) {
                    dates.add(current);
                    count++;
                }
                if (shouldStopRecurrence(current, tempRecurrence, dates.size())) {
                    break;
                }
                current = advanceDate(current, tempRecurrence);
            }
        } else if (DateMode.MULTIPLE.equals(req.dateMode()) && req.multipleDates() != null) {
            dates.addAll(req.multipleDates());
        } else {
            dates.add(req.startDate());
        }

        return dates;
    }

    private boolean shouldIncludeDate(LocalDate date, ScheduleRecurrence recurrence) {
        if (RecurrencePattern.WEEKLY.equals(recurrence.getPattern())) {
            int dayOfWeek = date.getDayOfWeek().getValue() % 7;
            return recurrence.getDaysOfWeek() != null && recurrence.getDaysOfWeek().contains(dayOfWeek);
        }
        return true;
    }

    private boolean shouldStopRecurrence(LocalDate current, ScheduleRecurrence recurrence, int count) {
        if (EndCondition.UNTIL_DATE.equals(recurrence.getEndCondition()) && recurrence.getEndDate() != null) {
            return current.isAfter(recurrence.getEndDate());
        }
        if (EndCondition.COUNT.equals(recurrence.getEndCondition()) && recurrence.getOccurrenceCount() != null) {
            return count >= recurrence.getOccurrenceCount();
        }
        if (count >= 365) {
            return true;
        }
        return false;
    }

    private LocalDate advanceDate(LocalDate current, ScheduleRecurrence recurrence) {
        if (RecurrencePattern.DAILY.equals(recurrence.getPattern())) {
            return current.plusDays(recurrence.getIntervalValue());
        } else if (RecurrencePattern.WEEKLY.equals(recurrence.getPattern())) {
            return current.plusWeeks(recurrence.getIntervalValue());
        } else if (RecurrencePattern.MONTHLY.equals(recurrence.getPattern())) {
            return current.plusMonths(recurrence.getIntervalValue());
        }
        return current.plusDays(1);
    }
}
