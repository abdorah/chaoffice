# UniFFI Kotlin Bindings

This directory will contain the auto-generated Kotlin bindings for the `sweet-lab-core` Rust library.

## How to Generate Bindings

UniFFI 0.28 requires a small helper binary to generate bindings. Add the following to `core/Cargo.toml`:

```toml
[[bin]]
name = "uniffi-bindgen"
path = "uniffi-bindgen.rs"
```

Then create `core/uniffi-bindgen.rs`:

```rust
fn main() {
    uniffi::uniffi_bindgen_main()
}
```

### Generation Command

After building the library:

```bash
cd core
cargo build --release
cargo run --bin uniffi-bindgen -- generate --library target/release/libsweet_lab_core.so --language kotlin --out-dir ../android/app/src/main/kotlin/org/sweetlab/bindings/
```

On Windows, replace `libsweet_lab_core.so` with `sweet_lab_core.dll`.
On macOS, replace with `libsweet_lab_core.dylib`.

### What Gets Generated

UniFFI will produce Kotlin files that mirror the Rust API surface defined in `core/src/api.rs`:

- `SweetLabCore` class with all public methods
- Data classes for all domain types (AppUser, RawMaterial, FinishedGood, Recipe, etc.)
- Enum classes for UserRole, WalletType, ExpenseCategory, SyncStatus
- Error types mapping to AppError variants

### Android Integration

The generated bindings use JNA to call into the native `.so` library. The Android app must:

1. Include the compiled `.so` in `jniLibs/` for each target ABI (arm64-v8a, armeabi-v7a, x86_64)
2. Cross-compile the Rust library using Android NDK targets
3. Load the native library at app startup

### Current Status

The Rust API surface (`SweetLabCore` in `core/src/api.rs`) is fully defined and compiles successfully. The UDL file (`core/src/sweet_lab_core.udl`) contains the namespace declaration. To generate full bindings, the API methods need to be either:

1. Declared in the UDL file, or
2. Annotated with `#[uniffi::export]` proc macros (requires switching from UDL scaffolding to proc-macro approach)

The current architecture uses the UDL scaffolding approach (`uniffi::include_scaffolding!`). To export the `SweetLabCore` API, either expand the UDL file with all type and function declarations, or migrate to the proc-macro approach.
