#!/bin/bash
set -euo pipefail

# Build Sweet Lab Core for all Android targets
# This script runs INSIDE the container

TOOLCHAIN="/opt/android-sdk/ndk/r27c/toolchains/llvm/prebuilt/linux-x86_64"

TARGETS=(
    "aarch64-linux-android:arm64-v8a:aarch64-linux-android26"
    "armv7-linux-androideabi:armeabi-v7a:armv7a-linux-androideabi26"
    "x86_64-linux-android:x86_64:x86_64-linux-android26"
)

OUTPUT_DIR="/output"
mkdir -p "$OUTPUT_DIR"

# Override the project's .cargo/config.toml which has Windows paths
# by writing a container-local config that takes precedence
mkdir -p /build/.cargo
cat > /build/.cargo/config.toml <<EOF
[target.aarch64-linux-android]
ar = "${TOOLCHAIN}/bin/llvm-ar"
linker = "${TOOLCHAIN}/bin/aarch64-linux-android26-clang"

[target.armv7-linux-androideabi]
ar = "${TOOLCHAIN}/bin/llvm-ar"
linker = "${TOOLCHAIN}/bin/armv7a-linux-androideabi26-clang"

[target.x86_64-linux-android]
ar = "${TOOLCHAIN}/bin/llvm-ar"
linker = "${TOOLCHAIN}/bin/x86_64-linux-android26-clang"
EOF

for entry in "${TARGETS[@]}"; do
    IFS=':' read -r rust_target abi ndk_prefix <<< "$entry"

    echo "========================================="
    echo "Building for $rust_target ($abi)"
    echo "========================================="

    export CC="${TOOLCHAIN}/bin/${ndk_prefix}-clang"
    export AR="${TOOLCHAIN}/bin/llvm-ar"
    export RANLIB="${TOOLCHAIN}/bin/llvm-ranlib"

    cargo build \
        --package sweet-lab-core \
        --release \
        --target "$rust_target"

    # Copy .so to output directory
    mkdir -p "${OUTPUT_DIR}/${abi}"
    cp "target/${rust_target}/release/libsweet_lab_core.so" "${OUTPUT_DIR}/${abi}/"

    echo "Done: ${abi}/libsweet_lab_core.so"
done

echo ""
echo "========================================="
echo "All cross-compilation complete."
echo "========================================="
ls -lhR "$OUTPUT_DIR"

# ── Generate UniFFI Kotlin bindings ─────────────────────────────────────────
# Use one of the compiled .so files (arm64) to generate Kotlin bindings.
# UniFFI reads the embedded metadata from the .so to produce the Kotlin source.
BINDINGS_DIR="/build/android/app/src/main/kotlin"
mkdir -p "$BINDINGS_DIR"

echo ""
echo "========================================="
echo "Generating UniFFI Kotlin bindings"
echo "========================================="

# Build the uniffi-bindgen binary for the host (Linux x86_64)
cargo build --package sweet-lab-core --bin uniffi-bindgen

# Generate Kotlin bindings from the arm64 .so
cargo run --package sweet-lab-core --bin uniffi-bindgen -- \
    generate \
    --library "target/aarch64-linux-android/release/libsweet_lab_core.so" \
    --language kotlin \
    --out-dir "$BINDINGS_DIR"

echo "Kotlin bindings generated in: $BINDINGS_DIR"
ls -la "$BINDINGS_DIR"
