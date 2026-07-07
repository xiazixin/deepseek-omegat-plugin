@echo off
REM Build All script for DeepSeek OmegaT Plugin (Windows)
REM This script performs a complete build: clean, build, and test

echo ========================================
echo DeepSeek OmegaT Plugin (Dev) - Complete Build
echo ========================================
echo.

if not exist "gradlew.bat" (
    echo Error: gradlew.bat not found. Please ensure you have Gradle wrapper set up.
    exit /b 1
)

echo Step 1: Cleaning previous build artifacts...
call gradlew.bat clean
if %errorlevel% neq 0 (
    echo Clean failed with error code %errorlevel%
    exit /b %errorlevel%
)
echo Clean completed successfully!
echo.

echo Step 2: Building the project...
call gradlew.bat build
if %errorlevel% neq 0 (
    echo Build failed with error code %errorlevel%
    exit /b %errorlevel%
)
echo Build completed successfully!
echo.

echo Step 3: Running tests...
call gradlew.bat test
if %errorlevel% neq 0 (
    echo Tests failed with error code %errorlevel%
    exit /b %errorlevel%
)
echo Tests completed successfully!
echo.

echo ========================================
echo Build process completed successfully!
echo Output files are in the 'build' directory.
echo ========================================
pause
