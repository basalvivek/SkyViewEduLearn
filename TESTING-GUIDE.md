# SkyViewEduLearn — Complete Testing Guide

> Covers everything built through **Phase 3 + Student Portal** (the current state of the codebase).  
> Work through each section top-to-bottom. Every step has an expected result so you know whether it passed or failed.

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Database Setup](#2-database-setup)
3. [Start the Application](#3-start-the-application)
4. [Seed / Default Accounts](#4-seed--default-accounts)
5. [Authentication Testing](#5-authentication-testing)
6. [Content Hierarchy Testing](#6-content-hierarchy-testing)
7. [Approval Workflow Testing](#7-approval-workflow-testing)
8. [Phase 1 — Org Settings & Notifications](#8-phase-1--org-settings--notifications)
9. [Phase 2 — Teacher Workspace](#9-phase-2--teacher-workspace)
10. [Phase 3 — Exam Builder (Admin)](#10-phase-3--exam-builder-admin)
11. [Phase 3 — Classroom & Schedule Management](#11-phase-3--classroom--schedule-management)
12. [Phase 3 — Student Management](#12-phase-3--student-management)
13. [Student Portal — First Login & Password Change](#13-student-portal--first-login--password-change)
14. [Student Portal — Dashboard & Exams](#14-student-portal--dashboard--exams)
15. [Phase 3 — Exam Attempt (Student)](#15-phase-3--exam-attempt-student)
16. [Phase 3 — Marking (Teacher / Admin)](#16-phase-3--marking-teacher--admin)
17. [API Quick-Reference (cURL / Postman)](#17-api-quick-reference-curl--postman)
18. [Common Errors & Fixes](#18-common-errors--fixes)

---

## 1. Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Java | 17 | `java -version` must show 17.x |
| Maven | 3.8+ | bundled `mvnw` works too |
| PostgreSQL | 15 | running on **port 5433** (see application.properties) |
| Browser | Any modern | Chrome / Edge / Firefox |
| (Optional) Postman | Any | for raw API tests |

**PostgreSQL connection details (from `application.properties`):**

```
Host     : localhost
Port     : 5433
Database : edulearn
User     : edulearn_user
Password : changeme
```

Create the database and user if not done yet:

```sql
-- run as postgres superuser
CREATE USER edulearn_user WITH PASSWORD 'changeme';
CREATE DATABASE edulearn OWNER edulearn_user;
GRANT ALL PRIVILEGES ON DATABASE edulearn TO edulearn_user;
```

---

## 2. Database Setup

Run the SQL files **in order** using psql or pgAdmin:

```bash
# Step 1 — core schema
psql -U edulearn_user -d edulearn -f sql/02-database-schema.sql

# Step 2 — question/answer corrections (adds columns to questions + options)
psql -U edulearn_user -d edulearn -f sql/05-question-answer-corrections.sql

# Step 3 — student portal (adds must_change_password column)
psql -U edulearn_user -d edulearn -f sql/09-student-portal-additions.sql

# Step 4 — seed default accounts
psql -U edulearn_user -d edulearn -f sql/init-seed.sql
```

**Expected result:** No errors. Check with:

```sql
SELECT full_name, email, role FROM users;
-- Should show: Admin User + Demo Teacher
```

> **Note:** Phase 4, 5, 6 SQL files are NOT yet applied — do NOT run them yet.

---

## 3. Start the Application

From the project root (`SkyViewEduLearn/`):

```bash
./mvnw spring-boot:run
# Windows:
mvnw.cmd spring-boot:run
```

**Expected startup output (last few lines):**

```
Started EduLearnApplication in X.XXX seconds
Tomcat started on port 8080
```

Open browser: **http://localhost:8080/login.html**

You should see the login page with three tabs: **Admin | Teacher | Student**

---

## 4. Seed / Default Accounts

| Role | Email | Password | Notes |
|------|-------|----------|-------|
| Admin | `admin@localhost` | `Admin@123` | Created by schema seed |
| Teacher | `teacher@localhost` | `Teacher@123` | Created by init-seed.sql |
| Student | Created manually | Temp password set by admin | `must_change_password = true` |

---

## 5. Authentication Testing

### 5.1 Admin Login

1. Go to `http://localhost:8080/login.html`
2. Click the **Admin** tab
3. Enter `admin@localhost` / `Admin@123`
4. Click **Sign In**

**Expected:** Redirected to `/admin/dashboard.html`  
**Failure signs:** Stays on login page, "Invalid credentials" toast

### 5.2 Teacher Login

1. Click the **Teacher** tab
2. Enter `teacher@localhost` / `Teacher@123`
3. Click **Sign In**

**Expected:** Redirected to `/teacher/dashboard.html`

### 5.3 Wrong Password

1. Enter `admin@localhost` / `wrongpassword`

**Expected:** Error toast "Invalid email or password". No redirect.

### 5.4 Role Guard (access control)

1. Log in as Teacher
2. Manually navigate to `http://localhost:8080/admin/dashboard.html`

**Expected:** Redirected back to `/login.html` (JS auth guard fires)

### 5.5 Logout

1. Log in as Admin
2. Click your name / avatar in the top-right
3. Click **Logout**

**Expected:** Session cleared, redirected to `/login.html`  
Verify: opening DevTools → Application → Session Storage → `edu_token` is gone

### 5.6 Token Expiry (optional)

- Token TTL is 24 hours (`app.jwt.expiration-ms=86400000`)
- To test: manually delete `edu_token` from sessionStorage mid-session
- Next API call should redirect to `/login.html`

---

## 6. Content Hierarchy Testing

The hierarchy is: **Category → Subject → Topic → Question**

### 6.1 Admin Creates Category (auto-APPROVED)

1. Log in as Admin → go to `http://localhost:8080/admin/courses.html`
2. Click **+ Add Category**
3. Fill in: Name = `Year 10`, Description = `Year 10 curriculum`
4. Save

**Expected:** Category appears in the left tree with status **APPROVED** (no pending badge)

### 6.2 Teacher Creates Category (starts DRAFT)

1. Log in as Teacher → go to `http://localhost:8080/teacher/courses.html`
2. Click **+ Add Category**
3. Fill in: Name = `Year 11`, Description = `Year 11 curriculum`
4. Save

**Expected:** Category appears with status **DRAFT**

### 6.3 Teacher Submits for Approval

1. Select the `Year 11` category just created
2. Click **Submit for Approval**

**Expected:** Status changes to **PENDING**

### 6.4 Admin Approves

1. Log in as Admin → Admin Dashboard shows pending approval count
2. Go to **Courses** page, find `Year 11` with PENDING badge
3. Click **Approve**

**Expected:** Status becomes **APPROVED**, visible to all users

### 6.5 Admin Rejects with Reason

1. As Admin, find another PENDING item
2. Click **Reject**
3. Enter rejection reason: `Name too generic, please be more specific`
4. Confirm

**Expected:** Status becomes **REJECTED**, reason stored

### 6.6 Teacher Sees Rejection Reason

1. As Teacher, open the rejected item
2. Should see a red banner with the rejection reason text

**Expected:** Rejection reason is visible in the detail panel

### 6.7 Create Full Hierarchy (Admin — all auto-approved)

Follow this sequence to set up content for later exam testing:

```
Category:  "Year 10"
  Subject:   "English Language"
    Topic:     "Non-Fiction Writing"
      Question:  (see 6.8)
    Topic:     "Creative Writing"
  Subject:   "Mathematics"
    Topic:     "Algebra"
      Question:  (see 6.8)
```

### 6.8 Create Questions (per Topic)

For each topic, add at least 3 questions of different types:

**MCQ Single (for "Non-Fiction Writing"):**
- Type: `MCQ_SINGLE`
- Question text: `Which technique is used to persuade the reader?`
- Options:
  - A: `Alliteration` — ✗
  - B: `Rhetorical question` — ✓ (mark as correct)
  - C: `Onomatopoeia` — ✗
  - D: `Hyperbole` — ✗
- Marks: 2

**True/False:**
- Type: `TRUE_FALSE`
- Question: `A formal letter should use contractions.`
- Correct answer: `False`
- Marks: 1

**Short Answer:**
- Type: `SHORT_ANSWER`
- Question: `Define the term 'bias' in a news article.`
- Marks: 3, Word limit: 50

**Expected after each save:** Question appears in topic's question list

### 6.9 Soft Delete Test

1. As Admin, select any Category and click **Delete**
2. Confirm deletion

**Expected:** Item disappears from the UI  
**DB check:** `SELECT * FROM categories WHERE is_deleted = true;` — row still exists with `deleted_at` set

---

## 7. Approval Workflow Testing

This is a full end-to-end pass through the approval state machine.

| # | Action | Actor | Resulting Status |
|---|--------|-------|-----------------|
| 1 | Create content | Teacher | DRAFT |
| 2 | Submit | Teacher | PENDING |
| 3 | Reject with reason | Admin | REJECTED |
| 4 | Edit rejected item | Teacher | DRAFT (auto) |
| 5 | Submit again | Teacher | PENDING |
| 6 | Approve | Admin | APPROVED |
| 7 | Edit approved item | Teacher | DRAFT (auto) |
| 8 | Submit | Teacher | PENDING |
| 9 | Approve | Admin | APPROVED |

**Verify each state change** by refreshing the page and checking the badge/status label.

**Admin read-only enforcement:**

1. As Teacher, select a Category created by Admin
2. Try to click **Edit** or **Delete**

**Expected:** Buttons are absent or disabled for admin-owned content

---

## 8. Phase 1 — Org Settings & Notifications

### 8.1 Organisation Settings (Admin)

1. Log in as Admin → Admin Dashboard
2. Look for **Organisation Settings** link in sidebar or user menu
3. Test each section:

**Branding:**
- School Name: `SkyView Academy`
- Tagline: `Learning Without Limits`
- Primary Color: `#0e7490`
- Save → success toast

**Legal:**
- Registration Number: `SCH-2024-001`
- Country: `United Kingdom`
- Save

**Address:**
- Fill all fields, Save

**Academic:**
- Academic Year: `2024-2025`
- Term Structure: `Three Terms`
- Save

**Expected after each save:** `PUT /api/v1/org/settings/{section}` returns 200, success toast shown

### 8.2 Logo Upload

1. In Branding section, click **Upload Logo**
2. Choose a PNG file < 2 MB

**Expected:** Logo preview updates, file stored in `uploads/org/`

### 8.3 Notifications Bell

1. Log in as Admin
2. Approve or reject a teacher's submission (this triggers a notification for the teacher)
3. Log in as Teacher → check notification bell in top bar

**Expected:** Badge shows unread count, clicking bell shows the notification

### 8.4 Mark All Read

1. As Teacher with unread notifications
2. Click **Mark All Read**

**Expected:** Badge disappears, all items shown as read

---

## 9. Phase 2 — Teacher Workspace

### 9.1 View Own Profile

1. Log in as Teacher
2. Click your name in top-right → **My Profile**

**Expected:** Profile page loads showing name, email, role

### 9.2 Update Profile

1. Change **Display Name** to `Mr. Demo Teacher`
2. Change **Bio** to `Teaching English since 2010`
3. Save

**Expected:** Success toast, name updates in top bar

### 9.3 Upload Avatar

1. On profile page, click avatar area
2. Upload a JPEG/PNG < 2 MB

**Expected:** Avatar preview updates

### 9.4 Change Password

1. Click **Change Password** tab/button
2. Enter current password: `Teacher@123`
3. New password: `Teacher@456`
4. Confirm new password: `Teacher@456`
5. Save

**Expected:** Success toast, logout and log back in with new password `Teacher@456`

**Negative test:**
- Enter wrong current password → should show "Current password is incorrect"
- Enter mismatched new passwords → should show validation error

### 9.5 Submission History

1. As Teacher, go to `http://localhost:8080/teacher/submissions.html`
2. Filter by status **PENDING**

**Expected:** All PENDING items you submitted appear in the table

3. Filter by status **APPROVED**

**Expected:** Only approved items appear

4. Use search box to search by name

**Expected:** Results filter in real time

### 9.6 Submit Button from Course Tree

1. As Teacher, go to Courses
2. Select a DRAFT topic
3. Click **Submit for Approval**

**Expected:** Status changes to PENDING immediately in the UI

---

## 10. Phase 3 — Exam Builder (Admin)

### 10.1 Create an Exam (Admin)

1. Log in as Admin → go to `http://localhost:8080/admin/exams.html`
2. Click **+ New Exam**
3. Fill in:
   - Title: `Year 10 English Mid-Term`
   - Description: `Mid-term assessment covering Non-Fiction Writing`
   - Duration: `60` minutes
   - Total Marks: `20`
   - Pass Mark: `12`
4. Save

**Expected:** Exam appears in the list with status **APPROVED** (admin-created)

### 10.2 Add Questions to Exam

1. Open the exam just created
2. Click **Add Questions**
3. Browse the question picker: navigate to `Year 10 → English Language → Non-Fiction Writing`
4. Select 3–5 questions
5. Drag to reorder (if supported)
6. Click **Save Questions**

**Expected:** Questions appear in the exam's question list with order preserved

### 10.3 Teacher Creates Exam

1. Log in as Teacher → go to `http://localhost:8080/teacher/exams.html`
2. Click **+ New Exam**
3. Fill in same fields as above but title: `Year 10 Algebra Quiz`
4. Add questions from `Year 10 → Mathematics → Algebra`
5. Save

**Expected:** Exam created with status **DRAFT**

### 10.4 Teacher Submits Exam for Approval

1. Select the exam just created
2. Click **Submit for Approval**

**Expected:** Status → **PENDING**

### 10.5 Admin Approves Exam

1. Log in as Admin → Exams page
2. Find pending exam, click **Approve**

**Expected:** Status → **APPROVED**, now schedulable

---

## 11. Phase 3 — Classroom & Schedule Management

### 11.1 Create a Classroom (Admin)

1. Log in as Admin → go to `http://localhost:8080/admin/schedules.html`
2. Click **+ New Class**
3. Fill in:
   - Class Name: `10A`
   - Description: `Year 10 Group A`
4. Save

**Expected:** Class appears in the class list

### 11.2 Create Another Class

- Repeat for `10B`

### 11.3 Schedule an Exam

1. Click **+ New Schedule**
2. Fill in:
   - Exam: select `Year 10 English Mid-Term`
   - Class: select `10A`
   - Start Date/Time: a future date (e.g., tomorrow 09:00)
   - End Date/Time: +60 minutes from start
3. Save

**Expected:** Schedule appears in the schedule table with status **SCHEDULED**

### 11.4 Teacher Views Schedules

1. Log in as Teacher → go to `http://localhost:8080/teacher/schedules.html`

**Expected:** Teacher can see schedules for exams they created or are assigned to

---

## 12. Phase 3 — Student Management

### 12.1 Create a Student (Admin)

1. Log in as Admin → go to `http://localhost:8080/admin/students.html`
2. Click **+ Add Student**
3. Fill in:
   - Full Name: `Alice Smith`
   - Email: `alice@school.com`
   - Temporary Password: `Temp@1234`
4. Save

**Expected:** Student appears in list, `must_change_password = true` in DB

**DB verify:**
```sql
SELECT full_name, email, role, must_change_password 
FROM users WHERE email = 'alice@school.com';
```

### 12.2 Assign Student to Class

1. Click the **Alice Smith** row → **Assign to Class**
2. Select `10A`
3. Save

**Expected:** Alice now appears under class `10A`

### 12.3 Create a Second Student

- Full Name: `Bob Jones`, Email: `bob@school.com`, Password: `Temp@1234`
- Assign to `10A`

### 12.4 Bulk Import via CSV

Create a file `students.csv`:

```csv
full_name,email,password
Carol White,carol@school.com,Temp@1234
David Brown,david@school.com,Temp@1234
```

1. Click **Bulk Import**
2. Upload `students.csv`
3. Confirm import

**Expected:** 2 new students created, shown in the list

---

## 13. Student Portal — First Login & Password Change

### 13.1 Student Login

1. Go to `http://localhost:8080/login.html`
2. Click the **Student** tab (teal, third tab)
3. Enter `alice@school.com` / `Temp@1234`
4. Click **Sign In**

**Expected:** Because `must_change_password = true`, redirected to `/student/change-password.html`

### 13.2 Forced Password Change

On the change-password page:

1. Enter current password: `Temp@1234`
2. Enter new password: `Alice@Secure1`
3. Confirm: `Alice@Secure1`
4. Observe password strength meter (should show "Strong")
5. Click **Change Password**

**Expected:** Success → redirected to `/student/dashboard.html`

**DB verify:**
```sql
SELECT must_change_password FROM users WHERE email = 'alice@school.com';
-- Should be: false
```

### 13.3 Subsequent Login (no force-change)

1. Logout
2. Log back in as `alice@school.com` / `Alice@Secure1`

**Expected:** Goes straight to `/student/dashboard.html` — no password change screen

### 13.4 Negative: Wrong Current Password

1. On change-password page, enter wrong current password
2. Click Change

**Expected:** Error message "Current password is incorrect"

### 13.5 Student Cannot Access Admin/Teacher Routes

1. Log in as Student (Alice)
2. Navigate manually to `http://localhost:8080/admin/dashboard.html`

**Expected:** Redirected to `/login.html`

---

## 14. Student Portal — Dashboard & Exams

### 14.1 Student Dashboard

1. Log in as Alice
2. View `/student/dashboard.html`

**Expected:**
- Teal navigation bar with "SkyViewEduLearn" branding
- 4 stat cards: Total Exams, Completed, Average Score, Upcoming
- Upcoming exam cards (if schedule overlaps current time + 7 days)
- Recent results section (empty if no attempts yet)

### 14.2 My Exams Page

1. Click **My Exams** in nav
2. Goes to `/student/exams.html`

**Expected:** Three tabs — **Active | Upcoming | Completed**

- **Active tab:** Shows exams currently within their schedule window
- **Upcoming tab:** Exams scheduled in the future
- **Completed tab:** Empty until Alice has submitted an attempt

### 14.3 Active Exam Card

If you scheduled `Year 10 English Mid-Term` for `10A` and Alice is in `10A`, and the schedule window is now active:

1. Click **Active** tab
2. See the exam card with duration, question count, pass mark
3. Click **Start Exam**

---

## 15. Phase 3 — Exam Attempt (Student)

### 15.1 Start Exam

1. On the Active Exams tab, click **Start Exam** on `Year 10 English Mid-Term`

**Expected:**
- Redirected to `/student/exam.html?attemptId=xxx`
- Timer starts counting down (60:00)
- Question navigator panel on the right shows Q1…QN buttons
- Q1 is displayed

### 15.2 Answer Questions

**MCQ Single:**
1. Click option B (Rhetorical question)

**Expected:** Option highlights, navigator button turns blue

**True/False:**
1. Click **False**

**Short Answer:**
1. Type a short response in the text area

**Navigate questions:**
1. Click **Next** or click a question number in the navigator

### 15.3 Auto-Save

- The page auto-saves every 30 seconds
- Simulate: Answer Q1, wait 30 seconds
- **Expected:** No spinner freeze, answers are preserved (refresh the page to verify — attempt data should still be there)

### 15.4 Submit Exam

1. Answer all questions
2. Click **Submit Exam**
3. Confirm in the modal

**Expected:**
- Auto-marking runs immediately for MCQ/TF questions
- Redirected to result screen (or a "submitted" confirmation)
- Score shown for auto-markable questions

**DB verify:**
```sql
SELECT status, total_score, max_score 
FROM exam_attempts 
WHERE student_id = (SELECT id FROM users WHERE email = 'alice@school.com');
```

### 15.5 Timer Expiry

1. Start a second attempt (if allowed) or test in isolation
2. Wait for timer OR manually set a past end-time in DB for a test schedule
3. When timer hits 00:00

**Expected:** Exam auto-submits with current answers

---

## 16. Phase 3 — Marking (Teacher / Admin)

Short Answer and Essay answers require manual marking.

### 16.1 View Marking Queue

1. Log in as Teacher → go to `http://localhost:8080/teacher/marking.html`

**Expected:** List of attempts with ungraded answers

### 16.2 Open Attempt for Marking

1. Click an attempt in the queue
2. See each question with the student's answer

**For Short Answer questions:**
- Question, student's answer, and a marks input field are shown
- Enter marks: e.g., `2` out of 3
- Enter feedback: `Good definition but missing the source bias angle`
- Click **Save Mark**

**Expected:** Question marked, moves to the next ungraded answer

### 16.3 Finalise Marking

1. After marking all manual questions, click **Finalise**

**Expected:**
- Attempt status → `MARKED`
- Final score computed: auto-mark score + manual marks
- Student can now see full result on their dashboard

### 16.4 View Result as Student

1. Log in as Alice
2. Go to Dashboard → Recent Results OR My Exams → Completed tab
3. Click the exam to see result detail

**Expected:**
- Total score, pass/fail status
- Per-question breakdown with feedback from teacher

---

## 17. API Quick-Reference (cURL / Postman)

Use these to test the backend directly without the UI.

### Get JWT Token

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@localhost","password":"Admin@123"}' | jq .
```

**Expected response:**
```json
{
  "status": "success",
  "data": {
    "token": "eyJhbGci...",
    "role": "ADMIN",
    "name": "Admin User"
  }
}
```

Save the token:
```bash
TOKEN="eyJhbGci..."
```

### Auth: Who Am I

```bash
curl -s http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer $TOKEN" | jq .
```

### Create a Category

```bash
curl -s -X POST http://localhost:8080/api/v1/categories \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Year 10","description":"Year 10 curriculum"}' | jq .
```

### List Categories

```bash
curl -s http://localhost:8080/api/v1/categories \
  -H "Authorization: Bearer $TOKEN" | jq .
```

### Get Approval Queue

```bash
curl -s "http://localhost:8080/api/v1/approvals/pending?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

### Create Student

```bash
curl -s -X POST http://localhost:8080/api/v1/students \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Alice Smith",
    "email": "alice@school.com",
    "password": "Temp@1234"
  }' | jq .
```

### List Exams

```bash
curl -s http://localhost:8080/api/v1/exams \
  -H "Authorization: Bearer $TOKEN" | jq .
```

### Start Exam Attempt (Student Token)

```bash
STUDENT_TOKEN="..."  # get from student login

curl -s -X POST http://localhost:8080/api/v1/student/attempts \
  -H "Authorization: Bearer $STUDENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"scheduleId": "YOUR-SCHEDULE-UUID"}' | jq .
```

### Submit Answers

```bash
curl -s -X PUT http://localhost:8080/api/v1/student/attempts/ATTEMPT_ID/answers \
  -H "Authorization: Bearer $STUDENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "answers": [
      {"questionId": "Q1-UUID", "selectedOptions": ["OPTION-UUID"]},
      {"questionId": "Q2-UUID", "trueFalseAnswer": false},
      {"questionId": "Q3-UUID", "textAnswer": "Bias means..."}
    ]
  }' | jq .
```

### Submit Exam

```bash
curl -s -X POST http://localhost:8080/api/v1/student/attempts/ATTEMPT_ID/submit \
  -H "Authorization: Bearer $STUDENT_TOKEN" | jq .
```

### Org Settings — Update Branding (Admin)

```bash
curl -s -X PUT http://localhost:8080/api/v1/org/settings/branding \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "schoolName": "SkyView Academy",
    "tagline": "Learning Without Limits",
    "primaryColor": "#0e7490"
  }' | jq .
```

---

## 18. Common Errors & Fixes

### App won't start — "Connection refused" to PostgreSQL

- **Check:** PostgreSQL is running on port **5433** (not the default 5432)
- **Fix:** `application.properties` has `spring.datasource.url=jdbc:postgresql://localhost:5433/edulearn`
- Verify PostgreSQL port: `SHOW port;` in psql

### 401 Unauthorized on API calls

- Token expired or not sent
- Check sessionStorage in DevTools → Application → `edu_token` exists
- If missing: log in again

### 403 Forbidden

- Wrong role for that endpoint
- E.g., teacher trying to call `/api/v1/approvals/**` (ADMIN only)

### 404 on static pages

- Ensure `spring.web.resources.add-mappings=true` is in `application.properties`
- Check the HTML file exists at `src/main/resources/static/<path>`

### "must_change_password" not redirecting

- Verify `sql/09-student-portal-additions.sql` was run: `\d users` should show `must_change_password boolean`
- Verify `AuthService` returns `mustChangePassword: true` in login response
- Check browser console for JS errors on login.html

### Student can't see scheduled exam

- Check `exam_schedules` table: `status = 'SCHEDULED'`, date range covers now
- Check `student_classes` table: student is linked to the class on the schedule
- Check exam status is `APPROVED`

### Auto-marking didn't run

- Only triggers on `POST /api/v1/student/attempts/{id}/submit`
- MCQ_SINGLE and TRUE_FALSE are auto-marked
- SHORT_ANSWER, ESSAY need manual marking via `/teacher/marking.html`

### Soft-delete item still appearing

- Check `@Where(clause = "is_deleted = false")` annotation on the entity
- Restart app if entity was compiled before annotation was added

### Password change fails with 400

- Verify request body has all three fields: `currentPassword`, `newPassword`, `confirmPassword`
- New password must differ from current

---

## Test Completion Checklist

Use this as a sign-off checklist before moving to Phase 4:

### Authentication
- [ ] Admin login → redirects to admin dashboard
- [ ] Teacher login → redirects to teacher dashboard
- [ ] Student login → redirects to student dashboard (or change-password)
- [ ] Wrong password → error shown, no redirect
- [ ] Teacher accessing `/admin/` → redirected to login
- [ ] Logout clears sessionStorage

### Content & Approval
- [ ] Admin creates Category → auto-APPROVED
- [ ] Teacher creates Category → DRAFT
- [ ] Teacher submits → PENDING
- [ ] Admin approves → APPROVED
- [ ] Admin rejects with reason → REJECTED
- [ ] Teacher sees rejection reason
- [ ] Teacher edits REJECTED item → back to DRAFT
- [ ] Soft delete removes from UI, DB row has `is_deleted=true`
- [ ] Admin-owned items not editable by teacher

### Organisation Settings
- [ ] Admin updates branding, legal, address, academic sections
- [ ] Logo upload succeeds
- [ ] Notifications appear for teacher after admin action

### Teacher Workspace
- [ ] Profile update saves correctly
- [ ] Password change works; wrong current password rejected
- [ ] Submission history filters by status

### Phase 3 — Exam Engine
- [ ] Admin creates exam → APPROVED
- [ ] Teacher creates exam → DRAFT → submit → PENDING → Admin approves → APPROVED
- [ ] Questions added to exam in order
- [ ] Classroom created
- [ ] Schedule created linking exam to class
- [ ] Student created with temp password
- [ ] Student assigned to class

### Student Portal
- [ ] First login with temp password → forced to change-password page
- [ ] Password changed → `must_change_password = false` in DB
- [ ] Second login goes directly to dashboard
- [ ] Stat cards, upcoming exams, recent results visible on dashboard
- [ ] My Exams tabs: Active / Upcoming / Completed

### Exam Attempt
- [ ] Student can start exam (Active tab, within schedule window)
- [ ] Timer counts down
- [ ] Answers saved (auto-save every 30 seconds)
- [ ] Exam submits successfully
- [ ] MCQ/TF answers auto-marked
- [ ] Attempt appears in Completed tab

### Marking
- [ ] Short Answer / Essay appear in marking queue
- [ ] Teacher awards marks and feedback
- [ ] Finalise computes total score
- [ ] Student can view final result
