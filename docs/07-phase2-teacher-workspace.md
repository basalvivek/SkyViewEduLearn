# EduLearn Platform — Phase 2: Teacher Workspace

## Modules Covered

| # | Module | Description |
|---|--------|-------------|
| 4 | My Profile | Teacher updates name, password, avatar |
| 5 | Submission History | Full audit trail of teacher's own content |
| 6 | Rejection Feedback | Inline rejection reason, edit and re-submit flow |

---

## Module 4 — My Profile

### Overview
Every teacher can view and edit their own profile. Admins can view and edit any user's profile.
Accessible from the **top-right user dropdown → My Profile**.

---

### 4.1 UI — Profile Page

**Route:** rendered as a page within `teacher-dashboard.html` (page id: `page-profile`)
**Nav trigger:** user dropdown top-right → "My Profile"

#### Sections

**Personal Information**
| Field | Type | Required | Notes |
|-------|------|----------|-------|
| Avatar / photo | File upload | No | PNG/JPG max 1MB, shown as circle |
| Full name | Text | Yes | |
| Display name | Text | No | Shown in tree nodes and submissions |
| Job title | Text | No | e.g. Head of English |
| Department | Text | No | e.g. Humanities |
| Bio | Textarea | No | Max 500 chars, shown on teacher profile card |

**Contact Information**
| Field | Type | Required |
|-------|------|----------|
| Email | Email | Yes (read-only — contact admin to change) |
| Phone | Tel | No |

**Change Password**
| Field | Type | Rules |
|-------|------|-------|
| Current password | Password | Must match existing hash |
| New password | Password | Min 8 chars, 1 uppercase, 1 number |
| Confirm new password | Password | Must match new password |

**Account Info (read-only)**
| Field | Value |
|-------|-------|
| Role | Teacher |
| Account created | Formatted date |
| Last login | Formatted date/time |
| Account status | Active / Inactive badge |

#### Save behaviour
- Personal info and contact saved together (one Save button)
- Password change is a separate form with its own Submit button
- Success → green toast: "Profile updated successfully"
- Wrong current password → inline red error under field
- Passwords don't match → inline red error under confirm field

---

### 4.2 Database — Users table additions

Add to existing `users` table via ALTER (or include in initial schema):

```sql
ALTER TABLE users ADD COLUMN IF NOT EXISTS display_name   VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_path    VARCHAR(500);
ALTER TABLE users ADD COLUMN IF NOT EXISTS job_title      VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS department     VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS bio            TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone          VARCHAR(30);
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login_at  TIMESTAMPTZ;
```

---

### 4.3 API Endpoints — Profile

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET    | `/api/v1/my/profile` | Any | Get own profile |
| PUT    | `/api/v1/my/profile` | Any | Update own profile |
| POST   | `/api/v1/my/profile/avatar` | Any | Upload avatar (multipart) |
| PUT    | `/api/v1/my/profile/password` | Any | Change own password |
| GET    | `/api/v1/users/{id}/profile` | Admin | View any user's profile |

**PUT /my/profile request:**
```json
{
  "fullName": "Sarah Clarke",
  "displayName": "Ms. Clarke",
  "jobTitle": "Head of English",
  "department": "Humanities",
  "bio": "10 years teaching GCSE English at Oakfield Academy.",
  "phone": "+44 7911 123456"
}
```

**PUT /my/profile/password request:**
```json
{
  "currentPassword": "OldPass@123",
  "newPassword": "NewPass@456",
  "confirmPassword": "NewPass@456"
}
```

---

### 4.4 Spring Boot Classes — Profile

```
controller/  ProfileController.java
service/     ProfileService.java
dto/request/ ProfileUpdateRequest.java
             PasswordChangeRequest.java
dto/response/ProfileResponse.java
```

**ProfileService key logic:**
```java
public void changePassword(UUID userId, PasswordChangeRequest req) {
    User user = userRepo.findById(userId).orElseThrow();
    if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash()))
        throw new ValidationException("Current password is incorrect");
    if (!req.newPassword().equals(req.confirmPassword()))
        throw new ValidationException("Passwords do not match");
    user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
    user.setUpdatedAt(OffsetDateTime.now());
    userRepo.save(user);
}
```

---

## Module 5 — Submission History

### Overview
Teachers see a complete audit trail of all content they have ever created or edited —
with status, rejection reasons, timestamps, and quick links to edit.

---

### 5.1 UI — Submission History Page

**Route:** page within `teacher-dashboard.html` (page id: `page-submissions`)
**Nav item:** "Submissions" in left sidebar

#### Layout
Full-width table with filters at the top.

**Filters (horizontal row):**
- Status: All | Draft | Pending | Approved | Rejected (pill toggle)
- Content type: All | Category | Subject | Topic | Question (dropdown)
- Date range: From / To date pickers
- Search: text input (searches by content name)

**Table columns:**

| Column | Notes |
|--------|-------|
| Content | Name with content-type icon (📁📚📂❓) |
| Type | Category / Subject / Topic / Question badge |
| Path | Breadcrumb e.g. Year 10 › English › Topic |
| Status | Coloured badge: Draft (grey) / Pending (amber) / Approved (green) / Rejected (red) |
| Submitted | Date submitted for approval |
| Reviewed | Date admin actioned it (or "—") |
| Reviewed by | Admin name (or "—") |
| Actions | Edit button (pencil) · Re-submit button (if Rejected) |

**Pagination:** 20 rows per page, with page controls.

**Empty state:** "No submissions yet. Start by adding content to a course."

---

### 5.2 API Endpoints — Submissions

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | `/api/v1/my/submissions` | Teacher | Paginated list with filters |
| GET | `/api/v1/my/submissions/summary` | Teacher | Count by status (for dashboard stat cards) |

**GET /my/submissions query params:**
```
?status=REJECTED&type=TOPIC&from=2025-01-01&to=2025-12-31&q=reading&page=0&size=20
```

**Response:**
```json
{
  "status": "success",
  "data": [
    {
      "id": "uuid",
      "entityType": "TOPIC",
      "entityId": "uuid",
      "label": "Comparative Reading",
      "path": "Year 10 › English Language",
      "status": "REJECTED",
      "rejectionReason": "Topic name too vague, please be more specific.",
      "submittedAt": "2025-05-29T10:00:00Z",
      "reviewedAt": "2025-05-30T14:00:00Z",
      "reviewedByName": "Admin User"
    }
  ],
  "pagination": { "page": 0, "size": 20, "total": 45, "totalPages": 3 }
}
```

**GET /my/submissions/summary response:**
```json
{
  "status": "success",
  "data": {
    "draft": 3,
    "pending": 2,
    "approved": 38,
    "rejected": 2,
    "total": 45
  }
}
```

---

### 5.3 Spring Boot Classes — Submissions

```
controller/  SubmissionController.java
service/     SubmissionService.java
dto/response/SubmissionResponse.java
             SubmissionSummaryResponse.java
```

**SubmissionService — query logic:**

The submissions list is built by querying all four content tables
(`categories`, `subjects`, `topics`, `questions`) where `created_by = currentUser`
and applying status/type filters, then merging and sorting by `updated_at DESC`.

Use a UNION view or individual queries merged in Java:

```java
// Pseudocode — run per entity type matching filters, merge results
List<SubmissionResponse> results = new ArrayList<>();
if (type == null || type == CATEGORY)  results.addAll(categoryRepo.findByCreatedBy(userId, status));
if (type == null || type == SUBJECT)   results.addAll(subjectRepo.findByCreatedBy(userId, status));
if (type == null || type == TOPIC)     results.addAll(topicRepo.findByCreatedBy(userId, status));
if (type == null || type == QUESTION)  results.addAll(questionRepo.findByCreatedBy(userId, status));
results.sort(Comparator.comparing(SubmissionResponse::submittedAt).reversed());
return paginate(results, page, size);
```

---

## Module 6 — Rejection Feedback

### Overview
When admin rejects a teacher's submission, the teacher must:
1. See the rejection reason clearly in both the submissions list and tree
2. Be able to open the rejected item and edit it
3. Re-submit it for approval (status goes back to PENDING)

---

### 6.1 UI — Rejection Display

#### In Submission History table
- Status badge: red `Rejected` pill
- Below the row (expandable) or in a tooltip: rejection reason text

#### In the Course Tree sidebar
- Rejected nodes show a **red** `Rejected` badge (not amber like Pending)
- Clicking the node loads the detail form

#### In the Detail Form (right panel)
Show a rejection banner above the form:

```
┌─────────────────────────────────────────────────────┐
│ ✕  Rejected by Admin User on 30 May 2025            │
│    "Topic name too vague, please be more specific." │
│    Edit below and re-submit for approval.           │
└─────────────────────────────────────────────────────┘
```

- Banner is **red** with red border
- All form fields are **editable** (teacher can fix the issue)
- Button row: `Re-submit for approval` | `Save as draft`
- On re-submit: `rejection_reason` is cleared, status → `PENDING`

---

### 6.2 Status Badge Colours — Full Reference

| Status | Badge colour | Text |
|--------|-------------|------|
| DRAFT | Grey `#9399a8` | Draft |
| PENDING | Amber `#92400e` bg `#fef3c7` | Pending |
| APPROVED | Green `#0f5940` bg `#e4f5ee` | Approved |
| REJECTED | Red `#991b1b` bg `#fee2e2` | Rejected |

---

### 6.3 Re-submission API Flow

```
Teacher edits rejected item
        │
        ▼
PUT /api/v1/topics/{id}          ← saves field changes (status stays REJECTED)
        │
Teacher clicks "Re-submit"
        │
        ▼
PUT /api/v1/topics/{id}/submit   ← sets status = PENDING, clears rejection_reason
        │                           logs to approval_log
        ▼
Admin sees it in approval queue again
```

**New endpoint — submit for approval:**

| Method | Path | Role | Description |
|--------|------|------|-------------|
| PUT | `/api/v1/categories/{id}/submit` | Teacher (owner) | Submit DRAFT or REJECTED → PENDING |
| PUT | `/api/v1/subjects/{id}/submit` | Teacher (owner) | |
| PUT | `/api/v1/topics/{id}/submit` | Teacher (owner) | |
| PUT | `/api/v1/questions/{id}/submit` | Teacher (owner) | |

**Rules enforced in service layer:**
- Only the `created_by` user can submit (not any teacher)
- Item must be in `DRAFT` or `REJECTED` status to submit
- `APPROVED` or `PENDING` items cannot be re-submitted without an edit first
- Edit (PUT) to an `APPROVED` item auto-sets status back to `DRAFT` — teacher must then submit

---

### 6.4 Approval Log — rejection entries

Every rejection is recorded in `approval_log`:
```sql
-- Example row when admin rejects
INSERT INTO approval_log (entity_type, entity_id, from_status, to_status, actioned_by, note)
VALUES ('TOPIC', 'uuid', 'PENDING', 'REJECTED', 'admin-uuid', 'Topic name too vague.');
```

Teacher can view their full history via `/my/submissions` which joins `approval_log`
to show who actioned it and when.

---

## Database Schema Additions (Phase 2)

Run this after the base schema from `sql/02-database-schema.sql`:

```sql
-- ── Phase 2 additions ────────────────────────────────────────

-- Profile fields on users (if not already added)
ALTER TABLE users ADD COLUMN IF NOT EXISTS display_name   VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_path    VARCHAR(500);
ALTER TABLE users ADD COLUMN IF NOT EXISTS job_title      VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS department     VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS bio            TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone          VARCHAR(30);
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login_at  TIMESTAMPTZ;

-- Submission view (optional — makes querying all 4 tables easier)
CREATE OR REPLACE VIEW teacher_submissions AS
  SELECT 'CATEGORY' AS entity_type, id, name AS label, status, rejection_reason,
         created_by, updated_by, approved_by, approved_at, created_at, updated_at
  FROM categories WHERE is_deleted = FALSE
UNION ALL
  SELECT 'SUBJECT', id, name, status, rejection_reason,
         created_by, updated_by, approved_by, approved_at, created_at, updated_at
  FROM subjects WHERE is_deleted = FALSE
UNION ALL
  SELECT 'TOPIC', id, name, status, rejection_reason,
         created_by, updated_by, approved_by, approved_at, created_at, updated_at
  FROM topics WHERE is_deleted = FALSE
UNION ALL
  SELECT 'QUESTION', id, LEFT(question_text, 100), status, rejection_reason,
         created_by, updated_by, approved_by, approved_at, created_at, updated_at
  FROM questions WHERE is_deleted = FALSE;
```

---

## Package Structure Additions (Phase 2)

```
com.edulearn
├── controller/
│   ├── ProfileController.java          # GET/PUT /my/profile, POST /my/profile/avatar
│   └── SubmissionController.java       # GET /my/submissions, GET /my/submissions/summary
├── service/
│   ├── ProfileService.java
│   └── SubmissionService.java
└── dto/
    ├── request/
    │   ├── ProfileUpdateRequest.java
    │   └── PasswordChangeRequest.java
    └── response/
        ├── ProfileResponse.java
        ├── SubmissionResponse.java
        └── SubmissionSummaryResponse.java
```

No new entities needed — Phase 2 uses the existing `users`, content tables, and `approval_log`.

---

## Frontend Pages — Phase 2

```
static/
└── teacher/
    ├── dashboard.html     ← add profile quick-link to user dropdown (already done)
    ├── courses.html       ← add red Rejected badge + rejection banner in detail panel
    └── submissions.html   ← NEW: full submissions history table with filters
```

**teacher/submissions.html layout:**

```
┌─────────────────────────────────────────────────────────┐
│ NAV  │  TOPBAR                                          │
│      ├─────────────────────────────────────────────────┤
│      │  My Submissions                                  │
│      │                                                  │
│      │  [All][Draft][Pending][Approved][Rejected]        │
│      │  Type ▾   From ___   To ___   🔍 Search          │
│      │                                                  │
│      │  ┌──────────────────────────────────────────┐   │
│      │  │ Content │ Type │ Path │Status│Date│Action│   │
│      │  ├──────────────────────────────────────────┤   │
│      │  │  rows...                                 │   │
│      │  └──────────────────────────────────────────┘   │
│      │                                                  │
│      │  ← 1  2  3 →   Showing 1–20 of 45              │
└─────────────────────────────────────────────────────────┘
```

---

## JS Conventions — Phase 2

All new pages follow the same JS pattern as Phase 1:

```javascript
// On page load
const token = sessionStorage.getItem('edu_token');
if (!token) window.location.href = '/login.html';

// API call pattern
async function loadSubmissions(filters) {
  const params = new URLSearchParams(filters);
  const res = await apiFetch(`/my/submissions?${params}`);
  renderTable(res.data);
  renderPagination(res.pagination);
}

// Edit rejected item → navigate to courses page with item pre-selected
function editRejectedItem(entityType, entityId) {
  sessionStorage.setItem('edu_open_node', JSON.stringify({ entityType, entityId }));
  window.location.href = '/teacher/courses.html';
}
```

---

## Security Rules — Phase 2 Additions

| Endpoint | Rule |
|----------|------|
| `PUT /my/profile` | Teacher can only update own profile |
| `PUT /my/profile/password` | Must verify current password first |
| `POST /my/profile/avatar` | Max 1MB, PNG/JPG only — validate server-side |
| `PUT /{entity}/{id}/submit` | Must be `created_by` = current user |
| `GET /my/submissions` | Returns only records where `created_by` = current user |
| `GET /users/{id}/profile` | Admin only |

---

## Files Added / Changed in Phase 2

| File | Change |
|------|--------|
| `docs/07-phase2-teacher-workspace.md` | **This file — new** |
| `sql/03-phase2-schema-additions.sql` | Profile columns + teacher_submissions view |
| `html/teacher-dashboard.html` | Add profile page, rejected banner in tree detail |
| `html/teacher-submissions.html` | New submissions history page with filters |
| `CLAUDE.md` | Updated package structure + build order |
