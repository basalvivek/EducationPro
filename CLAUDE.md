# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Status

Web application running on `localhost:9090` (dev profile). Modules 1, 2, and 3 are implemented. `docs/EducationPro_Specs_M1_M2.md` is the authoritative spec (covers M1–M3). All implementation decisions must align with it.

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

## Database Schema Key Points

- `users.role` CHECK constraint: `('ADMIN','TEACHER','STUDENT','PARENT')`
- `course_nodes.parent_id` self-references with `ON DELETE CASCADE`
- `course_nodes` has DB-level CHECK: `type != 'QUESTION' OR parent_id IS NOT NULL` (questions must have a parent)
- `exams.status` CHECK constraint: `('DRAFT','APPROVED')`
- `exam_questions` has UNIQUE constraint on `(exam_id, question_id)`; `ON DELETE CASCADE` from both `exams` and `course_nodes`
- Index on `users(email, role)` for login queries
- Flyway migration order: `V1__init_users.sql` → `V2__password_reset_tokens.sql` → `V3__course_nodes.sql` → `V4__course_nodes_expand.sql` → `V5__reset_admin_password.sql` → `V6__assessment_designer.sql`

## NFRs to Keep in Mind

- API response target: <300ms for tree fetch up to 500 nodes
- JWT: HS256, 24h expiry
- WCAG 2.1 AA: ARIA labels on tree nodes, keyboard navigation
- Logging: SLF4J/Logback; log all auth events
- Spring Profiles: `dev`, `staging`, `prod` for DB creds and JWT secret
- Testing: JUnit 5 + Mockito (service layer); Playwright/Selenium (E2E login)

## Planned Modules

- Module 4: Student Course View
