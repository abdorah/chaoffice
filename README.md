# Sweet Lab ERP

A polyglot, multi-platform ERP system for a confectionery factory. Built with a shared-core architecture: Protocol Buffers define the data schema, a pure Rust library implements all business logic, and thin UI shells (Android + Desktop) consume the core through well-defined APIs.

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Schema Layer                          │
│              .proto files (source of truth)              │
│         prost → Rust structs │ wire → Kotlin DCs         │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│              Core Engine (Rust Library Crate)            │
│                                                         │
│  auth/        ─ Casbin-RS RBAC + argon2 authentication  │
│  inventory/   ─ Raw materials + finished goods tracking  │
│  recipes/     ─ Recipe management + production execution │
│  sales/       ─ Sales transactions + customer management │
│  wallet/      ─ Wallet ops + atomic fund transfers       │
│  debt/        ─ FIFO debt allocation + aging reports     │
│  expenses/    ─ Expense recording + category reporting   │
│  reports/     ─ Financial summaries, PDF, Excel export   │
│  sync/        ─ Offline queue + conflict resolution      │
│  persistence/ ─ SQLx/SQLite with compile-time queries    │
│                                                         │
│  api.rs       ─ UniFFI-exported public API surface       │
└────────┬────────────────────────────────┬───────────────┘
         │                                │
┌────────▼────────┐            ┌──────────▼──────────┐
│   Android App   │            │    Desktop App      │
│ Kotlin/Compose  │            │    Slint (Rust)     │
│ UniFFI bindings │            │  Direct Rust calls  │
└─────────────────┘            └─────────────────────┘
```

## Project Structure

```
sweet-lab/
├── proto/                  Protobuf schema (source of truth)
│   └── sweetlab/           Entity, auth, and service definitions
├── core/                   Rust core engine (library crate)
│   ├── src/
│   │   ├── auth/           Authentication + Casbin-RS RBAC
│   │   ├── inventory/      Raw materials + finished goods
│   │   ├── recipes/        Recipe CRUD + production execution
│   │   ├── sales/          Sales, customers, receipts
│   │   ├── wallet/         Wallet management + fund transfers
│   │   ├── debt/           Debt tracking + FIFO allocation
│   │   ├── expenses/       Expense management
│   │   ├── reports/        Financial, inventory, PDF, Excel
│   │   ├── sync/           Offline queue + conflict resolution
│   │   ├── persistence/    SQLx pool, migrations, queries
│   │   ├── models/         Domain models + proto mappers
│   │   ├── api.rs          UniFFI public API surface
│   │   └── error.rs        Typed error handling (thiserror)
│   ├── migrations/         11 SQLx migration files
│   ├── policies/           Casbin model.conf + policy.csv
│   └── tests/              Property-based + unit tests
├── android/                Kotlin + Jetpack Compose app
├── desktop/                Slint UI framework app
└── build-android.sh/bat    Cross-compilation scripts
```

## Tech Stack

| Layer | Technology |
|---|---|
| Schema | Protocol Buffers, prost, wire |
| Core Logic | Rust (pure library crate, zero platform deps) |
| Persistence | SQLx + SQLite (compile-time checked queries) |
| RBAC | Casbin-RS (declarative policy files) |
| Auth | argon2 password hashing, session management |
| Reports | genpdf (PDF), rust_xlsxwriter (Excel) |
| Sync | reqwest HTTP client, offline queue, last-write-wins |
| FFI | UniFFI (Kotlin bindings for Android) |
| Android | Kotlin + Jetpack Compose + Material Design 3 |
| Desktop | Slint (Rust-native cross-platform UI) |
| Testing | proptest (property-based), cargo test (unit) |
| Async | tokio runtime |
| Logging | tracing + tracing-subscriber |

## Roles and Permissions

Three roles enforced via Casbin-RS:

- **Admin** — Full access: user management, financial reports, inventory, recipes, wallets, all modules
- **Chef** — Production only: execute recipes, view production history, view inventory
- **Representative** — Sales operations: sales, customers, expenses, debt collection, inventory management

## Business Logic Modules

### Authentication and Sessions
- argon2 password hashing with secure verification
- 8-hour inactivity timeout with session management
- Generic error messages that don't reveal which credential field is wrong

### Inventory Management
- Atomic multi-material deductions within SQLx transactions
- Insufficient stock rejection with specific material and shortfall details
- Real-time quantity tracking for raw materials and finished goods

### Recipe and Production
- Recipe validation: 1-10 ingredients, material existence checks
- Deletion protection for recipes referenced by production logs
- Production execution: atomic deduction of raw materials + increment of finished goods
- Recipe availability calculation: `max_producible = min(floor(stock/required))`

### Sales and Customers
- Sale orchestration: inventory deduction + wallet credit + debt creation (if partial payment)
- Receipt generation with customer details, itemized list, and payment status
- Customer search by name, city, or mobile number
- Reliability rating system (1-5 stars)

### Wallet and Fund Transfers
- Three wallet types: Bank, Cash, Representative
- Atomic fund transfers with balance conservation
- Insufficient funds rejection with unchanged balances

### Debt Tracking
- FIFO payment allocation (oldest debts first by sale date)
- Overdue days calculation: `(current_date - sale_date)` in days
- Critical flag: debts exceeding 30 days marked as critical
- Aging report sorted by overdue days descending

### Expense Management
- Wallet debit on expense recording with balance validation
- Category-based grouping (Purchase, OperatingCost) with subtotals

### Reporting
- Financial summaries: revenue, expenses, net profit, wallet balances
- Inventory reports with configurable low-stock threshold alerts
- Invoice generation with full customer and itemized product details
- PDF export (genpdf) and Excel export (rust_xlsxwriter)

### Offline Mode and Sync
- Local SQLite as source of truth, writes queued when offline
- FIFO sync processing on connectivity restore
- Last-write-wins conflict resolution with admin-reviewable conflict logs

## Testing

The project uses a specification-driven testing approach with 39 formal correctness properties validated through property-based testing (proptest) and unit tests.

### Running Tests

```bash
# Run all tests
cargo test

# Run a specific test suite
cargo test --test serialization_properties
cargo test --test auth_properties
cargo test --test inventory_properties
cargo test --test recipe_properties
cargo test --test production_properties
cargo test --test customer_properties
cargo test --test wallet_properties
cargo test --test sales_properties
cargo test --test debt_properties
cargo test --test expense_properties
cargo test --test report_properties
cargo test --test sync_properties
cargo test --test navigation_properties

# Run unit tests
cargo test --test auth_unit_tests
cargo test --test inventory_unit_tests
cargo test --test critical_flows
```

### Test Coverage by Module

| Test File | Properties | What It Validates |
|---|---|---|
| `serialization_properties.rs` | P38 | JSON round-trip for all 22+ domain types |
| `schema_validation_properties.rs` | P39 | Malformed data rejection (missing fields, wrong types, garbage) |
| `auth_properties.rs` | P2-P5 | RBAC enforcement, credential rejection, user creation, role updates |
| `auth_unit_tests.rs` | — | Session expiry boundary, empty credentials, argon2 verification |
| `inventory_properties.rs` | P6, P7, P15 | Purchase increases, insufficient stock rejection (raw + finished) |
| `inventory_unit_tests.rs` | — | Exact deduction boundary, zero deduction, atomic multi-material |
| `recipe_properties.rs` | P12-P14 | Ingredient count bounds, material existence, deletion protection |
| `production_properties.rs` | P8-P11 | Inventory conservation, rejection, log completeness, availability |
| `customer_properties.rs` | P16-P19 | Initial rating, rating bounds, mobile uniqueness, search |
| `wallet_properties.rs` | P23-P24 | Transfer conservation, insufficient funds rejection |
| `sales_properties.rs` | P20-P22 | Total calculation, financial orchestration, receipt completeness |
| `debt_properties.rs` | P27-P30 | Overdue days, sort order, FIFO allocation, critical flag |
| `expense_properties.rs` | P25, P31 | Wallet debit, report grouping and totals |
| `report_properties.rs` | P32-P34 | Financial summary, low stock alerts, invoice completeness |
| `sync_properties.rs` | P35-P37 | Queue persistence, FIFO ordering, conflict logging |
| `navigation_properties.rs` | P1 | Role-based navigation routing |
| `critical_flows.rs` | — | End-to-end integration: production, sale, payment flows |

## Building

### Core Library
```bash
cargo build --release
```

### Android
```bash
# Cross-compile for Android targets
./build-android.sh    # Linux/macOS
build-android.bat     # Windows
```

### Desktop
```bash
cargo build --release -p desktop
```

## Database Migrations

11 versioned SQLx migrations run automatically on startup:

1. `users` — Authentication with role-based access
2. `raw_materials` — Ingredient inventory with quantity constraints
3. `finished_goods` — Product inventory with pricing
4. `recipes` + `recipe_ingredients` — Recipe definitions with FK constraints
5. `production_logs` — Production history with material consumption
6. `customers` — Customer profiles with unique mobile constraint
7. `sales` + `sale_line_items` — Sales transactions with line items
8. `wallets` + `wallet_transactions` — Financial accounts with transaction history
9. `debt_records` — Customer debt tracking with settlement status
10. `expenses` — Expense records with category constraints
11. `sync_queue` + `conflict_log` — Offline sync infrastructure

## License

Private — Sweet Lab internal use.
