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
- **Admin dashboard**: http://localhost:8080/admin/dashboard.html
- **Admin courses** (tree + CRUD): http://localhost:8080/admin/courses.html
- **Teacher dashboard**: http://localhost:8080/teacher/dashboard.html
- **Teacher courses** (draft + submit): http://localhost:8080/teacher/courses.html
- **Teacher submissions**: http://localhost:8080/teacher/submissions.html

## API Base
`http://localhost:8080/api/v1`

## Stop
```
# Stop Spring Boot: Ctrl+C
# Stop database:
docker-compose down
```
