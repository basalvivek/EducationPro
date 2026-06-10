# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Status

Web application running on `localhost:9090` (dev profile). Modules 1–3 fully implemented. Module 4 (teacher/student management + assignments) partially implemented — backend persists but frontend listing/detail views are stubs. `docs/EducationPro_Specs_M1_M2.md` is the authoritative spec (covers M1–M4). All implementation decisions must align with it.

Default admin credentials: `admin@educationpro.com` / `Admin@123`

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.x, Java 17+ |
| ORM | Spring Data JPA + Hibernate |
| Database | PostgreSQL 15+ | Docker running on my machine 
| Security | Spring Security 6 + JWT (JJWT 0.11+) |
| Frontend | Thymeleaf + Bootstrap 5.3 + Vanilla JS |
| Build | Maven 3.9+ |
| Migrations | Flyway (`db/migration/V{n}__{desc}.sql`) |
| Email | Spring Mail (SMTP) |

## Build & Run Commands

Once the project is scaffolded:

```bash
# Build
mvn clean package -DskipTests

# Run
mvn spring-boot:run

# Run tests
mvn test

# Run single test class
mvn test -Dtest=CourseNodeServiceTest

# Run with profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Architecture

### Module 1 — Multi-Role Login (`/auth/**`)

- Four roles: `ADMIN`, `TEACHER`, `STUDENT`, `PARENT`
- Login page (`login.html`) uses Thymeleaf + `login.js` for pure-JS role toggling (no reload)
- Form POSTs to `/auth/login` with hidden `role` field; backend validates role match
- JWT returned in response body; stored client-side and sent as `Authorization: Bearer <token>`
- Password reset: UUID token, 30-min TTL, stored in `password_reset_tokens`; forgot-password endpoint always returns 200 (no email enumeration)

### Module 2 — Admin Course Designer (`/admin/courses/design`)

- Self-referencing `course_nodes` table; `ON DELETE CASCADE` handles descendant cleanup
- Backend returns **flat list** from `GET /api/admin/course-nodes`; frontend builds nested tree via `buildTree(flat)`
- Two node types: `NODE` (can have children) and `QUESTION` (leaf, no children enforced at both DB constraint and service layer)
- `QUESTION` nodes have extra fields: `questionText`, `questionType` (MCQ/TRUE_FALSE/SHORT/ESSAY), `marks`
- Tree UI is custom Vanilla JS (no jsTree dependency required)
- **UI**: Premium SaaS redesign with gradient hero banner, modern form inputs, stats panel, activity card, micro-interactions (200-300ms transitions). Course Explorer with search, node count badge, color-coded tree items (blue course, purple board, cyan topic, amber question). Modals: larger textareas (8 rows, 100% width), floating labels, focus glow effects

### Module 3 — Assessment Designer (`/admin/exams/builder`)

- Three-column layout: sidebar | Question Picker (360px, fixed) | Exam Paper Builder (flex-grow)
- Cascade dropdowns (Class → Subject → Exam Board → Topic → Sub Topic) mirror the Course Designer tree; each level fetches direct `NODE` children via `GET /api/admin/question-search/tree-nodes?parentId=X`
- Question search uses BFS from selected node → collects all descendant IDs in memory → returns `QUESTION` nodes whose `parentId` is in that set (no recursive SQL)
- Exam lifecycle: `DRAFT` → `APPROVED` (via `POST /api/admin/exams/{id}/submit`)
- Total marks auto-calculated: sum of `marksOverride ?? question.marks ?? 1` per `exam_questions` row
- Auto-save: clicking `[+]` on a question when no exam exists triggers `saveExam()` first (requires name field filled)
- Reorder sends full ordered `questionId[]` list; backend validates length + ID membership
- Domains: `Exam`, `ExamQuestion`; repositories: `ExamRepository`, `ExamQuestionRepository`
- JS: `static/js/exam.js`; template: `templates/admin/exam-builder.html`

### Module 4 — Teacher & Student Management + Assignments (`/admin/teachers`, `/admin/students`, `/admin/assignments`)

#### Teacher Management (`/api/admin/teachers`)
- Domain: `TeacherProfile` (extended teacher data linked 1:1 to `users`); migration `V9__teacher_profiles.sql`
- Register: `POST /api/admin/teachers` (multipart — profile photo + 6 document uploads)
- List: `GET /api/admin/teachers` → `TeacherSummaryDto[]`; `TeacherSummaryDto.id` = `teacher_profiles.id`
- Service: `TeacherManagementService`; Controller: `TeacherManagementController`

#### Student Management (`/api/admin/students`)
- Domain: `StudentProfile`; migration `V10__student_profiles.sql`
- Register: `POST /api/admin/students` (multipart); List: `GET /api/admin/students` → `StudentSummaryDto[]`
- `StudentSummaryDto.id` = `student_profiles.id` — use this as foreign key in assignments
- Service: `StudentManagementService`; Controller: `StudentManagementController`

#### Approvals (`/admin/approvals`)
- Template: `templates/admin/approvals.html`; approval items fetched from `/api/admin/approvals` (planned)
- Currently a UI stub — lists pending teacher/student registrations with approve/reject buttons

#### Assignments (`/admin/assignments`)
- Three-step flow: Select Course → Create Groups (with student enrolment) → Assign Teachers
- Frontend state: `var S` in `static/js/assignments.js` — holds `groups[]`, `assignments[]` (many-to-many array of {teacherId, groupId}), `activatedTeacherIds[]`, `scopeNodes`, `selectedScopeKey`
- **Many-to-Many Teachers-Groups**: Same teacher can be assigned to multiple groups; same group can have multiple teachers. `S.assignments` is array of `{teacherId, groupId}` pairs, not object. Each pair displays as separate table row with edit/delete buttons
- Scope selection: course (L1), subject (L2), or exam board (L3) radio — scope node ID persisted
- Search filter: real-time teacher search by name or department in Teachers & Assignments card
- Edit Group: add/remove students per group via modal with checkboxes
- Save endpoints:
  - `POST /api/admin/assignments` with `status=DRAFT` or `status=SAVED` → `AssignmentResultDto {sessionId, status, message}`
- DB tables (V11): `assignment_sessions`, `assignment_groups`, `assignment_group_students`, `assignment_teacher_mappings`
- DB constraint (V12): `assignment_teacher_mappings` UNIQUE on `(session_id, teacher_profile_id, group_id)` — allows same teacher in multiple groups per session
- Domains: `AssignmentSession`, `AssignmentGroup`, `AssignmentTeacherMapping`
- Repos: `AssignmentSessionRepository`, `AssignmentGroupRepository`, `AssignmentTeacherMappingRepository`
- Service: `AssignmentService`; Controller: `AssignmentController`
- **Key**: `groupLocalId` in JS payload maps frontend ephemeral group IDs to saved DB group IDs via `Map<Integer, AssignmentGroup>` in service
- **Key**: `assignment_group_students.student_profile_id` references `student_profiles.id`; `assignment_teacher_mappings.teacher_profile_id` references `teacher_profiles.id`

### Security Architecture

- Spring Security 6, stateless sessions (`STATELESS`)
- CSRF disabled (JWT-based)
- RBAC at controller layer via `@PreAuthorize("hasRole('ADMIN')")`
- Public paths: `/auth/**`, `/css/**`, `/js/**`
- Role-path mapping: `/admin/**` → ADMIN, `/teacher/**` → TEACHER, `/student/**` → STUDENT, `/parent/**` → PARENT

### Backend Patterns

- `@RestControllerAdvice` global handler catches `EntityNotFoundException` (404), `BusinessException` (400), `AccessDeniedException` (403), `MethodArgumentNotValidException` (400)
- Service layer is `@Transactional`; uses `@RequiredArgsConstructor` (Lombok)
- Mapper pattern: `CourseNodeMapper.toDto()`, `fromRequest()`, `applyUpdate()`
- `created_by` + `updated_at` on all domain tables; `updated_at` managed via Postgres trigger

### Frontend Patterns

- Role colours defined as CSS custom properties (`--role-admin`, `--bg-admin`, etc.) in `main.css`
- Custom Bootstrap overrides needed: `.btn-purple`, `.btn-orange`, `.badge.bg-purple`, `.badge.bg-orange`, `.border-purple`, `.border-orange`
- Font: Inter from Google Fonts
- Toast notifications via `showToast(message, type)` appended to `#toastContainer` (fixed, bottom-right)
- `escHtml()` required when injecting user content into `innerHTML`

#### Card / Panel Design System (applied to all admin pages)
- Workspace background: `background:#f8fafc; padding:.75rem; gap:.75rem`
- Panel cards: `background:#ffffff; border-radius:12px; box-shadow:0 1px 3px 0 rgba(0,0,0,.1),0 1px 2px -1px rgba(0,0,0,.08); border:1px solid #e2e8f0`
- Panel header icon squares: `width:28px;height:28px;border-radius:8px` with role-tinted background
- Panel header title: `font-weight:700; color:#1a2332; font-size:.875rem`
- Filter bar wrapper: `.filter-card` class
- Item grid cards: `.item-card` with `.item-card__bar--blue/purple/green` colored left bar
- Avatar circles: `.user-avatar.user-avatar--lg.user-avatar--blue/purple` gradient circles
- Empty states: `.empty-state` (full panel) or `.panel-empty` (inline)
- Bootstrap Icons 1.11.3 used throughout — no legacy text arrows (`▾`, `›`, `↑↓`); use `bi-chevron-down/right/up`
- Cache-bust CSS/JS in Thymeleaf: `th:href="@{/css/main.css(v=3)}"` pattern

### Admin Shell Layout

All admin pages share the same sidebar + topbar shell (no Thymeleaf fragments — duplicated across 10 templates). Update all when changing shared chrome.

**Sidebar** (`<nav class="sidebar">`)
- Background: `--sidebar-bg: #0c3577` (dark navy); width: 260px
- Scrollable nav (`overflow-y: auto`) with 7 categorized groups using `.sidebar__group-label`
- Disabled future items use `.nav-link.disabled` (38% opacity, pointer-events none)
- Groups: Overview | Academic Management | Faculty Management | Student Management | Assessment & Evaluation | Operations | Insights & Reporting
- Active items (implemented): Dashboard, Analytics, Design Courses, Assessment Designer, Teachers, Students, Approvals, Assignments

**Topbar** (`<header class="topbar">`)
- Right side: bell icon + user avatar dropdown
- User dropdown contains: **Super Admin** section header → Users, Roles & Permissions, Organizations, System Settings, Audit Logs (all disabled) → divider → Sign out
- No separate Administration button; all admin items live inside the user dropdown

## Database Schema Key Points

- `users.role` CHECK constraint: `('ADMIN','TEACHER','STUDENT','PARENT')`
- `course_nodes.parent_id` self-references with `ON DELETE CASCADE`
- `course_nodes` has DB-level CHECK: `type != 'QUESTION' OR parent_id IS NOT NULL` (questions must have a parent)
- `exams.status` CHECK constraint: `('DRAFT','APPROVED')`
- `exam_questions` has UNIQUE constraint on `(exam_id, question_id)`; `ON DELETE CASCADE` from both `exams` and `course_nodes`
- `assignment_sessions.status` CHECK: `('DRAFT','SAVED')`; `scope_level` CHECK: `('course','subject','board')`
- `assignment_teacher_mappings` UNIQUE on `(session_id, teacher_profile_id, group_id)` — allows same teacher in multiple groups per session
- `assignment_group_students` references `student_profiles(id)` NOT `users(id)`
- `assignment_teacher_mappings` references `teacher_profiles(id)` NOT `users(id)`
- Index on `users(email, role)` for login queries
- Flyway migration order:
  `V1__init_users.sql` → `V2__password_reset_tokens.sql` → `V3__course_nodes.sql` → `V4__course_nodes_expand.sql` → `V5__reset_admin_password.sql` → `V6__assessment_designer.sql` → `V7__add_teacher_user.sql` → `V8__add_pending_approval_status.sql` → `V9__teacher_profiles.sql` → `V10__student_profiles.sql` → `V11__assignments.sql` → `V12__fix_assignment_constraints.sql`

## NFRs to Keep in Mind

- API response target: <300ms for tree fetch up to 500 nodes
- JWT: HS256, 24h expiry
- WCAG 2.1 AA: ARIA labels on tree nodes, keyboard navigation
- Logging: SLF4J/Logback; log all auth events
- Spring Profiles: `dev`, `staging`, `prod` for DB creds and JWT secret
- Testing: JUnit 5 + Mockito (service layer); Playwright/Selenium (E2E login)

## Planned / Next Work

- Module 4 remaining: load saved assignment sessions on page open, edit/delete sessions, student course view
- Module 5: Student Course View (enrolled courses, quiz taking, progress tracking)
- Approvals: wire `/api/admin/approvals` endpoint to approve/reject pending teacher/student registrations
- Teacher portal: teacher-facing course designer and exam builder (templates exist as stubs in `templates/teacher/`)
