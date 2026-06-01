@echo off
title EduLearn — Stop
color 0C
cls

echo.
echo  =========================================
echo    EduLearn Platform  ^|  Stopping...
echo  =========================================
echo.

cd /d "%~dp0"

:: Step 1 — Kill the Spring Boot process on port 8080
echo  [1/2]  Stopping Spring Boot server...
for /f "tokens=5" %%P in ('netstat -ano ^| findstr "LISTENING" ^| findstr ":8080 "') do (
    taskkill /F /PID %%P >nul 2>&1
)
:: Also close the EduLearn Server window if open
taskkill /FI "WINDOWTITLE eq EduLearn Server*" /F >nul 2>&1
echo         Spring Boot server stopped.
echo.

:: Step 2 — Stop PostgreSQL container
echo  [2/2]  Stopping PostgreSQL database...
docker-compose stop >nul 2>&1
echo         Database stopped.
echo.

echo  =========================================
echo    EduLearn Platform stopped.
echo.
echo    To start again, run:  start.bat
echo  =========================================
echo.
pause
