# EduLearn Platform — Phase 6: Polish & Operations

## Modules Covered

| # | Module | Description |
|---|--------|-------------|
| 19 | Audit Log Viewer | Admin views full history of all approvals and system changes |
| 20 | Bulk Import | Upload questions via CSV/Excel — maps to content hierarchy |
| 21 | Settings & Config | Term dates, exam rules, password policy, session timeout |
| 22 | Backup & Export | Export question bank or results to PDF/Excel |

---

## Module 19 — Audit Log Viewer

### Overview
Admin can view the full, immutable history of all significant actions on the platform —
approvals, rejections, account changes, question flags, and schedule modifications.
Filters make it easy to investigate a specific user, entity, or time period.

---

### 19.1 UI — Audit Log Page

**Route:** `admin/audit-log.html`
**Nav item:** "Audit Log" under System section
**Access:** Admin only

#### Filters (top bar)
- User: search by name or email
- Action type: All / Approval / Rejection / Account / Flag / Schedule / Settings
- Entity type: All / Category / Subject / Topic / Question / Exam / User
- Date range: From / To (default: last 7 days)
- [Search] [Export CSV]

#### Audit Log Table

| Column | Notes |
|--------|-------|
| Timestamp | Date + time (local) |
| User | Name + role chip |
| Action | e.g. CONTENT_APPROVED, USER_CREATED, EXAM_REJECTED |
| Entity | Type + truncated name, clickable |
| Details | Rejection reason, changed fields, IP address |
| IP | Client IP (from request) |

**Log entry examples:**

```
2025-05-30 10:04  Admin User [ADMIN]   CONTENT_APPROVED   Topic: Comparative Reading     —
2025-05-30 09:51  Ms. Clarke [TEACHER] CONTENT_SUBMITTED  Topic: Comparative Reading     —
2025-05-29 14:22  Admin User [ADMIN]   USER_CREATED       User: Mr. Ahmed (TEACHER)      —
2025-05-29 12:10  Admin User [ADMIN]   CONTENT_REJECTED   Topic: Creative Writing        "Title too vague"
2025-05-28 09:00  Admin User [ADMIN]   SETTINGS_CHANGED   System: session_timeout        60→30 mins
```

---

### 19.2 Database — Audit Log

Extends the existing `approval_log` table with a broader `audit_log` table:

```sql
CREATE TYPE audit_action AS ENUM (
    -- Content
    'CONTENT_CREATED','CONTENT_UPDATED','CONTENT_SUBMITTED',
    'CONTENT_APPROVED','CONTENT_REJECTED','CONTENT_DELETED',
    -- Exam
    'EXAM_CREATED','EXAM_UPDATED','EXAM_SUBMITTED',
    'EXAM_APPROVED','EXAM_REJECTED','EXAM_DELETED',
    'EXAM_SCHEDULED','EXAM_SCHEDULE_CANCELLED',
    -- Users
    'USER_CREATED','USER_UPDATED','USER_DEACTIVATED',
    'USER_PASSWORD_RESET','USER_LOGIN','USER_LOGOUT',
    -- Questions
    'QUESTION_FLAGGED','QUESTION_UNFLAGGED',
    -- Org/Settings
    'SETTINGS_CHANGED','ORG_SETTINGS_UPDATED',
    -- Announcements
    'ANNOUNCEMENT_CREATED','ANNOUNCEMENT_DELETED'
);

CREATE TABLE audit_log (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    action      audit_action NOT NULL,
    entity_type VARCHAR(30),
    entity_id   UUID,
    entity_name VARCHAR(300),
    actor_id    UUID         NOT NULL REFERENCES users(id),
    actor_name  VARCHAR(120) NOT NULL,   -- denormalised for log immutability
    actor_role  user_role    NOT NULL,
    detail      JSONB,       -- { "from": "...", "to": "...", "reason": "..." }
    ip_address  VARCHAR(45),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Immutable — NO UPDATE trigger, NO soft delete, NO is_deleted
CREATE INDEX idx_alog_actor      ON audit_log(actor_id);
CREATE INDEX idx_alog_action     ON audit_log(action);
CREATE INDEX idx_alog_entity     ON audit_log(entity_type, entity_id);
CREATE INDEX idx_alog_created_at ON audit_log(created_at DESC);
```

---

### 19.3 API Endpoints — Audit Log

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | `/api/v1/audit-log` | Admin | Paginated log with filters |
| GET | `/api/v1/audit-log/export` | Admin | CSV export of filtered results |

**GET /audit-log query params:**
```
?actorId=uuid&action=CONTENT_APPROVED&entityType=TOPIC
  &from=2025-05-01&to=2025-05-31&page=0&size=50
```

**AuditLogService — write helper (inject everywhere):**
```java
@Service @RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditRepo;
    private final HttpServletRequest request;

    public void log(AuditAction action, String entityType, UUID entityId,
                    String entityName, User actor, Object detail) {
        AuditLog entry = new AuditLog();
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setEntityName(entityName);
        entry.setActorId(actor.getId());
        entry.setActorName(actor.getFullName());
        entry.setActorRole(actor.getRole());
        entry.setDetail(objectMapper.valueToTree(detail));  // JSONB
        entry.setIpAddress(getClientIp(request));
        auditRepo.save(entry);
    }

    private String getClientIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        return forwarded != null ? forwarded.split(",")[0].trim() : req.getRemoteAddr();
    }
}
```

---

### 19.4 Spring Boot Classes — Audit Log

```
entity/      AuditLog.java
enums/       AuditAction.java
repository/  AuditLogRepository.java
service/     AuditLogService.java       (inject into ALL services that change data)
controller/  AuditLogController.java
dto/response/AuditLogResponse.java
```

**Add ObjectMapper bean to config for JSONB serialisation.**

---

## Module 20 — Bulk Import

### Overview
Admin and teachers can bulk-import questions via CSV or Excel (.xlsx).
Each row maps to a question under a specified topic in the content hierarchy.
Import validates all rows first, then commits or reports errors.

---

### 20.1 UI — Bulk Import Page

**Route:** section within `admin/courses.html` and `teacher/courses.html`
**Trigger:** "Import questions" button in the tree sidebar header

#### Import Flow

```
Step 1: Select topic target
  [Category ▾] → [Subject ▾] → [Topic ▾]  — must be existing APPROVED topic

Step 2: Download template
  [Download CSV template]  [Download Excel template]

Step 3: Upload file
  [ Choose file — CSV or XLSX ]

Step 4: Preview & validate
  Shows first 5 rows + any errors per row:
  ┌─────┬────────────────────┬──────────────┬───────┬────────────┐
  │ Row │ Question text      │ Type         │ Marks │ Status     │
  ├─────┼────────────────────┼──────────────┼───────┼────────────┤
  │  2  │ What device is..   │ MCQ_SINGLE   │ 2     │ ✓ Valid    │
  │  3  │ (blank)            │ MCQ_SINGLE   │ 2     │ ✗ Missing  │
  │  4  │ Write an essay..   │ ESSAY        │ 10    │ ✓ Valid    │
  └─────┴────────────────────┴──────────────┴───────┴────────────┘
  Valid: 2 rows    Errors: 1 row

Step 5: Confirm import
  [Cancel]  [Import 2 valid rows]
  Errors skipped — download error report
```

---

### 20.2 CSV Template Format

**Template headers:**
```
question_text, question_type, marks, answer_explanation,
option_1, option_1_correct,
option_2, option_2_correct,
option_3, option_3_correct,
option_4, option_4_correct,
correct_boolean, model_answer, marking_scheme, word_limit,
code_language, starter_code, expected_output,
image_url, image_alt_text, image_answer_type
```

**Example rows:**
```csv
"What literary device is used in line 3?",MCQ_SINGLE,2,"A simile compares using like or as",
"Simile",TRUE,"Metaphor",FALSE,"Alliteration",FALSE,"Personification",FALSE,
,,,,,,,,,,

"The poem uses a positive tone. True or False?",TRUE_FALSE,1,"The tone is melancholic",
,,,,,,,,TRUE,,,,,,,,,

"Analyse the writer's use of language.",ESSAY,10,,
,,,,,,,,,,"Award 2 marks per: technique identified, effect explained, relevant quote",500,,,,,
```

---

### 20.3 API Endpoints — Bulk Import

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET  | `/api/v1/import/template/csv` | Admin/Teacher | Download CSV template |
| GET  | `/api/v1/import/template/xlsx` | Admin/Teacher | Download Excel template |
| POST | `/api/v1/import/validate` | Admin/Teacher | Validate file, return preview + errors |
| POST | `/api/v1/import/commit` | Admin/Teacher | Commit validated rows to DB |

**POST /import/validate — multipart:**
```
topicId: uuid
file: <CSV or XLSX binary>
```

**Response:**
```json
{
  "data": {
    "totalRows": 15,
    "validRows": 13,
    "errorRows": 2,
    "preview": [
      { "row": 2, "questionText": "What device...", "type": "MCQ_SINGLE", "valid": true },
      { "row": 3, "error": "question_text is required", "valid": false }
    ],
    "validationId": "uuid"   // passed to /commit
  }
}
```

**POST /import/commit:**
```json
{ "validationId": "uuid", "topicId": "uuid" }
```

**Add to pom.xml** (for Excel support):
```xml
<dependency>
  <groupId>org.apache.poi</groupId>
  <artifactId>poi-ooxml</artifactId>
  <version>5.2.5</version>
</dependency>
```

---

### 20.4 Spring Boot Classes — Bulk Import

```
controller/  BulkImportController.java
service/     BulkImportService.java
             BulkImportValidationService.java
             CsvImportParser.java
             ExcelImportParser.java
dto/response/ImportPreviewResponse.java
             ImportResultResponse.java
```

---

## Module 21 — Settings & Config

### Overview
Admin configures platform-wide settings stored in the DB.
All settings are key-value pairs with typed values and descriptions.

---

### 21.1 UI — Settings Page

**Route:** `admin/settings.html`
**Nav item:** "Settings" already in left sidebar
**Access:** Admin only

#### Settings Sections

**Academic / Term Settings**
| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| Current term name | Text | Autumn 2025 | Shown on dashboards |
| Term start date | Date | — | |
| Term end date | Date | — | |
| Academic year | Text | 2025–2026 | |

**Exam Rules**
| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| Default time limit | Number (mins) | 60 | Pre-filled in exam builder |
| Default pass mark % | Number | 50 | Pre-filled in exam builder |
| Max attempts per exam | Number | 1 | Default for new schedules |
| Allow exam re-sits | Toggle | ON | |
| Auto-release results | Toggle | OFF | |

**Security Settings**
| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| Session timeout | Select | 30 mins | 15 / 30 / 60 / 120 mins |
| Min password length | Number | 8 | |
| Require uppercase | Toggle | ON | |
| Require number | Toggle | ON | |
| Require special char | Toggle | OFF | |
| Max login attempts | Number | 5 | Before account lockout |
| Lockout duration | Number (mins) | 15 | |

**Approval Settings**
| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| Auto-approve admin content | Toggle | ON | Admin content skips PENDING |
| Notify all admins on submission | Toggle | ON | |

---

### 21.2 Database — Settings

```sql
CREATE TABLE platform_settings (
    key         VARCHAR(100) PRIMARY KEY,
    value       TEXT         NOT NULL,
    value_type  VARCHAR(20)  NOT NULL CHECK (value_type IN ('STRING','INTEGER','BOOLEAN','DATE')),
    category    VARCHAR(50)  NOT NULL,
    label       VARCHAR(200) NOT NULL,
    description TEXT,
    updated_by  UUID         REFERENCES users(id),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Seed default settings
INSERT INTO platform_settings (key, value, value_type, category, label) VALUES
('term.name',             'Autumn 2025', 'STRING',  'academic', 'Current term name'),
('term.start_date',       '',            'DATE',    'academic', 'Term start date'),
('term.end_date',         '',            'DATE',    'academic', 'Term end date'),
('exam.default_time',     '60',          'INTEGER', 'exam',     'Default time limit (minutes)'),
('exam.default_pass_pct', '50',          'INTEGER', 'exam',     'Default pass mark (%)'),
('exam.max_attempts',     '1',           'INTEGER', 'exam',     'Max attempts per schedule'),
('exam.allow_resits',     'true',        'BOOLEAN', 'exam',     'Allow exam re-sits'),
('exam.auto_release',     'false',       'BOOLEAN', 'exam',     'Auto-release results on submit'),
('security.session_mins', '30',          'INTEGER', 'security', 'Session timeout (minutes)'),
('security.min_password', '8',           'INTEGER', 'security', 'Minimum password length'),
('security.req_upper',    'true',        'BOOLEAN', 'security', 'Require uppercase letter'),
('security.req_number',   'true',        'BOOLEAN', 'security', 'Require number'),
('security.req_special',  'false',       'BOOLEAN', 'security', 'Require special character'),
('security.max_attempts', '5',           'INTEGER', 'security', 'Max login attempts'),
('security.lockout_mins', '15',          'INTEGER', 'security', 'Lockout duration (minutes)'),
('approval.admin_auto',   'true',        'BOOLEAN', 'approval', 'Auto-approve admin content'),
('approval.notify_all',   'true',        'BOOLEAN', 'approval', 'Notify all admins on submission');
```

---

### 21.3 API Endpoints — Settings

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | `/api/v1/settings` | Admin | All settings grouped by category |
| GET | `/api/v1/settings/{key}` | Admin | Single setting value |
| PUT | `/api/v1/settings/{key}` | Admin | Update a setting value |
| PUT | `/api/v1/settings/batch` | Admin | Update multiple settings at once |

**PUT /settings/batch request:**
```json
{
  "settings": [
    { "key": "security.session_mins", "value": "60" },
    { "key": "exam.default_pass_pct", "value": "40" }
  ]
}
```

---

### 21.4 Spring Boot Classes — Settings

```
entity/      PlatformSetting.java
repository/  PlatformSettingRepository.java
service/     SettingsService.java             (cached with @Cacheable)
controller/  SettingsController.java
dto/request/ SettingUpdateRequest.java
             BatchSettingUpdateRequest.java
dto/response/SettingResponse.java
             SettingGroupResponse.java
```

**SettingsService — typed getter helpers:**
```java
public String  getString(String key)  { return repo.findByKey(key).getValue(); }
public int     getInt(String key)     { return Integer.parseInt(getString(key)); }
public boolean getBool(String key)    { return Boolean.parseBoolean(getString(key)); }
// Used by other services: settingsService.getInt("exam.default_time") → 60
```

---

## Module 22 — Backup & Export

### Overview
Admin can export the question bank or exam results to PDF or Excel files
for offline use, printing, or record-keeping.

---

### 22.1 UI — Export Options

Exports are triggered from relevant pages via an Export button:

| Page | Export option | Format |
|------|--------------|--------|
| `admin/courses.html` | Export full question bank | Excel (.xlsx) |
| `admin/courses.html` | Export questions for a topic | Excel (.xlsx) |
| `admin/reports.html` | Export class results | Excel (.xlsx) |
| `admin/reports.html` | Export student result | PDF |
| `admin/reports.html` | Export audit log | CSV |
| `admin/exams.html` | Export exam paper | PDF (printable) |

---

### 22.2 Export Formats

#### Question Bank Export (Excel)
Columns match the bulk import CSV template format so the file can be
re-imported. Grouped by Subject → Topic with header rows.

```
Sheet: Year 10 — English Language
┌──────────────────┬──────────────┬───────┬────────┬───────────┐
│ Question text    │ Type         │ Marks │ Option1│ Correct1  │
├──────────────────┼──────────────┼───────┼────────┼───────────┤
│ TOPIC: Non-Fic.. │              │       │        │           │  ← topic header row
│ What device is.. │ MCQ_SINGLE   │ 2     │ Simile │ TRUE      │
│ ...              │ ...          │ ...   │ ...    │ ...       │
```

#### Exam Paper Export (PDF)
Printable exam paper for physical distribution:
- Page 1: cover page (exam name, time, instructions, student name/date line)
- Pages 2+: questions numbered, with answer spaces appropriate to type

#### Class Results Export (Excel)
```
Sheet: Year 10A — English Mock
┌───────────────┬───────┬─────┬────────────────┬─────────────────┐
│ Student name  │ Class │  %  │ Topic 1 score  │ Topic 2 score   │
├───────────────┼───────┼─────┼────────────────┼─────────────────┤
│ Sarah Jones   │ 10A   │ 96% │ 10/10          │ 15/15           │
│ Tom Brown     │ 10B   │ 22% │ 3/10           │ 5/15            │
```

---

### 22.3 API Endpoints — Export

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | `/api/v1/export/questions?subjectId=&topicId=` | Admin/Teacher | Download question bank as Excel |
| GET | `/api/v1/export/exam/{examId}/paper` | Admin/Teacher | Download exam paper as PDF |
| GET | `/api/v1/export/results/schedule/{scheduleId}` | Admin/Teacher | Class results as Excel |
| GET | `/api/v1/export/results/attempt/{attemptId}` | Admin/Teacher/Student | Single result as PDF |
| GET | `/api/v1/export/audit-log` | Admin | Audit log as CSV |

All export endpoints return binary file response:
```
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
Content-Disposition: attachment; filename="questions-year10-english.xlsx"
```

---

### 22.4 Spring Boot Classes — Export

```
service/     ExportService.java
             QuestionBankExportService.java   (Apache POI)
             ResultExportService.java          (Apache POI)
             ExamPaperPdfService.java          (iText or OpenPDF)
controller/  ExportController.java
```

**Add to pom.xml** (PDF generation):
```xml
<dependency>
  <groupId>com.github.librepdf</groupId>
  <artifactId>openpdf</artifactId>
  <version>1.3.43</version>
</dependency>
```

---

## Database Schema Additions — Phase 6

Save as `sql/08-phase6-schema-additions.sql`:

```sql
-- ── Module 19: Full audit log ────────────────────────────────
CREATE TYPE audit_action AS ENUM (
    'CONTENT_CREATED','CONTENT_UPDATED','CONTENT_SUBMITTED',
    'CONTENT_APPROVED','CONTENT_REJECTED','CONTENT_DELETED',
    'EXAM_CREATED','EXAM_UPDATED','EXAM_SUBMITTED',
    'EXAM_APPROVED','EXAM_REJECTED','EXAM_DELETED',
    'EXAM_SCHEDULED','EXAM_SCHEDULE_CANCELLED',
    'USER_CREATED','USER_UPDATED','USER_DEACTIVATED',
    'USER_PASSWORD_RESET','USER_LOGIN','USER_LOGOUT',
    'QUESTION_FLAGGED','QUESTION_UNFLAGGED',
    'SETTINGS_CHANGED','ORG_SETTINGS_UPDATED',
    'ANNOUNCEMENT_CREATED','ANNOUNCEMENT_DELETED'
);

CREATE TABLE audit_log (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    action      audit_action NOT NULL,
    entity_type VARCHAR(30),
    entity_id   UUID,
    entity_name VARCHAR(300),
    actor_id    UUID         NOT NULL REFERENCES users(id),
    actor_name  VARCHAR(120) NOT NULL,
    actor_role  user_role    NOT NULL,
    detail      JSONB,
    ip_address  VARCHAR(45),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Immutable: no UPDATE, no DELETE, no is_deleted
CREATE INDEX idx_alog_actor      ON audit_log(actor_id);
CREATE INDEX idx_alog_action     ON audit_log(action);
CREATE INDEX idx_alog_entity     ON audit_log(entity_type, entity_id);
CREATE INDEX idx_alog_created_at ON audit_log(created_at DESC);

-- ── Module 21: Platform settings ─────────────────────────────
CREATE TABLE platform_settings (
    key         VARCHAR(100) PRIMARY KEY,
    value       TEXT         NOT NULL,
    value_type  VARCHAR(20)  NOT NULL CHECK (value_type IN ('STRING','INTEGER','BOOLEAN','DATE')),
    category    VARCHAR(50)  NOT NULL,
    label       VARCHAR(200) NOT NULL,
    description TEXT,
    updated_by  UUID         REFERENCES users(id),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

INSERT INTO platform_settings (key,value,value_type,category,label) VALUES
('term.name',             'Autumn 2025','STRING', 'academic','Current term name'),
('term.start_date',       '',           'DATE',   'academic','Term start date'),
('term.end_date',         '',           'DATE',   'academic','Term end date'),
('exam.default_time',     '60',         'INTEGER','exam',    'Default time limit (minutes)'),
('exam.default_pass_pct', '50',         'INTEGER','exam',    'Default pass mark (%)'),
('exam.max_attempts',     '1',          'INTEGER','exam',    'Max attempts per schedule'),
('exam.allow_resits',     'true',       'BOOLEAN','exam',    'Allow exam re-sits'),
('exam.auto_release',     'false',      'BOOLEAN','exam',    'Auto-release results on submit'),
('security.session_mins', '30',         'INTEGER','security','Session timeout (minutes)'),
('security.min_password', '8',          'INTEGER','security','Minimum password length'),
('security.req_upper',    'true',       'BOOLEAN','security','Require uppercase letter'),
('security.req_number',   'true',       'BOOLEAN','security','Require number'),
('security.req_special',  'false',      'BOOLEAN','security','Require special character'),
('security.max_attempts', '5',          'INTEGER','security','Max login attempts'),
('security.lockout_mins', '15',         'INTEGER','security','Lockout duration (minutes)'),
('approval.admin_auto',   'true',       'BOOLEAN','approval','Auto-approve admin content'),
('approval.notify_all',   'true',       'BOOLEAN','approval','Notify all admins on submission');
```

---

## Package Structure Additions — Phase 6

```
com.edulearn
├── controller/
│   ├── AuditLogController.java
│   ├── BulkImportController.java
│   ├── SettingsController.java
│   └── ExportController.java
├── service/
│   ├── AuditLogService.java           (inject everywhere data changes)
│   ├── BulkImportService.java
│   ├── BulkImportValidationService.java
│   ├── CsvImportParser.java
│   ├── ExcelImportParser.java
│   ├── SettingsService.java
│   ├── QuestionBankExportService.java
│   ├── ResultExportService.java
│   └── ExamPaperPdfService.java
├── repository/
│   ├── AuditLogRepository.java
│   └── PlatformSettingRepository.java
├── entity/
│   ├── AuditLog.java
│   └── PlatformSetting.java
└── enums/
    └── AuditAction.java
```

---

## pom.xml Additions — Phase 6

```xml
<!-- Apache POI — Excel read/write (already added in Phase 3 for student import) -->
<dependency>
  <groupId>org.apache.poi</groupId>
  <artifactId>poi-ooxml</artifactId>
  <version>5.2.5</version>
</dependency>

<!-- OpenPDF — PDF generation (lighter than iText, no LGPL restrictions) -->
<dependency>
  <groupId>com.github.librepdf</groupId>
  <artifactId>openpdf</artifactId>
  <version>1.3.43</version>
</dependency>
```

---

## Frontend Pages — Phase 6

```
static/
└── admin/
    ├── audit-log.html      # Full audit log with filters + CSV export
    ├── settings.html       # Platform settings grouped by category
    └── import.html         # Bulk question import wizard (or modal in courses.html)
```

Export buttons are embedded in existing pages — no new pages needed for modules 22.

---

## Files Added in Phase 6

| File | Change |
|------|--------|
| `docs/12-phase6-polish-operations.md` | **This file — new** |
| `sql/08-phase6-schema-additions.sql` | `audit_log` + `platform_settings` tables with seed data |
| `CLAUDE.md` | Phase 6 classes, APIs, pom.xml additions, build steps |
