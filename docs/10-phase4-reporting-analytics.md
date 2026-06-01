# EduLearn Platform — Phase 4: Reporting & Analytics

## Modules Covered

| # | Module | Description |
|---|--------|-------------|
| 13 | Student Results | Per-student score breakdown by topic and question type |
| 14 | Class Reports | Cohort-level performance — weakest topics, score distributions |
| 15 | Teacher Reports | Content contribution rates, approval statistics |
| 16 | Question Analytics | Questions with highest fail rates — flag for review |

---

## Module 13 — Student Results

### Overview
After an exam attempt is fully marked, students and teachers/admins can view
a detailed results breakdown. Students see their own results only.
Teachers see results for exams they created. Admins see everything.

---

### 13.1 UI — Student Results Page

**Route:** `student/results.html`
**Access:** Student (own results only)

#### Results List (student dashboard)
Table of completed attempts:

| Column | Notes |
|--------|-------|
| Exam | Name |
| Subject | Derived from exam's question topics |
| Date taken | Submitted at |
| Score | e.g. 47 / 60 |
| Percentage | e.g. 78% |
| Result | Pass ✓ (green) / Fail ✗ (red) |
| Action | View details → |

#### Result Detail Screen

```
┌──────────────────────────────────────────────────────────┐
│  Year 10 English Mock — Results                          │
│  Taken: 30 May 2025 · Duration: 54 min                   │
├────────────────┬─────────────────────────────────────────┤
│ SCORE          │  BREAKDOWN BY TOPIC                     │
│                │                                         │
│   47 / 60      │  Non-Fiction Writing    8/10  ████████░ │
│    78%  ✓ Pass │  Textual Analysis       12/15 ████████░ │
│                │  Comparative Reading    6/10  ██████░░░ │
│                │  Creative Writing       21/25 ████████░ │
├────────────────┴─────────────────────────────────────────┤
│ QUESTION BY QUESTION                                      │
│                                                          │
│ Q1  MCQ      What device is used?       ✓  2/2           │
│ Q2  Essay    Analyse lines 1–6.         ~  6/10  Marked  │
│ Q3  True/F   This is a simile. T/F      ✗  0/1           │
│ Q4  MCQ      Pick all techniques.       ✓  3/3            │
│ ...                                                      │
│                                                          │
│ Legend: ✓ Correct  ✗ Incorrect  ~ Partially correct      │
└──────────────────────────────────────────────────────────┘
```

#### Teacher / Admin Result View
Same layout plus:
- Shows student name and class at top
- Shows all students' scores for that exam in a summary table
- Export to PDF/CSV button

---

### 13.2 Database — Results View

No new tables. Results are computed from `exam_attempts` + `attempt_answers`
joined with `exam_questions`, `questions`, `topics`, `subjects`.

```sql
-- Convenience view for result breakdown per attempt per topic
CREATE OR REPLACE VIEW attempt_topic_scores AS
SELECT
    aa.attempt_id,
    t.id          AS topic_id,
    t.name        AS topic_name,
    s.name        AS subject_name,
    COUNT(aa.id)  AS question_count,
    SUM(COALESCE(eq.marks_override, q.marks)) AS total_marks,
    SUM(COALESCE(aa.marks_awarded, 0))         AS marks_obtained
FROM attempt_answers aa
JOIN exam_questions  eq ON eq.id = aa.exam_question_id
JOIN questions        q ON q.id  = aa.question_id
JOIN topics           t ON t.id  = q.topic_id
JOIN subjects         s ON s.id  = t.subject_id
GROUP BY aa.attempt_id, t.id, t.name, s.name;
```

---

### 13.3 API Endpoints — Student Results

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | `/api/v1/student/results` | Student | List own completed attempts |
| GET | `/api/v1/student/results/{attemptId}` | Student | Full result detail with topic breakdown |
| GET | `/api/v1/results/attempt/{attemptId}` | Teacher/Admin | Same detail, any student |
| GET | `/api/v1/results/schedule/{scheduleId}` | Teacher/Admin | All student results for one schedule |
| GET | `/api/v1/results/schedule/{scheduleId}/export` | Teacher/Admin | CSV export of all scores |

**GET /student/results/{attemptId} response:**
```json
{
  "status": "success",
  "data": {
    "attemptId": "uuid",
    "examName": "Year 10 English Mock",
    "submittedAt": "2025-05-30T10:54:00Z",
    "durationMinutes": 54,
    "totalMarks": 60,
    "marksObtained": 47,
    "percentage": 78.33,
    "isPassed": true,
    "topicBreakdown": [
      { "topicName": "Non-Fiction Writing", "totalMarks": 10, "marksObtained": 8 },
      { "topicName": "Textual Analysis",    "totalMarks": 15, "marksObtained": 12 }
    ],
    "answers": [
      {
        "questionNumber": 1,
        "questionType": "MCQ_SINGLE",
        "questionText": "What device is used?",
        "marksAvailable": 2,
        "marksAwarded": 2,
        "isCorrect": true,
        "answerExplanation": "The correct answer is Simile because..."
      }
    ]
  }
}
```

---

### 13.4 Spring Boot Classes — Results

```
controller/  StudentResultController.java
service/     ResultService.java
dto/response/StudentResultSummaryResponse.java
             StudentResultDetailResponse.java
             TopicScoreResponse.java
             AnswerResultResponse.java
```

---

## Module 14 — Class Reports

### Overview
Admin and teachers see cohort-level performance for a given exam or class.
Identifies weakest topics, score distributions, and struggling students.

---

### 14.1 UI — Class Reports Page

**Route:** page within `admin/reports.html` and `teacher/reports.html`
**Nav item:** "Reports" in left sidebar

#### Filters
- Class / Year group selector
- Exam selector (filtered by selected class)
- Date range

#### Report Sections

**1 — Score Distribution Chart**
```
Score range  │ Students
0–20%        │ ██  2
21–40%       │ ███ 3
41–60%       │ █████████ 9
61–80%       │ ████████████ 12
81–100%      │ ████ 4
             └─────────────────
                Bar chart (horizontal)
```

**2 — Class Summary Cards**
| Card | Value |
|------|-------|
| Average score | 68% |
| Highest score | 96% — Sarah J. |
| Lowest score | 22% — Tom B. |
| Pass rate | 82% (25/30 students) |
| Questions attempted | 450 / 450 |
| Unattempted | 0 |

**3 — Topic Performance Table**

| Topic | Avg score | Students below 50% | Flag |
|-------|-----------|-------------------|------|
| Non-Fiction Writing | 74% | 4 | — |
| Comparative Reading | 41% | 16 | ⚠ Weak |
| Textual Analysis | 68% | 7 | — |

Weak topics (avg < 50%) highlighted in amber with a flag icon.
Admin/Teacher can click a flagged topic → see individual student scores for it.

**4 — Student Ranking Table**

| Rank | Student | Class | Score | % | Result |
|------|---------|-------|-------|---|--------|
| 1 | Sarah Jones | 10A | 58/60 | 96% | ✓ |
| 2 | Ali Hassan | 10A | 55/60 | 91% | ✓ |
| ... | | | | | |
| 28 | Tom Brown | 10B | 13/60 | 22% | ✗ |

Sortable columns. Click student → open their full result detail.

---

### 14.2 API Endpoints — Class Reports

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | `/api/v1/reports/class/{classId}/summary` | Teacher/Admin | Class summary stats |
| GET | `/api/v1/reports/class/{classId}/topics` | Teacher/Admin | Topic performance breakdown |
| GET | `/api/v1/reports/class/{classId}/students` | Teacher/Admin | Student ranking table |
| GET | `/api/v1/reports/class/{classId}/distribution` | Teacher/Admin | Score distribution data |
| GET | `/api/v1/reports/class/{classId}/export` | Admin | Full CSV export |

**GET /reports/class/{id}/summary response:**
```json
{
  "data": {
    "classId": "uuid",
    "className": "Year 10A",
    "examName": "Year 10 English Mock",
    "studentCount": 30,
    "attemptedCount": 30,
    "averageScore": 68.4,
    "highestScore": 96.7,
    "lowestScore": 21.7,
    "passRate": 82.0,
    "passCount": 24,
    "failCount": 6
  }
}
```

---

### 14.3 Spring Boot Classes — Class Reports

```
controller/  ReportController.java
service/     ReportService.java
             ReportExportService.java    (CSV generation)
dto/response/ClassSummaryResponse.java
             TopicPerformanceResponse.java
             StudentRankingResponse.java
             ScoreDistributionResponse.java
```

---

## Module 15 — Teacher Reports

### Overview
Admin views statistics about teacher activity — content created, approval rates,
time to approval, and exam creation. Useful for performance reviews and identifying
under-contributing staff.

---

### 15.1 UI — Teacher Activity Report

**Route:** section within `admin/reports.html`
**Access:** Admin only

#### Teacher Activity Table

| Column | Notes |
|--------|-------|
| Teacher | Name + avatar |
| Content created | Total questions/topics/subjects submitted |
| Approved | Count approved |
| Rejected | Count rejected (with % rejection rate) |
| Avg approval time | Median days from PENDING to APPROVED |
| Exams created | Number of exam papers |
| Last active | Most recent submission date |

Click teacher row → drill-down showing their full submission history.

#### Summary Stats (top of page)
- Total content across all teachers
- Platform-wide approval rate %
- Average time to approval (days)
- Most active teacher this month

---

### 15.2 API Endpoints — Teacher Reports

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | `/api/v1/reports/teachers` | Admin | All teacher activity summary |
| GET | `/api/v1/reports/teachers/{id}` | Admin | Single teacher detail |
| GET | `/api/v1/reports/teachers/export` | Admin | CSV export |

**GET /reports/teachers response (array):**
```json
{
  "data": [
    {
      "teacherId": "uuid",
      "fullName": "Ms. S. Clarke",
      "contentCreated": 47,
      "approved": 42,
      "rejected": 3,
      "pending": 2,
      "rejectionRate": 6.4,
      "avgApprovalDays": 1.8,
      "examsCreated": 3,
      "lastActiveAt": "2025-05-30T09:00:00Z"
    }
  ]
}
```

**Implementation note:** `avgApprovalDays` is computed from `approval_log`
where `from_status = 'PENDING'` and `to_status = 'APPROVED'`, using
`AVG(created_at - prev_pending_at)` per teacher.

---

### 15.3 Spring Boot Classes — Teacher Reports

```
controller/  ReportController.java        (add to existing)
service/     TeacherReportService.java
dto/response/TeacherActivityResponse.java
             TeacherActivitySummaryResponse.java
```

---

## Module 16 — Question Analytics

### Overview
Identifies questions where students consistently fail, score partially, or
skip — flagging them for teacher review or replacement.

---

### 16.1 UI — Question Analytics Page

**Route:** section within `admin/reports.html` and `teacher/reports.html`
**Nav:** "Reports → Question Analytics" tab

#### Question Performance Table

| Column | Notes |
|--------|-------|
| Question | Truncated text + type badge |
| Topic | Breadcrumb |
| Times attempted | Total across all exams |
| Correct % | % of students who got it right |
| Avg marks | Average marks awarded / available |
| Skip rate | % who left it blank |
| Flag | ⚠ if correct% < 30% |

Filter: Topic · Question type · Flag only toggle · Sort by correct% asc

#### Detail: Click a flagged question
Shows:
- Full question text + correct answer
- Option-level breakdown for MCQ (% who chose each option)
- Distribution of marks for essay/short answer
- "Flag for review" button — marks question with `needs_review = true`
- "Replace question" button — opens question builder with a copy

---

### 16.2 Database — Question Analytics

```sql
-- Add review flag to questions
ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS needs_review   BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS review_reason  TEXT;
ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS flagged_at     TIMESTAMPTZ;
ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS flagged_by     UUID REFERENCES users(id);

-- Convenience view: per-question attempt statistics
CREATE OR REPLACE VIEW question_analytics AS
SELECT
    q.id                                        AS question_id,
    q.question_text,
    q.question_type,
    q.marks                                     AS max_marks,
    q.needs_review,
    t.name                                      AS topic_name,
    s.name                                      AS subject_name,
    COUNT(aa.id)                                AS times_attempted,
    COUNT(aa.id) FILTER (WHERE aa.is_correct = TRUE)  AS correct_count,
    COUNT(aa.id) FILTER (WHERE aa.text_answer IS NULL
        AND aa.selected_option_ids IS NULL
        AND aa.boolean_answer IS NULL)          AS skip_count,
    ROUND(AVG(COALESCE(aa.marks_awarded, 0))::NUMERIC, 2) AS avg_marks_awarded,
    CASE WHEN COUNT(aa.id) > 0
        THEN ROUND(100.0 * COUNT(aa.id) FILTER (WHERE aa.is_correct = TRUE) / COUNT(aa.id), 1)
        ELSE NULL END                           AS correct_pct
FROM questions q
LEFT JOIN attempt_answers aa ON aa.question_id = q.id
JOIN topics   t ON t.id = q.topic_id
JOIN subjects s ON s.id = t.subject_id
WHERE q.is_deleted = FALSE
GROUP BY q.id, q.question_text, q.question_type, q.marks,
         q.needs_review, t.name, s.name;
```

---

### 16.3 API Endpoints — Question Analytics

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | `/api/v1/analytics/questions` | Teacher/Admin | Question performance table with filters |
| GET | `/api/v1/analytics/questions/{id}` | Teacher/Admin | Single question detail + option breakdown |
| PUT | `/api/v1/analytics/questions/{id}/flag` | Teacher/Admin | Flag for review |
| PUT | `/api/v1/analytics/questions/{id}/unflag` | Admin | Clear flag |

**GET /analytics/questions query params:**
```
?topicId=uuid&type=MCQ_SINGLE&flaggedOnly=true&sortBy=correct_pct&order=asc&page=0&size=20
```

**PUT /analytics/questions/{id}/flag request:**
```json
{ "reason": "Correct rate is 18% — question may be ambiguous or too difficult." }
```

---

### 16.4 Spring Boot Classes — Question Analytics

```
controller/  QuestionAnalyticsController.java
service/     QuestionAnalyticsService.java
dto/response/QuestionAnalyticsResponse.java
             QuestionOptionStatsResponse.java
```

---

## Database Schema Additions — Phase 4

Save as `sql/06-phase4-schema-additions.sql`:

```sql
-- ── Module 13: Topic-score convenience view ───────────────────
CREATE OR REPLACE VIEW attempt_topic_scores AS
SELECT
    aa.attempt_id,
    t.id          AS topic_id,
    t.name        AS topic_name,
    s.name        AS subject_name,
    COUNT(aa.id)  AS question_count,
    SUM(COALESCE(eq.marks_override, q.marks)) AS total_marks,
    SUM(COALESCE(aa.marks_awarded, 0))         AS marks_obtained
FROM attempt_answers aa
JOIN exam_questions  eq ON eq.id = aa.exam_question_id
JOIN questions        q ON q.id  = aa.question_id
JOIN topics           t ON t.id  = q.topic_id
JOIN subjects         s ON s.id  = t.subject_id
GROUP BY aa.attempt_id, t.id, t.name, s.name;

-- ── Module 16: Question review flag ──────────────────────────
ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS needs_review  BOOLEAN    NOT NULL DEFAULT FALSE;
ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS review_reason TEXT;
ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS flagged_at    TIMESTAMPTZ;
ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS flagged_by    UUID REFERENCES users(id);

-- ── Module 16: Question analytics view ───────────────────────
CREATE OR REPLACE VIEW question_analytics AS
SELECT
    q.id                                        AS question_id,
    q.question_text,
    q.question_type,
    q.marks                                     AS max_marks,
    q.needs_review,
    t.name                                      AS topic_name,
    s.name                                      AS subject_name,
    COUNT(aa.id)                                AS times_attempted,
    COUNT(aa.id) FILTER (WHERE aa.is_correct = TRUE)          AS correct_count,
    COUNT(aa.id) FILTER (WHERE aa.text_answer IS NULL
        AND aa.selected_option_ids IS NULL
        AND aa.boolean_answer IS NULL)                         AS skip_count,
    ROUND(AVG(COALESCE(aa.marks_awarded,0))::NUMERIC,2)       AS avg_marks_awarded,
    CASE WHEN COUNT(aa.id) > 0
        THEN ROUND(100.0 *
             COUNT(aa.id) FILTER (WHERE aa.is_correct = TRUE)
             / COUNT(aa.id), 1)
        ELSE NULL END                                          AS correct_pct
FROM questions q
LEFT JOIN attempt_answers aa ON aa.question_id = q.id
JOIN topics   t ON t.id = q.topic_id
JOIN subjects s ON s.id = t.subject_id
WHERE q.is_deleted = FALSE
GROUP BY q.id, q.question_text, q.question_type, q.marks,
         q.needs_review, t.name, s.name;
```

---

## Package Structure Additions — Phase 4

```
com.edulearn
├── controller/
│   ├── StudentResultController.java     # Module 13
│   ├── ReportController.java            # Modules 14 & 15
│   └── QuestionAnalyticsController.java # Module 16
├── service/
│   ├── ResultService.java
│   ├── ReportService.java
│   ├── TeacherReportService.java
│   ├── ReportExportService.java         # CSV generation
│   └── QuestionAnalyticsService.java
└── dto/response/
    ├── StudentResultSummaryResponse.java
    ├── StudentResultDetailResponse.java
    ├── TopicScoreResponse.java
    ├── AnswerResultResponse.java
    ├── ClassSummaryResponse.java
    ├── TopicPerformanceResponse.java
    ├── StudentRankingResponse.java
    ├── ScoreDistributionResponse.java
    ├── TeacherActivityResponse.java
    ├── QuestionAnalyticsResponse.java
    └── QuestionOptionStatsResponse.java
```

---

## Frontend Pages — Phase 4

```
static/
├── admin/
│   └── reports.html    # Class reports + Teacher reports + Question analytics (tabbed)
├── teacher/
│   └── reports.html    # Class reports + Question analytics (own exams only)
└── student/
    └── results.html    # Student's own attempt history + result detail
```

### reports.html tab structure

```
┌──────────────────────────────────────────────────┐
│ NAV │ TOPBAR                                      │
│     ├──────────────────────────────────────────  │
│     │  Reports                                    │
│     │  [Class Reports] [Question Analytics]       │
│     │  [Teacher Activity] ← admin only tab        │
│     │                                             │
│     │  Filters: Class ▾  Exam ▾  Date range       │
│     │                                             │
│     │  ┌──────────────────────────────────────┐  │
│     │  │  Content of active tab               │  │
│     │  └──────────────────────────────────────┘  │
└──────────────────────────────────────────────────┘
```

---

## Files Added in Phase 4

| File | Change |
|------|--------|
| `docs/10-phase4-reporting-analytics.md` | **This file — new** |
| `sql/06-phase4-schema-additions.sql` | Views + question review flag columns |
| `CLAUDE.md` | Updated with Phase 4 classes, APIs, build steps |
