@echo off
echo Stopping EducationPro (port 9090)...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":9090 "') do (
    echo Killing PID %%a
    taskkill /PID %%a /F
    goto :done
)
echo Nothing running on port 9090.
:done
