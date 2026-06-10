# Academic Scheduler — Module 5 Full Specification

> **EduPro Admin** | Extends & refactors the existing Module 5 (Curriculum Scheduler) stub  
> Version 1.0 | June 2025

---

## Table of Contents

1. [Overview](#1-overview)
2. [UI / UX Specification](#2-ui--ux-specification)
3. [Database Schema](#3-database-schema)
4. [Domain Model](#4-domain-model-java)
5. [DTOs](#5-dtos--request--response-contracts)
6. [REST API Endpoints](#6-rest-api-endpoints)
7. [Service Layer](#7-service-layer--scheduleservice-refactored)
8. [Frontend Implementation](#8-frontend-implementation)
9. [Module 4 Integration](#9-integration-with-module-4-assignments)
10. [Implementation Plan](#10-implementation-plan)
11. [Non-Functional Requirements](#11-non-functional-requirements)
12. [Appendix](#12-appendix--quick-reference)

---

## 1. Overview

The Academic Scheduler is the fully-realized Module 5 of EduPro Admin. It replaces the existing stub (`schedule.html` / `schedule.js` / `ScheduleService`) with a production-grade week/day/month calendar system, a rich tabbed Add-Schedule panel, conflict detection, and deep integration with Module 4 (Assignments).

### 1.1 Goals

- Provide admins with a visual week-grid calendar showing all scheduled classes, events, and holidays.
- Allow creation of Classes, Events, Holidays, and Other schedule types from a single panel.
- Enforce teacher-group assignment validation (only paired combinations from Module 4 are permitted).
- Detect and surface booking conflicts (teacher, group, classroom) before persisting.
- Support single-day, multi-day, and recurring (daily / weekly / monthly) date modes.
- Generate individual `schedule_occurrences` for recurring schedules.
- Deliver Quick Stats (totals, conflicts, completed) at a glance.
- Match the UI design shown in the requirement screenshot: two-panel layout, color-coded schedule cards, filter sidebar.

### 1.2 Scope

| Area | Detail |
|---|---|
| **In Scope** | Week/Day/Month/Agenda views, Add/Edit/Delete schedules, Recurring recurrence engine, Conflict detection, Quick Stats, Teacher/Group/Type/Status filters, Classroom dropdown |
| **Out of Scope (v1)** | Drag-and-drop rescheduling, Student-facing calendar, Email/SMS notification pipeline, Parent portal integration |
| **Existing assets reused** | DB tables V13–V18 (extended), `ScheduleService` (refactored), `ScheduleController` (extended), `schedule.html/.js` (replaced) |
| **Integration points** | Module 4: `assignment_sessions`, `assignment_groups`, `assignment_teacher_mappings`, `teacher_profiles`, `student_profiles` |

---

## 2. UI / UX Specification

The schedule page at `/admin/schedule` uses the standard EduPro Admin shell (sidebar + topbar). The content area is divided into three zones: **Topbar Controls**, **Left Filter Panel**, and the main area which splits into a **Calendar Grid** (left) and an **Add-Schedule Side Panel** (right, slides in on `+ New Schedule`).

### 2.1 Page Layout

| Property | Value |
|---|---|
| Route | `/admin/schedule` |
| Template | `templates/admin/schedule.html` (full replacement) |
| JavaScript | `static/js/schedule.js` (full replacement) |
| CSS additions | `static/css/schedule.css` (new, imported in schedule.html) |
| Sidebar active item | Schedule Management |

---

### Zone A — Page Topbar Controls

Sits above the calendar. Full-width row containing:

- **Left group:** Today button | prev/next chevron arrows (`bi-chevron-left` / `bi-chevron-right`) | Date range label (e.g. `12 – 18 May 2024`) | calendar picker icon
- **Center group:** Day | **Week** (default active, `--bg-admin` highlight) | Month | Agenda toggle buttons
- **Right group:** Filter button (`bi-funnel` icon) | `+ New Schedule` primary button (`--bg-admin` gradient)

---

### Zone B — Left Filter Sidebar

Fixed 240px panel. Collapsible via the Filter button. Contains:

- Header row `Filters` + `Clear All` link
- **Teacher** dropdown — All Teachers / individual teacher names from assignment data
- **Group** dropdown — All Groups / group names (filtered by selected teacher)
- **Schedule Type** dropdown — All Types / Classes / Events / Holidays / Others
- **Date Range** — two date pickers (from / to), defaults to current week
- **Status** dropdown — All Status / Active / Cancelled / Completed
- `Apply Filters` button (full width, `--bg-admin` gradient)
- **Schedule Types legend** — checkboxes with colored dots: Classes (blue), Events (teal), Holidays (green), Others (orange), Cancelled (red)
- **Quick Stats** — four stat cards in a 2×2 grid: Total Schedules, This Week, Conflicts (orange badge), Completed (green badge)

---

### Zone C — Calendar Grid

Flex-grow area. Changes based on the active view toggle:

- **Week view (default):** 8-column grid — first column is time labels (All Day row + hourly 08:00–18:00), columns 2–8 are Mon–Sun with date headers. Today's column highlighted with `--bg-admin` tint.
- **Day view:** Single-day version of week grid.
- **Month view:** Standard month grid with overflow `+N more` per cell.
- **Agenda view:** Vertical list grouped by day.

**Schedule cards:** colored left border per type (blue=Classes, teal=Events, green=Holidays, orange=Others), bold subject name, teacher name, group name, time range. Clicking opens an edit modal. Recurring occurrences show a loop icon (`bi-arrow-repeat`).

---

### Zone D — Add/Edit Schedule Panel

Slides in from the right (400px width, `box-shadow`). Contains a close `[x]` button. Four tabs across the top: **Classes | Events | Holidays | Others**.

#### Classes Tab Fields

| Field | Control | Notes / Validation |
|---|---|---|
| Teacher * | Dropdown (searchable) | Populated from `assignment_sessions` joined `teacher_profiles`. Only teachers with `SAVED` assignments. |
| Group * | Dropdown (searchable) | Filtered by selected teacher — only groups in same assignment session. Shows student count + academic year below. |
| Subject * | Dropdown | Auto-populated from `scope_node` of the assignment session (subject-level nodes). |
| Schedule Type * | Dropdown | `regular | revision | extra | practical | exam_prep | parent | workshop` |
| Date Mode * | Radio: Single Day / Multiple Days / Recurring | Controls which date fields appear below. |
| Date (Single) * | Date picker | Shown when Date Mode = Single Day. |
| Dates (Multiple) * | Multi-date picker | Shown when Date Mode = Multiple Days. Max 30 dates. |
| Recurrence | See Section 2.2 | Shown when Date Mode = Recurring. |
| Start Time * | Time picker (HH:MM) | 24h or 12h per locale. |
| End Time * | Time picker (HH:MM) | Must be after Start Time. Duration shown auto-calculated. |
| Topic * | Text input | Max 200 chars. |
| Classroom | Dropdown | From `classrooms` table. Shows room number + capacity. |
| Description | Rich text (bold/italic/underline/list) | Max 1000 chars. Character counter shown. |
| More Options | Expand toggle | Reveals: Learning Objectives (tag chips), Attendance toggle, Reminder settings, Equipment multi-select. |

#### Events Tab Fields

Title *, Date *, Start Time, End Time, Location (free text), Description, Audience (All / Teachers / Students / Parents).

#### Holidays Tab Fields

Title *, Date Range * (from/to), Description, Applicable to (All / specific group).

#### Others Tab Fields

Title *, Type (free text), Date *, Start Time, End Time, Location, Description.

**Panel action buttons:** `Cancel` (outline) | `Save Schedule` (`--bg-admin`, primary)

---

### 2.2 Recurring Schedule Sub-fields

| Field | Control |
|---|---|
| Recurrence pattern | Radio: Daily / Weekly / Monthly |
| Weekly checkboxes | Mon Tue Wed Thu Fri Sat Sun (multi-select) |
| Interval | Number input — e.g. "every 2 weeks" |
| End condition | Radio: Never / Until Date / After N occurrences |
| Until Date | Date picker (shown when end = Until Date) |
| Count | Number input (shown when end = After N occurrences) |

---

### 2.3 Color Coding & Design Tokens

All tokens follow the existing EduPro Admin design system in `main.css`.

| Schedule Type | Card Color | CSS Class |
|---|---|---|
| Classes | Blue `#3B82F6` left border | `.schedule-card--classes` |
| Events | Teal `#14B8A6` left border | `.schedule-card--events` |
| Holidays | Green `#22C55E` background | `.schedule-card--holidays` |
| Others | Orange `#F97316` left border | `.schedule-card--others` |
| Cancelled | Red `#EF4444` strikethrough | `.schedule-card--cancelled` |
| Today column | Light blue tint `#EFF6FF` | `.col-today { background: #EFF6FF }` |
| Conflict badge | Orange `#F97316` | `.badge.bg-orange` |

---

### 2.4 Responsive Breakpoints

- **≥1280px:** Full three-zone layout (filter 240px + calendar + panel 400px)
- **960–1279px:** Filter panel collapses to overlay; Add panel overlaps calendar
- **<960px:** Stack vertically; calendar scrolls horizontally on week view

---

## 3. Database Schema

Existing migrations V13–V18 are retained. Three new migrations are added: **V19**, **V20**, **V21**.

### 3.1 Existing Tables Summary (V13–V18)

| Table | Migration | Purpose |
|---|---|---|
| `classrooms` | V13 | Physical rooms: id, name, room_number, capacity, building, floor, facilities (jsonb), is_active |
| `equipment` | V13 | AV/lab equipment: id, name, type, quantity, is_available |
| `class_schedules` | V14 | Master schedule record: teacher, group, subject node, dates, times, type, status |
| `schedule_occurrences` | V15 | Individual instances generated from recurring schedules |
| `schedule_conflicts` | V16 | Detected conflicts: type (teacher/group/classroom), conflicting schedule IDs |
| `schedule_notifications` | V17 | Notification queue: recipient_id, type, channel, sent_at |
| `schedule_equipment` | V18 | Many-to-many: schedule ↔ equipment |

---

### 3.2 V19 — Extend `class_schedules`

**File:** `db/migration/V19__extend_class_schedules.sql`

| Column | Type | Nullable | Notes |
|---|---|---|---|
| `schedule_tab` | `VARCHAR(20)` | `NOT NULL DEFAULT 'CLASSES'` | `CLASSES | EVENTS | HOLIDAYS | OTHERS` |
| `event_title` | `VARCHAR(200)` | NULL | Used for Events/Holidays/Others tabs |
| `location` | `VARCHAR(200)` | NULL | Free-text location for events |
| `audience` | `VARCHAR(20)` | NULL | `ALL | TEACHERS | STUDENTS | PARENTS` |
| `learning_objectives` | `JSONB` | `NOT NULL DEFAULT '[]'` | Array of objective strings (chip tags) |
| `attendance_required` | `BOOLEAN` | `NOT NULL DEFAULT FALSE` | Toggle for attendance tracking |
| `reminder_minutes` | `INTEGER` | NULL | Minutes before start to send reminder |
| `topic` | `VARCHAR(200)` | NULL | Lesson topic (Classes tab) |
| `description` | `TEXT` | NULL | Rich text stored as HTML |
| `assignment_session_id` | `BIGINT` | NULL | FK → `assignment_sessions(id)` |
| `date_mode` | `VARCHAR(20)` | `NOT NULL DEFAULT 'SINGLE'` | `SINGLE | MULTIPLE | RECURRING` |
| `multiple_dates` | `JSONB` | NULL | Array of date strings for MULTIPLE mode |
| `updated_at` | `TIMESTAMP` | `NOT NULL DEFAULT now()` | Managed by Postgres trigger |

```sql
ALTER TABLE class_schedules
  ADD COLUMN IF NOT EXISTS schedule_tab          VARCHAR(20)  NOT NULL DEFAULT 'CLASSES',
  ADD COLUMN IF NOT EXISTS event_title           VARCHAR(200),
  ADD COLUMN IF NOT EXISTS location              VARCHAR(200),
  ADD COLUMN IF NOT EXISTS audience              VARCHAR(20),
  ADD COLUMN IF NOT EXISTS learning_objectives   JSONB        NOT NULL DEFAULT '[]',
  ADD COLUMN IF NOT EXISTS attendance_required   BOOLEAN      NOT NULL DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS reminder_minutes      INTEGER,
  ADD COLUMN IF NOT EXISTS topic                 VARCHAR(200),
  ADD COLUMN IF NOT EXISTS description           TEXT,
  ADD COLUMN IF NOT EXISTS assignment_session_id BIGINT REFERENCES assignment_sessions(id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS date_mode             VARCHAR(20)  NOT NULL DEFAULT 'SINGLE',
  ADD COLUMN IF NOT EXISTS multiple_dates        JSONB,
  ADD COLUMN IF NOT EXISTS updated_at            TIMESTAMP    NOT NULL DEFAULT now();

ALTER TABLE class_schedules
  ADD CONSTRAINT chk_schedule_tab CHECK (schedule_tab IN ('CLASSES','EVENTS','HOLIDAYS','OTHERS')),
  ADD CONSTRAINT chk_date_mode    CHECK (date_mode    IN ('SINGLE','MULTIPLE','RECURRING')),
  ADD CONSTRAINT chk_audience     CHECK (audience IS NULL OR audience IN ('ALL','TEACHERS','STUDENTS','PARENTS'));

CREATE INDEX IF NOT EXISTS idx_cs_session ON class_schedules(assignment_session_id);
CREATE INDEX IF NOT EXISTS idx_cs_date    ON class_schedules(start_date);
CREATE INDEX IF NOT EXISTS idx_cs_teacher ON class_schedules(teacher_profile_id);
```

---

### 3.3 V20 — Full `schedule_recurrence` Table

**File:** `db/migration/V20__schedule_recurrence_full.sql`

Replaces/extends the existing stub with a complete config row per recurring schedule.

| Column | Type | Nullable | Notes |
|---|---|---|---|
| `id` | `BIGSERIAL PK` | NOT NULL | Primary key |
| `schedule_id` | `BIGINT FK` | NOT NULL | → `class_schedules(id)` ON DELETE CASCADE |
| `pattern` | `VARCHAR(20)` | NOT NULL | `DAILY | WEEKLY | MONTHLY` |
| `interval_value` | `INTEGER` | `NOT NULL DEFAULT 1` | e.g. every 2 weeks |
| `days_of_week` | `INTEGER[]` | NULL | 0=Mon … 6=Sun; used for WEEKLY pattern |
| `end_condition` | `VARCHAR(20)` | `NOT NULL DEFAULT 'NEVER'` | `NEVER | UNTIL_DATE | COUNT` |
| `end_date` | `DATE` | NULL | Used when `end_condition = UNTIL_DATE` |
| `occurrence_count` | `INTEGER` | NULL | Used when `end_condition = COUNT` |
| `occurrences_generated` | `INTEGER` | `NOT NULL DEFAULT 0` | Counter updated after generation |
| `created_at` | `TIMESTAMP` | `NOT NULL DEFAULT now()` | |

```sql
-- Drop stub if it exists and recreate fully
DROP TABLE IF EXISTS schedule_recurrence CASCADE;

CREATE TABLE schedule_recurrence (
  id                    BIGSERIAL    PRIMARY KEY,
  schedule_id           BIGINT       NOT NULL REFERENCES class_schedules(id) ON DELETE CASCADE,
  pattern               VARCHAR(20)  NOT NULL,
  interval_value        INTEGER      NOT NULL DEFAULT 1,
  days_of_week          INTEGER[],
  end_condition         VARCHAR(20)  NOT NULL DEFAULT 'NEVER',
  end_date              DATE,
  occurrence_count      INTEGER,
  occurrences_generated INTEGER      NOT NULL DEFAULT 0,
  created_at            TIMESTAMP    NOT NULL DEFAULT now(),

  CONSTRAINT chk_pattern       CHECK (pattern       IN ('DAILY','WEEKLY','MONTHLY')),
  CONSTRAINT chk_end_condition CHECK (end_condition IN ('NEVER','UNTIL_DATE','COUNT')),
  CONSTRAINT uq_schedule_recurrence UNIQUE (schedule_id)
);

CREATE INDEX idx_sr_schedule ON schedule_recurrence(schedule_id);
```

---

### 3.4 V21 — Indexes & Triggers

**File:** `db/migration/V21__schedule_indexes_triggers.sql`

```sql
-- updated_at trigger for class_schedules
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_class_schedules_updated_at
  BEFORE UPDATE ON class_schedules
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Composite index for calendar queries
CREATE INDEX IF NOT EXISTS idx_so_schedule_date
  ON schedule_occurrences(schedule_id, occurrence_date);

-- Partial index for conflict checks (only active schedules)
CREATE INDEX IF NOT EXISTS idx_cs_active
  ON class_schedules(teacher_profile_id, start_date)
  WHERE status != 'CANCELLED';

-- Dedup index for conflicts
CREATE INDEX IF NOT EXISTS idx_sc_pair
  ON schedule_conflicts(schedule_id_1, schedule_id_2);
```

---

## 4. Domain Model (Java)

**Package:** `com.educationpro.schedule.domain`

### 4.1 Enums

```java
public enum ScheduleTab      { CLASSES, EVENTS, HOLIDAYS, OTHERS }
public enum DateMode         { SINGLE, MULTIPLE, RECURRING }
public enum ScheduleType     { REGULAR, REVISION, EXTRA, PRACTICAL, EXAM_PREP, PARENT, WORKSHOP }
public enum ScheduleStatus   { DRAFT, ACTIVE, CANCELLED, COMPLETED }
public enum RecurrencePattern{ DAILY, WEEKLY, MONTHLY }
public enum EndCondition     { NEVER, UNTIL_DATE, COUNT }
public enum ConflictType     { TEACHER, GROUP, CLASSROOM }
```

### 4.2 ClassSchedule — New Fields

Extend the existing `ClassSchedule` entity with:

| Field | Type | Notes |
|---|---|---|
| `scheduleTab` | `ScheduleTab` | `@Enumerated(STRING)`, `@Column(nullable=false)` |
| `dateMode` | `DateMode` | `@Enumerated(STRING)`, `@Column(nullable=false)` |
| `multipleDates` | `List<LocalDate>` | `@Type(JsonType)` for JSONB |
| `learningObjectives` | `List<String>` | `@Type(JsonType)` for JSONB array |
| `attendanceRequired` | `boolean` | `default false` |
| `topic` | `String` | max 200 |
| `description` | `String` | TEXT |
| `eventTitle` | `String` | max 200, used for non-CLASSES tabs |
| `location` | `String` | max 200 |
| `audience` | `String` | `@Column` |
| `reminderMinutes` | `Integer` | nullable |
| `assignmentSession` | `@ManyToOne(fetch=LAZY) AssignmentSession` | nullable FK |
| `recurrence` | `@OneToOne(cascade=ALL, orphanRemoval=true) ScheduleRecurrence` | nullable |
| `occurrences` | `@OneToMany(mappedBy="schedule", cascade=ALL) List<ScheduleOccurrence>` | lazy |
| `conflicts` | `@OneToMany(mappedBy="schedule") List<ScheduleConflict>` | lazy |

### 4.3 ScheduleRecurrence (New Entity)

```java
@Entity
@Table(name = "schedule_recurrence")
public class ScheduleRecurrence {
    @Id @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private ClassSchedule schedule;

    @Enumerated(STRING) @Column(nullable = false)
    private RecurrencePattern pattern;

    @Column(nullable = false)
    private int intervalValue = 1;

    @Column(columnDefinition = "integer[]")
    private List<Integer> daysOfWeek;   // 0=Mon .. 6=Sun

    @Enumerated(STRING) @Column(nullable = false)
    private EndCondition endCondition = EndCondition.NEVER;

    private LocalDate endDate;
    private Integer occurrenceCount;

    @Column(nullable = false)
    private int occurrencesGenerated = 0;

    private Instant createdAt = Instant.now();
}
```

---

## 5. DTOs / Request & Response Contracts

**Package:** `com.educationpro.schedule.dto`

### 5.1 CreateScheduleRequest

| Field | Type | Validation |
|---|---|---|
| `scheduleTab` | `ScheduleTab` | `@NotNull` |
| `teacherProfileId` | `Long` | `@NotNull` for CLASSES tab |
| `groupId` | `Long` | `@NotNull` for CLASSES tab |
| `subjectNodeId` | `Long` | `@NotNull` for CLASSES tab |
| `scheduleType` | `ScheduleType` | `@NotNull` for CLASSES tab |
| `assignmentSessionId` | `Long` | nullable — re-validated server-side |
| `dateMode` | `DateMode` | `@NotNull` |
| `startDate` | `LocalDate` | `@NotNull` |
| `endDate` | `LocalDate` | nullable — for date range (Holidays) |
| `multipleDates` | `List<LocalDate>` | required when `dateMode=MULTIPLE`; max 30 |
| `startTime` | `LocalTime` | `@NotNull` for CLASSES/EVENTS/OTHERS |
| `endTime` | `LocalTime` | `@NotNull`; must be after `startTime` |
| `topic` | `String` | `@Size(max=200)` for CLASSES |
| `classroomId` | `Long` | nullable |
| `description` | `String` | `@Size(max=5000)` |
| `learningObjectives` | `List<String>` | max 20 items, each max 100 chars |
| `attendanceRequired` | `Boolean` | default false |
| `reminderMinutes` | `Integer` | nullable; min 5, max 10080 |
| `eventTitle` | `String` | `@NotBlank` for EVENTS/HOLIDAYS/OTHERS; max 200 |
| `location` | `String` | max 200 |
| `audience` | `String` | `ALL|TEACHERS|STUDENTS|PARENTS` |
| `recurrence` | `RecurrenceRequest` | required when `dateMode=RECURRING` |
| `equipmentIds` | `List<Long>` | nullable |

### 5.2 RecurrenceRequest (nested)

| Field | Type | Validation |
|---|---|---|
| `pattern` | `RecurrencePattern` | `@NotNull` |
| `intervalValue` | `Integer` | default 1; min 1, max 52 |
| `daysOfWeek` | `List<Integer>` | 0=Mon..6=Sun; required when `pattern=WEEKLY` |
| `endCondition` | `EndCondition` | `@NotNull` |
| `endDate` | `LocalDate` | required when `endCondition=UNTIL_DATE` |
| `occurrenceCount` | `Integer` | required when `endCondition=COUNT`; min 1, max 365 |

### 5.3 ScheduleResponseDto

| Field | Type | Notes |
|---|---|---|
| `id` | `Long` | schedule primary key |
| `scheduleTab` | `ScheduleTab` | |
| `teacherName` | `String` | from `teacher_profiles` + `users` |
| `groupName` | `String` | from `assignment_groups` |
| `studentCount` | `Integer` | count from `assignment_group_students` |
| `subjectName` | `String` | from `course_nodes` |
| `scheduleType` | `ScheduleType` | |
| `scheduleTypeName` | `String` | display label e.g. "Regular Class" |
| `dateMode` | `DateMode` | |
| `startDate` | `LocalDate` | |
| `endDate` | `LocalDate` | nullable |
| `startTime` | `LocalTime` | |
| `endTime` | `LocalTime` | |
| `durationLabel` | `String` | e.g. "1 Hour 30 Minutes" (computed) |
| `topic` | `String` | |
| `classroomName` | `String` | nullable |
| `description` | `String` | |
| `learningObjectives` | `List<String>` | |
| `attendanceRequired` | `boolean` | |
| `status` | `ScheduleStatus` | |
| `isRecurring` | `boolean` | derived |
| `recurrence` | `RecurrenceDto` | nullable |
| `occurrenceCount` | `Integer` | total generated |
| `hasConflict` | `boolean` | true if any active conflict |
| `conflicts` | `List<ConflictSummaryDto>` | |
| `createdAt` | `Instant` | |

### 5.4 ScheduleCalendarDto (lightweight, for grid rendering)

Used by `GET /api/admin/schedules/calendar`:

```java
public record ScheduleCalendarDto(
    Long id,
    ScheduleTab scheduleTab,
    String title,           // subjectName for CLASSES, eventTitle otherwise
    String teacherName,
    String groupName,
    LocalDate occurrenceDate,
    LocalTime startTime,
    LocalTime endTime,
    String colorClass,      // e.g. "schedule-card--classes"
    ScheduleStatus status,
    boolean isRecurring
) {}
```

### 5.5 ScheduleStatsDto

```java
public record ScheduleStatsDto(
    long totalSchedules,
    long thisWeekSchedules,
    long activeConflicts,
    long completedSchedules
) {}
```

### 5.6 ConflictSummaryDto

```java
public record ConflictSummaryDto(
    ConflictType conflictType,
    Long conflictingScheduleId,
    String conflictingScheduleTitle,
    LocalDate conflictDate,
    LocalTime conflictStartTime,
    LocalTime conflictEndTime,
    String message           // human-readable explanation
) {}
```

### 5.7 Dropdown DTOs

```java
public record TeacherDropdownDto(Long id, String fullName, String subjectArea) {}

public record GroupDropdownDto(
    Long id, String groupName, int studentCount, String academicYear, Long sessionId
) {}

public record SubjectDropdownDto(Long nodeId, String subjectName) {}

public record ClassroomDto(Long id, String name, String roomNumber, int capacity) {}
```

---

## 6. REST API Endpoints

**Base path:** `/api/admin/schedules`  
**Security:** `@PreAuthorize("hasRole('ADMIN')")`  
**Response wrapper:** standard `ApiResponse<T>`

| Method | Path | Response | Description |
|---|---|---|---|
| `POST` | `/api/admin/schedules` | `ScheduleResponseDto` | Create schedule. Runs conflict detection. Generates occurrences for RECURRING mode. |
| `GET` | `/api/admin/schedules/{id}` | `ScheduleResponseDto` | Full detail including recurrence + occurrences. |
| `PUT` | `/api/admin/schedules/{id}` | `ScheduleResponseDto` | Update schedule. Re-runs conflict detection. For recurring: `?updateMode=THIS_ONLY|THIS_AND_FUTURE|ALL`. |
| `DELETE` | `/api/admin/schedules/{id}` | `204 No Content` | Cascades to occurrences, recurrence, conflicts via DB constraints. |
| `GET` | `/api/admin/schedules/calendar` | `List<ScheduleCalendarDto>` | Params: `from`, `to`, `teacherId?`, `groupId?`, `type?`, `status?`. Returns lightweight cards for the grid. |
| `GET` | `/api/admin/schedules/stats` | `ScheduleStatsDto` | Quick stats for the sidebar panel. |
| `GET` | `/api/admin/schedules/session/{sessionId}` | `List<ScheduleResponseDto>` | All schedules for a given `assignment_session_id`. |
| `POST` | `/api/admin/schedules/{id}/cancel` | `ScheduleResponseDto` | Sets `status=CANCELLED`. Body: `{ reason: String }`. |
| `GET` | `/api/admin/schedules/conflicts` | `List<ConflictSummaryDto>` | All active unresolved conflicts. Param: `type?` (TEACHER\|GROUP\|CLASSROOM). |
| `POST` | `/api/admin/schedules/conflicts/{id}/resolve` | `204 No Content` | Marks a conflict as resolved. |
| `GET` | `/api/admin/schedules/teachers` | `List<TeacherDropdownDto>` | Teachers with at least one SAVED assignment session. |
| `GET` | `/api/admin/schedules/groups` | `List<GroupDropdownDto>` | Groups linked to a teacher. Param: `teacherProfileId` (required). |
| `GET` | `/api/admin/schedules/subjects` | `List<SubjectDropdownDto>` | Subjects within the scope of a given session. Param: `sessionId`. |
| `GET` | `/api/admin/schedules/classrooms` | `List<ClassroomDto>` | All active classrooms. |
| `POST` | `/api/admin/schedules/check-conflicts` | `List<ConflictSummaryDto>` | **Dry-run** conflict check. Same body as `POST /schedules`. Does NOT persist. Used for real-time preview. |

### 6.1 GET /api/admin/schedules/calendar — Detail

- Queries `class_schedules JOIN schedule_occurrences`. For `SINGLE`/`MULTIPLE` date mode schedules, a synthetic occurrence is produced in-memory for each date.
- For `RECURRING` schedules, occurrences are read from the `schedule_occurrences` table.
- `colorClass` maps to CSS: `schedule-card--classes`, `schedule-card--events`, `schedule-card--holidays`, `schedule-card--others`, `schedule-card--cancelled`.
- `title` = `subjectName` for CLASSES tab, `eventTitle` for all other tabs.

### 6.2 Conflict Detection Logic

`ScheduleService.detectConflicts(request)` performs three checks in sequence:

1. **Teacher conflict:** query `class_schedules` where `teacher_profile_id = :teacherId` AND time ranges overlap AND `status != 'CANCELLED'`.
2. **Group conflict:** same pattern filtered on `group_id`.
3. **Classroom conflict:** same pattern filtered on `classroom_id` (only when `classroomId` is provided).

**Overlap condition:** `newStart < existingEnd AND newEnd > existingStart` (standard interval overlap).

For RECURRING mode, conflicts are checked against all generated occurrence dates up to 1 year out.

If conflicts exist on `POST /schedules`, the schedule is still saved (`status=DRAFT`) and conflicts are persisted to `schedule_conflicts`. The response includes `hasConflict=true` and the `conflicts` list. The UI shows an orange warning banner.

---

## 7. Service Layer — ScheduleService (Refactored)

**Package:** `com.educationpro.schedule.service`

### 7.1 Public Methods

| Method Signature | Responsibility |
|---|---|
| `createSchedule(CreateScheduleRequest req, Long adminId)` | Validates assignment pairing, generates occurrences, runs conflict detection, persists `ClassSchedule` + `ScheduleRecurrence` + `ScheduleOccurrence[]`, returns `ScheduleResponseDto`. |
| `updateSchedule(Long id, CreateScheduleRequest req, UpdateMode mode)` | For recurring schedules, `mode` controls whether to update a single occurrence, this-and-future, or all. Re-runs conflict detection. |
| `deleteSchedule(Long id)` | Soft-delete: sets `status=CANCELLED`. Hard-delete on confirmation. |
| `getCalendarData(LocalDate from, LocalDate to, ScheduleFilters filters)` | Returns `List<ScheduleCalendarDto>` optimised for grid rendering. |
| `getStats()` | Aggregation query returning `ScheduleStatsDto`. |
| `generateOccurrences(ClassSchedule schedule, ScheduleRecurrence recurrence)` | Pure function: produces all `LocalDate` occurrences up to the end condition. Max 365 occurrences safety cap. |
| `detectConflicts(CreateScheduleRequest req)` | Runs three conflict checks, returns `List<ConflictSummaryDto>`. |
| `getTeachersForDropdown()` | Joins `assignment_sessions → assignment_teacher_mappings → teacher_profiles` where `status='SAVED'`. |
| `getGroupsForTeacher(Long teacherProfileId)` | Returns groups assigned to the given teacher across all `SAVED` sessions. |
| `getSubjectsForSession(Long sessionId)` | Fetches `scope_node` from `assignment_sessions`, traverses `course_nodes` to find subject-level (L2) children. |

### 7.2 generateOccurrences Algorithm

1. Start from `startDate`.
2. If `pattern=DAILY`: add `startDate`, then increment by `intervalValue` days until end condition met.
3. If `pattern=WEEKLY`: for each week, iterate `daysOfWeek` list; add matching dates; advance by `intervalValue` weeks.
4. If `pattern=MONTHLY`: add same day-of-month each month, increment by `intervalValue` months.
5. Stop when: `endCondition=COUNT` and count reached, OR `endCondition=UNTIL_DATE` and date > `endDate`, OR `endCondition=NEVER` and 365 occurrences generated (safety cap).
6. Persist each occurrence as `ScheduleOccurrence` with `status=SCHEDULED`.
7. Update `recurrence.occurrencesGenerated` counter.

### 7.3 Assignment Validation

Before persisting any CLASSES-tab schedule, `ScheduleService.createSchedule()` validates:

- A `SAVED` assignment session exists where `teacher_profile_id` matches the requested `teacherProfileId` AND `group_id` matches `groupId`.
- The session's `scope_node` is an ancestor-or-equal of the requested `subjectNodeId` (using `course_nodes` parent traversal).
- If validation fails: `throw new BusinessException(400, "SCHEDULE_001", "Teacher is not assigned to this group in any active session")`.

---

## 8. Frontend Implementation

### 8.1 schedule.html Structure

```html
<!-- Standard EduPro Admin shell (sidebar + topbar) -->
<!-- th:src="@{/css/schedule.css(v=1)}" -->
<!-- th:src="@{/js/schedule.js(v=1)}" defer -->

<div id="schedulePageContent">

  <!-- Zone A: Topbar Controls -->
  <div id="scheduleTopbar" class="d-flex align-items-center justify-content-between mb-3">
    <!-- Today | prev/next | date range | Day/Week/Month/Agenda | Filter | + New Schedule -->
  </div>

  <div class="d-flex gap-0" style="height: calc(100vh - 140px);">

    <!-- Zone B: Filter Sidebar -->
    <aside id="filterSidebar" class="filter-sidebar">
      <!-- Teacher, Group, Type, Date Range, Status dropdowns -->
      <!-- Apply Filters button -->
      <!-- Schedule Types legend checkboxes -->
      <!-- Quick Stats 2x2 grid -->
    </aside>

    <!-- Zone C: Calendar Grid -->
    <main id="calendarGrid" class="flex-grow-1 overflow-auto">
      <!-- Rendered by schedule.js: renderWeekView() / renderMonthView() etc. -->
    </main>

  </div>

  <!-- Zone D: Add/Edit Schedule Panel -->
  <aside id="addSchedulePanel" class="add-schedule-panel">
    <div class="panel-header d-flex justify-content-between align-items-center p-3 border-bottom">
      <h6 class="fw-700 mb-0">Add Schedule</h6>
      <button id="closePanelBtn" class="btn-close"></button>
    </div>
    <ul class="nav nav-pills panel-tabs px-3 pt-3 gap-2" id="scheduleTabs">
      <li><button class="nav-link active" data-tab="CLASSES">Classes</button></li>
      <li><button class="nav-link" data-tab="EVENTS">Events</button></li>
      <li><button class="nav-link" data-tab="HOLIDAYS">Holidays</button></li>
      <li><button class="nav-link" data-tab="OTHERS">Others</button></li>
    </ul>
    <div id="panelFormBody" class="px-3 pb-3">
      <!-- Rendered by schedule.js: renderPanelForm() -->
    </div>
  </aside>

</div>
```

### 8.2 schedule.js — State Object

```javascript
var S = {
  view:             'week',          // 'week' | 'day' | 'month' | 'agenda'
  currentDate:      new Date(),      // anchor date for the visible range
  filters: {
    teacherId: null, groupId: null,
    type: null, dateFrom: null, dateTo: null, status: null
  },
  calendarData:     [],              // Array of ScheduleCalendarDto
  stats:            {},              // ScheduleStatsDto
  teachers:         [],              // TeacherDropdownDto[]
  groups:           [],              // GroupDropdownDto[] (filtered by teacher)
  subjects:         [],              // SubjectDropdownDto[]
  classrooms:       [],              // ClassroomDto[]
  panelOpen:        false,
  editingScheduleId: null,
  panelTab:         'CLASSES',
  pendingConflicts: [],              // ConflictSummaryDto[] from check-conflicts
};
```

### 8.3 Key JS Functions

| Function | Responsibility |
|---|---|
| `init()` | Calls `loadDropdowns()`, `loadStats()`, `loadCalendar()`. Attaches all event listeners. |
| `loadCalendar()` | `GET /calendar` with current date range + filters → `S.calendarData` → `renderCalendar()`. |
| `renderCalendar()` | Dispatches to `renderWeekView()` / `renderDayView()` / `renderMonthView()` / `renderAgendaView()`. |
| `renderWeekView()` | Builds 8-column grid. For each `ScheduleCalendarDto`, calculates column (Mon–Sun) and top offset (px) from `startTime`. Creates `.schedule-card` div with correct `colorClass`. |
| `openPanel(tab, scheduleId)` | Sets `S.panelOpen=true`, `S.panelTab`, `S.editingScheduleId`. Renders form fields. Slides panel in. |
| `closePanel()` | Resets panel state. Slides panel out via CSS transform. |
| `onTeacherChange(teacherId)` | `GET /groups?teacherProfileId=` → populates group dropdown. |
| `onGroupChange(groupId)` | `GET /subjects?sessionId=` → populates subject dropdown. |
| `onDateModeChange(mode)` | Shows/hides date fields: single date picker / multi-date picker / recurrence block. |
| `checkConflictsRealtime()` | Debounced 500ms. `POST /check-conflicts` with current form state → updates `S.pendingConflicts` → renders conflict banner. |
| `saveSchedule()` | Validates form. `POST /schedules` or `PUT /schedules/{id}`. On success: `closePanel()`, `loadCalendar()`, `loadStats()`, `showToast()`. |
| `deleteSchedule(id)` | Confirms with modal. `DELETE /schedules/{id}`. Refreshes grid. |
| `navigateDate(direction)` | Advances `S.currentDate` by ±1 week/day/month. Calls `loadCalendar()`. |
| `applyFilters()` | Reads filter form values into `S.filters`. Calls `loadCalendar()`. |
| `clearFilters()` | Resets `S.filters` to nulls. Calls `loadCalendar()`. |
| `renderStats(stats)` | Updates Quick Stats card values in the filter sidebar. |

### 8.4 schedule.css — Key Classes

```css
/* Calendar grid */
.calendar-grid {
  display: grid;
  grid-template-columns: 80px repeat(7, 1fr);
  gap: 1px;
  background: #E2E8F0;
}
.calendar-col-header {
  background: #fff;
  padding: 8px 12px;
  font-weight: 600;
  font-size: .8125rem;
  color: #1a2332;
}
.col-today .calendar-col-header {
  background: #EFF6FF;
  color: #1E40AF;
}
.time-slot {
  height: 60px;
  border-bottom: 1px solid #F1F5F9;
  position: relative;
}

/* Schedule cards */
.schedule-card {
  position: absolute;
  left: 4px; right: 4px;
  border-radius: 6px;
  padding: 4px 6px;
  font-size: .75rem;
  cursor: pointer;
  border-left: 3px solid;
  background: #fff;
  box-shadow: 0 1px 2px rgba(0,0,0,.06);
}
.schedule-card--classes  { border-left-color: #3B82F6; background: #EFF6FF; }
.schedule-card--events   { border-left-color: #14B8A6; background: #F0FDFA; }
.schedule-card--holidays { border-left-color: #22C55E; background: #F0FDF4; }
.schedule-card--others   { border-left-color: #F97316; background: #FFF7ED; }
.schedule-card--cancelled { opacity: .5; text-decoration: line-through; }

/* Add/Edit panel */
.add-schedule-panel {
  position: fixed;
  right: 0; top: 64px;
  width: 400px;
  height: calc(100vh - 64px);
  background: #fff;
  box-shadow: -4px 0 24px rgba(0,0,0,.1);
  transform: translateX(100%);
  transition: transform .3s ease;
  overflow-y: auto;
  z-index: 1040;
}
.add-schedule-panel.open { transform: translateX(0); }

/* Panel tabs */
.panel-tabs .nav-link.active {
  background: var(--bg-admin);
  color: #fff;
  border-radius: 8px;
}

/* Filter sidebar */
.filter-sidebar {
  width: 240px;
  flex-shrink: 0;
  background: #fff;
  border-right: 1px solid #E2E8F0;
  padding: .75rem;
  overflow-y: auto;
}

/* Quick Stats */
.stat-card {
  background: #F8FAFC;
  border-radius: 10px;
  padding: 12px;
  text-align: center;
}
.stat-card__value { font-size: 1.5rem; font-weight: 700; color: #1a2332; }
.stat-card--conflict  .stat-card__value { color: #F97316; }
.stat-card--completed .stat-card__value { color: #22C55E; }

/* Conflict banner */
.conflict-banner {
  background: #FFF7ED;
  border: 1px solid #FED7AA;
  border-radius: 8px;
  padding: 10px 14px;
  margin-bottom: 12px;
  display: none;
}
.conflict-banner.visible { display: block; }

/* Recurring badge on cards */
.recurring-badge {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: .65rem;
  color: #6B7280;
}
```

---

## 9. Integration with Module 4 (Assignments)

The CLASSES tab is tightly coupled to Module 4 data. No schedule can be created for a teacher-group pair that does not exist as a `SAVED` assignment.

### 9.1 Data Flow

1. Admin opens Add Schedule panel → Classes tab.
2. `GET /api/admin/schedules/teachers` → queries `assignment_sessions (status=SAVED)` → `assignment_teacher_mappings` → `teacher_profiles`. Returns id + full name.
3. Admin selects teacher → `GET /api/admin/schedules/groups?teacherProfileId=X` → groups in same session. Returns group id, name, student count, academic year.
4. Admin selects group → `GET /api/admin/schedules/subjects?sessionId=Y` → fetches session's `scope_node`, reads `course_nodes` children to populate Subject dropdown.
5. On save, `ScheduleService.createSchedule()` re-validates the teacher-group-session triple server-side.

### 9.2 SQL Queries

**Teachers dropdown:**
```sql
SELECT DISTINCT
  tp.id,
  CONCAT(u.first_name, ' ', u.last_name) AS name
FROM assignment_sessions ases
JOIN assignment_teacher_mappings atm ON atm.session_id = ases.id
JOIN teacher_profiles tp             ON tp.id = atm.teacher_profile_id
JOIN users u                         ON u.id  = tp.user_id
WHERE ases.status = 'SAVED'
ORDER BY name;
```

**Groups for a teacher:**
```sql
SELECT DISTINCT
  ag.id,
  ag.group_name,
  COUNT(ags.student_profile_id) AS student_count,
  ases.academic_year
FROM assignment_sessions ases
JOIN assignment_teacher_mappings atm ON atm.session_id = ases.id
JOIN assignment_groups ag            ON ag.session_id  = ases.id
                                    AND ag.id          = atm.group_id
LEFT JOIN assignment_group_students ags ON ags.group_id = ag.id
WHERE ases.status = 'SAVED'
  AND atm.teacher_profile_id = :teacherProfileId
GROUP BY ag.id, ag.group_name, ases.academic_year;
```

### 9.3 Group Dropdown Display

The Group dropdown renders:

```
Group A (Class 7 - A)
28 Students • Academic Year 2024-25
```

This matches the requirement screenshot. The second line is rendered as a small muted `<div>` inside a custom dropdown option, using `GroupDropdownDto.studentCount` and `GroupDropdownDto.academicYear`.

---

## 10. Implementation Plan

| # | Phase | Tasks | Files / Migrations |
|---|---|---|---|
| 1 | DB Extensions | Run V19, V20, V21. Verify `ADD COLUMN IF NOT EXISTS` guards. Add test classrooms + sample schedule. | V19, V20, V21 |
| 2 | Domain & Enums | Add all 7 enums. Extend `ClassSchedule` entity. Create `ScheduleRecurrence` entity. Update repositories. | Domain package |
| 3 | DTOs | Create all 8 DTOs listed in Section 5. | `dto` package |
| 4 | Service — Dropdowns | Implement `getTeachersForDropdown()`, `getGroupsForTeacher()`, `getSubjectsForSession()`. Write JUnit tests. | `ScheduleService` |
| 5 | Service — Core CRUD | Implement `createSchedule()` (assignment validation + conflict detection + occurrence generation), `updateSchedule()`, `deleteSchedule()`, `getCalendarData()`, `getStats()`. | `ScheduleService` |
| 6 | Controller | Add all 15 endpoints from Section 6. Wire bean validation. Add `GlobalExceptionHandler` cases for new error codes. | `ScheduleController` |
| 7 | Frontend — HTML | Replace `schedule.html` with new three-zone layout. Keep shell chrome identical to other admin pages. | `schedule.html` |
| 8 | Frontend — CSS | Create `schedule.css` with all classes from Section 8.4. | `schedule.css` |
| 9 | Frontend — JS Core | Implement `schedule.js` with state object `S`, `init()`, `loadCalendar()`, `renderWeekView()`, `renderStats()`. | `schedule.js` |
| 10 | Frontend — Panel | Implement `openPanel()`, form rendering for all four tabs, `onTeacherChange()`, `onGroupChange()`, `onDateModeChange()`, `checkConflictsRealtime()`, `saveSchedule()`. | `schedule.js` |
| 11 | Frontend — Navigation | `navigateDate()`, `applyFilters()`, `clearFilters()`, view toggle, `renderMonthView()`, `renderAgendaView()`. | `schedule.js` |
| 12 | QA & Integration | End-to-end: create recurring schedule, verify occurrences, verify conflict detection, verify teacher-group validation rejects unassigned pairs. | Tests |

> **Note:** Phases 1–6 (backend) can be completed and tested via Postman before any frontend work begins.

---

## 11. Non-Functional Requirements

### 11.1 Performance

- `GET /calendar` for a 1-week range with up to 500 schedule cards: **< 300ms** (matches existing NFR).
- Occurrence generation for a 1-year daily recurring schedule (≤365 rows): **< 100ms**.
- Conflict detection across up to 500 existing schedules: **< 200ms**.
- Calendar grid re-render on filter change (JS-only, no network): **< 100ms**.

### 11.2 Security

- All `/api/admin/schedules/**` endpoints require `ADMIN` role (`@PreAuthorize`).
- Teacher/group IDs in create requests are validated server-side — no cross-tenant data leak.
- All SQL uses parameterised queries via JPA; no string interpolation.

### 11.3 Accessibility

- Calendar grid cells: `aria-label="Monday 12 May"` etc.
- Schedule cards: `role="button"` with `aria-label` containing schedule details.
- Panel form: `<label for=…>` on all inputs.
- Color is never the only differentiator — cards also show a type icon (`bi-book`, `bi-calendar-event`, etc.).

### 11.4 CLAUDE.md Updates Required After Implementation

- New endpoint list (Section 6 of this spec)
- New migrations V19–V21 added to Flyway migration order
- Updated Flyway order: `...V18__schedule_equipment.sql` → `V19__extend_class_schedules.sql` → `V20__schedule_recurrence_full.sql` → `V21__schedule_indexes_triggers.sql`
- Frontend paths: `schedule.html` (replaced), `schedule.js` (replaced), `schedule.css` (new)
- Module 4 integration notes

---

## 12. Appendix — Quick Reference

### 12.1 File Inventory

| Op | File Path | Action |
|---|---|---|
| `NEW` | `db/migration/V19__extend_class_schedules.sql` | ALTER TABLE class_schedules |
| `NEW` | `db/migration/V20__schedule_recurrence_full.sql` | Full recurrence table |
| `NEW` | `db/migration/V21__schedule_indexes_triggers.sql` | Indexes + updated_at trigger |
| `EXTEND` | `src/.../schedule/domain/ClassSchedule.java` | Add new @Column fields + relationships |
| `NEW` | `src/.../schedule/domain/ScheduleRecurrence.java` | New entity |
| `NEW` | `src/.../schedule/domain/enums/*.java` | 7 new enums |
| `NEW` | `src/.../schedule/dto/CreateScheduleRequest.java` | Full request DTO |
| `NEW` | `src/.../schedule/dto/ScheduleResponseDto.java` | Full response DTO |
| `NEW` | `src/.../schedule/dto/ScheduleCalendarDto.java` | Lightweight card DTO |
| `NEW` | `src/.../schedule/dto/ScheduleStatsDto.java` | Stats DTO |
| `NEW` | `src/.../schedule/dto/ConflictSummaryDto.java` | Conflict detail DTO |
| `NEW` | `src/.../schedule/dto/TeacherDropdownDto.java` | Dropdown DTO |
| `NEW` | `src/.../schedule/dto/GroupDropdownDto.java` | Dropdown DTO with student count |
| `NEW` | `src/.../schedule/dto/SubjectDropdownDto.java` | Dropdown DTO |
| `REFACTOR` | `src/.../schedule/service/ScheduleService.java` | Full refactor per Section 7 |
| `REFACTOR` | `src/.../schedule/controller/ScheduleController.java` | Add all 15 endpoints per Section 6 |
| `REPLACE` | `src/main/resources/templates/admin/schedule.html` | New three-zone Thymeleaf template |
| `NEW` | `src/main/resources/static/css/schedule.css` | Calendar + panel styles |
| `REPLACE` | `src/main/resources/static/js/schedule.js` | New JS per Section 8 |

### 12.2 Key Invariants

- A CLASSES schedule **must** reference a valid `SAVED` assignment session. Orphan schedules are not allowed.
- Occurrences are always child records — deleting a `ClassSchedule` cascades to all occurrences via DB constraint.
- Conflicts are **informational, not blocking**. The admin can save a schedule with conflicts but receives a clear warning.
- Teacher and Group dropdowns are always derived from Module 4 data — they never accept free-text entry.
- `schedule_tab` (CLASSES/EVENTS/HOLIDAYS/OTHERS) is immutable after creation. To change type, delete and recreate.
- Recurring schedules generate occurrences once at creation. If dates/times are edited, occurrences are deleted and regenerated.

### 12.3 Error Codes

| Code | HTTP | Message |
|---|---|---|
| `SCHEDULE_001` | 400 | Teacher is not assigned to this group in any active session. |
| `SCHEDULE_002` | 400 | End time must be after start time. |
| `SCHEDULE_003` | 400 | Multiple dates mode requires at least 2 dates. |
| `SCHEDULE_004` | 400 | Recurring schedule requires recurrence configuration. |
| `SCHEDULE_005` | 400 | Weekly recurrence requires at least one day of week selected. |
| `SCHEDULE_006` | 400 | Until-date end condition requires an end date after the start date. |
| `SCHEDULE_007` | 400 | Occurrence count must be between 1 and 365. |
| `SCHEDULE_008` | 404 | Schedule not found. |
| `SCHEDULE_009` | 409 | Schedule conflicts detected — see `conflicts[]` in response body. |
| `SCHEDULE_010` | 400 | Classroom capacity exceeded for this group size. |

---

*End of Document*
