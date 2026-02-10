@echo off
REM ChaOffice Release Build Script for Windows
REM This script creates a distributable package of the application

echo ========================================
echo ChaOffice Release Build Script
echo ========================================
echo.

REM Step 1: Clean previous builds
echo [1/5] Cleaning previous builds...
call mvn clean
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Clean failed!
    pause
    exit /b 1
)
echo.

REM Step 2: Run tests
echo [2/5] Running tests...
call mvn test
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Tests failed!
    pause
    exit /b 1
)
echo.

REM Step 3: Package application
echo [3/5] Packaging application (Fat JAR)...
call mvn package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Packaging failed!
    pause
    exit /b 1
)
echo.

REM Step 4: Create jpackage runtime image (alternative to jlink)
echo [4/5] Creating portable application with jpackage...
jpackage --input target --name "ChaOffice" --main-jar chaoffice-1.0.0.jar --main-class org.chaos.office.ChaOfficeApplication --type app-image --app-version 1.0.0 --vendor "ChaOffice" --description "ChaOffice Parts Inventory Management System"
if %ERRORLEVEL% NEQ 0 (
    echo WARNING: jpackage failed. Continuing with JAR only...
    echo You can still use the JAR file: target\chaoffice-1.0.0.jar
)
echo.

REM Step 5: Create distribution zip
echo [5/5] Creating distribution package...
cd target
if exist "ChaOffice-1.0.0-windows.zip" del "ChaOffice-1.0.0-windows.zip"

REM Check if jpackage created the ChaOffice folder
if exist "..\ChaOffice" (
    echo Packaging jpackage output...
    cd ..
    tar -a -c -f target\ChaOffice-1.0.0-windows.zip ChaOffice
    cd target
) else (
    echo jpackage folder not found, packaging JAR only...
    tar -a -c -f ChaOffice-1.0.0-windows.zip chaoffice-1.0.0.jar
)
cd ..
echo.

echo ========================================
echo Build completed successfully!
echo ========================================
echo.
echo Output files:
echo   - Fat JAR: target\chaoffice-1.0.0.jar
if exist "ChaOffice" (
    echo   - Portable App: ChaOffice\ChaOffice.exe
    echo   - Distribution: target\ChaOffice-1.0.0-windows.zip
) else (
    echo   - Distribution: target\ChaOffice-1.0.0-windows.zip (JAR only)
)
echo.
echo To run the application:
if exist "ChaOffice" (
    echo   ChaOffice\ChaOffice.exe
    echo   OR
)
echo   java -jar target\chaoffice-1.0.0.jar
echo.
pause
