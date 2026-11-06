@echo off
setlocal
cd /d "%~dp0"

java -version 2>&1 | findstr /C:"25." >nul
if errorlevel 1 (
  echo [ERROR] JDK 25 is required. Install JDK 25 and set JAVA_HOME/PATH.
  pause
  exit /b 1
)

set "GRADLE_BAT="
if exist "C:\Gradle\gradle-9.8.1\bin\gradle.bat" (
  set "GRADLE_BAT=C:\Gradle\gradle-9.8.1\bin\gradle.bat"
) else (
  for /f "delims=" %%G in ('where gradle 2^>nul') do if not defined GRADLE_BAT set "GRADLE_BAT=%%G"
)

if not defined GRADLE_BAT (
  echo [ERROR] Gradle 9.8.1 was not found.
  echo Install Gradle 9.8.1 or add its bin folder to PATH.
  pause
  exit /b 1
)

echo Using Gradle: %GRADLE_BAT%
call "%GRADLE_BAT%" clean build --console=plain
if errorlevel 1 (
  echo.
  echo [ERROR] Build failed. Copy the first '* What went wrong:' section when asking for help.
  pause
  exit /b 1
)

echo.
echo [SUCCESS] Build complete:
echo %~dp0build\libs\redstone-transportation-1.12.0.jar
pause
