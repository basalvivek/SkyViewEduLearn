# EduLearn Platform — Requirements & Architecture

## 1. Overview

EduLearn is a Spring Boot + PostgreSQL education platform with role-based course management, a tree-structured content hierarchy, and a teacher-to-admin approval workflow.

---

## 2. User Roles

| Role | Capabilities |
|------|-------------|
| Admin | Full CRUD on all content, approve/reject teacher submissions, manage users |
| Teacher | Create/edit own content, submit for approval, view approval status |
| Student | (Future) Browse approved courses, attempt questions |

---

## 3. Content Hierarchy

```
Category (e.g. Year 10)
  └── Subject (e.g. English Language)
        └── Topic (e.g. Non-Fiction & Creative Writing)
              └── Question (MCQ / True-False / Short Answer / Essay / Code / Image)
```

---

## 4. Approval Workflow

```
Teacher creates/edits content
        │
        ▼
   Status: DRAFT
        │  (Teacher clicks Submit)
        ▼
   Status: PENDING  ──── notified to Admin
        │
   Admin reviews
   ┌────┴────┐
APPROVED   REJECTED
        │
  Visible to students
```

**Rules:**
- Admin-created content is auto-approved.
- Teacher edits to approved content re-triggers pending status.
- Admin-owned nodes are read-only for teachers (teachers may add children inside them).

---

## 5. Question Types

| Type | Extra Fields |
|------|-------------|
| MCQ_SINGLE | options[], correct_option_id |
| MCQ_MULTIPLE | options[], correct_option_ids[] |
| TRUE_FALSE | correct_answer (boolean) |
| SHORT_ANSWER | model_answer |
| ESSAY | marking_scheme |
| CODE | code_language, starter_code, expected_output |
| IMAGE_BASED | image_url, image_alt_text |

---

## 6. Technology Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3.x, Java 17+ |
| Database | PostgreSQL 15+ |
| ORM | Spring Data JPA + Hibernate |
| Auth | Spring Security + JWT |
| API | RESTful JSON (versioned `/api/v1/`) |
| Frontend | HTML5 / CSS3 / Vanilla JS (served from `resources/static/`) |

---

## 7. Hosting

This application is hosted **locally on localhost** only.

| Config | Value |
|--------|-------|
| Host | `localhost` |
| Default port | `8080` |
| Base URL | `http://localhost:8080` |
| API base | `http://localhost:8080/api/v1/` |
| Static UI | `http://localhost:8080/` |

**Spring Boot `application.properties` baseline:**
```properties
server.port=8080
server.address=127.0.0.1
spring.datasource.url=jdbc:postgresql://localhost:5432/edulearn
spring.datasource.username=edulearn_user
spring.datasource.password=changeme
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
```

No SSL, no reverse proxy, no cloud deployment required. CORS restrictions can be relaxed to `localhost` origins only.

---

## 8. Non-Functional Requirements

- All tables use UUID primary keys.
- Soft delete on all entities (`is_deleted`, `deleted_at`).
- Audit fields on all tables: `created_by`, `created_at`, `updated_by`, `updated_at`.
- Role-based endpoint security via Spring Security.
- Paginated list endpoints (default page size: 20).
- All API responses use a standard wrapper: `{ status, message, data, pagination? }`.
