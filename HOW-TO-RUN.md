# EduLearn Platform — Quick Start

## Prerequisites
- Docker Desktop running
- Java 17+
- Maven

## 1. Start Database (first time only)
```
docker-compose up -d
```
PostgreSQL starts on **localhost:5433** (not 5432, which may be in use).

## 2. Build & Run
```
mvn spring-boot:run
```
App starts on **http://localhost:8080**

## 3. Open Browser
**http://localhost:8080/login.html**

## Default Accounts
| Role | Email | Password |
|------|-------|----------|
| Admin | admin@localhost | Admin@123 |
| Teacher | teacher@localhost | Teacher@123 |

## Pages

### Admin
- **Dashboard**: http://localhost:8080/admin/dashboard.html
- **Courses** (tree + CRUD): http://localhost:8080/admin/courses.html
- **Exams** (builder + approval queue): http://localhost:8080/admin/exams.html
- **Schedules** (class mgmt + scheduling): http://localhost:8080/admin/schedules.html
- **Students** (list + add + bulk import): http://localhost:8080/admin/students.html

### Teacher
- **Dashboard**: http://localhost:8080/teacher/dashboard.html
- **Courses** (draft + submit): http://localhost:8080/teacher/courses.html
- **Submissions**: http://localhost:8080/teacher/submissions.html
- **Exams** (builder, submit for approval): http://localhost:8080/teacher/exams.html
- **Schedules**: http://localhost:8080/teacher/schedules.html
- **Marking** (queue + per-student marking): http://localhost:8080/teacher/marking.html

### Student
- **Dashboard** (stats, upcoming exams, recent results): http://localhost:8080/student/dashboard.html
- **My Exams** (active/upcoming/completed tabs): http://localhost:8080/student/exams.html
- **Exam** (attempt screen with timer): http://localhost:8080/student/exam.html
- **Change Password** (first-login forced change): http://localhost:8080/student/change-password.html

### Student account
Students are created by Admin in the Students page. They must change their temporary password on first login.
To create a test student: go to Admin → Students → Add Student.

## API Base
`http://localhost:8080/api/v1`

## Stop
```
# Stop Spring Boot: Ctrl+C
# Stop database:
docker-compose down
```
