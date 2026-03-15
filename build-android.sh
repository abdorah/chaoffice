#!/bin/bash
set -euo pipefail

# Build Android .so files using Podman
# Usage: ./build-android.sh
# Output: android/app/src/main/jniLibs/{arm64-v8a,armeabi-v7a,x86_64}/libsweet_lab_core.so

# Disable Git Bash POSIX path conversion (causes /build -> C:\Program Files\Git\build)
export MSYS_NO_PATHCONV=1

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
IMAGE_NAME="sweetlab-android-builder"
JNILIBS_DIR="${SCRIPT_DIR}/android/app/src/main/jniLibs"

# Ensure jniLibs output directories exist before mounting
mkdir -p "${JNILIBS_DIR}/arm64-v8a"
mkdir -p "${JNILIBS_DIR}/armeabi-v7a"
mkdir -p "${JNILIBS_DIR}/x86_64"

echo "[1/4] Building container image (first time takes a few minutes)..."
podman build -t "$IMAGE_NAME" -f android-build/Containerfile .

echo "[2/4] Cross-compiling Rust core and generating Kotlin bindings..."
podman run --rm \
    -v "${SCRIPT_DIR}:/build:Z" \
    -v "${JNILIBS_DIR}:/output:Z" \
    "$IMAGE_NAME" \
    bash /build/android-build/build-android.sh

echo "[3/4] Verifying output..."
for abi in arm64-v8a armeabi-v7a x86_64; do
    if [ -f "${JNILIBS_DIR}/${abi}/libsweet_lab_core.so" ]; then
        echo "  ${abi}: OK ($(du -h "${JNILIBS_DIR}/${abi}/libsweet_lab_core.so" | cut -f1))"
    else
        echo "  ${abi}: MISSING"
    fi
done

BINDINGS_DIR="${SCRIPT_DIR}/android/app/src/main/kotlin/org/sweetlab/core"
if [ -f "${BINDINGS_DIR}/sweet_lab_core.kt" ]; then
    echo "  Kotlin bindings: OK"
else
    echo "  Kotlin bindings: MISSING"
fi

echo "[4/4] Done! .so files are in android/app/src/main/jniLibs/"
echo "Kotlin bindings are in android/app/src/main/kotlin/org/sweetlab/core/"
echo "You can now build the APK:"
echo "  cd android && ./gradlew assembleDebug"
