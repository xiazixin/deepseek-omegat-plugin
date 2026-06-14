@echo off
REM Test script for DeepSeek OmegaT Plugin (Windows)
REM This script runs the unit tests using Gradle

echo Running tests for DeepSeek OmegaT Plugin...
echo.

if not exist "gradlew.bat" (
    echo Error: gradlew.bat not found. Please ensure you have Gradle wrapper set up.
    exit /b 1
)

call gradlew.bat test

if %errorlevel% neq 0 (
    echo.
    echo Tests failed with error code %errorlevel%
    exit /b %errorlevel%
)

echo.
echo Tests completed successfully!
pause
