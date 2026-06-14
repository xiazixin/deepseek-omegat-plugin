@echo off
REM Clean script for DeepSeek OmegaT Plugin (Windows)
REM This script cleans the build artifacts using Gradle

echo Cleaning DeepSeek OmegaT Plugin build artifacts...
echo.

if not exist "gradlew.bat" (
    echo Error: gradlew.bat not found. Please ensure you have Gradle wrapper set up.
    exit /b 1
)

call gradlew.bat clean

if %errorlevel% neq 0 (
    echo.
    echo Clean failed with error code %errorlevel%
    exit /b %errorlevel%
)

echo.
echo Clean completed successfully!
pause
