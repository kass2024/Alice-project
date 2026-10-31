@echo off
cd /d "%~dp0"
setlocal EnableExtensions
if exist "%~dp0backend\gemini-key.local" (
  set /p GEMINI_API_KEY=<"%~dp0backend\gemini-key.local"
)
echo Starting the school database...
C:\xampp7\mysql\bin\mysql.exe -u root -P 3307 -e "SELECT 1" >nul 2>&1
if errorlevel 1 (
  echo Opening MariaDB from XAMPP...
  start "MariaDB" /MIN C:\xampp7\mysql\bin\mysqld.exe --defaults-file=C:\xampp7\mysql\bin\my.ini --standalone
  timeout /t 6 >nul
)
echo Starting the API and the website. Leave both windows open.
if exist "%~dp0.tools\apache-maven-3.9.9\bin\mvn.cmd" (
  start "Smart Teacher API" cmd /k "cd /d "%~dp0backend" && set GEMINI_API_KEY=%GEMINI_API_KEY% && ..\.tools\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run"
) else (
  start "Smart Teacher API" cmd /k "cd /d "%~dp0backend" && set GEMINI_API_KEY=%GEMINI_API_KEY% && mvn spring-boot:run"
)
start "Smart Teacher UI" cmd /k "cd /d "%~dp0frontend" && npm run dev"
echo.
echo When both windows are ready, open http://localhost:5173
echo Login: admin    password: password123
echo Full guide: setup-guide.html
echo.
pause
