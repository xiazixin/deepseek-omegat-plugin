@echo off
REM Build script for DeepSeek OmegaT Plugin (Windows)
REM This script builds the plugin using Gradle

echo Building DeepSeek OmegaT Plugin (Dev)...
echo.

REM Check if gradlew.bat exists
if not exist "gradlew.bat" (
    echo Error: gradlew.bat not found. Please ensure you have Gradle wrapper set up.
    echo Run: gradle wrapper
    exit /b 1
)

REM Run Gradle build
call gradlew.bat build

if %errorlevel% neq 0 (
    echo.
    echo Build failed with error code %errorlevel%
    exit /b %errorlevel%
)

echo.
echo Build completed successfully!
echo Output files are in the 'build' directory.
pause
