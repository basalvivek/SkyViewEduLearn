---
name: project-edulearn
description: EduLearn platform — Spring Boot 3.2.5 + PostgreSQL + Vanilla JS education platform built and running
metadata:
  type: project
---

EduLearn is a fully built Spring Boot 3.2.5 web app at `C:\Users\VB\Downloads\00_AI_Projects\Claude\SkyViewEduLearn\`.

**Why:** User asked to build the web app described in CLAUDE.md.

**Key facts:**
- PostgreSQL runs in Docker on port **5433** (not 5432 — that was taken by skyview-postgres)
- Spring Boot runs on localhost:8080
- DB: `ddl-auto=none` (schema pre-created via SQL init scripts)
- Uses Lombok, Hibernate 6.4, jjwt 0.12.3
- `@JdbcTypeCode(SqlTypes.NAMED_ENUM)` on all PostgreSQL native enum columns (required for Hibernate 6.x + PostgreSQL enum types)
- `@Transactional(readOnly = true)` on ApprovalController class (lazy loading fix)

**Default credentials:**
- Admin: admin@localhost / Admin@123
- Teacher: teacher@localhost / Teacher@123

**Phase 1 (completed 2026-06-01):**
- Tables added: `organisation_settings` (singleton), `notifications`
- New enums: `OrgType`
- New entities: `OrgSettings`, `Notification`
- New repos: `OrgSettingsRepository`, `NotificationRepository`
- New services: `OrgSettingsService`, `NotificationService`
- New controllers: `OrgSettingsController` (PUT /branding/legal/address/contacts/academic, POST /logo), `NotificationController`
- Dashboard updated with topbar (user pill + notification bell + dropdown), Org Settings page
- `api.js` updated with `noJson` option for multipart file uploads
- **Known quirk:** Hibernate naming: `poc1Name` → `poc1name` (digit before capital doesn't get underscore), fixed with explicit `@Column(name="poc1_name")` etc.

**Phase 2 (completed 2026-06-01):**
- SQL: ALTER users table (displayName, avatarPath, jobTitle, department, bio, phone, lastLoginAt), teacher_submissions view
- New DTOs: ProfileUpdateRequest, PasswordChangeRequest, ProfileResponse, SubmissionResponse (unified for both ApprovalController and SubmissionController), SubmissionSummaryResponse
- New services: ProfileService (profile CRUD + avatar upload), SubmissionService (@Transactional readOnly — needed because of lazy Category/Subject/Topic lazy loads)
- New controllers: ProfileController (/my/profile GET/PUT/POST avatar, /my/profile/password PUT), SubmissionController (/my/submissions, /my/submissions/summary)
- Removed duplicate /my/submissions from ApprovalController (superseded by SubmissionController)
- Repos: added findByCreatedByOrderByUpdatedAtDesc + countByCreatedBy + countByStatusAndCreatedBy list methods to all four content repos
- Frontend: teacher/dashboard.html (SPA: dashboard + profile page, user pill topbar), teacher/courses.html (nav/topbar updated, all tree logic preserved), teacher/submissions.html (filter bar + paginated table + summary chips)
- Avatar uploads: /uploads/avatars/ served via filesystem resource handler, permitted in SecurityConfig

**How to apply:** Run `docker-compose up -d` then `mvn spring-boot:run` from project root.
