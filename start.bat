@echo off
title EducationPro — Starting...

echo.
echo  ============================================
echo   EducationPro Web Application
echo   URL: http://localhost:9090/auth/login
echo  ============================================
echo.
echo  Admin Login:
echo    Email   : admin@educationpro.com
echo    Password: Admin@1234
echo.
echo  Starting application...
echo.

cd /d "%~dp0"
mvn spring-boot:run

pause
