# EduLearn Platform — Phase 5: Communication & Notifications

## Modules Covered

| # | Module | Description |
|---|--------|-------------|
| 17 | In-App Notifications | Bell icon — approval updates, exam reminders, new submissions |
| 18 | Announcement Board | Admin posts notices visible to all teachers or all students |

---

## Module 17 — In-App Notifications

### Overview
Every user (admin, teacher, student) receives targeted in-app notifications
triggered by key platform events. Notifications appear in the bell icon (topbar)
and persist until read. No email or SMS in Phase 5 — in-app only.

---

### 17.1 Notification Events — Full Matrix

| Event | Trigger | Recipient(s) |
|-------|---------|-------------|
| Teacher submits content | Teacher clicks "Submit for approval" | All admins |
| Content approved | Admin approves | Submitting teacher |
| Content rejected | Admin rejects with reason | Submitting teacher |
| Exam submitted for approval | Teacher submits exam paper | All admins |
| Exam approved | Admin approves exam | Creating teacher |
| Exam rejected | Admin rejects exam | Creating teacher |
| Exam scheduled | Admin/Teacher creates a schedule | Students in that class |
| Exam starts (15 min warning) | Schedule start_at − 15 min | Students in that class |
| Exam results released | Attempt finalised + show_results_immediately=true | That student |
| Manual marking complete | All answers marked → attempt finalised | That student |
| New teacher account created | Admin creates teacher account | That teacher |
| New student account created | Admin creates student account | That student |
| Password reset | Admin resets a password | That user |
| Question flagged for review | Analytics flag triggered | Question creator |

---

### 17.2 UI — Notification Panel

Already built in `admin-dashboard.html` (Phase 1 — topbar bell icon).
Extend to teacher and student dashboards with same component.

#### Notification panel behaviour
- Bell shows red dot badge when `unread_count > 0`
- Panel lists notifications newest first, max 20 visible
- "Mark all read" button → `PUT /api/v1/notifications/read-all`
- Clicking a notification → marks it read + navigates to relevant page
- Unread = blue dot + light-blue background row
- Read = hollow grey dot + white background

#### Notification navigation targets

| Type | Navigate to |
|------|-------------|
| Content submitted | `/admin/courses.html` (with item pre-selected) |
| Content approved/rejected | `/teacher/submissions.html` |
| Exam approved/rejected | `/teacher/exams.html` |
| Exam scheduled | `/student/dashboard.html` |
| Exam reminder | `/student/exam.html` (if active) |
| Results released | `/student/results.html` |
| New account | `/teacher/dashboard.html` or `/student/dashboard.html` |

---

### 17.3 Database — Notifications

```sql
CREATE TYPE notification_type AS ENUM (
    'CONTENT_SUBMITTED','CONTENT_APPROVED','CONTENT_REJECTED',
    'EXAM_SUBMITTED','EXAM_APPROVED','EXAM_REJECTED',
    'EXAM_SCHEDULED','EXAM_REMINDER','RESULTS_RELEASED',
    'MARKING_COMPLETE','ACCOUNT_CREATED','PASSWORD_RESET',
    'QUESTION_FLAGGED','ANNOUNCEMENT'
);

CREATE TABLE notifications (
    id          UUID              PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID              NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type        notification_type NOT NULL,
    title       VARCHAR(200)      NOT NULL,
    message     TEXT              NOT NULL,
    link        VARCHAR(300),
    entity_type VARCHAR(20),      -- CONTENT / EXAM / ATTEMPT / QUESTION etc.
    entity_id   UUID,
    is_read     BOOLEAN           NOT NULL DEFAULT FALSE,
    read_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notif_user_unread ON notifications(user_id, is_read)
    WHERE is_read = FALSE;
CREATE INDEX idx_notif_user_created ON notifications(user_id, created_at DESC);
```

---

### 17.4 API Endpoints — Notifications

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET  | `/api/v1/notifications` | Any | List notifications (newest first, paginated) |
| GET  | `/api/v1/notifications/unread-count` | Any | Count of unread — for bell badge |
| PUT  | `/api/v1/notifications/{id}/read` | Any (owner) | Mark one as read |
| PUT  | `/api/v1/notifications/read-all` | Any | Mark all as read |
| DELETE | `/api/v1/notifications/{id}` | Any (owner) | Delete a notification |

**GET /notifications response:**
```json
{
  "status": "success",
  "data": [
    {
      "id": "uuid",
      "type": "CONTENT_SUBMITTED",
      "title": "New submission pending",
      "message": "Ms. Clarke submitted 'Comparative Reading' for approval.",
      "link": "/admin/courses.html",
      "entityType": "TOPIC",
      "entityId": "uuid",
      "isRead": false,
      "createdAt": "2025-05-30T10:00:00Z"
    }
  ],
  "pagination": { "page": 0, "size": 20, "total": 5 }
}
```

---

### 17.5 Notification Service — Trigger Points

Create a `NotificationService` with a `send()` method called from other services:

```java
@Service @RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notifRepo;
    private final UserRepository userRepo;

    public void send(UUID recipientId, NotificationType type,
                     String title, String message,
                     String link, String entityType, UUID entityId) {
        Notification n = new Notification();
        n.setUserId(recipientId);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setLink(link);
        n.setEntityType(entityType);
        n.setEntityId(entityId);
        notifRepo.save(n);
    }

    public void sendToAllAdmins(NotificationType type, String title,
                                String message, String link,
                                String entityType, UUID entityId) {
        userRepo.findAllByRole(UserRole.ADMIN).forEach(admin ->
            send(admin.getId(), type, title, message, link, entityType, entityId)
        );
    }
}
```

**Integration example — in ContentApprovalService:**
```java
// When teacher submits content:
notificationService.sendToAllAdmins(
    NotificationType.CONTENT_SUBMITTED,
    "New submission pending",
    teacher.getFullName() + " submitted '" + entity.getName() + "' for approval.",
    "/admin/courses.html",
    entity.getClass().getSimpleName().toUpperCase(),
    entity.getId()
);

// When admin approves:
notificationService.send(
    entity.getCreatedBy().getId(),
    NotificationType.CONTENT_APPROVED,
    "Content approved",
    "'" + entity.getName() + "' has been approved and is now published.",
    "/teacher/submissions.html",
    entity.getClass().getSimpleName().toUpperCase(),
    entity.getId()
);
```

---

### 17.6 Spring Boot Classes — Notifications

```
entity/      Notification.java
enums/       NotificationType.java
repository/  NotificationRepository.java
service/     NotificationService.java       (inject into ContentApproval, ExamService, etc.)
controller/  NotificationController.java
dto/response/NotificationResponse.java
             UnreadCountResponse.java
```

---

## Module 18 — Announcement Board

### Overview
Admin can post broadcast announcements visible to all teachers, all students,
or a specific class. Announcements appear as a pinned banner on dashboards
and in a dedicated "Announcements" page.

---

### 18.1 UI — Announcement Board

**Route:** `admin/announcements.html`, `teacher/announcements.html`, `student/announcements.html`
**Nav item:** "Announcements" (bell or megaphone icon) in all sidebars

#### Admin — Create Announcement

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| Title | Text | Yes | Max 200 chars |
| Body | Rich text / Textarea | Yes | Max 2000 chars |
| Audience | Select | Yes | All Teachers / All Students / Specific Class / Everyone |
| Class (if specific) | Select | Conditional | |
| Priority | Select | No | Normal / Important / Urgent |
| Pin to dashboard | Toggle | No | Shows banner on recipient dashboards |
| Publish at | DateTime | No | Schedule for future; null = publish immediately |
| Expires at | DateTime | No | Auto-hide after this date |

#### Announcement List (all users)

```
┌────────────────────────────────────────────────────┐
│ 📢 Announcements                     [+ New] ← admin│
├────────────────────────────────────────────────────┤
│ ⚠ URGENT  Exam timetable change — 30 May 2025      │
│ Year 10 English exam rescheduled to 2 June.         │
│ Posted by Admin · 2 hours ago                       │
├────────────────────────────────────────────────────┤
│ 📌 PINNED  Welcome to the new term                  │
│ Courses are now available for Year 10 and 11.       │
│ Posted by Admin · 3 days ago                        │
├────────────────────────────────────────────────────┤
│ Normal  Question bank updated                       │
│ 45 new questions added to Year 10 English.          │
│ Posted by Admin · 1 week ago                        │
└────────────────────────────────────────────────────┘
```

Priority styling:
- Urgent = red border + `⚠ URGENT` badge
- Important = amber border + `ℹ IMPORTANT` badge
- Normal = no border highlight

#### Dashboard Banner (pinned announcements)
If `pin_to_dashboard = true`, shows a dismissable banner at top of dashboard:

```
┌─────────────────────────────────────────────────── [✕]─┐
│ ⚠ Exam timetable change — Year 10 English rescheduled   │
│ to 2 June 2025. Please notify students.  [Read more →]  │
└─────────────────────────────────────────────────────────┘
```

---

### 18.2 Database — Announcements

```sql
CREATE TYPE announcement_audience AS ENUM (
    'ALL_TEACHERS','ALL_STUDENTS','SPECIFIC_CLASS','EVERYONE'
);
CREATE TYPE announcement_priority AS ENUM ('NORMAL','IMPORTANT','URGENT');

CREATE TABLE announcements (
    id               UUID                   PRIMARY KEY DEFAULT gen_random_uuid(),
    title            VARCHAR(200)           NOT NULL,
    body             TEXT                   NOT NULL,
    audience         announcement_audience  NOT NULL,
    class_id         UUID                   REFERENCES classes(id),
    priority         announcement_priority  NOT NULL DEFAULT 'NORMAL',
    pin_to_dashboard BOOLEAN                NOT NULL DEFAULT FALSE,
    publish_at       TIMESTAMPTZ            NOT NULL DEFAULT NOW(),
    expires_at       TIMESTAMPTZ,
    is_deleted       BOOLEAN                NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMPTZ,
    created_by       UUID                   NOT NULL REFERENCES users(id),
    created_at       TIMESTAMPTZ            NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ            NOT NULL DEFAULT NOW()
);

-- Track which users have dismissed pinned announcements
CREATE TABLE announcement_dismissals (
    announcement_id UUID NOT NULL REFERENCES announcements(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    dismissed_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (announcement_id, user_id)
);

CREATE INDEX idx_ann_audience   ON announcements(audience)   WHERE is_deleted = FALSE;
CREATE INDEX idx_ann_class      ON announcements(class_id)   WHERE is_deleted = FALSE;
CREATE INDEX idx_ann_publish_at ON announcements(publish_at) WHERE is_deleted = FALSE;
CREATE INDEX idx_ann_pinned     ON announcements(pin_to_dashboard)
    WHERE pin_to_dashboard = TRUE AND is_deleted = FALSE;

CREATE TRIGGER trg_ann_upd
    BEFORE UPDATE ON announcements FOR EACH ROW EXECUTE FUNCTION set_updated_at();
```

---

### 18.3 API Endpoints — Announcements

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | `/api/v1/announcements` | Any | List visible announcements for current user |
| GET | `/api/v1/announcements/pinned` | Any | Active pinned banners not yet dismissed |
| POST | `/api/v1/announcements` | Admin | Create announcement |
| GET | `/api/v1/announcements/{id}` | Any | Single announcement |
| PUT | `/api/v1/announcements/{id}` | Admin | Update |
| DELETE | `/api/v1/announcements/{id}` | Admin | Soft delete |
| POST | `/api/v1/announcements/{id}/dismiss` | Any | Dismiss pinned banner |

**POST /announcements request:**
```json
{
  "title": "Exam timetable change",
  "body": "Year 10 English exam rescheduled to 2 June 2025...",
  "audience": "ALL_TEACHERS",
  "priority": "URGENT",
  "pinToDashboard": true,
  "publishAt": "2025-05-30T08:00:00Z",
  "expiresAt": "2025-06-03T08:00:00Z"
}
```

**Visibility logic (service layer):**
```java
// Teacher sees: ALL_TEACHERS, EVERYONE, SPECIFIC_CLASS (if in that class)
// Student sees: ALL_STUDENTS, EVERYONE, SPECIFIC_CLASS (if enrolled)
// Admin sees:   All announcements
// Only shows if: publish_at <= NOW() AND (expires_at IS NULL OR expires_at > NOW())
```

---

### 18.4 Spring Boot Classes — Announcements

```
entity/      Announcement.java, AnnouncementDismissal.java
enums/       AnnouncementAudience.java, AnnouncementPriority.java
repository/  AnnouncementRepository.java, AnnouncementDismissalRepository.java
service/     AnnouncementService.java
controller/  AnnouncementController.java
dto/request/ AnnouncementRequest.java
dto/response/AnnouncementResponse.java
```

---

## Database Schema Additions — Phase 5

Save as `sql/07-phase5-schema-additions.sql`:

```sql
-- ── Module 17: Notifications ──────────────────────────────────
CREATE TYPE notification_type AS ENUM (
    'CONTENT_SUBMITTED','CONTENT_APPROVED','CONTENT_REJECTED',
    'EXAM_SUBMITTED','EXAM_APPROVED','EXAM_REJECTED',
    'EXAM_SCHEDULED','EXAM_REMINDER','RESULTS_RELEASED',
    'MARKING_COMPLETE','ACCOUNT_CREATED','PASSWORD_RESET',
    'QUESTION_FLAGGED','ANNOUNCEMENT'
);

CREATE TABLE notifications (
    id          UUID              PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID              NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type        notification_type NOT NULL,
    title       VARCHAR(200)      NOT NULL,
    message     TEXT              NOT NULL,
    link        VARCHAR(300),
    entity_type VARCHAR(20),
    entity_id   UUID,
    is_read     BOOLEAN           NOT NULL DEFAULT FALSE,
    read_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notif_user_unread   ON notifications(user_id, is_read)      WHERE is_read = FALSE;
CREATE INDEX idx_notif_user_created  ON notifications(user_id, created_at DESC);

-- ── Module 18: Announcements ──────────────────────────────────
CREATE TYPE announcement_audience AS ENUM (
    'ALL_TEACHERS','ALL_STUDENTS','SPECIFIC_CLASS','EVERYONE'
);
CREATE TYPE announcement_priority AS ENUM ('NORMAL','IMPORTANT','URGENT');

CREATE TABLE announcements (
    id               UUID                  PRIMARY KEY DEFAULT gen_random_uuid(),
    title            VARCHAR(200)          NOT NULL,
    body             TEXT                  NOT NULL,
    audience         announcement_audience NOT NULL,
    class_id         UUID                  REFERENCES classes(id),
    priority         announcement_priority NOT NULL DEFAULT 'NORMAL',
    pin_to_dashboard BOOLEAN               NOT NULL DEFAULT FALSE,
    publish_at       TIMESTAMPTZ           NOT NULL DEFAULT NOW(),
    expires_at       TIMESTAMPTZ,
    is_deleted       BOOLEAN               NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMPTZ,
    created_by       UUID                  NOT NULL REFERENCES users(id),
    created_at       TIMESTAMPTZ           NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ           NOT NULL DEFAULT NOW()
);

CREATE TABLE announcement_dismissals (
    announcement_id UUID NOT NULL REFERENCES announcements(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    dismissed_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (announcement_id, user_id)
);

CREATE INDEX idx_ann_audience    ON announcements(audience)        WHERE is_deleted = FALSE;
CREATE INDEX idx_ann_class       ON announcements(class_id)        WHERE is_deleted = FALSE;
CREATE INDEX idx_ann_publish_at  ON announcements(publish_at)      WHERE is_deleted = FALSE;
CREATE INDEX idx_ann_pinned      ON announcements(pin_to_dashboard) WHERE pin_to_dashboard = TRUE AND is_deleted = FALSE;

CREATE TRIGGER trg_ann_upd
    BEFORE UPDATE ON announcements FOR EACH ROW EXECUTE FUNCTION set_updated_at();
```

---

## Package Structure Additions — Phase 5

```
com.edulearn
├── controller/
│   ├── NotificationController.java
│   └── AnnouncementController.java
├── service/
│   ├── NotificationService.java      (injected into ALL other services that trigger events)
│   └── AnnouncementService.java
├── repository/
│   ├── NotificationRepository.java
│   ├── AnnouncementRepository.java
│   └── AnnouncementDismissalRepository.java
├── entity/
│   ├── Notification.java
│   ├── Announcement.java
│   └── AnnouncementDismissal.java
├── enums/
│   ├── NotificationType.java
│   ├── AnnouncementAudience.java
│   └── AnnouncementPriority.java
└── dto/
    ├── request/  AnnouncementRequest.java
    └── response/ NotificationResponse.java
                  UnreadCountResponse.java
                  AnnouncementResponse.java
```

---

## Integration Points — NotificationService Must Be Called From

| Service | Event | Notification sent |
|---------|-------|------------------|
| `ContentApprovalService.submit()` | Teacher submits | `CONTENT_SUBMITTED` → all admins |
| `ContentApprovalService.approve()` | Admin approves | `CONTENT_APPROVED` → creator |
| `ContentApprovalService.reject()` | Admin rejects | `CONTENT_REJECTED` → creator |
| `ExamService.submit()` | Teacher submits exam | `EXAM_SUBMITTED` → all admins |
| `ExamService.approve()` | Admin approves exam | `EXAM_APPROVED` → creator |
| `ExamService.reject()` | Admin rejects exam | `EXAM_REJECTED` → creator |
| `ScheduleService.create()` | Exam scheduled | `EXAM_SCHEDULED` → class students |
| `MarkingService.finalise()` | Marking complete | `RESULTS_RELEASED` → student |
| `ProfileService.create()` | New user created | `ACCOUNT_CREATED` → new user |
| `QuestionAnalyticsService.flag()` | Question flagged | `QUESTION_FLAGGED` → creator |

---

## Frontend Pages — Phase 5

```
static/
├── admin/
│   └── announcements.html    # Create + manage announcements
├── teacher/
│   └── announcements.html    # View announcements
└── student/
    └── announcements.html    # View announcements
```

Notification bell panel is already built in `admin-dashboard.html`.
Extend the same component to `teacher-dashboard.html` and `student/dashboard.html`
by wiring to `GET /api/v1/notifications` and `GET /api/v1/notifications/unread-count`.

---

## Files Added in Phase 5

| File | Change |
|------|--------|
| `docs/11-phase5-communications.md` | **This file — new** |
| `sql/07-phase5-schema-additions.sql` | `notifications` + `announcements` + `announcement_dismissals` tables |
| `CLAUDE.md` | Phase 5 classes, APIs, integration points, build steps |
