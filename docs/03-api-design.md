# EduLearn Platform — API Design Reference

Base URL: `http://localhost:8080/api/v1`

## Standard Response Envelope

```json
{
  "status": "success | error",
  "message": "Human-readable message",
  "data": { },
  "pagination": {
    "page": 0, "size": 20, "total": 100, "totalPages": 5
  }
}
```

---

## Auth Endpoints

| Method | Path | Role | Description |
|--------|------|------|-------------|
| POST | `/auth/login` | Public | Returns JWT token |
| POST | `/auth/logout` | Any | Invalidates token |
| GET  | `/auth/me` | Any | Current user profile |

**Login request:**
```json
{ "email": "admin@localhost", "password": "Admin@123" }
```
**Login response:**
```json
{ "token": "eyJ...", "role": "ADMIN", "name": "Platform Admin" }
```

---

## Content Hierarchy Endpoints

### Categories

| Method | Path | Role | Notes |
|--------|------|------|-------|
| GET    | `/categories` | Any | List approved; Admin sees all |
| POST   | `/categories` | Admin, Teacher | Admin → APPROVED; Teacher → PENDING |
| GET    | `/categories/{id}` | Any | Single category |
| PUT    | `/categories/{id}` | Owner, Admin | Teacher edit → re-triggers PENDING |
| DELETE | `/categories/{id}` | Admin | Soft delete |
| PUT    | `/categories/{id}/approve` | Admin | Approve pending |
| PUT    | `/categories/{id}/reject` | Admin | Reject with note |

### Subjects

| Method | Path | Role | Notes |
|--------|------|------|-------|
| GET    | `/categories/{cid}/subjects` | Any | Scoped to category |
| POST   | `/categories/{cid}/subjects` | Admin, Teacher | |
| GET    | `/subjects/{id}` | Any | |
| PUT    | `/subjects/{id}` | Owner, Admin | |
| DELETE | `/subjects/{id}` | Admin | Soft delete |
| PUT    | `/subjects/{id}/approve` | Admin | |
| PUT    | `/subjects/{id}/reject` | Admin | |

### Topics

| Method | Path | Role | Notes |
|--------|------|------|-------|
| GET    | `/subjects/{sid}/topics` | Any | |
| POST   | `/subjects/{sid}/topics` | Admin, Teacher | |
| GET    | `/topics/{id}` | Any | |
| PUT    | `/topics/{id}` | Owner, Admin | |
| DELETE | `/topics/{id}` | Admin | Soft delete |
| PUT    | `/topics/{id}/approve` | Admin | |
| PUT    | `/topics/{id}/reject` | Admin | |

### Questions

| Method | Path | Role | Notes |
|--------|------|------|-------|
| GET    | `/topics/{tid}/questions` | Any | |
| POST   | `/topics/{tid}/questions` | Admin, Teacher | |
| GET    | `/questions/{id}` | Any | |
| PUT    | `/questions/{id}` | Owner, Admin | |
| DELETE | `/questions/{id}` | Admin | Soft delete |
| PUT    | `/questions/{id}/approve` | Admin | |
| PUT    | `/questions/{id}/reject` | Admin | |

---

## Full Tree Endpoint

```
GET /categories/{id}/tree
```
Returns the full nested hierarchy (category → subjects → topics → questions).
Used to populate the sidebar tree in the UI.

---

## Approval Queue (Admin only)

```
GET /approvals/pending?type=TOPIC&page=0&size=20
```

Returns all pending items across all entity types, sorted by `created_at` ascending.

---

## Teacher Submissions (Teacher only)

```
GET /my/submissions?status=PENDING
```

Returns all content submitted by the logged-in teacher, filterable by status.

---

## Sample Question Payloads

**MCQ Single:**
```json
{
  "questionText": "What is the capital of France?",
  "questionType": "MCQ_SINGLE",
  "marks": 1,
  "options": [
    { "optionText": "Berlin", "isCorrect": false },
    { "optionText": "Paris",  "isCorrect": true  },
    { "optionText": "Rome",   "isCorrect": false }
  ]
}
```

**Code question:**
```json
{
  "questionText": "Write a function that returns the sum of two numbers.",
  "questionType": "CODE",
  "marks": 3,
  "codeLang": "PYTHON",
  "starterCode": "def add(a, b):\n    pass",
  "expectedOutput": "5"
}
```

**Image-based:**
```json
{
  "questionText": "Look at the diagram. What process does arrow B represent?",
  "questionType": "IMAGE_BASED",
  "marks": 2,
  "imageUrl": "/static/images/photosynthesis.png",
  "imageAltText": "Diagram of photosynthesis process"
}
```

---

## HTTP Status Codes Used

| Code | Meaning |
|------|---------|
| 200 | OK |
| 201 | Created |
| 400 | Validation error |
| 401 | Unauthenticated |
| 403 | Forbidden (wrong role) |
| 404 | Not found |
| 409 | Conflict (e.g. duplicate name) |
| 500 | Server error |

---

## Rejection Handling

When admin rejects content, `rejection_reason` is stored and returned in the response.

**Teacher sees rejected items in:**
```
GET /my/submissions?status=REJECTED
```

**Reject payload:**
```json
PUT /topics/uuid/reject
{ "note": "Topic name too vague, please be more specific." }
```

**Teacher response includes:**
```json
{
  "id": "uuid",
  "name": "Comparative Reading",
  "status": "REJECTED",
  "rejectionReason": "Topic name too vague, please be more specific.",
  "updatedAt": "2025-05-31T10:00:00Z"
}
```

Teacher can edit and re-submit a REJECTED item (status returns to PENDING).
