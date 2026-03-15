@echo off
REM Build Android .so files using Podman
REM Usage: build-android.bat
REM Output: android/app/src/main/jniLibs/{arm64-v8a,armeabi-v7a,x86_64}/libsweet_lab_core.so

set IMAGE_NAME=sweetlab-android-builder
set JNILIBS_DIR=%~dp0android\app\src\main\jniLibs

echo [1/4] Building container image (first time takes a few minutes)...
podman build -t %IMAGE_NAME% -f android-build/Containerfile .
if %ERRORLEVEL% neq 0 (
    echo ERROR: Container image build failed.
    exit /b 1
)

echo [2/4] Cross-compiling Rust core and generating Kotlin bindings...
podman run --rm ^
    -v "%~dp0:/build:Z" ^
    -v "%JNILIBS_DIR%:/output:Z" ^
    %IMAGE_NAME% ^
    bash /build/android-build/build-android.sh
if %ERRORLEVEL% neq 0 (
    echo ERROR: Cross-compilation failed.
    exit /b 1
)

echo [3/4] Verifying output...
if exist "%JNILIBS_DIR%\arm64-v8a\libsweet_lab_core.so" (
    echo   arm64-v8a: OK
) else (
    echo   arm64-v8a: MISSING
)
if exist "%JNILIBS_DIR%\armeabi-v7a\libsweet_lab_core.so" (
    echo   armeabi-v7a: OK
) else (
    echo   armeabi-v7a: MISSING
)
if exist "%JNILIBS_DIR%\x86_64\libsweet_lab_core.so" (
    echo   x86_64: OK
) else (
    echo   x86_64: MISSING
)

set BINDINGS_DIR=%~dp0android\app\src\main\kotlin\org\sweetlab\core
if exist "%BINDINGS_DIR%\sweet_lab_core.kt" (
    echo   Kotlin bindings: OK
) else (
    echo   Kotlin bindings: MISSING
)

echo [4/4] Done! .so files are in android\app\src\main\jniLibs\
echo Kotlin bindings are in android\app\src\main\kotlin\org\sweetlab\core\
