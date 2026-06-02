# EduLearn Platform — Correction: Student Registration & Portal

## What This Document Fixes

Phase 3 (Module 9) covered admin-side student account management only.
The **student-facing portal** — login, onboarding, dashboard, navigation,
profile, and exam browsing — was never specified. This document fills all 7 gaps.
It must be implemented **between Phase 3 and Phase 4**.

---

## Gap Summary

| # | Gap | Impact |
|---|-----|--------|
| 1 | No Student tab on login.html | Students cannot log in |
| 2 | No first-login / forced password change flow | Security risk — temp passwords never changed |
| 3 | student/dashboard.html never designed | No shell, no nav, no content spec |
| 4 | Student profile (self-service) not specified | Student cannot update own details post-login |
| 5 | Student navigation structure not documented | Claude Code cannot build the portal |
| 6 | No "My Exams" page design | Exam browsing UI missing |
| 7 | login.html does not route STUDENT role | Portal unreachable after login |

---

## Fix 1 & 7 — Student Login + Routing

### Add Student tab to login.html

The existing login has two pills: `[Admin]` `[Teacher]`.
Add a third: `[Student]`.

**Student left panel (teal theme `#0e7490`):**
- Icon: graduation cap
- Title: "Welcome back, Student"
- Sub: "View your exams, track your progress, and see your results."
- Features: My exams · My results · Announcements · My profile

**Student form:**
- Email placeholder: `student@school.edu`
- Submit button: "Sign in as Student"
- Footer: "Need help? Contact your teacher"

**Routing on login success:**
```javascript
if (res.data.role === 'ADMIN')   window.location.href = '/admin/dashboard.html';
if (res.data.role === 'TEACHER') window.location.href = '/teacher/dashboard.html';
if (res.data.role === 'STUDENT') window.location.href = '/student/dashboard.html';
```

**Left panel colour per role:**

| Role | Nav colour | Accent |
|------|-----------|--------|
| Admin | `#1a3a5c` (dark blue) | `#2563a8` |
| Teacher | `#0f5940` (dark green) | `#1a8a5a` |
| Student | `#0e7490` (teal) | `#0891b2` |

---

## Fix 2 — First Login & Forced Password Change

### DB change
```sql
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE;
-- Set TRUE when admin creates a student account with a temp password
-- Set FALSE after student successfully changes it
```

### Flow

```
Student logs in with temp password
        │
        ▼
Backend checks: user.mustChangePassword === true?
        │ YES
        ▼
Return special response: { status: "PASSWORD_CHANGE_REQUIRED", token: "..." }
        │
Browser redirects to → /student/change-password.html
        │
Student enters: new password + confirm
        │
PUT /api/v1/my/profile/password  (with mustChangePassword token)
        │
        ▼
must_change_password = FALSE  →  redirect to /student/dashboard.html
```

### change-password.html — Fields

| Field | Type | Rules |
|-------|------|-------|
| New password | Password | Min 8 chars, 1 uppercase, 1 number |
| Confirm password | Password | Must match |
| [Set password & continue] | Button | — |

**Same page used by all roles** (not student-specific).
Apply `security.*` settings from `platform_settings` table for rules.

### Backend — AuthService change
```java
// In login():
if (user.isMustChangePassword()) {
    String tempToken = jwtUtil.generateTempToken(user, "PASSWORD_CHANGE");
    return ApiResponse.of("PASSWORD_CHANGE_REQUIRED",
        Map.of("token", tempToken, "redirectTo", "/student/change-password.html"));
}
```

---

## Fix 3 — Student Dashboard Layout

**File:** `static/student/dashboard.html`
**Theme colour:** Teal `#0e7490`

### Full Layout

```
┌───────────┬──────────────────────────────────────────────┐
│  NAV      │  TOPBAR                                      │
│  (teal)   │  [breadcrumb]    [🔔] [Avatar ▾]            │
│           ├──────────────────────────────────────────────┤
│ Dashboard │                                              │
│ My Exams  │  Good morning, Sarah                         │
│ My Results│  Here's your study activity today.           │
│ Announce. │                                              │
│           │  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐        │
│           │  │Exams │ │Done  │ │Avg % │ │Pass  │        │
│           │  │ avail│ │ 5    │ │ 74%  │ │rate  │        │
│           │  │  2   │ │taken │ │      │ │ 80%  │        │
│           │  └──────┘ └──────┘ └──────┘ └──────┘        │
│           │                                              │
│           │  Upcoming exams                              │
│           │  ┌────────────────────────────────────────┐  │
│           │  │ English Mock  · Thu 5 Jun · 10:00am   │  │
│           │  │ 60 mins · Year 10A       [Start →]    │  │
│           │  └────────────────────────────────────────┘  │
│           │                                              │
│           │  Recent results                              │
│           │  ┌────────────────────────────────────────┐  │
│           │  │ Maths Mock · 78% · ✓ Pass · 30 May   │  │
│           │  └────────────────────────────────────────┘  │
│           │                                              │
│ [Avatar]  │  Announcements                              │
│ Sarah J.  │  ┌────────────────────────────────────────┐  │
│ Student   │  │ 📢 Exam timetable change — 2 days ago  │  │
└───────────┴──────────────────────────────────────────────┘
```

### Student Stat Cards

| Card | Value | Notes |
|------|-------|-------|
| Available exams | Count of ACTIVE schedules for student's class | |
| Exams taken | Total SUBMITTED attempts | |
| Average % | Mean percentage across all marked attempts | |
| Pass rate | % of attempts where is_passed = true | |

### Upcoming Exam Card Design

```
┌──────────────────────────────────────────────────────┐
│  📝  Year 10 English Mock Paper 1                    │
│      Thursday 5 June 2025  ·  10:00 – 11:00am       │
│      60 minutes  ·  Year 10A  ·  45 questions        │
│                                    [Start exam →]    │
└──────────────────────────────────────────────────────┘
```

- `[Start exam →]` button only active if schedule is ACTIVE now
- If UPCOMING: shows countdown "Starts in 2 days"
- If CLOSED: shows "Closed" badge in grey

---

## Fix 4 — Student Profile (Self-Service)

**Page:** rendered as a page within `student/dashboard.html` (page id: `page-profile`)
**Access:** via user dropdown → "My Profile"

### Fields

**Personal Information**
| Field | Type | Notes |
|-------|------|-------|
| Avatar | File upload | PNG/JPG max 1MB, circle display |
| Full name | Text | Editable |
| Display name | Text | e.g. "Sarah" |

**Contact (read-only — admin manages)**
| Field | Value |
|-------|-------|
| Email | Read-only |
| Year group | Read-only |
| Class | Read-only |

**Change Password**
| Field | Type |
|-------|------|
| Current password | Password |
| New password | Password |
| Confirm new password | Password |

**Account Info (read-only)**
| Field | Value |
|-------|-------|
| Role | Student |
| Account created | Date |
| Last login | Date/time |

Reuses `PUT /api/v1/my/profile` and `PUT /api/v1/my/profile/password` —
same endpoints as teacher profile (already defined in Phase 2 doc).

---

## Fix 5 — Student Navigation Structure

### Left sidebar nav items

```
nav (teal #0e7490)
  ──── My learning ────
  Dashboard
  My Exams           ← badge: active exam count
  My Results
  ──── Communication ──
  Announcements      ← badge: unread count
  ──── Account ────────
  (nav footer: avatar + name + logout via user dropdown)
```

### User dropdown (top-right — same pattern as admin/teacher)

| Item | Action |
|------|--------|
| My Profile | Opens profile page |
| | divider |
| Sign out | Clear sessionStorage → /login.html |

No "Organisation Settings", no "Preferences" for students.

---

## Fix 6 — My Exams Page

**File:** `static/student/exams.html`
**Nav item:** "My Exams"

### Layout — Three tabs

```
[ Active now ]  [ Upcoming ]  [ Completed ]
```

#### Active Now tab
```
┌────────────────────────────────────────────────────────┐
│  📝  Year 10 English Mock                              │
│      Started: 10:02am  ·  Remaining: 48 minutes        │
│      Progress: 7 / 15 questions answered               │
│                              [Continue exam →]         │
└────────────────────────────────────────────────────────┘
```
Shows IN_PROGRESS attempts. "Continue exam →" resumes from last saved answer.

#### Upcoming tab
Sorted by `start_at` ascending:
```
┌────────────────────────────────────────────────────────┐
│  📅  Year 10 Maths Practice Paper                      │
│      Thursday 12 Jun · 09:00 – 10:00am · 60 mins       │
│      45 questions · Pass mark: 50%                     │
│                    Starts in 7 days  [Set reminder 🔔] │
└────────────────────────────────────────────────────────┘
```

#### Completed tab
Sorted by `submitted_at` descending:

| Exam | Date | Score | Result | Action |
|------|------|-------|--------|--------|
| English Mock | 30 May | 78% | ✓ Pass | View results |
| Maths Test 1 | 15 May | 44% | ✗ Fail | View results |

---

## DB Change — Fix 2

```sql
-- Add first-login flag to users table
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

-- Create index for auth check
CREATE INDEX IF NOT EXISTS idx_users_must_change
    ON users(must_change_password) WHERE must_change_password = TRUE;
```

Update `StudentService.create()` to set `must_change_password = TRUE`
whenever a student account is created with an admin-generated temp password.

---

## API Changes — Student Portal

### New / modified endpoints

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | `/api/v1/student/dashboard` | Student | Dashboard stats (exam count, avg %, pass rate) |
| GET | `/api/v1/student/exams/active` | Student | In-progress + currently-active schedules |
| GET | `/api/v1/student/exams/upcoming` | Student | Future scheduled exams for student's class |
| GET | `/api/v1/student/exams/completed` | Student | Past attempts with results |

**GET /student/dashboard response:**
```json
{
  "data": {
    "availableExams": 2,
    "examsTaken": 5,
    "averagePercentage": 73.6,
    "passRate": 80.0,
    "upcomingExams": [
      {
        "scheduleId": "uuid",
        "examName": "Year 10 English Mock",
        "startAt": "2025-06-05T10:00:00Z",
        "endAt": "2025-06-05T11:00:00Z",
        "timeLimitMins": 60,
        "questionCount": 45,
        "status": "UPCOMING"
      }
    ],
    "recentResults": [
      {
        "attemptId": "uuid",
        "examName": "Maths Practice Paper",
        "percentage": 78.0,
        "isPassed": true,
        "submittedAt": "2025-05-30T10:54:00Z"
      }
    ]
  }
}
```

**AUTH — first login response (triggers forced password change):**
```json
{
  "status": "PASSWORD_CHANGE_REQUIRED",
  "message": "You must change your password before continuing.",
  "data": {
    "tempToken": "eyJ...",
    "redirectTo": "/student/change-password.html"
  }
}
```

---

## Spring Boot Changes

### New classes

```
controller/  StudentDashboardController.java   # /student/dashboard, /student/exams/*
service/     StudentDashboardService.java
dto/response/StudentDashboardResponse.java
             StudentExamCardResponse.java
```

### Modified classes

```
service/AuthService.java         — check must_change_password on login
service/StudentService.java      — set must_change_password=TRUE on account creation
entity/User.java                 — add mustChangePassword field
```

### Frontend files

```
static/
├── login.html                   ← ADD Student tab + teal panel + routing
├── student/
│   ├── change-password.html     ← NEW: forced password change screen
│   ├── dashboard.html           ← NEW: full dashboard with nav + stat cards
│   ├── exams.html               ← NEW: My Exams (active / upcoming / completed tabs)
│   ├── exam.html                ← already specified in Phase 3
│   ├── results.html             ← already specified in Phase 4
│   └── announcements.html       ← already specified in Phase 5
```

---

## Where This Fits in the Build Order

Insert these steps **after Phase 3 step 42** and **before Phase 4 step 51**:

```
Step 42b: Run `sql/09-student-portal-additions.sql`
Step 42c: Update `User` entity — add `mustChangePassword` field
Step 42d: Update `AuthService.login()` — return PASSWORD_CHANGE_REQUIRED if flag set
Step 42e: Update `StudentService.create()` — set mustChangePassword=TRUE for temp passwords
Step 42f: Create `StudentDashboardController` + `StudentDashboardService`
Step 42g: Add Student tab + teal panel to `login.html`; add STUDENT route to JS login handler
Step 42h: Create `student/change-password.html`
Step 42i: Create `student/dashboard.html` — full shell with nav, stat cards, upcoming + results
Step 42j: Create `student/exams.html` — Active / Upcoming / Completed tabs
Step 42k: Test: admin creates student → student logs in → forced password change →
          dashboard shows correct data → can browse and start exams
```

---

## Files Added / Changed

| File | Change |
|------|--------|
| `docs/13-correction-student-portal.md` | **This file — new** |
| `sql/09-student-portal-additions.sql` | `must_change_password` column + index |
| `html/login.html` | Add Student tab, teal theme, routing |
| `student/change-password.html` | Forced first-login password change |
| `student/dashboard.html` | Full student dashboard |
| `student/exams.html` | My Exams — Active / Upcoming / Completed tabs |
| `CLAUDE.md` | Updated with student portal classes, routes, build steps |
