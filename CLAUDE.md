# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Status

Web application, currently hosted on `localhost`. No code exists yet. `docs/EducationPro_Specs_M1_M2.md` is the authoritative spec. All implementation decisions must align with it.

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
- Index on `users(email, role)` for login queries
- Flyway migration order: `V1__init_users.sql` → `V2__password_reset_tokens.sql` → `V3__course_nodes.sql`

## NFRs to Keep in Mind

- API response target: <300ms for tree fetch up to 500 nodes
- JWT: HS256, 24h expiry
- WCAG 2.1 AA: ARIA labels on tree nodes, keyboard navigation
- Logging: SLF4J/Logback; log all auth events
- Spring Profiles: `dev`, `staging`, `prod` for DB creds and JWT secret
- Testing: JUnit 5 + Mockito (service layer); Playwright/Selenium (E2E login)

## Planned Modules

- Module 3: Teacher Submission Workflow
- Module 4: Student Course View
