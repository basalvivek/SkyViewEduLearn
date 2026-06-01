@echo off
title EduLearn — Start
color 0A
cls

echo.
echo  =========================================
echo    EduLearn Platform  ^|  Starting...
echo  =========================================
echo.

cd /d "%~dp0"

:: Step 1 — Start PostgreSQL
echo  [1/3]  Starting PostgreSQL database...
docker-compose up -d >nul 2>&1
if errorlevel 1 (
    echo.
    echo  ERROR: Could not start Docker.
    echo  Make sure Docker Desktop is running, then try again.
    echo.
    pause
    exit /b 1
)
echo         PostgreSQL container started on port 5433.
echo.

:: Step 2 — Wait for DB to accept connections
echo  [2/3]  Waiting for database to be ready...
:waitdb
docker exec edulearn-postgres pg_isready -U edulearn_user -d edulearn >nul 2>&1
if errorlevel 1 (
    timeout /t 2 /nobreak >nul
    goto waitdb
)
echo         Database is ready!
echo.

:: Step 3 — Launch Spring Boot in a new window
echo  [3/3]  Launching Spring Boot server...
start "EduLearn Server" cmd /k "cd /d "%~dp0" && color 0B && echo. && echo  EduLearn Server is starting... && echo  Open: http://localhost:8080/login.html && echo. && mvn spring-boot:run"

:: Wait a few seconds for Spring Boot to start, then open browser
echo         Waiting for server to start (this takes ~15 seconds)...
timeout /t 18 /nobreak >nul

:: Open browser
start "" "http://localhost:8080/login.html"

echo.
echo  =========================================
echo    EduLearn is running!
echo.
echo    URL:     http://localhost:8080/login.html
echo    Admin:   admin@localhost  /  Admin@123
echo    Teacher: teacher@localhost  /  Teacher@123
echo.
echo    To stop the server, run:  stop.bat
echo  =========================================
echo.
pause
