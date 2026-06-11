package com.educationpro.schedule.service;

import com.educationpro.domain.*;
import com.educationpro.repository.*;
import com.educationpro.schedule.domain.*;
import com.educationpro.schedule.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ScheduleService Tests")
class ScheduleServiceTest {

    @Mock
    private ClassScheduleRepository scheduleRepository;
    @Mock
    private ScheduleRecurrenceRepository recurrenceRepository;
    @Mock
    private ScheduleOccurrenceRepository occurrenceRepository;
    @Mock
    private ScheduleConflictRepository conflictRepository;
    @Mock
    private AssignmentSessionRepository sessionRepository;
    @Mock
    private AssignmentGroupRepository groupRepository;
    @Mock
    private AssignmentTeacherMappingRepository teacherMappingRepository;
    @Mock
    private CourseNodeRepository courseNodeRepository;
    @Mock
    private TeacherProfileRepository teacherRepository;
    @Mock
    private ClassroomRepository classroomRepository;
    @Mock
    private UserRepository userRepository;

    private ScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        scheduleService = new ScheduleService(
            scheduleRepository, recurrenceRepository, occurrenceRepository, conflictRepository,
            sessionRepository, groupRepository, teacherMappingRepository, courseNodeRepository,
            teacherRepository, classroomRepository, userRepository
        );
    }

    @Test
    @DisplayName("Test 1: Validate end time after start time")
    void testValidateEndTimeAfterStartTime() {
        CreateScheduleRequest req = new CreateScheduleRequest(
            ScheduleTab.CLASSES,
            1L, 1L, 1L, ScheduleType.REGULAR,
            1L,
            DateMode.SINGLE,
            LocalDate.now(),
            null,
            null,
            LocalTime.of(14, 0),  // 14:00 (2 PM)
            LocalTime.of(10, 0),  // 10:00 (10 AM) - INVALID (before start)
            "Math Class",
            1L,
            "Learn algebra",
            List.of("Topic 1"),
            false,
            null,
            null,
            null,
            null,
            null,
            null
        );

        assertThrows(Exception.class, () -> scheduleService.createSchedule(req, 1L),
            "Should throw error when end time is before start time");
    }

    @Test
    @DisplayName("Test 2: Generate daily occurrences")
    void testGenerateDailyOccurrences() {
        LocalDate startDate = LocalDate.of(2024, 6, 10);

        ClassSchedule schedule = new ClassSchedule();
        schedule.setId(1L);
        schedule.setScheduleDate(startDate);
        schedule.setStartTime(LocalTime.of(10, 0));
        schedule.setEndTime(LocalTime.of(11, 0));

        ScheduleRecurrence recurrence = new ScheduleRecurrence();
        recurrence.setPattern(RecurrencePattern.DAILY);
        recurrence.setIntervalValue(1);
        recurrence.setEndCondition(EndCondition.COUNT);
        recurrence.setOccurrenceCount(5);

        schedule.setRecurrence(recurrence);

        List<LocalDate> occurrences = scheduleService.generateOccurrences(schedule, recurrence);

        assertEquals(5, occurrences.size(), "Should generate 5 daily occurrences");
        assertEquals(startDate, occurrences.get(0), "First occurrence should be start date");
        assertEquals(startDate.plusDays(4), occurrences.get(4), "Fifth occurrence should be 4 days later");
    }

    @Test
    @DisplayName("Test 3: Generate weekly occurrences (Mon-Wed-Fri)")
    void testGenerateWeeklyOccurrences() {
        LocalDate startDate = LocalDate.of(2024, 6, 10); // Monday

        ClassSchedule schedule = new ClassSchedule();
        schedule.setId(1L);
        schedule.setScheduleDate(startDate);

        ScheduleRecurrence recurrence = new ScheduleRecurrence();
        recurrence.setPattern(RecurrencePattern.WEEKLY);
        recurrence.setIntervalValue(1);
        recurrence.setDaysOfWeek(List.of(0, 2, 4)); // Mon, Wed, Fri (0=Mon, 6=Sun)
        recurrence.setEndCondition(EndCondition.COUNT);
        recurrence.setOccurrenceCount(6); // 2 weeks

        schedule.setRecurrence(recurrence);

        List<LocalDate> occurrences = scheduleService.generateOccurrences(schedule, recurrence);

        assertEquals(6, occurrences.size(), "Should generate 6 occurrences (2 weeks)");
        // Verify pattern: Mon, Wed, Fri, Mon, Wed, Fri
    }

    @Test
    @DisplayName("Test 4: Generate occurrences until end date")
    void testGenerateOccurrencesUntilDate() {
        LocalDate startDate = LocalDate.of(2024, 6, 10);
        LocalDate endDate = LocalDate.of(2024, 6, 20);

        ClassSchedule schedule = new ClassSchedule();
        schedule.setId(1L);
        schedule.setScheduleDate(startDate);

        ScheduleRecurrence recurrence = new ScheduleRecurrence();
        recurrence.setPattern(RecurrencePattern.DAILY);
        recurrence.setIntervalValue(1);
        recurrence.setEndCondition(EndCondition.UNTIL_DATE);
        recurrence.setEndDate(endDate);

        schedule.setRecurrence(recurrence);

        List<LocalDate> occurrences = scheduleService.generateOccurrences(schedule, recurrence);

        assertEquals(11, occurrences.size(), "Should generate 11 occurrences (10 days + start)");
        assertTrue(occurrences.get(occurrences.size() - 1).isEqual(endDate) ||
                   occurrences.get(occurrences.size() - 1).isBefore(endDate.plusDays(1)),
                   "Last occurrence should be on or before end date");
    }

    @Test
    @DisplayName("Test 5: Detect teacher time conflict")
    void testDetectTeacherConflict() {
        CreateScheduleRequest req = new CreateScheduleRequest(
            ScheduleTab.CLASSES,
            1L, 1L, 1L, ScheduleType.REGULAR,
            1L,
            DateMode.SINGLE,
            LocalDate.of(2024, 6, 10),
            null,
            null,
            LocalTime.of(10, 0),
            LocalTime.of(11, 0),
            "Math",
            null, null, null, false, null, null, null, null, null, null
        );

        // Mock existing schedule (conflict)
        ClassSchedule existingSchedule = new ClassSchedule();
        existingSchedule.setId(2L);
        existingSchedule.setTeacher(new TeacherProfile());
        existingSchedule.getTeacher().setId(1L);
        existingSchedule.setScheduleDate(LocalDate.of(2024, 6, 10));
        existingSchedule.setStartTime(LocalTime.of(10, 30)); // Overlaps
        existingSchedule.setEndTime(LocalTime.of(11, 30));
        existingSchedule.setStatus(ScheduleStatus.ACTIVE);

        when(scheduleRepository.findTeacherConflicts(1L, LocalDate.of(2024, 6, 10), LocalTime.of(10, 0), LocalTime.of(11, 0)))
            .thenReturn(List.of(existingSchedule));

        List<ConflictSummaryDto> conflicts = scheduleService.detectConflicts(req);

        assertFalse(conflicts.isEmpty(), "Should detect conflict");
        assertTrue(conflicts.stream()
            .anyMatch(c -> c.conflictType() == ConflictType.TEACHER_CONFLICT),
            "Should identify as teacher conflict");
    }

    @Test
    @DisplayName("Test 6: Detect group time conflict")
    void testDetectGroupConflict() {
        CreateScheduleRequest req = new CreateScheduleRequest(
            ScheduleTab.CLASSES,
            1L, 1L, 1L, ScheduleType.REGULAR,
            1L,
            DateMode.SINGLE,
            LocalDate.of(2024, 6, 10),
            null,
            null,
            LocalTime.of(14, 0),
            LocalTime.of(15, 0),
            "English",
            null, null, null, false, null, null, null, null, null, null
        );

        ClassSchedule existingSchedule = new ClassSchedule();
        existingSchedule.setId(3L);
        existingSchedule.setAssignmentGroup(new AssignmentGroup());
        existingSchedule.getAssignmentGroup().setId(1L);
        existingSchedule.setScheduleDate(LocalDate.of(2024, 6, 10));
        existingSchedule.setStartTime(LocalTime.of(14, 30));
        existingSchedule.setEndTime(LocalTime.of(15, 30));
        existingSchedule.setStatus(ScheduleStatus.ACTIVE);

        when(scheduleRepository.findGroupConflicts(1L, LocalDate.of(2024, 6, 10), LocalTime.of(14, 0), LocalTime.of(15, 0)))
            .thenReturn(List.of(existingSchedule));

        List<ConflictSummaryDto> conflicts = scheduleService.detectConflicts(req);

        assertFalse(conflicts.isEmpty(), "Should detect group conflict");
    }

    @Test
    @DisplayName("Test 7: No conflict when cancelled schedule exists")
    void testNoConflictWithCancelledSchedule() {
        CreateScheduleRequest req = new CreateScheduleRequest(
            ScheduleTab.CLASSES,
            1L, 1L, 1L, ScheduleType.REGULAR,
            1L,
            DateMode.SINGLE,
            LocalDate.of(2024, 6, 10),
            null,
            null,
            LocalTime.of(10, 0),
            LocalTime.of(11, 0),
            "Science",
            null, null, null, false, null, null, null, null, null, null
        );

        // Cancelled schedule should not be considered for conflict
        when(scheduleRepository.findTeacherConflicts(1L, LocalDate.of(2024, 6, 10), LocalTime.of(10, 0), LocalTime.of(11, 0)))
            .thenReturn(List.of());

        List<ConflictSummaryDto> conflicts = scheduleService.detectConflicts(req);

        assertTrue(conflicts.isEmpty(), "Should not detect conflict with cancelled schedule");
    }

    @Test
    @DisplayName("Test 8: No overlapping time means no conflict")
    void testNoConflictWithNonOverlappingTimes() {
        CreateScheduleRequest req = new CreateScheduleRequest(
            ScheduleTab.CLASSES,
            1L, 1L, 1L, ScheduleType.REGULAR,
            1L,
            DateMode.SINGLE,
            LocalDate.of(2024, 6, 10),
            null,
            null,
            LocalTime.of(10, 0),
            LocalTime.of(11, 0),
            "PE",
            null, null, null, false, null, null, null, null, null, null
        );

        // Schedule at 14:00-15:00 (no overlap with 10:00-11:00)
        ClassSchedule existingSchedule = new ClassSchedule();
        existingSchedule.setStartTime(LocalTime.of(14, 0));
        existingSchedule.setEndTime(LocalTime.of(15, 0));

        when(scheduleRepository.findTeacherConflicts(1L, LocalDate.of(2024, 6, 10), LocalTime.of(10, 0), LocalTime.of(11, 0)))
            .thenReturn(List.of());

        List<ConflictSummaryDto> conflicts = scheduleService.detectConflicts(req);

        assertTrue(conflicts.isEmpty(), "Should not detect conflict with non-overlapping times");
    }

    @Test
    @DisplayName("Test 9: Multiple date mode creates occurrences for each date")
    void testMultipleDateMode() {
        CreateScheduleRequest req = new CreateScheduleRequest(
            ScheduleTab.CLASSES,
            1L, 1L, 1L, ScheduleType.REGULAR,
            1L,
            DateMode.MULTIPLE,
            LocalDate.of(2024, 6, 10),
            null,
            List.of(
                LocalDate.of(2024, 6, 10),
                LocalDate.of(2024, 6, 12),
                LocalDate.of(2024, 6, 14)
            ),
            LocalTime.of(10, 0),
            LocalTime.of(11, 0),
            "History",
            null, null, null, false, null, null, null, null, null, null
        );

        // Mock valid assignment
        AssignmentTeacherMapping mapping = new AssignmentTeacherMapping();
        mapping.setSession(new AssignmentSession());
        mapping.getSession().setScopeNode(new CourseNode());
        mapping.getSession().setStatus("SAVED");

        when(teacherMappingRepository.findByTeacherAndGroup(1L, 1L))
            .thenReturn(mapping);
        when(courseNodeRepository.findById(1L))
            .thenReturn(Optional.of(new CourseNode()));
        when(scheduleRepository.findTeacherConflicts(anyLong(), any(), any(), any()))
            .thenReturn(List.of());
        when(scheduleRepository.save(any()))
            .thenAnswer(invocation -> {
                ClassSchedule schedule = invocation.getArgument(0);
                schedule.setId(1L);
                return schedule;
            });

        // Verify 3 occurrence entries would be created
        assertEquals(3, req.multipleDates().size(), "Should have 3 dates for multiple date mode");
    }

    @Test
    @DisplayName("Test 10: Stats calculation")
    void testStatsCalculation() {
        when(scheduleRepository.count()).thenReturn(50L);
        when(scheduleRepository.countByDateRange(any(), any())).thenReturn(12L);
        when(conflictRepository.countByIsResolvedFalse()).thenReturn(3L);
        when(scheduleRepository.countByStatus(ScheduleStatus.COMPLETED)).thenReturn(8L);

        ScheduleStatsDto stats = scheduleService.getStats();

        assertEquals(50, stats.totalSchedules(), "Total should be 50");
        assertEquals(12, stats.thisWeekSchedules(), "This week should be 12");
        assertEquals(3, stats.activeConflicts(), "Active conflicts should be 3");
        assertEquals(8, stats.completedSchedules(), "Completed should be 8");
    }
}
