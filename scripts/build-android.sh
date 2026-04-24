#!/usr/bin/env bash
# Build script for cross-compiling the mobile_ffi crate for Android targets
# and generating Kotlin bindings via UniFFI.
#
# Prerequisites:
#   - Android NDK installed and ANDROID_NDK_HOME set
#   - cargo-ndk installed: cargo install cargo-ndk
#   - Rust Android targets added:
#       rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android
#   - uniffi-bindgen installed: cargo install uniffi-bindgen

set -e

# Resolve project root (script lives in scripts/)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

JNILIBS_DIR="android/app/src/main/jniLibs"
KOTLIN_OUT_DIR="android/app/src/main/kotlin"

echo "==> Building mobile_ffi for Android targets..."

cargo ndk \
  -t aarch64-linux-android \
  -t armv7-linux-androideabi \
  -t x86_64-linux-android \
  -o "$PROJECT_ROOT/$JNILIBS_DIR" \
  build --release -p inventory-manager-mobile-ffi

echo "==> Building host-native library for UniFFI bindgen..."

cargo build --release -p inventory-manager-mobile-ffi

echo "==> Generating Kotlin bindings via UniFFI..."

# Use the host-native library (not the Android cross-compiled one)
# because uniffi-bindgen needs to dlopen the library on the host OS.
if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" || "$OSTYPE" == "win32" ]]; then
  HOST_LIB="$PROJECT_ROOT/target/release/mobile_ffi.dll"
else
  HOST_LIB="$PROJECT_ROOT/target/release/libmobile_ffi.so"
fi

cargo run -p inventory-manager-mobile-ffi --bin uniffi-bindgen -- \
  generate \
  --library "$HOST_LIB" \
  --language kotlin \
  --out-dir "$PROJECT_ROOT/$KOTLIN_OUT_DIR"

echo "==> Patching generated Kotlin bindings for Kotlin 2.x compatibility..."

# Fix UniFFI 0.28 + Kotlin 2.x conflict: `val message` clashes with Throwable.message
# Replace `val \`message\`: kotlin.String` with `override val message: kotlin.String`
# in the generated FfiException subclasses, and remove the duplicate override getter.
GENERATED_FILE="$PROJECT_ROOT/$KOTLIN_OUT_DIR/com/inventory/ffi/mobile_ffi.kt"
if [ -f "$GENERATED_FILE" ]; then
  sed -i 's/val `message`: kotlin.String/override val message: kotlin.String/g' "$GENERATED_FILE"
  # Remove the now-duplicate override getter lines
  sed -i '/override val message$/d' "$GENERATED_FILE"
  sed -i '/get() = "message=\${ `message` }"/d' "$GENERATED_FILE"
  echo "    Patched $GENERATED_FILE"
fi

echo "==> Android build complete."
echo "    Native libs: $JNILIBS_DIR/{arm64-v8a,armeabi-v7a,x86_64}/libmobile_ffi.so"
echo "    Kotlin bindings: $KOTLIN_OUT_DIR/"
