@echo off
echo Launching Privacy-Focused Email Agent Development Environment...

echo Starting Backend Server...
REM Use start "Title" cmd /k "commands" to keep window open
start "Backend" cmd /k "cd /d "%~dp0" && echo Activating venv... && venv\Scripts\activate && echo Starting python backend... && python run_backend.py"

echo Starting Frontend Application...
REM Give the backend a moment to initialize
timeout /t 3 > nul
REM Use start "Title" cmd /k "commands" to keep window open
start "Frontend" cmd /k "cd /d "%~dp0frontend" && echo Starting frontend... && mvn org.openjfx:javafx-maven-plugin:0.0.8:run"

echo Development environment launched.
echo Close this window to exit. The backend and frontend windows will remain open.
