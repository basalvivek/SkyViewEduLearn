# EduLearn Platform — Phase 3: Assessment Engine

## Modules Covered

| # | Module | Description |
|---|--------|-------------|
| 7 | Exam Builder | Assemble questions into a timed exam paper |
| 8 | Exam Scheduling | Set date, time window, target class / year group |
| 9 | Student Registration | Student accounts, linked to year group and subjects |
| 10 | Exam Attempt | Student takes exam — timer, navigation, auto-save |
| 11 | Auto Marking | MCQ + True/False marked instantly; Essay/Short flagged for manual |
| 12 | Manual Marking | Teacher reviews essay/short answers and assigns marks |

---

## Module 7 — Exam Builder

### Overview
Admin or Teacher assembles a set of approved questions into a named exam paper.
Questions can be picked from the content tree by topic, subject, or type.
Teachers must submit exam papers for admin approval before they can be scheduled.

---

### 7.1 UI — Exam Builder Page

**Route:** `admin/exams.html` and `teacher/exams.html`
**Nav item:** "Exams" under Main section in both sidebars

#### Layout — Three-column

```
┌──────────┬──────────────────────┬───────────────────────────┐
│ NAV      │ QUESTION PICKER       │ EXAM PAPER BUILDER        │
│          │                       │                           │
│          │ Filter by:            │  Exam name: ___________   │
│          │ [Subject ▾][Topic ▾]  │  Total marks: 60  Time:   │
│          │ [Type ▾][Difficulty▾] │  [+] Auto-calculate       │
│          │                       │                           │
│          │ ┌───────────────────┐ │  1. [Question text...]    │
│          │ │ Q1 MCQ  3 marks   │ │     MCQ · 3 marks [✕]    │
│          │ │ [Add →]           │ │                           │
│          │ ├───────────────────┤ │  2. [Question text...]    │
│          │ │ Q2 Essay 5 marks  │ │     Essay · 5 marks [✕]  │
│          │ │ [Add →]           │ │                           │
│          │ └───────────────────┘ │  [↑][↓] Reorder           │
│          │ Page 1/5  [< >]       │                           │
│          │                       │  [Save draft][Submit]     │
└──────────┴──────────────────────┴───────────────────────────┘
```

#### Question Picker (left panel)
- Filter bar: Subject dropdown, Topic dropdown, Question Type multi-select, Difficulty select
- Only shows **APPROVED** questions
- Each question card shows: truncated text, type badge, marks, "Add →" button
- Pagination: 10 per page

#### Exam Paper Builder (right panel)
| Field | Type | Required | Notes |
|-------|------|----------|-------|
| Exam name | Text | Yes | e.g. "Year 10 English — Mock Paper 1" |
| Description | Textarea | No | Instructions shown to student |
| Time limit | Number (minutes) | Yes | e.g. 60 |
| Total marks | Number | Auto | Calculated from questions, overridable |
| Pass mark | Number | No | e.g. 40 (out of 60) |
| Shuffle questions | Toggle | No | Randomise order per student attempt |
| Shuffle options | Toggle | No | Randomise MCQ option order |

- Questions listed in order with drag-handle (↑↓ buttons)
- Each row: question text preview · type badge · marks · remove [✕]
- Total marks updates live as questions are added/removed
- Save as draft → status `DRAFT`
- Submit → status `PENDING` (teacher) or `APPROVED` (admin)

---

### 7.2 Database — Exam Tables

```sql
CREATE TYPE exam_status AS ENUM ('DRAFT','PENDING','APPROVED','REJECTED','ARCHIVED');

CREATE TABLE exams (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name             VARCHAR(200) NOT NULL,
    description      TEXT,
    time_limit_mins  SMALLINT    NOT NULL DEFAULT 60 CHECK (time_limit_mins > 0),
    total_marks      SMALLINT    NOT NULL DEFAULT 0,
    pass_mark        SMALLINT,
    shuffle_questions BOOLEAN    NOT NULL DEFAULT FALSE,
    shuffle_options   BOOLEAN    NOT NULL DEFAULT FALSE,
    status           exam_status NOT NULL DEFAULT 'DRAFT',
    rejection_reason TEXT,
    created_by       UUID        NOT NULL REFERENCES users(id),
    updated_by       UUID        REFERENCES users(id),
    approved_by      UUID        REFERENCES users(id),
    approved_at      TIMESTAMPTZ,
    is_deleted       BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE exam_questions (
    id           UUID     PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id      UUID     NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    question_id  UUID     NOT NULL REFERENCES questions(id),
    display_order SMALLINT NOT NULL DEFAULT 0,
    marks_override SMALLINT,        -- optional override of question's default marks
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (exam_id, question_id)
);

CREATE INDEX idx_exam_status     ON exams(status)     WHERE is_deleted = FALSE;
CREATE INDEX idx_exam_created_by ON exams(created_by) WHERE is_deleted = FALSE;
CREATE INDEX idx_exam_q_exam_id  ON exam_questions(exam_id);

CREATE TRIGGER trg_exams_upd
    BEFORE UPDATE ON exams FOR EACH ROW EXECUTE FUNCTION set_updated_at();
```

---

### 7.3 API Endpoints — Exam Builder

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET    | `/api/v1/exams` | Admin/Teacher | List exams (admin sees all, teacher sees own) |
| POST   | `/api/v1/exams` | Admin/Teacher | Create exam |
| GET    | `/api/v1/exams/{id}` | Admin/Teacher | Get exam with questions |
| PUT    | `/api/v1/exams/{id}` | Owner/Admin | Update exam metadata |
| DELETE | `/api/v1/exams/{id}` | Admin | Soft delete |
| PUT    | `/api/v1/exams/{id}/questions` | Owner/Admin | Replace full question list (ordered array of question IDs) |
| PUT    | `/api/v1/exams/{id}/submit` | Teacher (owner) | DRAFT/REJECTED → PENDING |
| PUT    | `/api/v1/exams/{id}/approve` | Admin | PENDING → APPROVED |
| PUT    | `/api/v1/exams/{id}/reject` | Admin | PENDING → REJECTED with reason |
| GET    | `/api/v1/questions?subjectId=&topicId=&type=&difficulty=&page=0&size=10` | Admin/Teacher | Question picker feed (APPROVED only) |

**POST /exams request:**
```json
{
  "name": "Year 10 English — Mock Paper 1",
  "description": "Answer all questions. You have 60 minutes.",
  "timeLimitMins": 60,
  "passMarkOverride": 40,
  "shuffleQuestions": false,
  "shuffleOptions": true,
  "questionIds": ["uuid1", "uuid2", "uuid3"]
}
```

---

### 7.4 Spring Boot Classes — Exam Builder

```
entity/      Exam.java, ExamQuestion.java
enums/       ExamStatus.java
repository/  ExamRepository.java, ExamQuestionRepository.java
service/     ExamService.java
controller/  ExamController.java
dto/request/ ExamRequest.java, ExamQuestionsRequest.java
dto/response/ExamResponse.java, ExamSummaryResponse.java
```

---

## Module 8 — Exam Scheduling

### Overview
Admin or Teacher (with approval) schedules an approved exam for a specific class/year group
within a defined date and time window.

---

### 8.1 UI — Exam Scheduling Page

**Accessible from:** Exams list → "Schedule" button on an APPROVED exam

#### Schedule Form Fields

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| Exam | Read-only | — | Pre-selected from exam list |
| Class / Year group | Multi-select | Yes | e.g. Year 10A, Year 10B |
| Start date & time | DateTime | Yes | |
| End date & time | DateTime | Yes | Must be > start + time_limit_mins |
| Access window | Duration | Auto | end − start (students can start any time in window) |
| Max attempts | Number | No | Default 1 |
| Show results immediately | Toggle | No | Auto-release marks on submit |
| Instructions | Textarea | No | Shown on exam start screen |

**Upcoming schedules list:**
- Table: Exam name · Class · Start · End · Status (Upcoming/Active/Closed) · Students attempted
- Status badge: Upcoming (blue) / Active (green pulsing) / Closed (grey)

---

### 8.2 Database — Scheduling Tables

```sql
CREATE TYPE schedule_status AS ENUM ('UPCOMING','ACTIVE','CLOSED','CANCELLED');

CREATE TABLE classes (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,        -- e.g. "Year 10A"
    year_group  VARCHAR(20),                  -- e.g. "Year 10"
    is_active   BOOLEAN     NOT NULL DEFAULT TRUE,
    created_by  UUID        NOT NULL REFERENCES users(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE exam_schedules (
    id                   UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id              UUID            NOT NULL REFERENCES exams(id),
    class_id             UUID            NOT NULL REFERENCES classes(id),
    start_at             TIMESTAMPTZ     NOT NULL,
    end_at               TIMESTAMPTZ     NOT NULL,
    max_attempts         SMALLINT        NOT NULL DEFAULT 1,
    show_results_immediately BOOLEAN     NOT NULL DEFAULT FALSE,
    instructions         TEXT,
    status               schedule_status NOT NULL DEFAULT 'UPCOMING',
    created_by           UUID            NOT NULL REFERENCES users(id),
    updated_by           UUID            REFERENCES users(id),
    is_deleted           BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at           TIMESTAMPTZ,
    created_at           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_schedule_dates CHECK (end_at > start_at)
);

CREATE INDEX idx_sched_exam_id  ON exam_schedules(exam_id)  WHERE is_deleted = FALSE;
CREATE INDEX idx_sched_class_id ON exam_schedules(class_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_sched_status   ON exam_schedules(status)   WHERE is_deleted = FALSE;
CREATE INDEX idx_classes_year   ON classes(year_group)      WHERE is_active = TRUE;

CREATE TRIGGER trg_classes_upd  BEFORE UPDATE ON classes       FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_sched_upd    BEFORE UPDATE ON exam_schedules FOR EACH ROW EXECUTE FUNCTION set_updated_at();
```

---

### 8.3 API Endpoints — Scheduling

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET    | `/api/v1/classes` | Admin/Teacher | List all classes |
| POST   | `/api/v1/classes` | Admin | Create class |
| PUT    | `/api/v1/classes/{id}` | Admin | Update class |
| DELETE | `/api/v1/classes/{id}` | Admin | Soft delete class |
| GET    | `/api/v1/schedules` | Admin/Teacher | List schedules |
| POST   | `/api/v1/schedules` | Admin/Teacher | Create schedule |
| GET    | `/api/v1/schedules/{id}` | Admin/Teacher | Get schedule details |
| PUT    | `/api/v1/schedules/{id}` | Owner/Admin | Update schedule |
| DELETE | `/api/v1/schedules/{id}` | Admin | Cancel schedule |
| GET    | `/api/v1/schedules/{id}/attempts` | Admin/Teacher | Student attempt summary |

---

### 8.4 Spring Boot Classes — Scheduling

```
entity/      ExamSchedule.java, Class.java (rename to Classroom to avoid java.lang.Class clash)
enums/       ScheduleStatus.java
repository/  ExamScheduleRepository.java, ClassroomRepository.java
service/     ScheduleService.java, ClassroomService.java
controller/  ScheduleController.java, ClassroomController.java
dto/request/ ScheduleRequest.java, ClassroomRequest.java
dto/response/ScheduleResponse.java, ClassroomResponse.java
```

> ⚠️ **Important:** Name the entity `Classroom.java` (not `Class.java`) to avoid
> collision with `java.lang.Class`. Table name stays `classes`.

---

## Module 9 — Student Registration

### Overview
Admin creates student accounts and assigns them to classes.
Students log in via the same login page but are routed to a student-facing interface (future Phase 5).
In Phase 3, student accounts are created and managed by admin; students can log in to take exams.

---

### 9.1 UI — Student Management (Admin)

**Route:** page within `admin-dashboard.html` (page id: `page-students`)
**Nav item:** "Students" already in left sidebar

#### Student List Table

| Column | Notes |
|--------|-------|
| Name | Full name + avatar circle |
| Email | Student email |
| Class | Assigned class badge |
| Year group | e.g. Year 10 |
| Status | Active / Inactive |
| Joined | Date account created |
| Actions | Edit · Reset password · Deactivate |

**Add student form (modal or side panel):**

| Field | Type | Required |
|-------|------|----------|
| Full name | Text | Yes |
| Email | Email | Yes |
| Year group | Select | Yes |
| Class | Select (filtered by year) | Yes |
| Temporary password | Auto-generated | — |
| Send welcome email | Toggle | No |

**Bulk import:** CSV upload — columns: `full_name, email, year_group, class_name`

---

### 9.2 Database — Student Tables

Students reuse the existing `users` table with `role = 'STUDENT'`.
Add a join table to assign students to classes:

```sql
CREATE TABLE student_classes (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id  UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    class_id    UUID        NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    joined_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_active   BOOLEAN     NOT NULL DEFAULT TRUE,
    UNIQUE (student_id, class_id)
);

CREATE INDEX idx_sc_student_id ON student_classes(student_id) WHERE is_active = TRUE;
CREATE INDEX idx_sc_class_id   ON student_classes(class_id)   WHERE is_active = TRUE;
```

---

### 9.3 API Endpoints — Students

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET    | `/api/v1/students` | Admin | List students with filters |
| POST   | `/api/v1/students` | Admin | Create student account |
| GET    | `/api/v1/students/{id}` | Admin | Student profile |
| PUT    | `/api/v1/students/{id}` | Admin | Update student |
| PUT    | `/api/v1/students/{id}/deactivate` | Admin | Deactivate account |
| PUT    | `/api/v1/students/{id}/reset-password` | Admin | Generate temp password |
| POST   | `/api/v1/students/bulk-import` | Admin | CSV upload |
| GET    | `/api/v1/students/{id}/classes` | Admin | Student's class assignments |
| POST   | `/api/v1/students/{id}/classes` | Admin | Assign student to class |
| DELETE | `/api/v1/students/{id}/classes/{classId}` | Admin | Remove from class |

---

### 9.4 Spring Boot Classes — Students

```
controller/  StudentController.java
service/     StudentService.java
repository/  StudentClassRepository.java
             (UserRepository already exists — add student-specific query methods)
dto/request/ StudentCreateRequest.java
             StudentBulkImportRequest.java
dto/response/StudentResponse.java
             StudentSummaryResponse.java
```

**StudentService — bulk import:**
```java
public BulkImportResult importFromCsv(MultipartFile file) {
    // Parse CSV using OpenCSV or Apache Commons CSV
    // For each row: validate, create user with STUDENT role, assign to class
    // Return: { imported: 45, skipped: 2, errors: [{row: 3, reason: "duplicate email"}] }
}
```

Add to `pom.xml`:
```xml
<dependency>
  <groupId>com.opencsv</groupId>
  <artifactId>opencsv</artifactId>
  <version>5.9</version>
</dependency>
```

---

## Module 10 — Exam Attempt

### Overview
Student logs in, sees available exams, starts an attempt, navigates questions,
answers are auto-saved every 30 seconds, and submitted at end of time or manually.

---

### 10.1 UI — Student Exam Interface

**Routes:** `student/dashboard.html`, `student/exam.html`

#### Student Dashboard
- Lists scheduled exams available to the student's class
- Each exam card: name · subject · date/time · duration · status (Upcoming/Available/Completed)
- "Start exam" button only active during the schedule window

#### Exam Screen Layout

```
┌────────────────────────────────────────────────────────┐
│  Year 10 English Mock          ⏱ 45:23 remaining      │
│                                [Submit exam]           │
├───────────┬────────────────────────────────────────────┤
│ Questions │  Question 3 of 15                          │
│           │                                            │
│ 1  ✓      │  What literary device is used in line 4?  │
│ 2  ✓      │                                            │
│ 3  ●      │  ○ Metaphor                               │
│ 4         │  ● Simile                                  │
│ 5         │  ○ Alliteration                            │
│ ...       │  ○ Personification                         │
│           │                                            │
│ ✓ answered│  [← Previous]  [Save & Next →]            │
│ ● current │                                            │
└───────────┴────────────────────────────────────────────┘
```

**Exam screen features:**
- Countdown timer (top right) — turns red at 5 minutes remaining
- Question navigator panel (left) — shows answered (✓), current (●), skipped (○)
- Auto-save every 30 seconds to prevent data loss
- "Flag for review" toggle per question
- Confirmation modal before final submit
- On time expiry → auto-submit current answers

---

### 10.2 Database — Attempt Tables

```sql
CREATE TYPE attempt_status AS ENUM ('IN_PROGRESS','SUBMITTED','TIMED_OUT','ABANDONED');

CREATE TABLE exam_attempts (
    id              UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    schedule_id     UUID           NOT NULL REFERENCES exam_schedules(id),
    student_id      UUID           NOT NULL REFERENCES users(id),
    started_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    submitted_at    TIMESTAMPTZ,
    status          attempt_status NOT NULL DEFAULT 'IN_PROGRESS',
    total_marks     SMALLINT,      -- populated after marking
    marks_obtained  SMALLINT,      -- populated after marking
    percentage      NUMERIC(5,2),  -- populated after marking
    is_passed       BOOLEAN,       -- populated after marking
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    UNIQUE (schedule_id, student_id)   -- one attempt per student per schedule (unless max_attempts > 1)
);

CREATE TABLE attempt_answers (
    id                UUID     PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_id        UUID     NOT NULL REFERENCES exam_attempts(id) ON DELETE CASCADE,
    question_id       UUID     NOT NULL REFERENCES questions(id),
    exam_question_id  UUID     NOT NULL REFERENCES exam_questions(id),

    -- Answer storage per question type
    selected_option_ids UUID[],        -- MCQ_SINGLE / MCQ_MULTIPLE
    boolean_answer      BOOLEAN,       -- TRUE_FALSE
    text_answer         TEXT,          -- SHORT_ANSWER / ESSAY / CODE

    -- Marking
    marks_awarded       SMALLINT,
    is_correct          BOOLEAN,       -- for auto-marked types
    is_flagged          BOOLEAN    NOT NULL DEFAULT FALSE,
    marker_note         TEXT,          -- teacher's feedback on essay/short answer
    marked_by           UUID       REFERENCES users(id),
    marked_at           TIMESTAMPTZ,

    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (attempt_id, question_id)
);

CREATE INDEX idx_attempt_schedule ON exam_attempts(schedule_id);
CREATE INDEX idx_attempt_student  ON exam_attempts(student_id);
CREATE INDEX idx_attempt_status   ON exam_attempts(status);
CREATE INDEX idx_answer_attempt   ON attempt_answers(attempt_id);
CREATE INDEX idx_answer_marker    ON attempt_answers(marked_by) WHERE marked_by IS NOT NULL;

CREATE TRIGGER trg_attempts_upd BEFORE UPDATE ON exam_attempts  FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_answers_upd  BEFORE UPDATE ON attempt_answers FOR EACH ROW EXECUTE FUNCTION set_updated_at();
```

---

### 10.3 API Endpoints — Exam Attempt

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET    | `/api/v1/student/exams` | Student | Available exams for student's class |
| POST   | `/api/v1/student/attempts` | Student | Start an attempt `{ scheduleId }` |
| GET    | `/api/v1/student/attempts/{id}` | Student | Get attempt with questions (shuffled if set) |
| PUT    | `/api/v1/student/attempts/{id}/answers` | Student | Auto-save answers batch |
| POST   | `/api/v1/student/attempts/{id}/submit` | Student | Final submit |
| GET    | `/api/v1/student/attempts/{id}/result` | Student | View result (if show_results_immediately=true) |

**PUT /attempts/{id}/answers request (auto-save):**
```json
{
  "answers": [
    { "questionId": "uuid", "selectedOptionIds": ["uuid-option-2"] },
    { "questionId": "uuid", "booleanAnswer": true },
    { "questionId": "uuid", "textAnswer": "The author uses a simile here..." },
    { "questionId": "uuid", "isFlagged": true }
  ]
}
```

---

### 10.4 Spring Boot Classes — Attempt

```
entity/      ExamAttempt.java, AttemptAnswer.java
enums/       AttemptStatus.java
repository/  ExamAttemptRepository.java, AttemptAnswerRepository.java
service/     ExamAttemptService.java
controller/  StudentExamController.java
dto/request/ StartAttemptRequest.java, SaveAnswersRequest.java
dto/response/AttemptResponse.java, AttemptResultResponse.java
```

---

## Module 11 — Auto Marking

### Overview
On attempt submission, auto-marking runs immediately for:
`MCQ_SINGLE`, `MCQ_MULTIPLE`, `TRUE_FALSE`

`SHORT_ANSWER`, `ESSAY`, `CODE`, `IMAGE_BASED` are flagged for manual marking.

---

### 11.1 Auto-marking Logic

```java
@Service
public class AutoMarkingService {

    public void markAttempt(ExamAttempt attempt) {
        List<AttemptAnswer> answers = answerRepo.findByAttemptId(attempt.getId());
        int totalMarks = 0;
        int obtainedMarks = 0;

        for (AttemptAnswer answer : answers) {
            Question q = answer.getQuestion();
            int questionMarks = resolveMarks(answer); // exam_questions.marks_override ?? question.marks

            switch (q.getQuestionType()) {
                case MCQ_SINGLE -> {
                    boolean correct = answer.getSelectedOptionIds() != null
                        && answer.getSelectedOptionIds().size() == 1
                        && isCorrectOption(answer.getSelectedOptionIds().get(0), q);
                    answer.setIsCorrect(correct);
                    answer.setMarksAwarded(correct ? questionMarks : 0);
                }
                case MCQ_MULTIPLE -> {
                    Set<UUID> correctIds = getCorrectOptionIds(q);
                    Set<UUID> selected  = new HashSet<>(answer.getSelectedOptionIds());
                    boolean correct = correctIds.equals(selected);
                    answer.setIsCorrect(correct);
                    answer.setMarksAwarded(correct ? questionMarks : 0);
                }
                case TRUE_FALSE -> {
                    boolean correct = Objects.equals(answer.getBooleanAnswer(), q.getCorrectBoolean());
                    answer.setIsCorrect(correct);
                    answer.setMarksAwarded(correct ? questionMarks : 0);
                }
                // Needs manual marking — leave marks_awarded null
                case SHORT_ANSWER, ESSAY, CODE, IMAGE_BASED -> {
                    answer.setIsCorrect(null);
                    answer.setMarksAwarded(null);
                }
            }
            totalMarks += questionMarks;
            if (answer.getMarksAwarded() != null) obtainedMarks += answer.getMarksAwarded();
        }

        answerRepo.saveAll(answers);

        // Update attempt totals (partial — manual questions excluded)
        attempt.setTotalMarks(totalMarks);
        attempt.setMarksObtained(obtainedMarks);
        // Percentage and pass status finalised after manual marking completes
        attemptRepo.save(attempt);
    }
}
```

---

### 11.2 API Endpoints — Auto Marking

Auto marking triggers internally on `POST /student/attempts/{id}/submit`.
No separate endpoint needed. Results available immediately via:

```
GET /api/v1/student/attempts/{id}/result
```

Response includes `autoMarkedCount`, `pendingManualCount`, and per-question `isCorrect`.

---

## Module 12 — Manual Marking

### Overview
Teacher reviews `SHORT_ANSWER`, `ESSAY`, `CODE`, and `IMAGE_BASED` responses.
Each response is shown with the question, model answer/marking scheme, and a marks input.

---

### 12.1 UI — Manual Marking Page

**Route:** page within `teacher-dashboard.html` (page id: `page-marking`)
**Nav item:** "Marking" in left sidebar (badge showing count pending)

#### Marking Queue — List view

Table of attempts needing manual marks:

| Column | Notes |
|--------|-------|
| Student | Name + class |
| Exam | Exam name |
| Submitted | Date/time |
| Auto-marked | e.g. 10/12 questions done |
| Pending | e.g. 2 essay answers to mark |
| Status | Not started / In progress / Complete |
| Action | Mark now → |

#### Marking Screen — Per student per exam

```
┌─────────────────────────────────────────────────────────┐
│  Marking: Sarah Jones — Year 10 English Mock            │
│  Auto-marked: 38/60 marks  Manual: 2 questions left     │
├─────────────────────────────────────────────────────────┤
│  Question 8 of 15 · Essay · Max 10 marks                │
│  ─────────────────────────────────────────────────────  │
│  Q: Analyse the writer's use of language in lines 1-6.  │
│                                                         │
│  Student's answer:                                      │
│  ┌─────────────────────────────────────────────────┐   │
│  │ The writer uses a range of techniques including │   │
│  │ metaphor and personification to create...       │   │
│  └─────────────────────────────────────────────────┘   │
│                                                         │
│  Model answer / marking scheme:                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │ Award marks for: identification of technique,   │   │
│  │ explanation, effect on reader (2+2+2 = 6 max)   │   │
│  └─────────────────────────────────────────────────┘   │
│                                                         │
│  Marks awarded:  [ 7 ] / 10                             │
│  Feedback note:  ________________________________       │
│                  Good analysis, needs more effect.      │
│                                                         │
│  [Save & next →]          [← Previous]                  │
└─────────────────────────────────────────────────────────┘
```

---

### 12.2 API Endpoints — Manual Marking

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET    | `/api/v1/marking/queue` | Teacher/Admin | Attempts pending manual marking |
| GET    | `/api/v1/marking/attempts/{id}` | Teacher/Admin | Full attempt with all answers |
| PUT    | `/api/v1/marking/attempts/{attemptId}/answers/{answerId}` | Teacher/Admin | Award marks + feedback |
| POST   | `/api/v1/marking/attempts/{id}/finalise` | Teacher/Admin | Mark attempt as fully marked, compute final % |

**PUT /marking/attempts/{aid}/answers/{qid} request:**
```json
{
  "marksAwarded": 7,
  "markerNote": "Good analysis, needs more discussion of effect on reader."
}
```

**POST /marking/attempts/{id}/finalise — logic:**
```
Sum all marks_awarded (auto + manual)
Compute percentage = marks_obtained / total_marks * 100
Set is_passed = percentage >= exam.pass_mark
Update attempt.status if not already SUBMITTED
Send result notification to student (future)
```

---

### 12.3 Spring Boot Classes — Manual Marking

```
controller/  MarkingController.java
service/     MarkingService.java
dto/request/ MarkAnswerRequest.java
dto/response/MarkingQueueResponse.java
             AttemptMarkingResponse.java
```

---

## Database Schema Additions — Phase 3

Save as `sql/04-phase3-schema-additions.sql`:

```sql
-- ── Phase 3 Enums ─────────────────────────────────────────────
CREATE TYPE exam_status     AS ENUM ('DRAFT','PENDING','APPROVED','REJECTED','ARCHIVED');
CREATE TYPE schedule_status AS ENUM ('UPCOMING','ACTIVE','CLOSED','CANCELLED');
CREATE TYPE attempt_status  AS ENUM ('IN_PROGRESS','SUBMITTED','TIMED_OUT','ABANDONED');

-- ── Module 7: Exams ───────────────────────────────────────────
CREATE TABLE exams (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name              VARCHAR(200) NOT NULL,
    description       TEXT,
    time_limit_mins   SMALLINT    NOT NULL DEFAULT 60,
    total_marks       SMALLINT    NOT NULL DEFAULT 0,
    pass_mark         SMALLINT,
    shuffle_questions BOOLEAN     NOT NULL DEFAULT FALSE,
    shuffle_options   BOOLEAN     NOT NULL DEFAULT FALSE,
    status            exam_status NOT NULL DEFAULT 'DRAFT',
    rejection_reason  TEXT,
    created_by        UUID        NOT NULL REFERENCES users(id),
    updated_by        UUID        REFERENCES users(id),
    approved_by       UUID        REFERENCES users(id),
    approved_at       TIMESTAMPTZ,
    is_deleted        BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at        TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE exam_questions (
    id              UUID     PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id         UUID     NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    question_id     UUID     NOT NULL REFERENCES questions(id),
    display_order   SMALLINT NOT NULL DEFAULT 0,
    marks_override  SMALLINT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (exam_id, question_id)
);
CREATE INDEX idx_exam_status     ON exams(status)       WHERE is_deleted = FALSE;
CREATE INDEX idx_exam_created_by ON exams(created_by)   WHERE is_deleted = FALSE;
CREATE INDEX idx_exam_q_exam     ON exam_questions(exam_id);
CREATE TRIGGER trg_exams_upd BEFORE UPDATE ON exams FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ── Module 8: Scheduling ──────────────────────────────────────
CREATE TABLE classes (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    year_group  VARCHAR(20),
    is_active   BOOLEAN     NOT NULL DEFAULT TRUE,
    created_by  UUID        NOT NULL REFERENCES users(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE exam_schedules (
    id                       UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id                  UUID            NOT NULL REFERENCES exams(id),
    class_id                 UUID            NOT NULL REFERENCES classes(id),
    start_at                 TIMESTAMPTZ     NOT NULL,
    end_at                   TIMESTAMPTZ     NOT NULL,
    max_attempts             SMALLINT        NOT NULL DEFAULT 1,
    show_results_immediately BOOLEAN         NOT NULL DEFAULT FALSE,
    instructions             TEXT,
    status                   schedule_status NOT NULL DEFAULT 'UPCOMING',
    created_by               UUID            NOT NULL REFERENCES users(id),
    updated_by               UUID            REFERENCES users(id),
    is_deleted               BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at               TIMESTAMPTZ,
    created_at               TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_dates CHECK (end_at > start_at)
);
CREATE INDEX idx_sched_exam   ON exam_schedules(exam_id)  WHERE is_deleted = FALSE;
CREATE INDEX idx_sched_class  ON exam_schedules(class_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_sched_status ON exam_schedules(status)   WHERE is_deleted = FALSE;
CREATE TRIGGER trg_classes_upd BEFORE UPDATE ON classes        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_sched_upd   BEFORE UPDATE ON exam_schedules FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ── Module 9: Students ────────────────────────────────────────
CREATE TABLE student_classes (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id  UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    class_id    UUID        NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    joined_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_active   BOOLEAN     NOT NULL DEFAULT TRUE,
    UNIQUE (student_id, class_id)
);
CREATE INDEX idx_sc_student ON student_classes(student_id) WHERE is_active = TRUE;
CREATE INDEX idx_sc_class   ON student_classes(class_id)   WHERE is_active = TRUE;

-- ── Modules 10-12: Attempts & Answers ────────────────────────
CREATE TABLE exam_attempts (
    id              UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    schedule_id     UUID           NOT NULL REFERENCES exam_schedules(id),
    student_id      UUID           NOT NULL REFERENCES users(id),
    started_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    submitted_at    TIMESTAMPTZ,
    status          attempt_status NOT NULL DEFAULT 'IN_PROGRESS',
    total_marks     SMALLINT,
    marks_obtained  SMALLINT,
    percentage      NUMERIC(5,2),
    is_passed       BOOLEAN,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    UNIQUE (schedule_id, student_id)
);
CREATE TABLE attempt_answers (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_id          UUID        NOT NULL REFERENCES exam_attempts(id) ON DELETE CASCADE,
    question_id         UUID        NOT NULL REFERENCES questions(id),
    exam_question_id    UUID        NOT NULL REFERENCES exam_questions(id),
    selected_option_ids UUID[],
    boolean_answer      BOOLEAN,
    text_answer         TEXT,
    marks_awarded       SMALLINT,
    is_correct          BOOLEAN,
    is_flagged          BOOLEAN     NOT NULL DEFAULT FALSE,
    marker_note         TEXT,
    marked_by           UUID        REFERENCES users(id),
    marked_at           TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (attempt_id, question_id)
);
CREATE INDEX idx_attempt_sched  ON exam_attempts(schedule_id);
CREATE INDEX idx_attempt_student ON exam_attempts(student_id);
CREATE INDEX idx_attempt_status  ON exam_attempts(status);
CREATE INDEX idx_answer_attempt  ON attempt_answers(attempt_id);
CREATE INDEX idx_answer_marker   ON attempt_answers(marked_by) WHERE marked_by IS NOT NULL;
CREATE TRIGGER trg_attempts_upd BEFORE UPDATE ON exam_attempts   FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_answers_upd  BEFORE UPDATE ON attempt_answers FOR EACH ROW EXECUTE FUNCTION set_updated_at();
```

---

## Package Structure Additions — Phase 3

```
com.edulearn
├── controller/
│   ├── ExamController.java            # Module 7
│   ├── ClassroomController.java       # Module 8
│   ├── ScheduleController.java        # Module 8
│   ├── StudentController.java         # Module 9
│   ├── StudentExamController.java     # Module 10
│   └── MarkingController.java         # Modules 11 & 12
├── service/
│   ├── ExamService.java
│   ├── ClassroomService.java
│   ├── ScheduleService.java
│   ├── StudentService.java
│   ├── ExamAttemptService.java
│   ├── AutoMarkingService.java        # runs on submit, no controller
│   └── MarkingService.java
├── repository/
│   ├── ExamRepository.java
│   ├── ExamQuestionRepository.java
│   ├── ClassroomRepository.java
│   ├── ExamScheduleRepository.java
│   ├── StudentClassRepository.java
│   ├── ExamAttemptRepository.java
│   └── AttemptAnswerRepository.java
├── entity/
│   ├── Exam.java
│   ├── ExamQuestion.java
│   ├── Classroom.java                 # table name = "classes"
│   ├── ExamSchedule.java
│   ├── StudentClass.java
│   ├── ExamAttempt.java
│   └── AttemptAnswer.java
├── enums/
│   ├── ExamStatus.java
│   ├── ScheduleStatus.java
│   └── AttemptStatus.java
└── dto/
    ├── request/
    │   ├── ExamRequest.java
    │   ├── ExamQuestionsRequest.java
    │   ├── ClassroomRequest.java
    │   ├── ScheduleRequest.java
    │   ├── StudentCreateRequest.java
    │   ├── StudentBulkImportRequest.java
    │   ├── StartAttemptRequest.java
    │   ├── SaveAnswersRequest.java
    │   └── MarkAnswerRequest.java
    └── response/
        ├── ExamResponse.java
        ├── ExamSummaryResponse.java
        ├── ClassroomResponse.java
        ├── ScheduleResponse.java
        ├── StudentResponse.java
        ├── AttemptResponse.java
        ├── AttemptResultResponse.java
        ├── MarkingQueueResponse.java
        └── AttemptMarkingResponse.java
```

---

## Frontend Pages — Phase 3

```
static/
├── admin/
│   ├── exams.html          # Exam builder (3-col picker + builder)
│   ├── schedules.html      # Schedule management table
│   └── students.html       # Student list + add/import
├── teacher/
│   ├── exams.html          # Teacher exam builder (submit for approval)
│   ├── marking.html        # Marking queue + marking screen
│   └── schedules.html      # View/create schedules
└── student/
    ├── dashboard.html      # Available exams list
    └── exam.html           # Exam attempt screen with timer
```

---

## Security Rules — Phase 3

| Endpoint pattern | Rule |
|-----------------|------|
| `POST /exams` | Admin → auto APPROVED; Teacher → PENDING |
| `PUT /exams/{id}/submit` | Owner (teacher) only; must be DRAFT or REJECTED |
| `PUT /exams/{id}/approve` | Admin only |
| `POST /student/attempts` | Student only; schedule must be ACTIVE for their class |
| `PUT /student/attempts/{id}/answers` | Must be attempt owner; status IN_PROGRESS |
| `POST /student/attempts/{id}/submit` | Must be attempt owner; status IN_PROGRESS |
| `GET /marking/queue` | Teacher sees own exams' attempts; Admin sees all |
| `PUT /marking/attempts/…/answers/…` | Teacher who created exam OR admin |
| `POST /students/bulk-import` | Admin only |

---

## Key Implementation Notes

- **Auto-save:** use `setInterval(saveAnswers, 30000)` in `student/exam.html` JS
- **Timer:** store `started_at` server-side; compute remaining = `timeLimitMins - (now - started_at)`; never trust client timer
- **Shuffle:** if `shuffle_questions = true`, shuffle order when building `AttemptResponse` (store shuffled order in `exam_attempts` or derive from student_id seed for reproducibility)
- **UUID array in PostgreSQL:** `UUID[]` type; use `@Type(PostgreSQLEnumArray.class)` or store as `TEXT` JSON array and convert in service layer
- **Class name collision:** entity is `Classroom.java` with `@Table(name = "classes")`
- **OpenCSV dependency:** add to pom.xml for bulk student import

---

## Files Added in Phase 3

| File | Change |
|------|--------|
| `docs/08-phase3-assessment-engine.md` | **This file — new** |
| `sql/04-phase3-schema-additions.sql` | All Phase 3 DB tables and indexes |
| `CLAUDE.md` | Updated with Phase 3 package, APIs, build steps, frontend pages |
