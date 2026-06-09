# EducationPro — Developer-Ready Specification
### Modules 1–4 | Spring Boot + PostgreSQL + Bootstrap 5
**Version:** 1.3 | **Status:** Draft | **Audience:** Senior Developers & Architects

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Tech Stack](#2-tech-stack)
3. [Design System & Colour Tokens](#3-design-system--colour-tokens)
4. [Module 1 — Multi-Role Login Portal](#4-module-1--multi-role-login-portal)
   - 4.1 UI Layout & Behaviour
   - 4.2 Bootstrap Implementation
   - 4.3 Role Colour Mapping
   - 4.4 Forgotten Password Flow
   - 4.5 API Contracts
   - 4.6 Database Schema
   - 4.7 Security & Validation
5. [Module 2 — Admin Course Designer (Tree)](#5-module-2--admin-course-designer-tree)
   - 5.1 Dashboard Shell
   - 5.2 Tree Component Behaviour
   - 5.3 Context Menu — Add Node Modal (Name, Description, Tag Line)
   - 5.4 Context Menu — Add Question Modal (7 Types + Complexity)
   - 5.5 Node Detail Panel
   - 5.6 Question Leaf Behaviour
   - 5.7 API Contracts
   - 5.8 Database Schema
   - 5.9 Backend Service Layer
6. [Module 3 — Assessment Designer](#6-module-3--assessment-designer)
   - 6.1 Layout
   - 6.2 Question Picker & Cascade Filters
   - 6.3 Exam Paper Builder
   - 6.4 API Contracts
   - 6.5 Database Schema
   - 6.6 Key Implementation Notes
7. [Module 4 — Teacher/Student Management & Assignments](#7-module-4--teacherstudent-management--assignments)
   - 7.1 UI Design System (Card Pattern)
   - 7.2 Teacher Management
   - 7.3 Student Management
   - 7.4 Assignments Page
   - 7.5 API Contracts
   - 7.6 Database Schema
8. [Error Handling Standards](#8-error-handling-standards)
9. [Non-Functional Requirements](#9-non-functional-requirements)

---

## 1. System Overview

**EducationPro** is a multi-tenant education platform built on Spring Boot (Java 17+) and PostgreSQL 15+. It serves four distinct user roles — Admin, Teacher, Student, Parent — each with a visually differentiated portal and a role-appropriate feature set.

### Core Principles
- **Role Isolation:** Each role sees only its permitted features and colour identity.
- **Approval Workflow:** Teachers propose; Admins approve. No content goes live without admin sign-off.
- **Tree Content Model:** Courses are structured as an unbounded tree (nodes), terminated only by Question leaf nodes.
- **Secure by Default:** JWT-based stateless auth; RBAC enforced at controller and service layers.

---

## 2. Tech Stack

| Layer | Technology |
|---|---|
| Backend Framework | Spring Boot 3.x (Java 17+) |
| ORM | Spring Data JPA + Hibernate |
| Database | PostgreSQL 15+ |
| Security | Spring Security 6 + JWT (JJWT 0.11+) |
| Frontend | Thymeleaf + Bootstrap 5.3 + Vanilla JS |
| Build Tool | Maven 3.9+ |
| API Style | REST (JSON) |
| Password Reset | Spring Mail (SMTP) + UUID tokens |
| Tree UI | jsTree 3.x OR custom Bootstrap + Vanilla JS |

---

## 3. Design System & Colour Tokens

### 3.1 Role Colour Palette

| Role | Primary | Light BG | Badge Class | Portal Border |
|---|---|---|---|---|
| **Admin** | `#0d6efd` (Bootstrap Blue) | `#e8f0fe` | `badge bg-primary` | `border-primary` |
| **Teacher** | `#198754` (Bootstrap Green) | `#e8f5ee` | `badge bg-success` | `border-success` |
| **Student** | `#6f42c1` (Bootstrap Indigo) | `#f0ebff` | `badge bg-purple` | `border-purple` |
| **Parent** | `#fd7e14` (Bootstrap Orange) | `#fff4e8` | `badge bg-orange` | `border-orange` |

> **Custom CSS variables** — add to `main.css`:

```css
:root {
  --role-admin:    #0d6efd;
  --role-teacher:  #198754;
  --role-student:  #6f42c1;
  --role-parent:   #fd7e14;

  --bg-admin:    #e8f0fe;
  --bg-teacher:  #e8f5ee;
  --bg-student:  #f0ebff;
  --bg-parent:   #fff4e8;

  /* Admin shell */
  --sidebar-bg:    #0c3577;  /* dark navy — intentionally darker than --role-admin */
  --sidebar-width: 260px;
  --topbar-h:      60px;
}

/* Custom badge overrides */
.badge.bg-purple { background-color: var(--role-student) !important; }
.badge.bg-orange { background-color: var(--role-parent)  !important; }

.border-purple { border-color: var(--role-student) !important; }
.border-orange { border-color: var(--role-parent)  !important; }
```

### 3.2 Typography

```css
/* Import in <head> */
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap');

body {
  font-family: 'Inter', sans-serif;
  font-size: 0.95rem;
  color: #212529;
}

h1, h2, h3 { font-weight: 700; letter-spacing: -0.3px; }
.label-sm   { font-size: 0.78rem; font-weight: 600; text-transform: uppercase; letter-spacing: 0.6px; }
```

### 3.3 Elevation / Shadow

```css
.card-portal  { box-shadow: 0 8px 32px rgba(0,0,0,0.10); border-radius: 16px; }
.card-form    { box-shadow: 0 2px 12px rgba(0,0,0,0.08); border-radius: 12px; }
```

---

## 4. Module 1 — Multi-Role Login Portal

### 4.1 UI Layout & Behaviour

#### Overall Structure

```
┌──────────────────────────────────────────────────────────────┐
│  [LOGO]    EducationPro                   [Admin][Teacher]   │
│                                           [Student][Parent]  │
├────────────────────────────┬─────────────────────────────────┤
│                            │                                  │
│  LEFT PANEL                │  RIGHT PANEL                    │
│  Role-specific features    │  Login Form                     │
│  (background tinted to     │  (card with role colour border) │
│   active role colour)      │                                  │
│                            │                                  │
│  • Feature bullet list     │  [Role Badge]                   │
│  • Icon + description      │  Email ____________             │
│  • Highlight key benefit   │  Password __________            │
│                            │  [Forgot password?]             │
│                            │  [Login Button]                 │
│                            │                                  │
└────────────────────────────┴─────────────────────────────────┘
```

#### Portal Toggle (Pill Buttons)

- Four Bootstrap pill buttons in top-right of the page header.
- Clicking a pill: updates left panel content, updates form border colour, updates role badge, updates page background tint, and updates login button colour — all without page reload (pure JS state swap).
- Default active role: **Admin**.

#### Role Badge

Positioned above the login form heading:
```html
<span id="roleBadge" class="badge bg-primary fs-6 mb-2">Admin Portal</span>
```
Badge class and text swap dynamically when a pill is selected.

---

### 4.2 Bootstrap Implementation

#### Full Login Page HTML (Thymeleaf Template: `login.html`)

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head>
  <meta charset="UTF-8"/>
  <title>EducationPro — Login</title>
  <link rel="stylesheet"
        href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"/>
  <link rel="stylesheet"
        href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css"/>
  <link rel="stylesheet" th:href="@{/css/main.css}"/>
</head>
<body id="loginPage" class="min-vh-100 d-flex flex-column">

  <!-- ── NAVBAR ── -->
  <nav class="navbar navbar-light bg-white border-bottom px-4 py-2">
    <span class="navbar-brand fw-bold fs-4">
      <i class="bi bi-mortarboard-fill me-2 text-primary"></i>EducationPro
    </span>
    <!-- Role Toggle Pills -->
    <div class="btn-group" role="group" id="roleToggle">
      <button type="button" class="btn btn-primary   rounded-pill me-1 role-btn active"
              data-role="admin">Admin</button>
      <button type="button" class="btn btn-outline-success rounded-pill me-1 role-btn"
              data-role="teacher">Teacher</button>
      <button type="button" class="btn btn-outline-secondary rounded-pill me-1 role-btn"
              data-role="student" style="--bs-btn-active-bg:#6f42c1;">Student</button>
      <button type="button" class="btn btn-outline-warning rounded-pill role-btn"
              data-role="parent">Parent</button>
    </div>
  </nav>

  <!-- ── MAIN SPLIT ── -->
  <div class="container-fluid flex-grow-1 d-flex align-items-center py-5" id="mainArea">
    <div class="row w-100 g-0 justify-content-center">

      <!-- LEFT PANEL -->
      <div class="col-lg-5 col-md-6 p-5 rounded-start-4" id="leftPanel"
           style="background: var(--bg-admin);">
        <h2 class="fw-bold mb-1" id="panelTitle">Admin Portal</h2>
        <p class="text-muted mb-4" id="panelSubtitle">Platform management & oversight</p>
        <ul class="list-unstyled" id="featureList">
          <!-- JS-populated per role -->
        </ul>
      </div>

      <!-- RIGHT PANEL — Login Card -->
      <div class="col-lg-4 col-md-6 d-flex align-items-center justify-content-center
                  bg-white p-5 rounded-end-4 card-portal">
        <div class="w-100">
          <span id="roleBadge" class="badge bg-primary fs-6 mb-3">Admin Portal</span>
          <h3 class="fw-bold mb-4">Sign in</h3>

          <!-- Alert area -->
          <div id="loginAlert" class="alert alert-danger d-none" role="alert"></div>

          <form id="loginForm" th:action="@{/auth/login}" method="post" novalidate>
            <input type="hidden" name="role" id="roleInput" value="admin"/>

            <div class="mb-3">
              <label for="email" class="form-label fw-semibold">Email address</label>
              <input type="email" class="form-control form-control-lg" id="email"
                     name="email" placeholder="you@school.edu" required autocomplete="email"/>
              <div class="invalid-feedback">Please enter a valid email.</div>
            </div>

            <div class="mb-2">
              <label for="password" class="form-label fw-semibold">Password</label>
              <div class="input-group">
                <input type="password" class="form-control form-control-lg" id="password"
                       name="password" placeholder="••••••••" required autocomplete="current-password"/>
                <button class="btn btn-outline-secondary" type="button" id="togglePass"
                        tabindex="-1" aria-label="Show password">
                  <i class="bi bi-eye"></i>
                </button>
              </div>
              <div class="invalid-feedback">Password is required.</div>
            </div>

            <div class="text-end mb-4">
              <a href="#" id="forgotLink" class="small text-primary text-decoration-none">
                Forgot password?
              </a>
            </div>

            <button type="submit" class="btn btn-primary btn-lg w-100 fw-semibold"
                    id="loginBtn">Sign in as Admin</button>
          </form>
        </div>
      </div>

    </div>
  </div>

  <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
  <script th:src="@{/js/login.js}"></script>
</body>
</html>
```

---

#### JavaScript: `login.js`

```javascript
/* ── Role configuration ── */
const ROLES = {
  admin: {
    label:      'Admin Portal',
    badgeClass: 'bg-primary',
    btnClass:   'btn-primary',
    borderVar:  '#0d6efd',
    bgVar:      'var(--bg-admin)',
    subtitle:   'Platform management & oversight',
    features: [
      { icon: 'bi-people-fill',        text: 'Manage all users & roles' },
      { icon: 'bi-diagram-3-fill',     text: 'Design & publish course trees' },
      { icon: 'bi-check2-circle',      text: 'Approve teacher submissions' },
      { icon: 'bi-bar-chart-fill',     text: 'Platform-wide analytics' },
    ]
  },
  teacher: {
    label:      'Teacher Portal',
    badgeClass: 'bg-success',
    btnClass:   'btn-success',
    borderVar:  '#198754',
    bgVar:      'var(--bg-teacher)',
    subtitle:   'Create, submit & manage your courses',
    features: [
      { icon: 'bi-journal-plus',       text: 'Build & submit courses for approval' },
      { icon: 'bi-people',             text: 'View enrolled students' },
      { icon: 'bi-chat-dots-fill',     text: 'Grade & give feedback' },
      { icon: 'bi-calendar3',          text: 'Schedule classes & events' },
    ]
  },
  student: {
    label:      'Student Portal',
    badgeClass: 'bg-purple',
    btnClass:   'btn-purple',
    borderVar:  '#6f42c1',
    bgVar:      'var(--bg-student)',
    subtitle:   'Learn, practise & grow',
    features: [
      { icon: 'bi-book-fill',          text: 'Access enrolled courses' },
      { icon: 'bi-patch-question-fill',text: 'Take quizzes & assignments' },
      { icon: 'bi-graph-up-arrow',     text: 'Track your progress' },
      { icon: 'bi-award-fill',         text: 'Earn certificates' },
    ]
  },
  parent: {
    label:      'Parent Portal',
    badgeClass: 'bg-orange',
    btnClass:   'btn-orange',
    borderVar:  '#fd7e14',
    bgVar:      'var(--bg-parent)',
    subtitle:   "Monitor your child's journey",
    features: [
      { icon: 'bi-person-check-fill',  text: "View child's progress & grades" },
      { icon: 'bi-bell-fill',          text: 'Receive activity notifications' },
      { icon: 'bi-calendar-event',     text: 'See upcoming assessments' },
      { icon: 'bi-chat-left-text',     text: 'Message teachers directly' },
    ]
  }
};

let activeRole = 'admin';

function applyRole(role) {
  const cfg = ROLES[role];
  activeRole = role;

  // Badge
  const badge = document.getElementById('roleBadge');
  badge.className = `badge ${cfg.badgeClass} fs-6 mb-3`;
  badge.textContent = cfg.label;

  // Left panel
  document.getElementById('leftPanel').style.background = cfg.bgVar;
  document.getElementById('panelTitle').textContent = cfg.label;
  document.getElementById('panelSubtitle').textContent = cfg.subtitle;

  const list = document.getElementById('featureList');
  list.innerHTML = cfg.features.map(f => `
    <li class="d-flex align-items-start mb-3">
      <i class="bi ${f.icon} fs-4 me-3 mt-1" style="color:${cfg.borderVar}"></i>
      <span class="fs-6">${f.text}</span>
    </li>`).join('');

  // Login button
  const btn = document.getElementById('loginBtn');
  btn.className = `btn ${cfg.btnClass} btn-lg w-100 fw-semibold`;
  btn.textContent = `Sign in as ${role.charAt(0).toUpperCase() + role.slice(1)}`;

  // Forgot link colour
  document.getElementById('forgotLink').style.color = cfg.borderVar;

  // Hidden role input
  document.getElementById('roleInput').value = role;

  // Pill button states
  document.querySelectorAll('.role-btn').forEach(b => {
    const isActive = b.dataset.role === role;
    b.classList.toggle('active', isActive);
  });
}

// ── Toggle password visibility ──
document.getElementById('togglePass').addEventListener('click', () => {
  const pwd = document.getElementById('password');
  const icon = document.querySelector('#togglePass i');
  if (pwd.type === 'password') {
    pwd.type = 'text';
    icon.className = 'bi bi-eye-slash';
  } else {
    pwd.type = 'password';
    icon.className = 'bi bi-eye';
  }
});

// ── Pill click handlers ──
document.querySelectorAll('.role-btn').forEach(btn => {
  btn.addEventListener('click', () => applyRole(btn.dataset.role));
});

// ── Client-side form validation ──
document.getElementById('loginForm').addEventListener('submit', e => {
  const form = e.target;
  if (!form.checkValidity()) {
    e.preventDefault();
    e.stopPropagation();
  }
  form.classList.add('was-validated');
});

// ── Init ──
applyRole('admin');
```

---

### 4.3 Role Colour Mapping Summary

| Role | Button active class | Form border | BG token |
|---|---|---|---|
| Admin | `btn-primary` | `#0d6efd` | `--bg-admin` |
| Teacher | `btn-success` | `#198754` | `--bg-teacher` |
| Student | `btn-purple` | `#6f42c1` | `--bg-student` |
| Parent | `btn-orange` | `#fd7e14` | `--bg-parent` |

Add to `main.css`:
```css
.btn-purple { background-color:#6f42c1; border-color:#6f42c1; color:#fff; }
.btn-purple:hover { background-color:#5a359a; color:#fff; }
.btn-orange { background-color:#fd7e14; border-color:#fd7e14; color:#fff; }
.btn-orange:hover { background-color:#e06f10; color:#fff; }
```

---

### 4.4 Forgotten Password Flow

#### User Flow
```
1. User clicks "Forgot password?"
2. Modal appears — enter registered email
3. Backend validates email exists for selected role
4. If valid → generate UUID token, store in DB, send reset email
5. Email contains link: /auth/reset-password?token=<UUID>
6. User opens link → enters new password + confirm
7. Backend validates token (not expired, not used) → updates password → invalidates token
8. Redirect to login with success toast
```

#### Forgot Password Modal (HTML)
```html
<div class="modal fade" id="forgotModal" tabindex="-1">
  <div class="modal-dialog modal-dialog-centered">
    <div class="modal-content rounded-4">
      <div class="modal-header border-0">
        <h5 class="modal-title fw-bold">Reset your password</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body px-4">
        <p class="text-muted small">Enter the email linked to your account.
           We'll send a reset link within a minute.</p>
        <div id="resetAlert" class="alert d-none"></div>
        <input type="email" class="form-control form-control-lg" id="resetEmail"
               placeholder="you@school.edu"/>
      </div>
      <div class="modal-footer border-0 px-4 pb-4">
        <button class="btn btn-primary w-100 fw-semibold" id="sendResetBtn">
          Send reset link
        </button>
      </div>
    </div>
  </div>
</div>
```

#### JavaScript (trigger modal, send AJAX)
```javascript
document.getElementById('forgotLink').addEventListener('click', e => {
  e.preventDefault();
  new bootstrap.Modal(document.getElementById('forgotModal')).show();
});

document.getElementById('sendResetBtn').addEventListener('click', async () => {
  const email = document.getElementById('resetEmail').value.trim();
  const alertEl = document.getElementById('resetAlert');
  if (!email) { showResetAlert('Please enter your email.', 'danger'); return; }

  const res = await fetch('/auth/forgot-password', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, role: activeRole })
  });
  const data = await res.json();

  if (res.ok) {
    showResetAlert('Check your inbox — link sent!', 'success');
  } else {
    showResetAlert(data.message || 'Something went wrong.', 'danger');
  }
});

function showResetAlert(msg, type) {
  const el = document.getElementById('resetAlert');
  el.className = `alert alert-${type}`;
  el.textContent = msg;
}
```

---

### 4.5 API Contracts

#### `POST /auth/login`

**Request:**
```json
{
  "email":    "admin@school.edu",
  "password": "P@ssword123",
  "role":     "ADMIN"
}
```

**Response 200:**
```json
{
  "token":     "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 86400,
  "role":      "ADMIN",
  "name":      "Jane Smith",
  "redirectTo": "/admin/dashboard"
}
```

**Response 401:**
```json
{
  "status":  401,
  "message": "Invalid credentials or role mismatch."
}
```

#### `POST /auth/forgot-password`

**Request:**
```json
{ "email": "teacher@school.edu", "role": "TEACHER" }
```

**Response 200:**
```json
{ "message": "Reset link sent if account exists." }
```
> Always return 200 to prevent email enumeration.

#### `POST /auth/reset-password`

**Request:**
```json
{
  "token":           "uuid-string",
  "newPassword":     "NewP@ss!99",
  "confirmPassword": "NewP@ss!99"
}
```

**Response 200:**
```json
{ "message": "Password updated. You may now log in." }
```

**Response 400:**
```json
{ "message": "Token expired or already used." }
```

---

### 4.6 Database Schema

```sql
-- Users table
CREATE TABLE users (
  id            BIGSERIAL PRIMARY KEY,
  email         VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  full_name     VARCHAR(255) NOT NULL,
  role          VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN','TEACHER','STUDENT','PARENT')),
  active        BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email_role ON users(email, role);

-- Password reset tokens
CREATE TABLE password_reset_tokens (
  id          BIGSERIAL PRIMARY KEY,
  user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token       VARCHAR(64)  NOT NULL UNIQUE,
  expires_at  TIMESTAMPTZ  NOT NULL,
  used        BOOLEAN      NOT NULL DEFAULT FALSE,
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_prt_token ON password_reset_tokens(token);
```

---

### 4.7 Security & Validation

#### Spring Security Config (excerpt)
```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .csrf(AbstractHttpConfigurer::disable)          // Using JWT — CSRF not needed
      .sessionManagement(sm -> sm
          .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(auth -> auth
          .requestMatchers("/auth/**", "/css/**", "/js/**").permitAll()
          .requestMatchers("/admin/**").hasRole("ADMIN")
          .requestMatchers("/teacher/**").hasRole("TEACHER")
          .requestMatchers("/student/**").hasRole("STUDENT")
          .requestMatchers("/parent/**").hasRole("PARENT")
          .anyRequest().authenticated())
      .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
}
```

#### Password Policy (enforced via Bean Validation)
```java
@NotBlank
@Size(min = 8, max = 64)
@Pattern(
  regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#]).+$",
  message = "Password must contain upper, lower, digit and special character."
)
private String newPassword;
```

#### Token Expiry
```java
// PasswordResetService.java
private static final Duration TOKEN_TTL = Duration.ofMinutes(30);

public void sendResetLink(String email, String role) {
    userRepository.findByEmailAndRole(email, Role.valueOf(role)).ifPresent(user -> {
        String token = UUID.randomUUID().toString();
        PasswordResetToken prt = new PasswordResetToken();
        prt.setUser(user);
        prt.setToken(token);
        prt.setExpiresAt(Instant.now().plus(TOKEN_TTL));
        tokenRepository.save(prt);
        mailService.sendResetEmail(user.getEmail(), token);
    });
    // Always return void — no enumeration leak
}
```

---

## 5. Module 2 — Admin Course Designer (Tree)

### 5.1 Dashboard Shell

After successful Admin login, redirect to `/admin/dashboard`.

```
┌──────────────────────────────────────────────────────────────────────┐
│  EduPro Admin      [Breadcrumb]          [🔔] [Avatar ▾ Admin Name]  │
├────────────┬─────────────────────────────────────────────────────────┤
│            │                                                          │
│  SIDEBAR   │  MAIN CONTENT AREA                                      │
│ (#0c3577)  │                                                          │
│            │                                                          │
│ OVERVIEW   │                                                          │
│  Dashboard │                                                          │
│  Analytics │                                                          │
│  Reports⊘  │                                                          │
│  Notifs⊘   │                                                          │
│            │                                                          │
│ ACADEMIC   │                                                          │
│  Design ←  │                                                          │
│  Paths⊘    │                                                          │
│  Assessmt  │                                                          │
│  ...       │                                                          │
│            │                                                          │
│ FACULTY    │                                                          │
│ STUDENT    │                                                          │
│ ASSESSMENT │                                                          │
│ OPERATIONS │                                                          │
│ INSIGHTS   │                                                          │
│            │                                                          │
└────────────┴─────────────────────────────────────────────────────────┘
  ⊘ = disabled / planned, not yet implemented
```

#### Sidebar Structure

- **CSS class:** `<nav class="sidebar">` — background `#0c3577` (dark navy), width 260px
- **Scrollable:** `.sidebar__nav` has `overflow-y: auto`
- **Group labels:** `.sidebar__group-label` — small all-caps muted white text, non-interactive
- **Disabled items:** `.nav-link.disabled` — 38% opacity, pointer-events none
- **Compact spacing:** nav-link padding `0.25rem 0.75rem`, font-size `0.8125rem`, no margin-bottom

| # | Group | Implemented items | Planned (disabled) |
|---|---|---|---|
| 1 | Overview | Dashboard, Analytics | Reports, Notifications |
| 2 | Academic Management | Design Courses, Assessment Designer | Learning Paths, Question Bank, Assignments, Certificates |
| 3 | Faculty Management | Teachers | Departments, Workload Management, Attendance, Performance Reviews |
| 4 | Student Management | Students | Enrollments, Attendance, Progress Tracking, Achievements |
| 5 | Assessment & Evaluation | — | Question Bank, Exam Scheduling, Submissions, Results, Grading |
| 6 | Operations | Approvals | Requests, Leave Management, Announcements, Events |
| 7 | Insights & Reporting | Analytics | Performance Dashboard, Student Success Metrics, Faculty Analytics, Custom Reports |

#### Topbar Structure

- Right side: bell icon → user avatar dropdown
- **User dropdown** (`topbar-user-btn`) contains:
  1. **Super Admin** section header (`dropdown-header`)
  2. Users, Roles & Permissions, Organizations, System Settings, Audit Logs — all disabled (planned)
  3. `<hr class="dropdown-divider">`
  4. Sign out → `/auth/logout`
- No separate Administration button exists; all admin controls live inside the user dropdown

#### Dashboard HTML Shell (reference pattern)
```html
<div class="d-flex" id="adminWrapper" style="height:100vh;">

  <!-- Sidebar -->
  <nav class="sidebar" aria-label="Admin navigation">
    <a href="/admin/dashboard" class="sidebar__brand">
      <i class="bi bi-mortarboard-fill"></i>
      <span>EduPro Admin</span>
    </a>
    <ul class="sidebar__nav">
      <li class="sidebar__group-label">Overview</li>
      <li><a class="nav-link active" href="/admin/dashboard"><i class="bi bi-speedometer2"></i><span>Dashboard</span></a></li>
      <li><a class="nav-link" href="/admin/analytics"><i class="bi bi-bar-chart-line"></i><span>Analytics</span></a></li>
      <li><a class="nav-link disabled" href="#" tabindex="-1" aria-disabled="true"><i class="bi bi-file-earmark-bar-graph"></i><span>Reports</span></a></li>
      <!-- ... remaining groups ... -->
    </ul>
    <ul class="sidebar__nav-bottom">
      <li><a class="nav-link" href="/auth/logout" onclick="sessionStorage.clear()"><i class="bi bi-box-arrow-right"></i><span>Sign out</span></a></li>
    </ul>
  </nav>

  <!-- Main -->
  <div class="flex-grow-1 d-flex flex-column overflow-hidden">
    <!-- Topbar -->
    <header class="topbar">
      <nav aria-label="breadcrumb">...</nav>
      <div class="topbar__actions">
        <button class="topbar__icon-btn" aria-label="Notifications"><i class="bi bi-bell"></i></button>
        <div class="dropdown">
          <button class="topbar-user-btn dropdown-toggle" data-bs-toggle="dropdown">
            <span class="topbar-avatar" id="topbarAvatar">A</span>
            <span id="topbarName">Admin</span>
          </button>
          <ul class="dropdown-menu dropdown-menu-end">
            <li><h6 class="dropdown-header">Super Admin</h6></li>
            <li><a class="dropdown-item disabled">...</a></li>
            <li><hr class="dropdown-divider"></li>
            <li><a class="dropdown-item" href="/auth/logout" onclick="sessionStorage.clear()">Sign out</a></li>
          </ul>
        </div>
      </div>
    </header>

    <!-- Split: Tree + Detail -->
    <div class="flex-grow-1 d-flex overflow-hidden">
      <!-- Tree Panel -->
      <div id="treePanel" class="border-end bg-white overflow-auto p-3"
           style="width:340px; min-width:280px; max-width:480px; resize:horizontal;">
        <div class="d-flex justify-content-between align-items-center mb-3">
          <span class="fw-semibold">Course Tree</span>
          <button class="btn btn-sm btn-primary" id="addRootBtn">
            <i class="bi bi-plus-lg me-1"></i>Add Course
          </button>
        </div>
        <div id="courseTree"></div>
      </div>

      <!-- Divider / Drag handle -->
      <div id="dragHandle" style="width:5px;cursor:col-resize;background:#dee2e6;"></div>

      <!-- Detail Panel -->
      <div id="detailPanel" class="flex-grow-1 overflow-auto p-4 bg-light">
        <div class="text-center text-muted mt-5" id="detailPlaceholder">
          <i class="bi bi-cursor-fill fs-1 mb-3 d-block opacity-25"></i>
          Select a node to view or edit its details.
        </div>
        <div id="detailForm" class="d-none"></div>
      </div>
    </div>
  </div>
</div>
```

---

### 5.2 Tree Component Behaviour

#### Rules
| Behaviour | Detail |
|---|---|
| **Expand / Collapse** | Click the `›` arrow icon on any parent node to toggle children |
| **Select Node** | Click node label → opens its detail form in the right panel |
| **Right-click Context Menu** | Appears at cursor position; shows "Add Node" and/or "Add Question" |
| **Question is a leaf** | A Question node has no expand arrow; right-click shows no add options |
| **Drag-resize panel** | The divider between tree and detail can be dragged horizontally |

#### Tree Data Model (JS)
```javascript
// Each node in memory:
{
  id:         1,
  parentId:   null,          // null = root
  type:       'NODE',        // 'NODE' | 'QUESTION'
  title:      'Mathematics',
  description:'...',
  order:      0,
  children:   [],            // populated recursively
  expanded:   false
}
```

#### Tree Rendering (Vanilla JS)

```javascript
// tree.js

let treeData  = [];   // flat array from API
let selectedId = null;

/* ── Build nested structure ── */
function buildTree(flat) {
  const map = {};
  flat.forEach(n => { map[n.id] = { ...n, children: [] }; });
  const roots = [];
  flat.forEach(n => {
    if (n.parentId) map[n.parentId].children.push(map[n.id]);
    else roots.push(map[n.id]);
  });
  return roots;
}

/* ── Render tree ── */
function renderTree(nodes, container) {
  container.innerHTML = '';
  nodes.forEach(node => container.appendChild(renderNode(node)));
}

function renderNode(node) {
  const isQuestion = node.type === 'QUESTION';
  const hasChildren = node.children && node.children.length > 0;

  const wrapper = document.createElement('div');
  wrapper.className = 'tree-node-wrapper';
  wrapper.dataset.id = node.id;

  const row = document.createElement('div');
  row.className = `tree-node d-flex align-items-center px-2 py-1 rounded
                   ${node.id === selectedId ? 'tree-node--selected' : ''}`;
  row.dataset.id = node.id;

  // Expand arrow (hidden for questions)
  const arrow = document.createElement('span');
  arrow.className = `tree-arrow me-1 ${isQuestion ? 'invisible' : ''}`;
  arrow.innerHTML = node.expanded ? '&#8964;' : '&#8250;';
  arrow.addEventListener('click', e => { e.stopPropagation(); toggleNode(node); });

  // Icon
  const icon = document.createElement('i');
  icon.className = `bi ${isQuestion ? 'bi-patch-question-fill text-warning'
                                    : 'bi-folder2 text-primary'} me-2`;

  // Label
  const label = document.createElement('span');
  label.className = 'tree-label flex-grow-1 text-truncate';
  label.textContent = node.title;

  row.append(arrow, icon, label);
  row.addEventListener('click', () => selectNode(node));
  row.addEventListener('contextmenu', e => { e.preventDefault(); showContextMenu(e, node); });

  wrapper.appendChild(row);

  // Children container
  const childContainer = document.createElement('div');
  childContainer.className = `tree-children ps-3 ${node.expanded ? '' : 'd-none'}`;
  if (hasChildren) {
    node.children.forEach(child => childContainer.appendChild(renderNode(child)));
  }
  wrapper.appendChild(childContainer);

  return wrapper;
}

function toggleNode(node) {
  node.expanded = !node.expanded;
  refreshTree();
}

function selectNode(node) {
  selectedId = node.id;
  refreshTree();
  openDetailPanel(node);
}

function refreshTree() {
  const nested = buildTree(treeData);
  renderTree(nested, document.getElementById('courseTree'));
}
```

#### Tree CSS
```css
/* tree.css */
.tree-node {
  cursor: pointer;
  user-select: none;
  transition: background 0.15s;
  border-radius: 6px;
}
.tree-node:hover          { background: #f0f4ff; }
.tree-node--selected      { background: #e8f0fe; font-weight: 600; }
.tree-arrow               { font-size: 1rem; width: 18px; display: inline-block;
                            transition: transform 0.2s; cursor: pointer; }
.tree-label               { font-size: 0.9rem; }
.tree-children            { border-left: 2px solid #dee2e6; margin-left: 9px; }
```

---

### 5.3 Context Menu (Right-Click)

#### HTML (injected dynamically)
```html
<!-- Placed in body, hidden by default -->
<ul class="dropdown-menu shadow" id="treeContextMenu" style="display:none; position:fixed; z-index:9999;">
  <li><a class="dropdown-item" id="ctxAddNode" href="#">
    <i class="bi bi-folder-plus me-2 text-primary"></i>Add Node
  </a></li>
  <li><a class="dropdown-item" id="ctxAddQuestion" href="#">
    <i class="bi bi-patch-question me-2 text-warning"></i>Add Question
  </a></li>
</ul>
```

#### JavaScript
```javascript
let ctxTargetNode = null;

function showContextMenu(event, node) {
  ctxTargetNode = node;
  const menu = document.getElementById('treeContextMenu');

  // Questions cannot have children
  document.getElementById('ctxAddNode').parentElement.style.display =
    node.type === 'QUESTION' ? 'none' : 'block';
  document.getElementById('ctxAddQuestion').parentElement.style.display =
    node.type === 'QUESTION' ? 'none' : 'block';

  menu.style.top  = `${event.clientY}px`;
  menu.style.left = `${event.clientX}px`;
  menu.style.display = 'block';
}

document.addEventListener('click', () => {
  document.getElementById('treeContextMenu').style.display = 'none';
});

document.getElementById('ctxAddNode').addEventListener('click', e => {
  e.preventDefault();
  openAddNodeModal(ctxTargetNode, 'NODE');
});

document.getElementById('ctxAddQuestion').addEventListener('click', e => {
  e.preventDefault();
  openAddNodeModal(ctxTargetNode, 'QUESTION');
});
```

#### Add Node Modal
> Triggered by right-click → **Add Node**. Collects: Node Name, Description, Tag Line.

```html
<!-- ── ADD NODE MODAL ── -->
<div class="modal fade" id="addNodeModal" tabindex="-1" aria-labelledby="addNodeModalLabel">
  <div class="modal-dialog modal-dialog-centered modal-md">
    <div class="modal-content rounded-4 shadow">

      <div class="modal-header border-0 pb-0 px-4 pt-4">
        <div class="d-flex align-items-center gap-2">
          <i class="bi bi-folder-plus fs-4 text-primary"></i>
          <h5 class="modal-title fw-bold mb-0" id="addNodeModalLabel">Add Node</h5>
        </div>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
      </div>

      <div class="modal-body px-4 py-3">
        <div id="nodeFormAlert" class="alert alert-danger d-none py-2"></div>

        <!-- Node Name -->
        <div class="mb-3">
          <label class="form-label fw-semibold label-sm">
            Node Name <span class="text-danger">*</span>
          </label>
          <input type="text" class="form-control" id="newNodeTitle"
                 maxlength="150" placeholder="e.g. Algebra Fundamentals" required/>
          <div class="form-text">Max 150 characters. Shown as the tree label.</div>
        </div>

        <!-- Description -->
        <div class="mb-3">
          <label class="form-label fw-semibold label-sm">Description</label>
          <textarea class="form-control" id="newNodeDesc" rows="3"
                    maxlength="500"
                    placeholder="Briefly describe what this section covers…"></textarea>
          <div class="d-flex justify-content-end">
            <small class="text-muted" id="nodeDescCount">0 / 500</small>
          </div>
        </div>

        <!-- Tag Line -->
        <div class="mb-3">
          <label class="form-label fw-semibold label-sm">Tag Line</label>
          <input type="text" class="form-control" id="newNodeTagline"
                 maxlength="100"
                 placeholder="e.g. Build a solid foundation in algebraic thinking"/>
          <div class="form-text">
            Short marketing-style line shown on course cards. Max 100 characters.
          </div>
        </div>

        <input type="hidden" id="newNodeParentId"/>
      </div>

      <div class="modal-footer border-0 px-4 pb-4 gap-2">
        <button class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancel</button>
        <button class="btn btn-primary fw-semibold px-4" id="saveNewNodeBtn">
          <i class="bi bi-plus-circle me-1"></i>Add Node
        </button>
      </div>
    </div>
  </div>
</div>
```

#### Add Question Modal
> Triggered by right-click → **Add Question**. Full dynamic form that swaps inner fields based on selected Question Type.

```html
<!-- ── ADD QUESTION MODAL ── -->
<div class="modal fade" id="addQuestionModal" tabindex="-1" aria-labelledby="addQModalLabel">
  <div class="modal-dialog modal-dialog-centered modal-lg modal-dialog-scrollable">
    <div class="modal-content rounded-4 shadow">

      <div class="modal-header border-0 pb-0 px-4 pt-4">
        <div class="d-flex align-items-center gap-2">
          <i class="bi bi-patch-question-fill fs-4 text-warning"></i>
          <h5 class="modal-title fw-bold mb-0" id="addQModalLabel">Add Question</h5>
        </div>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
      </div>

      <div class="modal-body px-4 py-3">
        <div id="qFormAlert" class="alert alert-danger d-none py-2"></div>

        <!-- Row: Question Type + Complexity -->
        <div class="row g-3 mb-3">
          <div class="col-md-7">
            <label class="form-label fw-semibold label-sm">
              Question Type <span class="text-danger">*</span>
            </label>
            <select class="form-select" id="qType">
              <option value="">— Select type —</option>
              <option value="MCQ_SINGLE">MCQ – Single Answer</option>
              <option value="MCQ_MULTIPLE">MCQ – Multiple Answer</option>
              <option value="TRUE_FALSE">True / False</option>
              <option value="SHORT_ANSWER">Short Answer</option>
              <option value="ESSAY">Essay</option>
              <option value="CODE">Code</option>
              <option value="IMAGE_BASED">Image Based</option>
            </select>
          </div>
          <div class="col-md-5">
            <label class="form-label fw-semibold label-sm">
              Complexity <span class="text-danger">*</span>
            </label>
            <div class="d-flex gap-2 mt-1">
              <div class="form-check form-check-inline">
                <input class="form-check-input" type="radio" name="qComplexity"
                       id="cmpFoundation" value="FOUNDATION"/>
                <label class="form-check-label" for="cmpFoundation">
                  <span class="badge bg-success-subtle text-success border border-success">
                    Foundation
                  </span>
                </label>
              </div>
              <div class="form-check form-check-inline">
                <input class="form-check-input" type="radio" name="qComplexity"
                       id="cmpIntermediate" value="INTERMEDIATE"/>
                <label class="form-check-label" for="cmpIntermediate">
                  <span class="badge bg-warning-subtle text-warning border border-warning">
                    Intermediate
                  </span>
                </label>
              </div>
              <div class="form-check form-check-inline">
                <input class="form-check-input" type="radio" name="qComplexity"
                       id="cmpHigher" value="HIGHER"/>
                <label class="form-check-label" for="cmpHigher">
                  <span class="badge bg-danger-subtle text-danger border border-danger">
                    Higher
                  </span>
                </label>
              </div>
            </div>
          </div>
        </div>

        <!-- Question Text (shared across all types) -->
        <div class="mb-3" id="qTextGroup">
          <label class="form-label fw-semibold label-sm">
            Question Text <span class="text-danger">*</span>
          </label>
          <textarea class="form-control" id="qText" rows="3"
                    maxlength="2000"
                    placeholder="Enter the question that will be shown to the student…"></textarea>
        </div>

        <!-- ── Dynamic type-specific fields ── -->
        <div id="qTypeFields">
          <!-- Populated by JS on qType change -->
          <div class="text-center text-muted py-4 border rounded-3 bg-light">
            <i class="bi bi-arrow-up-circle me-1"></i>
            Select a question type above to configure its options.
          </div>
        </div>

        <input type="hidden" id="qParentNodeId"/>
      </div>

      <div class="modal-footer border-0 px-4 pb-4 gap-2">
        <button class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancel</button>
        <button class="btn btn-warning fw-semibold px-4 text-dark" id="saveNewQuestionBtn">
          <i class="bi bi-patch-plus me-1"></i>Add Question
        </button>
      </div>
    </div>
  </div>
</div>
```

#### JavaScript — Modal Launchers

```javascript
/* ── Open correct modal from context menu ── */
function openAddNodeModal(parentNode) {
  document.getElementById('newNodeParentId').value = parentNode.id;
  ['newNodeTitle','newNodeDesc','newNodeTagline'].forEach(id =>
    document.getElementById(id).value = '');
  document.getElementById('nodeFormAlert').classList.add('d-none');
  new bootstrap.Modal(document.getElementById('addNodeModal')).show();
}

function openAddQuestionModal(parentNode) {
  document.getElementById('qParentNodeId').value = parentNode.id;
  document.getElementById('qType').value = '';
  document.getElementById('qText').value = '';
  document.getElementById('qTypeFields').innerHTML = `
    <div class="text-center text-muted py-4 border rounded-3 bg-light">
      <i class="bi bi-arrow-up-circle me-1"></i>
      Select a question type above to configure its options.
    </div>`;
  document.querySelectorAll('input[name="qComplexity"]').forEach(r => r.checked = false);
  document.getElementById('qFormAlert').classList.add('d-none');
  new bootstrap.Modal(document.getElementById('addQuestionModal')).show();
}

/* Update context menu handlers */
document.getElementById('ctxAddNode').addEventListener('click', e => {
  e.preventDefault();
  openAddNodeModal(ctxTargetNode);
});

document.getElementById('ctxAddQuestion').addEventListener('click', e => {
  e.preventDefault();
  openAddQuestionModal(ctxTargetNode);
});
```

#### JavaScript — Question Type Dynamic Fields

```javascript
/* ── Character counter for node description ── */
document.getElementById('newNodeDesc').addEventListener('input', function() {
  document.getElementById('nodeDescCount').textContent = `${this.value.length} / 500`;
});

/* ── Question type switcher ── */
document.getElementById('qType').addEventListener('change', function() {
  const container = document.getElementById('qTypeFields');
  container.innerHTML = renderQuestionTypeFields(this.value);
  // Re-attach dynamic option button listeners after re-render
  attachOptionListeners();
});

function renderQuestionTypeFields(type) {
  switch(type) {
    case 'MCQ_SINGLE':   return tmplMcqSingle();
    case 'MCQ_MULTIPLE': return tmplMcqMultiple();
    case 'TRUE_FALSE':   return tmplTrueFalse();
    case 'SHORT_ANSWER': return tmplShortAnswer();
    case 'ESSAY':        return tmplEssay();
    case 'CODE':         return tmplCode();
    case 'IMAGE_BASED':  return tmplImageBased();
    default:             return '';
  }
}

/* ── MCQ SINGLE ── */
function tmplMcqSingle() {
  return `
  <div class="border rounded-3 p-3 bg-white mb-3">
    <div class="fw-semibold label-sm mb-3">
      <i class="bi bi-ui-radios me-1 text-primary"></i>Options
      <span class="text-muted fw-normal ms-1">(exactly ONE correct answer)</span>
    </div>
    <div id="mcqSingleOptions">
      ${[1,2,3,4].map(i => mcqSingleOptionRow(i)).join('')}
    </div>
    <button type="button" class="btn btn-sm btn-outline-primary mt-2" id="addMcqSingleOption">
      <i class="bi bi-plus-lg me-1"></i>Add option
      <span class="text-muted ms-1">(max 6)</span>
    </button>
  </div>
  ${marksAndExplanation()}`;
}

function mcqSingleOptionRow(i) {
  return `
  <div class="d-flex align-items-center gap-2 mb-2 mcq-single-row">
    <span class="text-muted label-sm" style="width:24px">${i}.</span>
    <input type="text" class="form-control form-control-sm" placeholder="Option text…"
           name="mcqSingleOption" maxlength="200"/>
    <div class="form-check mb-0 ms-1 d-flex align-items-center gap-1">
      <input class="form-check-input" type="radio" name="mcqSingleCorrect"
             value="${i}" title="Mark as correct answer"/>
      <label class="form-check-label small text-success fw-semibold">Correct</label>
    </div>
    <button type="button" class="btn btn-sm btn-outline-danger border-0 px-1 remove-opt"
            title="Remove"><i class="bi bi-x-lg"></i></button>
  </div>`;
}

/* ── MCQ MULTIPLE ── */
function tmplMcqMultiple() {
  return `
  <div class="border rounded-3 p-3 bg-white mb-3">
    <div class="fw-semibold label-sm mb-3">
      <i class="bi bi-ui-checks me-1 text-primary"></i>Options
      <span class="text-muted fw-normal ms-1">(one or more correct answers)</span>
    </div>
    <div id="mcqMultiOptions">
      ${[1,2,3,4].map(i => mcqMultiOptionRow(i)).join('')}
    </div>
    <button type="button" class="btn btn-sm btn-outline-primary mt-2" id="addMcqMultiOption">
      <i class="bi bi-plus-lg me-1"></i>Add option
    </button>
    <!-- Partial marking -->
    <div class="mt-3 p-3 rounded-3 bg-light border">
      <div class="fw-semibold label-sm mb-2">Partial Marking</div>
      <div class="form-check">
        <input class="form-check-input" type="radio" name="partialMarking"
               id="pmFull" value="FULL_ONLY" checked/>
        <label class="form-check-label" for="pmFull">
          Full marks only — all correct options must be selected
        </label>
      </div>
      <div class="form-check">
        <input class="form-check-input" type="radio" name="partialMarking"
               id="pmPer" value="PER_OPTION"/>
        <label class="form-check-label" for="pmPer">
          Award marks per correct option selected
        </label>
      </div>
    </div>
  </div>
  ${marksAndExplanation()}`;
}

function mcqMultiOptionRow(i) {
  return `
  <div class="d-flex align-items-center gap-2 mb-2 mcq-multi-row">
    <span class="text-muted label-sm" style="width:24px">${i}.</span>
    <input type="text" class="form-control form-control-sm" placeholder="Option text…"
           name="mcqMultiOption" maxlength="200"/>
    <div class="form-check mb-0 ms-1 d-flex align-items-center gap-1">
      <input class="form-check-input" type="checkbox" name="mcqMultiCorrect"
             value="${i}" title="Mark as correct"/>
      <label class="form-check-label small text-success fw-semibold">Correct</label>
    </div>
    <button type="button" class="btn btn-sm btn-outline-danger border-0 px-1 remove-opt"
            title="Remove"><i class="bi bi-x-lg"></i></button>
  </div>`;
}

/* ── TRUE / FALSE ── */
function tmplTrueFalse() {
  return `
  <div class="border rounded-3 p-3 bg-white mb-3">
    <div class="fw-semibold label-sm mb-3">Correct Answer</div>
    <div class="d-flex gap-4">
      <div class="form-check">
        <input class="form-check-input" type="radio" name="tfAnswer"
               id="tfTrue" value="TRUE"/>
        <label class="form-check-label fw-semibold text-success" for="tfTrue">True</label>
      </div>
      <div class="form-check">
        <input class="form-check-input" type="radio" name="tfAnswer"
               id="tfFalse" value="FALSE"/>
        <label class="form-check-label fw-semibold text-danger" for="tfFalse">False</label>
      </div>
    </div>
  </div>
  ${marksAndExplanation()}`;
}

/* ── SHORT ANSWER ── */
function tmplShortAnswer() {
  return `
  <div class="border rounded-3 p-3 bg-white mb-3">
    <label class="form-label fw-semibold label-sm">
      Model Answer
      <span class="badge bg-secondary-subtle text-secondary ms-1">Shown to marker only</span>
    </label>
    <textarea class="form-control" id="shortModelAnswer" rows="3" maxlength="1000"
              placeholder="Reference answer for manual marking — not shown to student…"></textarea>
    <div class="form-text">Used as a guide during marking. Students do not see this.</div>
  </div>
  ${marksAndExplanation()}`;
}

/* ── ESSAY ── */
function tmplEssay() {
  return `
  <div class="border rounded-3 p-3 bg-white mb-3">
    <div class="mb-3">
      <label class="form-label fw-semibold label-sm">Marking Scheme</label>
      <textarea class="form-control" id="essayMarkingScheme" rows="4" maxlength="2000"
                placeholder="e.g. Award 2 marks for identifying the technique, 2 for explanation, 2 for effect on reader…"></textarea>
    </div>
    <div class="mb-0">
      <label class="form-label fw-semibold label-sm">Word Limit</label>
      <div class="d-flex align-items-center gap-2">
        <input type="number" class="form-control" id="essayWordLimit"
               value="0" min="0" max="5000" style="width:120px"/>
        <span class="text-muted small">Set to 0 for no limit</span>
      </div>
    </div>
  </div>
  ${marksRow()}`;     /* Essay has no explanation field */
}

/* ── CODE ── */
function tmplCode() {
  return `
  <div class="border rounded-3 p-3 bg-white mb-3">
    <div class="mb-3">
      <label class="form-label fw-semibold label-sm">Language</label>
      <select class="form-select" id="codeLanguage" style="max-width:220px">
        <option value="PYTHON">Python</option>
        <option value="JAVA">Java</option>
        <option value="JAVASCRIPT">JavaScript</option>
        <option value="SQL">SQL</option>
        <option value="HTML">HTML</option>
        <option value="CSS">CSS</option>
        <option value="OTHER">Other</option>
      </select>
    </div>
    <div class="mb-3">
      <label class="form-label fw-semibold label-sm">Starter Code
        <span class="text-muted fw-normal">(shown to student in editor)</span>
      </label>
      <textarea class="form-control font-monospace" id="codeStarter" rows="6"
                style="font-size:0.85rem; background:#1e1e2e; color:#cdd6f4; resize:vertical;"
                placeholder="def add(a, b):&#10;    pass"></textarea>
    </div>
    <div class="mb-0">
      <label class="form-label fw-semibold label-sm">Expected Output
        <span class="text-muted fw-normal">(optional — used in auto-check)</span>
      </label>
      <input type="text" class="form-control font-monospace" id="codeExpectedOutput"
             placeholder="5" maxlength="500"/>
    </div>
  </div>
  <div class="mb-3">
    <label class="form-label fw-semibold label-sm">Marks</label>
    <input type="number" class="form-control" id="qMarks" value="3" min="1" max="100"
           style="width:100px"/>
  </div>
  <div class="mb-3">
    <label class="form-label fw-semibold label-sm">Answer Guide / Marking Notes</label>
    <textarea class="form-control" id="qExplanation" rows="2" maxlength="1000"
              placeholder="Notes for the marker or auto-grader…"></textarea>
  </div>`;
}

/* ── IMAGE BASED ── */
function tmplImageBased() {
  return `
  <div class="border rounded-3 p-3 bg-white mb-3">
    <div class="mb-3">
      <label class="form-label fw-semibold label-sm">Question Image <span class="text-danger">*</span></label>
      <input type="file" class="form-control" id="qImage" accept="image/png,image/jpeg"/>
      <div class="form-text">PNG or JPG, max 5 MB.</div>
      <div id="imgPreviewWrap" class="mt-2 d-none">
        <img id="imgPreview" src="" alt="Preview"
             class="img-thumbnail" style="max-height:200px; object-fit:contain;"/>
      </div>
    </div>
    <div class="mb-3">
      <label class="form-label fw-semibold label-sm">Alt Text (Accessibility) <span class="text-danger">*</span></label>
      <input type="text" class="form-control" id="qImageAlt" maxlength="255"
             placeholder="Describe the image for screen readers…"/>
    </div>
    <div class="mb-3">
      <label class="form-label fw-semibold label-sm">Answer Type</label>
      <div class="form-check">
        <input class="form-check-input" type="radio" name="imgAnswerType"
               id="imgWritten" value="WRITTEN" checked/>
        <label class="form-check-label" for="imgWritten">Written response (textarea)</label>
      </div>
      <div class="form-check">
        <input class="form-check-input" type="radio" name="imgAnswerType"
               id="imgMcq" value="MCQ"/>
        <label class="form-check-label" for="imgMcq">
          MCQ options (builds single-answer option list)
        </label>
      </div>
      <div id="imgMcqOptions" class="mt-3 d-none">
        <!-- Rendered same as MCQ_SINGLE option rows when imgMcq is selected -->
      </div>
    </div>
  </div>
  ${marksAndExplanation('Answer Guide')}`;
}

/* ── Shared sub-templates ── */
function marksAndExplanation(explanationLabel = 'Answer Explanation') {
  return `
  ${marksRow()}
  <div class="mb-3">
    <label class="form-label fw-semibold label-sm">
      ${explanationLabel}
      <span class="text-muted fw-normal">(shown to student after attempt)</span>
    </label>
    <textarea class="form-control" id="qExplanation" rows="2" maxlength="1000"
              placeholder="Explain why the correct answer is correct…"></textarea>
  </div>`;
}

function marksRow() {
  return `
  <div class="mb-3">
    <label class="form-label fw-semibold label-sm">Marks</label>
    <input type="number" class="form-control" id="qMarks" value="1" min="1" max="100"
           style="width:100px"/>
  </div>`;
}

/* ── Dynamic add/remove option buttons ── */
function attachOptionListeners() {
  // MCQ Single — add option
  document.getElementById('addMcqSingleOption')?.addEventListener('click', () => {
    const container = document.getElementById('mcqSingleOptions');
    const count = container.querySelectorAll('.mcq-single-row').length;
    if (count >= 6) { showToast('Maximum 6 options allowed.', 'warning'); return; }
    container.insertAdjacentHTML('beforeend', mcqSingleOptionRow(count + 1));
    attachRemoveListeners();
  });

  // MCQ Multiple — add option
  document.getElementById('addMcqMultiOption')?.addEventListener('click', () => {
    const container = document.getElementById('mcqMultiOptions');
    const count = container.querySelectorAll('.mcq-multi-row').length;
    if (count >= 6) { showToast('Maximum 6 options allowed.', 'warning'); return; }
    container.insertAdjacentHTML('beforeend', mcqMultiOptionRow(count + 1));
    attachRemoveListeners();
  });

  // Image answer type toggle
  document.querySelectorAll('input[name="imgAnswerType"]').forEach(r => {
    r.addEventListener('change', () => {
      const mcqDiv = document.getElementById('imgMcqOptions');
      if (r.value === 'MCQ' && r.checked) {
        mcqDiv.classList.remove('d-none');
        mcqDiv.innerHTML = [1,2,3,4].map(i => mcqSingleOptionRow(i)).join('');
        attachRemoveListeners();
      } else {
        mcqDiv.classList.add('d-none');
      }
    });
  });

  // Image preview
  document.getElementById('qImage')?.addEventListener('change', function() {
    const file = this.files[0];
    if (!file) return;
    if (file.size > 5 * 1024 * 1024) {
      showToast('Image must be under 5 MB.', 'danger');
      this.value = ''; return;
    }
    const reader = new FileReader();
    reader.onload = e => {
      document.getElementById('imgPreview').src = e.target.result;
      document.getElementById('imgPreviewWrap').classList.remove('d-none');
    };
    reader.readAsDataURL(file);
  });

  attachRemoveListeners();
}

function attachRemoveListeners() {
  document.querySelectorAll('.remove-opt').forEach(btn => {
    btn.replaceWith(btn.cloneNode(true)); // de-dupe listeners
  });
  document.querySelectorAll('.remove-opt').forEach(btn => {
    btn.addEventListener('click', () => {
      const row = btn.closest('.mcq-single-row, .mcq-multi-row');
      const container = row.parentElement;
      if (container.querySelectorAll('[class*="-row"]').length <= 2) {
        showToast('Minimum 2 options required.', 'warning'); return;
      }
      row.remove();
    });
  });
}
```

#### JavaScript — Save Handlers

```javascript
/* ── Save Node ── */
document.getElementById('saveNewNodeBtn').addEventListener('click', async () => {
  const title   = document.getElementById('newNodeTitle').value.trim();
  const desc    = document.getElementById('newNodeDesc').value.trim();
  const tagline = document.getElementById('newNodeTagline').value.trim();
  const parentId= document.getElementById('newNodeParentId').value;

  if (!title) {
    showFormAlert('nodeFormAlert', 'Node Name is required.');
    return;
  }

  const res = await fetch('/api/admin/course-nodes', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${getToken()}` },
    body: JSON.stringify({ title, description: desc, tagline, type: 'NODE',
                           parentId: parseInt(parentId) })
  });

  if (res.ok) {
    bootstrap.Modal.getInstance(document.getElementById('addNodeModal')).hide();
    showToast('Node added successfully.', 'success');
    await loadTree();
  } else {
    const err = await res.json();
    showFormAlert('nodeFormAlert', err.message || 'Failed to save. Please try again.');
  }
});

/* ── Save Question ── */
document.getElementById('saveNewQuestionBtn').addEventListener('click', async () => {
  const qType      = document.getElementById('qType').value;
  const qText      = document.getElementById('qText').value.trim();
  const complexity = document.querySelector('input[name="qComplexity"]:checked')?.value;
  const parentId   = document.getElementById('qParentNodeId').value;
  const marks      = parseInt(document.getElementById('qMarks')?.value || '1');
  const explanation= document.getElementById('qExplanation')?.value?.trim() || '';

  // Base validation
  if (!qType)      { showFormAlert('qFormAlert', 'Select a question type.'); return; }
  if (!qText)      { showFormAlert('qFormAlert', 'Question text is required.'); return; }
  if (!complexity) { showFormAlert('qFormAlert', 'Select a complexity level.'); return; }

  // Build type-specific payload
  const typeData = collectTypeData(qType);
  if (typeData.error) { showFormAlert('qFormAlert', typeData.error); return; }

  const payload = {
    type: 'QUESTION', parentId: parseInt(parentId),
    title: qText.substring(0, 150),   // tree label
    questionText: qText, questionType: qType,
    complexity, marks, explanation,
    ...typeData.data
  };

  const res = await fetch('/api/admin/course-nodes', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${getToken()}` },
    body: JSON.stringify(payload)
  });

  if (res.ok) {
    bootstrap.Modal.getInstance(document.getElementById('addQuestionModal')).hide();
    showToast('Question added successfully.', 'success');
    await loadTree();
  } else {
    const err = await res.json();
    showFormAlert('qFormAlert', err.message || 'Failed to save.');
  }
});

function collectTypeData(type) {
  switch(type) {
    case 'MCQ_SINGLE': {
      const opts = [...document.querySelectorAll('input[name="mcqSingleOption"]')]
                    .map(i => i.value.trim()).filter(Boolean);
      const correct = document.querySelector('input[name="mcqSingleCorrect"]:checked')?.value;
      if (opts.length < 2)   return { error: 'Add at least 2 options.' };
      if (!correct)          return { error: 'Select the correct answer.' };
      return { data: { options: opts, correctIndex: parseInt(correct) - 1 } };
    }
    case 'MCQ_MULTIPLE': {
      const opts    = [...document.querySelectorAll('input[name="mcqMultiOption"]')]
                       .map(i => i.value.trim()).filter(Boolean);
      const corrects= [...document.querySelectorAll('input[name="mcqMultiCorrect"]:checked')]
                       .map(c => parseInt(c.value) - 1);
      const partial = document.querySelector('input[name="partialMarking"]:checked')?.value;
      if (opts.length < 2)    return { error: 'Add at least 2 options.' };
      if (!corrects.length)   return { error: 'Select at least one correct answer.' };
      return { data: { options: opts, correctIndices: corrects, partialMarking: partial } };
    }
    case 'TRUE_FALSE': {
      const ans = document.querySelector('input[name="tfAnswer"]:checked')?.value;
      if (!ans) return { error: 'Select the correct answer (True or False).' };
      return { data: { correctAnswer: ans } };
    }
    case 'SHORT_ANSWER': {
      return { data: { modelAnswer: document.getElementById('shortModelAnswer')?.value?.trim()||'' } };
    }
    case 'ESSAY': {
      return { data: {
        markingScheme: document.getElementById('essayMarkingScheme')?.value?.trim()||'',
        wordLimit:     parseInt(document.getElementById('essayWordLimit')?.value||'0')
      }};
    }
    case 'CODE': {
      return { data: {
        codeLanguage:    document.getElementById('codeLanguage')?.value,
        starterCode:     document.getElementById('codeStarter')?.value||'',
        expectedOutput:  document.getElementById('codeExpectedOutput')?.value?.trim()||''
      }};
    }
    case 'IMAGE_BASED': {
      const alt = document.getElementById('qImageAlt')?.value?.trim();
      if (!alt) return { error: 'Alt text is required for accessibility.' };
      const answerType = document.querySelector('input[name="imgAnswerType"]:checked')?.value;
      let extra = {};
      if (answerType === 'MCQ') {
        const opts = [...document.querySelectorAll('#imgMcqOptions input[name="mcqSingleOption"]')]
                      .map(i => i.value.trim()).filter(Boolean);
        const correct = document.querySelector('#imgMcqOptions input[name="mcqSingleCorrect"]:checked')?.value;
        if (opts.length < 2) return { error: 'Add at least 2 MCQ options for image question.' };
        if (!correct) return { error: 'Select the correct MCQ option.' };
        extra = { options: opts, correctIndex: parseInt(correct) - 1 };
      }
      return { data: { imageAlt: alt, imageAnswerType: answerType, ...extra } };
    }
    default: return { error: 'Unknown question type.' };
  }
}

function showFormAlert(id, msg) {
  const el = document.getElementById(id);
  el.textContent = msg;
  el.classList.remove('d-none');
}
```

---

### 5.4 Node Detail Panel

Clicking any node populates the right panel with an editable form.

```javascript
function openDetailPanel(node) {
  const isQuestion = node.type === 'QUESTION';
  document.getElementById('detailPlaceholder').classList.add('d-none');
  const form = document.getElementById('detailForm');
  form.classList.remove('d-none');

  form.innerHTML = `
    <div class="d-flex align-items-center mb-4 gap-2">
      <i class="bi ${isQuestion ? 'bi-patch-question-fill text-warning'
                                : 'bi-folder2 text-primary'} fs-3"></i>
      <h4 class="fw-bold mb-0">${isQuestion ? 'Question' : 'Node'} Details</h4>
      <span class="badge ${isQuestion ? 'bg-warning text-dark' : 'bg-primary'} ms-auto">
        ${node.type}
      </span>
    </div>

    <div class="card card-form p-4 bg-white mb-3">
      ${isQuestion ? renderQuestionFields(node) : renderNodeFields(node)}
    </div>

    <div class="d-flex gap-2">
      <button class="btn btn-primary fw-semibold px-4" onclick="saveNode(${node.id})">
        <i class="bi bi-save me-1"></i>Save changes
      </button>
      <button class="btn btn-outline-danger fw-semibold px-4" onclick="deleteNode(${node.id})">
        <i class="bi bi-trash me-1"></i>Delete
      </button>
    </div>
  `;
  // After render, populate type-specific fields for questions
  if (isQuestion && node.questionType) {
    setTimeout(() => {
      document.getElementById('detailTypeFields').innerHTML =
        renderQuestionTypeFields(node.questionType);
      populateTypeFieldValues(node);
      attachOptionListeners();
    }, 0);
  }
}

/* ── Node detail fields (for NODE type) ── */
function renderNodeFields(node) {
  return `
    <div class="mb-3">
      <label class="form-label fw-semibold label-sm">
        Node Name <span class="text-danger">*</span>
      </label>
      <input type="text" class="form-control" id="detailTitle"
             value="${escHtml(node.title)}" maxlength="150"/>
    </div>
    <div class="mb-3">
      <label class="form-label fw-semibold label-sm">Description</label>
      <textarea class="form-control" id="detailDesc" rows="3"
                maxlength="500">${escHtml(node.description || '')}</textarea>
    </div>
    <div class="mb-3">
      <label class="form-label fw-semibold label-sm">Tag Line</label>
      <input type="text" class="form-control" id="detailTagline"
             value="${escHtml(node.tagline || '')}" maxlength="100"
             placeholder="Short line shown on course cards…"/>
      <div class="form-text">Max 100 characters.</div>
    </div>
  `;
}

/* ── Question detail fields (for QUESTION type) ── */
function renderQuestionFields(node) {
  const complexityBadge = {
    FOUNDATION:   'bg-success-subtle text-success border-success',
    INTERMEDIATE: 'bg-warning-subtle text-warning border-warning',
    HIGHER:       'bg-danger-subtle text-danger border-danger',
  };
  const types = [
    ['MCQ_SINGLE',   'MCQ – Single Answer'],
    ['MCQ_MULTIPLE', 'MCQ – Multiple Answer'],
    ['TRUE_FALSE',   'True / False'],
    ['SHORT_ANSWER', 'Short Answer'],
    ['ESSAY',        'Essay'],
    ['CODE',         'Code'],
    ['IMAGE_BASED',  'Image Based'],
  ];
  return `
    <div class="mb-3">
      <label class="form-label fw-semibold label-sm">
        Question Text <span class="text-danger">*</span>
      </label>
      <textarea class="form-control" id="qText" rows="3"
                maxlength="2000">${escHtml(node.questionText || '')}</textarea>
    </div>
    <div class="row g-3 mb-3">
      <div class="col-md-7">
        <label class="form-label fw-semibold label-sm">Question Type</label>
        <select class="form-select" id="qType"
                onchange="document.getElementById('detailTypeFields').innerHTML =
                          renderQuestionTypeFields(this.value); attachOptionListeners();">
          ${types.map(([v,l]) =>
            `<option value="${v}" ${node.questionType===v?'selected':''}>${l}</option>`
          ).join('')}
        </select>
      </div>
      <div class="col-md-5">
        <label class="form-label fw-semibold label-sm">Complexity</label>
        <div class="d-flex flex-wrap gap-2 mt-1">
          ${['FOUNDATION','INTERMEDIATE','HIGHER'].map(c => `
            <div class="form-check">
              <input class="form-check-input" type="radio" name="qComplexityEdit"
                     value="${c}" id="cmpEdit${c}"
                     ${node.complexity===c?'checked':''}/>
              <label class="form-check-label" for="cmpEdit${c}">
                <span class="badge border ${complexityBadge[c]||''}">
                  ${c.charAt(0)+c.slice(1).toLowerCase()}
                </span>
              </label>
            </div>`).join('')}
        </div>
      </div>
    </div>
    <div id="detailTypeFields">
      <!-- populated after render via setTimeout -->
    </div>
  `;
}

function escHtml(str) {
  return str.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')
            .replace(/"/g,'&quot;');
}
```

---

### 5.5 Question Leaf Behaviour

| Rule | Enforcement |
|---|---|
| A `QUESTION` node **cannot** have children | Backend rejects `POST /api/admin/course-nodes` if `parentId` points to a QUESTION |
| Right-click on QUESTION hides Add Node & Add Question | JS hides both context menu items |
| No expand arrow shown | `invisible` CSS class applied |
| Selecting a QUESTION shows full question form | Fields: Text, Type (7 types), Complexity, type-specific fields, Marks |
| MCQ max 6 options | Enforced in JS (UI) and Bean Validation (`@Size(max=6)`) on `options` list |
| MCQ_SINGLE exactly 1 correct | Backend validates `correctIndex` is within options bounds |
| MCQ_MULTIPLE ≥1 correct | Backend validates `correctIndices` is non-empty |
| IMAGE_BASED requires alt text | `@NotBlank` on `imageAlt`; validated before save |
| Image upload max 5 MB | JS file size check + Spring `MultipartFile` size limit config |

Backend guard:
```java
// CourseNodeService.java
public CourseNode addNode(CreateNodeRequest req) {
    if (req.getParentId() != null) {
        CourseNode parent = nodeRepository.findById(req.getParentId())
            .orElseThrow(() -> new EntityNotFoundException("Parent node not found"));
        if (parent.getType() == NodeType.QUESTION) {
            throw new BusinessException("Cannot add children to a Question node.");
        }
    }
    // ... save logic
}
```

---

### 5.6 API Contracts

#### `GET /api/admin/course-nodes`
Returns flat list; frontend builds tree.

**Response 200:**
```json
[
  {
    "id": 1, "parentId": null, "type": "NODE",
    "title": "Mathematics", "description": "Core maths curriculum",
    "tagline": "Build analytical thinking from the ground up",
    "sortOrder": 0
  },
  {
    "id": 2, "parentId": 1, "type": "NODE",
    "title": "Algebra", "description": "", "tagline": "", "sortOrder": 0
  },
  {
    "id": 5, "parentId": 2, "type": "QUESTION",
    "title": "Solve x + 2 = 5",
    "questionText": "What is the value of x in x + 2 = 5?",
    "questionType": "MCQ_SINGLE",
    "complexity": "FOUNDATION",
    "options": ["x = 1","x = 2","x = 3","x = 5"],
    "correctIndex": 2,
    "marks": 1,
    "explanation": "Subtract 2 from both sides: x = 3.",
    "sortOrder": 0
  }
]
```

#### `POST /api/admin/course-nodes` — Add Node

**Request:**
```json
{
  "parentId":    1,
  "type":        "NODE",
  "title":       "Geometry",
  "description": "Shapes, angles and spatial reasoning",
  "tagline":     "See the world through mathematical shapes"
}
```

**Response 201:** Created node object.

#### `POST /api/admin/course-nodes` — Add Question (MCQ_SINGLE example)

**Request:**
```json
{
  "parentId":     2,
  "type":         "QUESTION",
  "title":        "Solve x + 2 = 5",
  "questionText": "What is the value of x in x + 2 = 5?",
  "questionType": "MCQ_SINGLE",
  "complexity":   "FOUNDATION",
  "options":      ["x = 1", "x = 2", "x = 3", "x = 5"],
  "correctIndex": 2,
  "marks":        1,
  "explanation":  "Subtract 2 from both sides: x = 3."
}
```

**Request (MCQ_MULTIPLE):**
```json
{
  "questionType":   "MCQ_MULTIPLE",
  "complexity":     "INTERMEDIATE",
  "options":        ["Option A","Option B","Option C","Option D"],
  "correctIndices": [1, 2],
  "partialMarking": "PER_OPTION",
  "marks":          4,
  "explanation":    "B and C are both correct because…"
}
```

**Request (TRUE_FALSE):**
```json
{
  "questionType":  "TRUE_FALSE",
  "complexity":    "FOUNDATION",
  "correctAnswer": "TRUE",
  "marks":         1,
  "explanation":   "The statement is true because…"
}
```

**Request (SHORT_ANSWER):**
```json
{
  "questionType": "SHORT_ANSWER",
  "complexity":   "INTERMEDIATE",
  "modelAnswer":  "Photosynthesis converts light to glucose.",
  "marks":        2,
  "explanation":  "See marking guide."
}
```

**Request (ESSAY):**
```json
{
  "questionType":   "ESSAY",
  "complexity":     "HIGHER",
  "markingScheme":  "2 marks: technique identified. 2 marks: explanation. 2 marks: effect.",
  "wordLimit":      500,
  "marks":          6
}
```

**Request (CODE):**
```json
{
  "questionType":   "CODE",
  "complexity":     "HIGHER",
  "codeLanguage":   "PYTHON",
  "starterCode":    "def add(a, b):\n    pass",
  "expectedOutput": "5",
  "marks":          3,
  "explanation":    "Return the sum of a and b."
}
```

**Request (IMAGE_BASED — written response):**
```json
{
  "questionType":     "IMAGE_BASED",
  "complexity":       "INTERMEDIATE",
  "imageAlt":         "Bar chart showing sales by region",
  "imageAnswerType":  "WRITTEN",
  "marks":            2,
  "explanation":      "The North region has the highest sales."
}
```

**Response 400 — validation failures:**
```json
{ "status": 400, "message": "correctIndex: must be within options range; options: size must be between 2 and 6" }
```

**Response 400 — leaf violation:**
```json
{ "status": 400, "message": "Cannot add children to a Question node." }
```

#### `PUT /api/admin/course-nodes/{id}`

Accepts the same shape as POST for the given node type. Returns updated node object (200).

#### `DELETE /api/admin/course-nodes/{id}`

Deletes node and all descendants (cascade). Returns 204 No Content.

---

### 5.7 Database Schema

```sql
-- ─────────────────────────────────────────────────────────────
-- V3__course_nodes.sql
-- ─────────────────────────────────────────────────────────────

-- Course nodes (self-referencing tree)
CREATE TABLE course_nodes (
  id              BIGSERIAL    PRIMARY KEY,
  parent_id       BIGINT       REFERENCES course_nodes(id) ON DELETE CASCADE,
  type            VARCHAR(20)  NOT NULL CHECK (type IN ('NODE','QUESTION')),

  -- NODE fields
  title           VARCHAR(150) NOT NULL,
  description     TEXT,
  tagline         VARCHAR(100),          -- short marketing line for NODE type

  -- QUESTION shared fields
  question_text   TEXT,
  question_type   VARCHAR(20)
                  CHECK (question_type IN (
                    'MCQ_SINGLE','MCQ_MULTIPLE','TRUE_FALSE',
                    'SHORT_ANSWER','ESSAY','CODE','IMAGE_BASED'
                  )),
  complexity      VARCHAR(20)
                  CHECK (complexity IN ('FOUNDATION','INTERMEDIATE','HIGHER')),
  marks           SMALLINT     DEFAULT 1 CHECK (marks >= 1 AND marks <= 100),
  explanation     TEXT,                  -- shown to student after attempt

  -- MCQ_SINGLE / MCQ_MULTIPLE / IMAGE_BASED(MCQ) options stored as JSON array
  options         JSONB,                 -- e.g. ["Option A","Option B","Option C","Option D"]
  correct_index   SMALLINT,              -- MCQ_SINGLE: 0-based index
  correct_indices JSONB,                 -- MCQ_MULTIPLE: e.g. [1,2]
  partial_marking VARCHAR(20)
                  CHECK (partial_marking IN ('FULL_ONLY','PER_OPTION')),

  -- TRUE_FALSE
  correct_answer  VARCHAR(5)
                  CHECK (correct_answer IN ('TRUE','FALSE')),

  -- SHORT_ANSWER
  model_answer    TEXT,

  -- ESSAY
  marking_scheme  TEXT,
  word_limit      SMALLINT     DEFAULT 0,

  -- CODE
  code_language   VARCHAR(20)
                  CHECK (code_language IN (
                    'PYTHON','JAVA','JAVASCRIPT','SQL','HTML','CSS','OTHER'
                  )),
  starter_code    TEXT,
  expected_output TEXT,

  -- IMAGE_BASED
  image_path      VARCHAR(500),          -- stored path after S3/filesystem upload
  image_alt       VARCHAR(255),
  image_answer_type VARCHAR(10)
                  CHECK (image_answer_type IN ('WRITTEN','MCQ')),

  -- Audit
  sort_order      INT          NOT NULL DEFAULT 0,
  created_by      BIGINT       NOT NULL REFERENCES users(id),
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_cn_parent      ON course_nodes(parent_id);
CREATE INDEX idx_cn_type        ON course_nodes(type);
CREATE INDEX idx_cn_q_type      ON course_nodes(question_type) WHERE type = 'QUESTION';
CREATE INDEX idx_cn_complexity  ON course_nodes(complexity)    WHERE type = 'QUESTION';

-- Keep updated_at current
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN NEW.updated_at = NOW(); RETURN NEW; END;
$$;

CREATE TRIGGER trg_cn_updated_at
  BEFORE UPDATE ON course_nodes
  FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- ─────────────────────────────────────────────────────────────
-- V4__question_images.sql  (separate migration for image uploads)
-- ─────────────────────────────────────────────────────────────
-- Images are stored on S3 (or local filesystem in dev).
-- The image_path column in course_nodes stores the relative path.
-- Spring Boot multipart config (application.yml):
--
--   spring:
--     servlet:
--       multipart:
--         max-file-size: 5MB
--         max-request-size: 10MB
```

---

### 5.8 Backend Service Layer

#### `CourseNodeController.java`
```java
@RestController
@RequestMapping("/api/admin/course-nodes")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class CourseNodeController {

    private final CourseNodeService service;

    @GetMapping
    public ResponseEntity<List<CourseNodeDto>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @PostMapping
    public ResponseEntity<CourseNodeDto> create(@Valid @RequestBody CreateNodeRequest req,
                                                 Authentication auth) {
        return ResponseEntity.status(201).body(service.addNode(req, auth.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseNodeDto> update(@PathVariable Long id,
                                                 @Valid @RequestBody UpdateNodeRequest req) {
        return ResponseEntity.ok(service.updateNode(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteNode(id);
        return ResponseEntity.noContent().build();
    }
}
```

#### `CourseNodeService.java` (core logic)
```java
@Service
@Transactional
@RequiredArgsConstructor
public class CourseNodeService {

    private final CourseNodeRepository nodeRepo;
    private final UserRepository       userRepo;

    public List<CourseNodeDto> findAll() {
        return nodeRepo.findAllByOrderBySortOrderAsc()
                       .stream().map(CourseNodeMapper::toDto).toList();
    }

    public CourseNodeDto addNode(CreateNodeRequest req, String email) {
        if (req.getParentId() != null) {
            CourseNode parent = nodeRepo.findById(req.getParentId())
                .orElseThrow(() -> new EntityNotFoundException("Parent not found"));
            if (parent.getType() == NodeType.QUESTION) {
                throw new BusinessException("Question nodes cannot have children.");
            }
        }
        User creator = userRepo.findByEmail(email).orElseThrow();
        CourseNode node = CourseNodeMapper.fromRequest(req, creator);
        return CourseNodeMapper.toDto(nodeRepo.save(node));
    }

    public CourseNodeDto updateNode(Long id, UpdateNodeRequest req) {
        CourseNode node = nodeRepo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Node not found: " + id));
        CourseNodeMapper.applyUpdate(node, req);
        return CourseNodeMapper.toDto(nodeRepo.save(node));
    }

    public void deleteNode(Long id) {
        // Cascade DELETE on DB handles descendants
        nodeRepo.deleteById(id);
    }
}
```

---

## 6. Module 3 — Assessment Designer

Admin-only feature at `/admin/exams/builder`. Lets an admin compose exam papers from existing Question nodes and manage their lifecycle (DRAFT → APPROVED).

### 6.1 Layout

Three-column workspace (full-viewport height, no scroll):

```
┌──────────────┬───────────────────────────┬──────────────────────────────┐
│   SIDEBAR    │   QUESTION PICKER (360px) │   EXAM PAPER BUILDER (flex)  │
│              │                           │                              │
│  Dashboard   │  ┌─ Cascade Filters ────┐ │  [Select exam ▾] [+ New]    │
│  Design      │  │ Class      ▾          │ │  [🗑 Delete]                 │
│  Courses     │  │ Subject    ▾ (locked) │ │                              │
│  Assessment  │  │ Exam Board ▾ (locked) │ │  Exam Name *                 │
│  Designer ←  │  │ Topic      ▾ (locked) │ │  Description                 │
│  Users       │  │ Sub Topic  ▾ (locked) │ │  Time Limit  Total Marks     │
│  Approvals   │  │ Complexity ▾          │ │  Pass Mark                   │
│  Analytics   │  │ [Apply Filter]        │ │  ☐ Shuffle Questions         │
│              │  └───────────────────────┘ │  ☐ Shuffle Options           │
│              │                           │                              │
│              │  ┌─ Results ─────────────┐ │  Questions in this exam [3]  │
│              │  │ [Q title] [+]         │ │  ↑↓ Q1  Solve x+2=5  1mk [✕]│
│              │  │ [Q title] [✓] disabled│ │  ↑↓ Q2  …            2mk [✕]│
│              │  │ …                     │ │                              │
│              │  └───────────────────────┘ │  [Save Draft] [Submit/Approve]│
└──────────────┴───────────────────────────┴──────────────────────────────┘
```

Route: `GET /admin/exams/builder` → Thymeleaf template `admin/exam-builder.html`.

---

### 6.2 Question Picker & Cascade Filters

#### Cascade Dropdown Behaviour

Five dependent dropdowns map directly onto the Course Designer tree hierarchy:

| Dropdown | Level | Loads |
|---|---|---|
| Class | 0 (root) | `GET /api/admin/question-search/tree-nodes` (no parentId) |
| Subject | 1 | `GET /api/admin/question-search/tree-nodes?parentId=<classId>` |
| Exam Board | 2 | `…?parentId=<subjectId>` |
| Topic | 3 | `…?parentId=<examBoardId>` |
| Sub Topic | 4 | `…?parentId=<topicId>` |

Rules:
- Only `NODE`-type nodes appear in dropdowns (not QUESTIONs).
- Each level is disabled until its parent is selected.
- Selecting a higher level resets and disables all levels below it.
- Changing any dropdown auto-triggers a question search.
- **Complexity** dropdown is independent (Foundation / Intermediate / Higher / All).
- **Apply Filter** button re-runs the search with current selections.
- **Refresh** button (↺) reloads the Class list and resets all cascade state.

#### Question Search — BFS Logic

The `/api/admin/question-search` endpoint does **not** rely on metadata columns. Instead:

1. Load all `course_nodes` flat from DB.
2. Build an in-memory `parentId → [childIds]` map.
3. BFS from the selected `nodeId` to collect all descendant node IDs (including the selected node itself).
4. Return all `QUESTION`-type nodes whose `parentId` is in that descendant set.
5. If no `nodeId` → return all questions.
6. Apply optional `complexity` filter on top.

This means questions surface at any depth below the selected tree node, regardless of how many levels deep they sit.

#### Picker Item Display

Each result shows: title (truncated), first 70 chars of questionText, type badge, complexity badge, marks badge, and a `[+]` add button. Already-added questions show a disabled `[✓]` button.

---

### 6.3 Exam Paper Builder

#### Exam Lifecycle

```
New form (unsaved) → [Save Draft] → DRAFT → [Submit/Approve] → APPROVED
```

- APPROVED exams: Submit button disabled, shows "Approved" label.
- Delete available at any status.
- Auto-save: if user clicks `[+]` on a question while no exam is selected and the Name field is filled, the exam is auto-saved as DRAFT first, then the question is added.

#### Exam Form Fields

| Field | Constraint |
|---|---|
| Name | Required, max 200 chars |
| Description | Optional, textarea |
| Time Limit (min) | Required, default 60, min 1 |
| Total Marks | Auto-calculated (sum of per-question marks); admin may override |
| Pass Mark | Optional integer |
| Shuffle Questions | Boolean toggle |
| Shuffle Options | Boolean toggle |

#### Question List Controls

- `↑` / `↓` buttons reorder questions (calls `PUT /api/admin/exams/{id}/questions/reorder` with full ordered list of question IDs).
- `✕` removes a question from the exam.
- Question count badge and total marks display update after every add/remove/reorder.

---

### 6.4 API Contracts

All endpoints under `/api/admin/exams` require `ADMIN` role.

#### Exam CRUD

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/admin/exams` | List all exams (`ExamSummaryDto[]`) |
| `GET` | `/api/admin/exams/{id}` | Get exam with full question list (`ExamDto`) |
| `POST` | `/api/admin/exams` | Create exam (DRAFT) |
| `PUT` | `/api/admin/exams/{id}` | Update exam metadata |
| `DELETE` | `/api/admin/exams/{id}` | Delete exam and all exam_questions |
| `POST` | `/api/admin/exams/{id}/submit` | Promote status to APPROVED |

**`POST /api/admin/exams` Request:**
```json
{
  "name": "Year 10 Mock Paper 1",
  "description": "End of term mock",
  "timeLimitMinutes": 90,
  "passMark": 50,
  "shuffleQuestions": true,
  "shuffleOptions": false
}
```

**`ExamDto` Response:**
```json
{
  "id": 1,
  "name": "Year 10 Mock Paper 1",
  "description": "End of term mock",
  "timeLimitMinutes": 90,
  "totalMarks": 15,
  "passMark": 50,
  "shuffleQuestions": true,
  "shuffleOptions": false,
  "status": "DRAFT",
  "questions": [
    {
      "examQuestionId": 3,
      "questionId": 15,
      "title": "Solve x + 2 = 5",
      "questionText": "What is the value of x?",
      "questionType": "MCQ_SINGLE",
      "complexity": "FOUNDATION",
      "marks": 1,
      "position": 0
    }
  ]
}
```

**`ExamSummaryDto` (list response):**
```json
{
  "id": 1,
  "name": "Year 10 Mock Paper 1",
  "status": "DRAFT",
  "timeLimitMinutes": 90,
  "totalMarks": 15,
  "questionCount": 5
}
```

#### Question Management

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/admin/exams/{id}/questions` | Add question to exam |
| `DELETE` | `/api/admin/exams/{id}/questions/{questionId}` | Remove question |
| `PUT` | `/api/admin/exams/{id}/questions/reorder` | Reorder (full ordered list) |

**`POST /api/admin/exams/{id}/questions` Request:**
```json
{ "questionId": 15, "marksOverride": null }
```
`marksOverride` — if set, overrides the question's default marks for this exam only. If null, uses `course_nodes.marks` (default 1).

**`PUT /api/admin/exams/{id}/questions/reorder` Request:**
```json
[15, 18, 22, 16, 17]
```
Array of **questionIds** in desired order. Must contain exactly the same IDs currently in the exam — any mismatch returns 400.

#### Question Search

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/admin/question-search/tree-nodes` | NODE children for cascade dropdown |
| `GET` | `/api/admin/question-search` | Search questions by subtree + complexity |

**`GET /api/admin/question-search/tree-nodes?parentId=10`**
```json
[
  { "id": 11, "title": "Subject - Maths" },
  { "id": 12, "title": "Subject - Science" }
]
```
Omit `parentId` → returns root-level nodes (Class level).

**`GET /api/admin/question-search?nodeId=10&complexity=INTERMEDIATE`**
```json
[
  {
    "id": 16,
    "title": "True & False Question",
    "questionText": "The earth is flat.",
    "questionType": "TRUE_FALSE",
    "complexity": "INTERMEDIATE",
    "marks": 1,
    "className": null,
    "subject": null,
    "examBoard": null,
    "topic": null,
    "subTopic": null
  }
]
```

---

### 6.5 Database Schema

```sql
-- V6__assessment_designer.sql

CREATE TABLE exams (
  id                BIGSERIAL     PRIMARY KEY,
  name              VARCHAR(200)  NOT NULL,
  description       TEXT,
  time_limit_minutes INT          NOT NULL DEFAULT 60,
  total_marks       INT           NOT NULL DEFAULT 0,
  pass_mark         INT,
  shuffle_questions  BOOLEAN      NOT NULL DEFAULT FALSE,
  shuffle_options    BOOLEAN      NOT NULL DEFAULT FALSE,
  status            VARCHAR(20)   NOT NULL DEFAULT 'DRAFT'
                    CHECK (status IN ('DRAFT','APPROVED')),
  created_by        BIGINT        NOT NULL REFERENCES users(id),
  created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
  updated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE exam_questions (
  id              BIGSERIAL   PRIMARY KEY,
  exam_id         BIGINT      NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
  question_id     BIGINT      NOT NULL REFERENCES course_nodes(id) ON DELETE CASCADE,
  position        INT         NOT NULL DEFAULT 0,
  marks_override  INT,
  CONSTRAINT uq_exam_question UNIQUE (exam_id, question_id)
);

CREATE INDEX idx_eq_exam     ON exam_questions(exam_id);
CREATE INDEX idx_eq_question ON exam_questions(question_id);

CREATE TRIGGER trg_exams_updated_at
  BEFORE UPDATE ON exams
  FOR EACH ROW EXECUTE FUNCTION update_updated_at();
```

Full migration order:
```
V1__init_users.sql
V2__password_reset_tokens.sql
V3__course_nodes.sql
V4__course_nodes_expand.sql
V5__reset_admin_password.sql
V6__assessment_designer.sql
V7__add_teacher_user.sql
V8__add_pending_approval_status.sql
V9__teacher_profiles.sql
V10__student_profiles.sql
V11__assignments.sql
```

---

### 6.6 Key Implementation Notes

#### BFS in `QuestionSearchController`

```java
private Set<Long> collectSubtreeIds(List<CourseNode> allNodes, Long rootId) {
    Map<Long, List<Long>> childrenMap = new HashMap<>();
    for (CourseNode n : allNodes) {
        if (n.getParentId() != null)
            childrenMap.computeIfAbsent(n.getParentId(), k -> new ArrayList<>()).add(n.getId());
    }
    Set<Long> result = new HashSet<>();
    Deque<Long> queue = new ArrayDeque<>();
    queue.add(rootId);
    while (!queue.isEmpty()) {
        Long curr = queue.poll();
        result.add(curr);
        queue.addAll(childrenMap.getOrDefault(curr, Collections.emptyList()));
    }
    return result;
}
```

All nodes are loaded once (`findAllByOrderBySortOrderAsc`); BFS runs in memory. Performs within target <300ms for up to 500 nodes.

#### Total Marks Auto-Calc (`ExamService`)

```java
private void recalcTotalMarks(Exam exam, Long examId) {
    int total = examQRepo.findByExamIdOrderByPositionAsc(examId).stream()
        .mapToInt(eq -> {
            if (eq.getQuestion() == null) return 0;
            return eq.getMarksOverride() != null
                ? eq.getMarksOverride()
                : (eq.getQuestion().getMarks() != null ? eq.getQuestion().getMarks() : 1);
        }).sum();
    exam.setTotalMarks(total);
    examRepo.save(exam);
}
```

#### Auto-Save on First Question Add (`exam.js`)

```javascript
if (!currentExam) {
  var name = (document.getElementById('examName').value || '').trim();
  if (!name) {
    showToast('Enter an exam name first, then add questions.', 'warning');
    document.getElementById('examName').focus();
    return;
  }
  await saveExam();          // POSTs to /api/admin/exams, sets currentExam
  if (!currentExam) return;  // saveExam failed (server error already toasted)
}
```

#### Cascade Dropdown State Reset

When a higher-level dropdown changes, `loadTreeNodes(parentId, level)` must reset `selectedLevelId[level] = null` before repopulating to avoid stale node IDs in the search query:

```javascript
async function loadTreeNodes(parentNodeId, level) {
  // ...fetch nodes...
  selectedLevelId[level] = null;   // critical: clear before repopulating
  el.innerHTML = '<option value="">' + cfg.label + '</option>';
  nodes.forEach(n => el.add(new Option(n.title, n.id)));
  el.disabled = false;
  clearLevelsBelow(level);
}
```

#### Reorder Validation

`PUT /api/admin/exams/{id}/questions/reorder` validates:
1. Submitted list length equals current question count.
2. Every ID in the list is an existing `questionId` for this exam.

Returns `400 BusinessException` on any mismatch.

---

## 7. Module 4 — Teacher/Student Management & Assignments

### 7.1 UI Design System (Card Pattern)

All admin pages use a unified card/panel design applied consistently from the assignments page:

| Token | Value |
|---|---|
| Workspace background | `#f8fafc` |
| Panel card background | `#ffffff` |
| Panel card border | `1px solid #e2e8f0` |
| Panel card border-radius | `12px` |
| Panel card shadow | `0 1px 3px 0 rgba(0,0,0,.1), 0 1px 2px -1px rgba(0,0,0,.08)` |
| Panel header icon square | `28×28px`, `border-radius:8px`, role-tinted bg (e.g. `#eff6ff` for blue) |
| Panel header title | `font-weight:700; color:#1a2332; font-size:.875rem` |
| Empty state (full panel) | `.empty-state` class — centered icon + title + subtitle |
| Empty state (inline) | `.panel-empty` class |
| Item grid cards | `.item-card` with `.item-card__bar--blue/purple/green` accent bar |
| User avatars | `.user-avatar.user-avatar--lg.user-avatar--blue/purple` gradient circles |
| Filter bar | `.filter-card` wrapper class |

**Icons:** Bootstrap Icons 1.11.3. No legacy text arrows (`▾`, `›`, `↑↓`). Use `bi-chevron-down/right`, `bi-chevron-up/down`.

**Cache-busting:** Thymeleaf `th:href="@{/css/main.css(v=3)}"` generates `/css/main.css?v=3`.

---

### 7.2 Teacher Management

#### Route & Template
- Page: `GET /admin/teachers` → `templates/admin/teachers.html`
- Register form: `GET /admin/teachers/register` → `templates/admin/teacher-register.html`

#### Domain
- `TeacherProfile` entity (`teacher_profiles` table, 1:1 with `users`)
- Migration: `V9__teacher_profiles.sql`

#### API
| Method | Path | Description |
|---|---|---|
| `GET` | `/api/admin/teachers` | List all → `TeacherSummaryDto[]` |
| `POST` | `/api/admin/teachers` | Register (multipart: profile photo + 6 doc uploads) |

**`TeacherSummaryDto`:**
```json
{
  "id": 1,
  "userId": 3,
  "teacherId": "TCH-001",
  "firstName": "Jane",
  "lastName": "Smith",
  "email": "jane@school.edu",
  "designation": "Senior Teacher",
  "department": "Mathematics",
  "employmentStatus": "ACTIVE",
  "employmentType": "FULL_TIME",
  "profilePhotoPath": "uploads/teacher_1/photo.jpg",
  "joiningDate": "2024-01-15"
}
```

> **Important:** `TeacherSummaryDto.id` = `teacher_profiles.id` (NOT `users.id`). Use this ID when referencing teachers in assignment payloads.

---

### 7.3 Student Management

#### Route & Template
- Page: `GET /admin/students` → `templates/admin/students.html`
- Register form: `GET /admin/students/register` → `templates/admin/student-register.html`

#### Domain
- `StudentProfile` entity (`student_profiles` table, linked to `users`)
- Migration: `V10__student_profiles.sql`

#### API
| Method | Path | Description |
|---|---|---|
| `GET` | `/api/admin/students` | List all → `StudentSummaryDto[]` |
| `POST` | `/api/admin/students` | Register (multipart) |

**`StudentSummaryDto`:**
```json
{
  "id": 3,
  "studentId": "STU-2024-003",
  "firstName": "Arnav",
  "lastName": "Basal",
  "gradeYear": "Year 10",
  "className": "10A",
  "section": "A",
  "studentStatus": "ACTIVE",
  "enrollmentType": "FULL_TIME",
  "programCourse": "GCSE Sciences"
}
```

> **Important:** `StudentSummaryDto.id` = `student_profiles.id`. Use this in assignment group student lists.

---

### 7.4 Assignments Page

#### Route & Template
`GET /admin/assignments` → `templates/admin/assignments.html`
JS: `static/js/assignments.js`

#### Three-Step Flow

```
Card 1: Select Course     →  Card 2: Create Groups     →  Card 3: Assign Teachers
(dropdown + scope radio)      (modal, student enrol)        (modal, teacher select)
         ↓                           ↓                              ↓
  S.selectedCourseId           S.groups[]                  S.activatedTeacherIds[]
  S.selectedScopeKey           S.maxPerGroup                S.assignments{}
```

#### Frontend State (`var S`)
```javascript
var S = {
  nodes:            [],   // flat course_nodes from GET /api/admin/course-nodes
  students:         [],   // StudentSummaryDto[] from GET /api/admin/students
  teachers:         [],   // TeacherSummaryDto[] from GET /api/admin/teachers
  selectedCourseId: null,
  maxPerGroup:      30,
  groups:           [],   // {id(local), name, desc, period, studentIds:[]}
  assignments:      {},   // teacherProfileId -> groupLocalId
  nextGroupId:      1,
  activatedTeacherIds: [], // teacher profile IDs added via modal
  scopeNodes:       { course: null, subject: null, board: null },
  selectedScopeKey: null   // 'course' | 'subject' | 'board'
};
```

#### Save Payload (`POST /api/admin/assignments`)

```json
{
  "courseNodeId":  10,
  "scopeNodeId":   11,
  "scopeLevel":    "subject",
  "maxPerGroup":   30,
  "status":        "DRAFT",
  "groups": [
    {
      "localId":    1,
      "name":       "Group Alpha",
      "description":"Morning batch",
      "period":     "2025-2026",
      "studentIds": [1, 3, 5]
    }
  ],
  "teacherAssignments": [
    { "teacherId": 1, "groupLocalId": 1 }
  ]
}
```

`localId` — ephemeral frontend ID; service maps it to the saved `assignment_groups.id` via `Map<Integer, AssignmentGroup>`. `teacherId` = `teacher_profiles.id`. `studentIds` = `student_profiles.id[]`.

**Response:**
```json
{ "sessionId": 5, "status": "SAVED", "message": "Assignment saved successfully" }
```

#### Save Button IDs
- `id="btnSaveDraft"` → calls `saveAsDraft()` → status `DRAFT`
- `id="btnReviewSave"` → calls `reviewAndSave()` → status `SAVED`

Both show a spinner while the POST is in flight; restore label on completion.

---

### 7.5 API Contracts (Module 4)

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/admin/teachers` | ADMIN | List teacher profiles |
| `POST` | `/api/admin/teachers` | ADMIN | Register teacher (multipart) |
| `GET` | `/api/admin/students` | ADMIN | List student profiles |
| `POST` | `/api/admin/students` | ADMIN | Register student (multipart) |
| `POST` | `/api/admin/assignments` | ADMIN | Create assignment session |

---

### 7.6 Database Schema (Module 4)

```sql
-- V9: teacher_profiles (key columns only)
CREATE TABLE teacher_profiles (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    teacher_id  VARCHAR(50),
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL,
    designation VARCHAR(150),
    department  VARCHAR(150),
    employment_status VARCHAR(20) DEFAULT 'ACTIVE'
      CHECK (employment_status IN ('ACTIVE','INACTIVE','ON_LEAVE')),
    employment_type   VARCHAR(20)
      CHECK (employment_type IN ('FULL_TIME','PART_TIME','CONTRACT')),
    joining_date DATE,
    -- + address, emergency contact, payroll, document paths, audit cols
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- V10: student_profiles (key columns only)
CREATE TABLE student_profiles (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT REFERENCES users(id) ON DELETE SET NULL,
    student_id     VARCHAR(50),
    first_name     VARCHAR(100) NOT NULL,
    last_name      VARCHAR(100) NOT NULL,
    grade_year     VARCHAR(50),
    class_name     VARCHAR(50),
    section        VARCHAR(20),
    student_status VARCHAR(20) DEFAULT 'ACTIVE'
      CHECK (student_status IN ('ACTIVE','INACTIVE','GRADUATED','SUSPENDED')),
    -- + guardian info, address, documents, audit cols
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- V11: assignments
CREATE TABLE assignment_sessions (
    id             BIGSERIAL PRIMARY KEY,
    course_node_id BIGINT NOT NULL REFERENCES course_nodes(id) ON DELETE CASCADE,
    scope_node_id  BIGINT REFERENCES course_nodes(id) ON DELETE SET NULL,
    scope_level    VARCHAR(20) NOT NULL CHECK (scope_level IN ('course','subject','board')),
    max_per_group  INT NOT NULL DEFAULT 30,
    status         VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                   CHECK (status IN ('DRAFT','SAVED')),
    created_by     BIGINT NOT NULL REFERENCES users(id),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE assignment_groups (
    id          BIGSERIAL PRIMARY KEY,
    session_id  BIGINT NOT NULL REFERENCES assignment_sessions(id) ON DELETE CASCADE,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    period      VARCHAR(100)
);

CREATE TABLE assignment_group_students (
    group_id           BIGINT NOT NULL REFERENCES assignment_groups(id) ON DELETE CASCADE,
    student_profile_id BIGINT NOT NULL REFERENCES student_profiles(id) ON DELETE CASCADE,
    PRIMARY KEY (group_id, student_profile_id)
);

CREATE TABLE assignment_teacher_mappings (
    id                 BIGSERIAL PRIMARY KEY,
    session_id         BIGINT NOT NULL REFERENCES assignment_sessions(id) ON DELETE CASCADE,
    teacher_profile_id BIGINT NOT NULL REFERENCES teacher_profiles(id) ON DELETE CASCADE,
    group_id           BIGINT REFERENCES assignment_groups(id) ON DELETE SET NULL,
    UNIQUE (session_id, teacher_profile_id)
);
```

> **Key constraint:** `assignment_group_students` → `student_profiles(id)` and `assignment_teacher_mappings` → `teacher_profiles(id)`. These are profile IDs, not `users.id`. The APIs (`GET /api/admin/students`, `GET /api/admin/teachers`) return profile IDs in their `id` field.

---

## 8. Error Handling Standards

### Global Exception Handler
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(EntityNotFoundException ex) {
        return ResponseEntity.status(404).body(new ErrorResponse(404, ex.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> business(BusinessException ex) {
        return ResponseEntity.status(400).body(new ErrorResponse(400, ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> forbidden(AccessDeniedException ex) {
        return ResponseEntity.status(403).body(new ErrorResponse(403, "Access denied."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return ResponseEntity.status(400).body(new ErrorResponse(400, msg));
    }
}

record ErrorResponse(int status, String message) {}
```

### Frontend Error Toast
```javascript
function showToast(message, type = 'danger') {
  const toast = document.createElement('div');
  toast.className = `toast align-items-center text-white bg-${type} border-0 show`;
  toast.setAttribute('role', 'alert');
  toast.innerHTML = `
    <div class="d-flex">
      <div class="toast-body fw-semibold">${message}</div>
      <button type="button" class="btn-close btn-close-white me-2 m-auto"
              data-bs-dismiss="toast"></button>
    </div>`;
  document.getElementById('toastContainer').appendChild(toast);
  setTimeout(() => toast.remove(), 4000);
}
```

```html
<!-- Place near </body> -->
<div id="toastContainer"
     class="toast-container position-fixed bottom-0 end-0 p-3"
     style="z-index:9999;"></div>
```

---

## 9. Non-Functional Requirements

| Category | Requirement |
|---|---|
| **Performance** | API responses < 300ms for tree fetch (up to 500 nodes) |
| **Security** | JWT HS256, 24h expiry, refresh token mechanism for sessions > 24h |
| **Accessibility** | WCAG 2.1 AA — all interactive elements keyboard-navigable, ARIA labels on tree nodes |
| **Responsive** | Sidebar collapses to icon-only on screens < 992px; tree panel stacks below on mobile |
| **Logging** | SLF4J + Logback; log all auth events (login, failed login, password reset) |
| **Audit** | `created_by` and `updated_at` on all domain tables |
| **Testing** | JUnit 5 + Mockito for service layer; Playwright or Selenium for E2E login flow |
| **Environment** | `.env` or Spring Profiles (`dev`, `staging`, `prod`) for DB creds and JWT secret |
| **DB Migrations** | Flyway — all schema changes versioned under `db/migration/` |

### Flyway Migration Naming Convention
```
db/migration/
  V1__init_users.sql
  V2__password_reset_tokens.sql
  V3__course_nodes.sql
```

---

*End of Specification — EducationPro Modules 1–4*
*Next: Module 4 remaining (load/edit saved sessions), Module 5 (Student Course View)*
