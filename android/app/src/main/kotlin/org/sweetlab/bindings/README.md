# UniFFI Kotlin Bindings

The auto-generated Kotlin bindings for the `sweet-lab-core` Rust library live at:

```
android/app/src/main/kotlin/org/sweetlab/core/sweet_lab_core.kt
```

Package: `org.sweetlab.core`

## How to Regenerate Bindings

The bindings are generated as part of the containerized Android build. Run from the project root:

**Git Bash (Windows):**
```bash
./build-android.sh
```

**CMD (Windows):**
```cmd
build-android.bat
```

This will:
1. Build the Rust library for all Android ABIs (arm64-v8a, armeabi-v7a, x86_64)
2. Generate Kotlin bindings via `uniffi-bindgen`
3. Place `.so` files in `android/app/src/main/jniLibs/{abi}/`
4. Place Kotlin bindings in `android/app/src/main/kotlin/org/sweetlab/core/`

### Manual Generation (host machine)

If you have the library already built locally:

```bash
cd core
cargo build --lib --release
cargo run --bin uniffi-bindgen -- generate \
    --library ../target/release/sweet_lab_core.dll \
    --language kotlin \
    --out-dir ../android/app/src/main/kotlin/
```

On Linux, replace `sweet_lab_core.dll` with `libsweet_lab_core.so`.
On macOS, replace with `libsweet_lab_core.dylib`.

## Usage in Android Code

```kotlin
import org.sweetlab.core.SweetLabCore
import org.sweetlab.core.Session
import org.sweetlab.core.UserRole
// ... etc
```
