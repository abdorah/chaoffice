# Inventory Manager

A cross-platform inventory management system built with a Rust backend and native Android (Kotlin/Compose) frontend. The entire business logic, database, authentication, and sync engine live in Rust, exposed to Android via UniFFI-generated Kotlin bindings.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                    Android App (Kotlin)                  │
│  Jetpack Compose UI → ViewModels → Coroutines (IO)      │
├─────────────────────────────────────────────────────────┤
│                  UniFFI / JNA Bridge                     │
│         mobile_ffi crate ↔ generated Kotlin bindings     │
├─────────────────────────────────────────────────────────┤
│                   Rust Backend (19 crates)               │
│                                                         │
│  ┌─────────┐  ┌──────────────┐  ┌───────────────────┐  │
│  │ frontend│  │ feature crates│  │  inventory_sync   │  │
│  │ commands│→ │ (use cases,   │  │ redb ↔ libsql ↔   │  │
│  │         │  │  UoW, DTOs)   │  │ TursoDB (cloud)   │  │
│  └────┬────┘  └──────┬───────┘  └───────────────────┘  │
│       │              │                                   │
│  ┌────▼──────────────▼───────┐  ┌───────────────────┐  │
│  │     direct_access         │  │ inventory_security │  │
│  │  repositories + tables    │  │ RBAC + permissions │  │
│  └────────────┬──────────────┘  └───────────────────┘  │
│               │                                         │
│  ┌────────────▼──────────────┐  ┌───────────────────┐  │
│  │        common             │  │  inventory_auth    │  │
│  │  entities, redb, events,  │  │  argon2, sessions  │  │
│  │  undo/redo, types         │  │  password policy   │  │
│  └───────────────────────────┘  └───────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

The project follows Clean Architecture principles, scaffolded by [Qleany v1.5.5](https://github.com/nicogetz/qleany). Each feature domain is isolated in its own crate with Use Cases, Units of Work, DTOs, and Controllers.

## Domain Model

| Entity | Description | Key Fields |
|--------|-------------|------------|
| Product | Inventory items | name, reference, quantity, price_unit, status, category, supplier, location |
| Category | Hierarchical product grouping | name, description, parent_category, subcategories |
| Person | Suppliers and managers | name, role (Manager/Supplier), contact |
| Contact | Phone and email for a person | phone, email |
| Deal | Purchase agreements | title, unit_cost, total_value, frequency, status, product, supplier, manager |
| Location | Warehouses and storage sites | name, address, lat/lng, capacity, manager |
| StockMovement | Inventory transactions | movement_type (Inbound/Outbound/Transfer/Adjustment/Return), quantity, product, from/to location |
| BudgetEntry | Financial records | entry_type (Purchase/Sale/Expense/Forecast), amount, description, entry_date |
| User | Application users | username, password_hash, display_name, role, is_active |
| Session | Auth sessions | token, expires_at |

All entities use `u64` IDs, UTC timestamps (`created_at`, `updated_at`), and are serialized with [postcard](https://crates.io/crates/postcard) for storage in [redb](https://crates.io/crates/redb).

## Crate Map


```
crates/
├── common/                 # Shared types, entities, redb database, event hub, undo/redo
├── direct_access/          # Repository pattern: CRUD for all entities via redb tables
├── macros/                 # Procedural macros (#[uow_action]) for Unit of Work trait generation
├── frontend/               # AppContext + command dispatchers for all features
│
├── inventory_management/   # Product CRUD use cases and controllers
├── purchasing/             # Deal management, purchase workflows
├── location_management/    # Location CRUD
├── authentication/         # Login, logout, password change (use cases + UoW)
├── user_management/        # User creation, deactivation, role assignment
├── stock_tracking/         # Stock movements, summaries, history
├── budget_finance/         # Budget entries, summaries, projections, charts
├── reporting/              # Multi-format report generation (PDF/Excel/CSV)
│
├── inventory_security/     # RBAC: SecurityContext, Permission, role-based access
├── inventory_auth/         # Password hashing (Argon2), session tokens, bootstrap
├── inventory_security_macros/ # #[check_permission] proc macro
├── inventory_sync/         # Sync engine: redb ↔ libsql ↔ TursoDB cloud
│
├── mobile_ffi/             # UniFFI bindings: all FFI exports for Android
├── cli/                    # Command-line interface
└── slint_ui/               # Desktop UI (Slint framework)
```

## Database Layer

The primary data store is [redb](https://crates.io/crates/redb) — an embedded, ACID-compliant key-value database written in pure Rust. It runs in-process with zero external dependencies.

- Desktop/CLI: in-memory database (`DbContext::new()`)
- Android: file-backed database at `{filesDir}/inventory_data.db` (`DbContext::new_with_path()`)
- Each entity type has its own redb table definition and repository implementation
- Serialization uses [postcard](https://crates.io/crates/postcard) (compact binary format)
- Transactions are explicit: `begin_read_transaction` / `begin_write_transaction` with commit

## Cloud Sync

The sync engine bridges the local redb database with [Turso](https://turso.tech/) (TursoDB) via [libsql](https://crates.io/crates/libsql):

```
redb (local) ──push──▶ libsql (local SQLite) ──sync──▶ TursoDB (cloud)
redb (local) ◀──pull── libsql (local SQLite) ◀──sync── TursoDB (cloud)
```

- **Push (dehydrate)**: reads all entities from redb, upserts them into the local libsql database, then calls `db.sync()` to push to TursoDB
- **Pull (hydrate)**: calls `db.sync()` to pull from TursoDB into local libsql, then imports all rows back into redb
- **Lazy remote**: the engine starts in local-only mode for fast init, rebuilds as a remote replica on first sync operation
- **Conflict resolution**: configurable strategies (Full, Incremental, ConflictResolveLocal, ConflictResolveRemote)
- **Auto-sync**: optional timer-based sync at configurable intervals
- **TLS on Android**: bundled Mozilla CA roots via `webpki-roots` + `SSL_CERT_DIR` pointing to Android's system CA store

Configuration is stored in `sync_config.json`:
```json
{
  "turso_url": "libsql://your-db.turso.io",
  "turso_auth_token": "...",
  "auto_sync_enabled": true,
  "sync_interval_seconds": 300,
  "default_strategy": "Incremental"
}
```

## Security Model

Role-Based Access Control (RBAC) with wildcard permission matching:

| Role | Permissions | Description |
|------|------------|-------------|
| Admin | `*:*` | Full access to everything |
| Manager | `product:*`, `category:*`, `stock:*`, `budget:*`, `report:*`, `sync:trigger`, ... | All CRUD + sync |
| Operator | `product:read`, `category:read`, `stock:*`, `dashboard:read` | Stock operations + read-only catalog |
| Viewer | `*:read`, `report:generate` | Read-only + report generation |

- Passwords hashed with [Argon2id](https://crates.io/crates/argon2) + random salt
- Session tokens are UUID v4, stored as `Session` entities
- Default bootstrap: username `admin`, password `Password1` (flagged for mandatory change on first login)
- Permission checks via `#[check_permission]` proc macro on use case methods

## Report Generation

Multi-format report engine supporting PDF, Excel, and CSV:

| Format | Library | Notes |
|--------|---------|-------|
| PDF | [typst](https://typst.app/) via `typst-as-lib` | Template-driven with embedded Liberation Sans fonts |
| Excel | [rust_xlsxwriter](https://crates.io/crates/rust_xlsxwriter) | Native .xlsx generation |
| CSV | [csv](https://crates.io/crates/csv) | Standard CSV output |

Available reports:
- Inventory Report (all products with category, location, supplier)
- Stock Movement Report (date-filtered transaction history)
- Budget Report (income/expenses with optional projections)
- Purchasing Report (deals filtered by status)

Reports are generated as long-running operations with progress callbacks, polled from the FFI layer.

## Android App

### Tech Stack
- Kotlin 2.x + Jetpack Compose (Material 3)
- Navigation Compose with modal drawer
- ViewModel + StateFlow + Coroutines
- UniFFI 0.28 generated bindings over JNA
- Targets API 26–36 (Android 8.0 to Android 16)
- 16 KB page alignment for Android 15+ devices

### Screens
| Screen | Features |
|--------|----------|
| Login | Username/password auth, session management |
| Dashboard | Entity counts grid, quick navigation |
| Products | List, create, edit, delete with form validation |
| Categories | Hierarchical category management |
| People | Suppliers and managers with contact info |
| Deals | Purchase agreements with status tracking |
| Locations | Warehouse management with coordinates |
| Stock Tracking | Record movements, view summaries and history |
| Budget | Financial entries, summaries, projections |
| Reports | Generate PDF/Excel/CSV, share via intent |
| Users | User management, role assignment, deactivation |
| Sync Settings | Configure Turso URL/token, push/pull operations |

### FFI Bridge

The `mobile_ffi` crate exposes ~50 functions to Kotlin via UniFFI:

```
InventoryApp.onCreate()
  → System.loadLibrary("mobile_ffi")
  → mobileInit(dbPath)          // creates redb + sets up sync paths
  → mobileBootstrap()           // creates Root entity + default admin user
```

All FFI calls are synchronous from Kotlin's perspective (dispatched to `Dispatchers.IO`), with the Rust side handling async operations via a tokio runtime stored in a `OnceLock<AppState>` singleton.

## Building

### Prerequisites

- Rust (edition 2024) with Android targets:
  ```bash
  rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android
  ```
- [cargo-ndk](https://github.com/nickelc/cargo-ndk): `cargo install cargo-ndk`
- Android NDK with `ANDROID_NDK_HOME` set
- Android Studio (for the Gradle build)

### Build Native Libraries + Kotlin Bindings

```bash
./scripts/build-android.sh
```

This script:
1. Cross-compiles `mobile_ffi` for arm64-v8a, armeabi-v7a, x86_64 with 16 KB page alignment
2. Builds the host-native library for uniffi-bindgen
3. Generates Kotlin bindings via `uniffi-bindgen`
4. Patches generated code for Kotlin 2.x compatibility (`val message` → `override val message`)

### Build Android APK

```bash
cd android
./gradlew assembleDebug
```

### Run Desktop UI (Slint)

```bash
cargo run
```

### Run CLI

The cli is currently unimplemented, sowwy for that.

## Project Conventions

- Code generation by Qleany v1.5.5 (templates: `*.tera`)
- Each feature crate follows: `dtos.rs` → `use_cases/` → `units_of_work/` → `*_controller.rs`
- Unit of Work traits are generated via `#[macros::uow_action]` proc macros
- All timestamps are UTC `chrono::DateTime<Utc>`
- Entity IDs are `u64` (type alias `EntityId`)
- Error handling: `thiserror` for domain errors, `anyhow` for infrastructure
- Release profile: LTO fat, panic=abort, stripped symbols, single codegen unit

