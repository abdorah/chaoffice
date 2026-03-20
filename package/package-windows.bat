@echo off
REM ============================================================
REM ChaOffice - Windows Installer Builder
REM Requires: JDK 17+, WiX Toolset 3.x (for .msi) or InnoSetup
REM ============================================================

echo ========================================
echo  ChaOffice Windows Packaging
echo ========================================

REM 1. Build the fat JAR
echo [1/3] Building fat JAR...
cd /d "%~dp0\.."
call mvn clean package -DskipTests -q
if %ERRORLEVEL% neq 0 (
    echo ERROR: Maven build failed!
    exit /b 1
)

REM 2. Set up staging directory
echo [2/3] Preparing staging directory...
set APP_VERSION=1.0.0
set INPUT_DIR=target\jpackage-input
set OUTPUT_DIR=target\installer

if exist "%INPUT_DIR%" rmdir /s /q "%INPUT_DIR%"
mkdir "%INPUT_DIR%"

REM Copy fat JAR to input directory
copy "target\chaoffice-%APP_VERSION%-fat.jar" "%INPUT_DIR%\chaoffice.jar"

REM 3. Run jpackage
echo [3/3] Running jpackage...
jpackage ^
    --type exe ^
    --name "ChaOffice" ^
    --app-version "%APP_VERSION%" ^
    --vendor "Chaos Office" ^
    --description "Parts Inventory Management System" ^
    --icon "package\icon\chaoffice.ico" ^
    --input "%INPUT_DIR%" ^
    --main-jar "chaoffice.jar" ^
    --main-class "org.chaos.office.Launcher" ^
    --dest "%OUTPUT_DIR%" ^
    --java-options "-Xmx512m" ^
    --java-options "--add-opens=java.base/java.lang=ALL-UNNAMED" ^
    --java-options "--add-opens=java.base/java.io=ALL-UNNAMED" ^
    --java-options "--add-opens=java.base/java.util=ALL-UNNAMED" ^
    --win-dir-chooser ^
    --win-shortcut ^
    --win-shortcut-prompt ^
    --win-menu ^
    --win-menu-group "ChaOffice" ^
    --win-per-user-install

if %ERRORLEVEL% neq 0 (
    echo ERROR: jpackage failed!
    exit /b 1
)

echo ========================================
echo  SUCCESS! Installer created in:
echo  %OUTPUT_DIR%
echo ========================================
pause
