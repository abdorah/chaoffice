# Security & Authentication Crate Design

## Overview

Two companion crates for the Qleany-generated inventory manager:

- `inventory_auth` — Login/logout, password hashing, session tokens, user lifecycle
- `inventory_security` — RBAC enforcement via proc macros on use cases

Inspired by Spring Security SpEL, `axum_grant`, and `actix_web_grants`.

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                    Slint UI / CLI                         │
│              ┌──────────────────────┐                    │
│              │   Login Screen       │                    │
│              └──────┬───────────────┘                    │
│                     │ username + password                 │
├─────────────────────▼────────────────────────────────────┤
│         authentication::login(LoginDto)                   │
│              │                                           │
│              ├─ verify password_hash (argon2)             │
│              ├─ create Session entity with token + expiry │
│              └─ return LoginResultDto { token, role }     │
├──────────────────────────────────────────────────────────┤
│         SecurityContext (per-operation)                   │
│         ┌─────────────────────────────────┐              │
│         │  user_id, role, token           │              │
│         └─────────────────────────────────┘              │
│              ↓ extracted from active session              │
├──────────────────────────────────────────────────────────┤
│         #[require_role(UserRole::Manager)]                │
│         #[require_any_role(UserRole::Manager, ...)]      │
│         #[require_permission("product:write")]           │
│              ↓ Proc macro gate ↓                         │
├──────────────────────────────────────────────────────────┤
│              Qleany Controllers / Use Cases               │
├──────────────────────────────────────────────────────────┤
│              Repositories / Database                      │
└──────────────────────────────────────────────────────────┘
```

## Roles Explained

This is a buyer/inventory manager application. All users are internal staff.

| Role | Who | Purpose |
|------|-----|---------|
| Admin | IT / system owner | Full control: user management, sync config, all operations |
| Manager | Purchasing / inventory manager | Full business operations: products, deals, budget, reports, sync trigger |
| Operator | Warehouse staff | Hands-on: records stock movements, views inventory. No deals, budget, or settings |
| Viewer | Auditor / stakeholder | Read-only: dashboards, reports, budget summaries. Cannot modify anything |

## Permission Matrix

| Permission Area | Admin | Manager | Operator | Viewer |
|---|---|---|---|---|
| Dashboard / KPIs: view | ✓ | ✓ | ✓ | ✓ |
| Products: view | ✓ | ✓ | ✓ | ✓ |
| Products: create/edit/delete | ✓ | ✓ | ✗ | ✗ |
| Categories: manage | ✓ | ✓ | ✗ | ✗ |
| Persons (contacts): manage | ✓ | ✓ | ✗ | ✗ |
| Purchasing deals: manage | ✓ | ✓ | ✗ | ✗ |
| Locations: manage | ✓ | ✓ | ✗ | ✗ |
| Stock movements: record | ✓ | ✓ | ✓ | ✗ |
| Stock movements: view history | ✓ | ✓ | ✓ | ✓ |
| Budget: record entries | ✓ | ✓ | ✗ | ✗ |
| Budget: view / projections | ✓ | ✓ | ✗ | ✓ |
| Reports: generate | ✓ | ✓ | ✗ | ✓ |
| Import / Export data | ✓ | ✓ | ✗ | ✗ |
| Users: manage | ✓ | ✗ | ✗ | ✗ |
| Sync: configure credentials | ✓ | ✗ | ✗ | ✗ |
| Sync: trigger push/pull | ✓ | ✓ | ✗ | ✗ |

## Authentication Flow

### Login
1. UI sends `LoginDto { username, password }` to `authentication::login()`
2. Use case fetches `User` by username
3. Verifies `password` against stored `password_hash` using argon2
4. If valid and `user.is_active == true`:
   - Generates a random session token (uuid v4)
   - Creates `Session { token, expires_at: now + 24h }`
   - Attaches session to user via `one_to_one` relationship
   - Returns `LoginResultDto { success: true, token, user_id, role, display_name }`
5. If invalid: returns `LoginResultDto { success: false, error_message }`

### Logout
1. UI sends `LogoutDto { token }`
2. Use case finds session by token, deletes it
3. UI clears local state, shows login screen

### Session Validation
```rust
impl SecurityContext {
    pub fn from_token(db_context: &DbContext, token: &str) -> Result<Self> {
        // 1. Find session by token
        // 2. Check expires_at > now
        // 3. Load associated User
        // 4. Build SecurityContext { user_id, role, permissions }
        // If expired or not found → Err(AuthError::SessionExpired)
    }
}
```

### Password Hashing
```rust
use argon2::{Argon2, PasswordHash, PasswordHasher, PasswordVerifier};
use argon2::password_hash::rand_core::OsRng;
use argon2::password_hash::SaltString;

pub fn hash_password(password: &str) -> Result<String> {
    let salt = SaltString::generate(&mut OsRng);
    Ok(Argon2::default().hash_password(password.as_bytes(), &salt)?.to_string())
}

pub fn verify_password(password: &str, hash: &str) -> Result<bool> {
    let parsed = PasswordHash::new(hash)?;
    Ok(Argon2::default().verify_password(password.as_bytes(), &parsed).is_ok())
}
```

## Session Storage Abstraction

Each frontend implements token persistence differently:

```rust
/// Trait for frontend-specific session persistence
pub trait SessionStore: Send + Sync {
    fn save_token(&self, token: &str) -> Result<()>;
    fn load_token(&self) -> Result<Option<String>>;
    fn clear_token(&self) -> Result<()>;
}

/// Desktop (Slint): in-memory, lost on app close
pub struct InMemorySessionStore { token: Mutex<Option<String>> }

/// CLI: file-based (~/.inventory_manager/session)
pub struct FileSessionStore { path: PathBuf }

/// Mobile (future): platform keychain via UniFFI
/// iOS: Keychain Services
/// Android: EncryptedSharedPreferences
```

## Core Types

```rust
// UserRole is generated by Qleany from the manifest enum:
// pub enum UserRole { Admin, Manager, Operator, Viewer }

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct Permission(pub String);

#[derive(Debug, Clone)]
pub struct SecurityContext {
    pub user_id: u32,
    pub role: UserRole,
    pub permissions: Vec<Permission>,
    pub token: String,
}

impl SecurityContext {
    pub fn has_role(&self, role: UserRole) -> bool {
        self.role == role
    }

    pub fn has_any_role(&self, roles: &[UserRole]) -> bool {
        roles.contains(&self.role)
    }

    pub fn has_permission(&self, perm: &str) -> bool {
        if self.role == UserRole::Admin { return true; }
        self.permissions.iter().any(|p| p.0 == perm)
    }

    pub fn is_admin(&self) -> bool {
        self.role == UserRole::Admin
    }
}
```

## Role → Permission Mapping

```rust
pub fn permissions_for_role(role: &UserRole) -> Vec<Permission> {
    match role {
        UserRole::Admin => vec![Permission("*:*".into())],
        UserRole::Manager => vec![
            Permission("product:*".into()),
            Permission("category:*".into()),
            Permission("person:*".into()),
            Permission("deal:*".into()),
            Permission("location:*".into()),
            Permission("stock:*".into()),
            Permission("budget:*".into()),
            Permission("report:*".into()),
            Permission("import:*".into()),
            Permission("export:*".into()),
            Permission("sync:trigger".into()),
        ],
        UserRole::Operator => vec![
            Permission("product:read".into()),
            Permission("category:read".into()),
            Permission("location:read".into()),
            Permission("stock:*".into()),
            Permission("dashboard:read".into()),
        ],
        UserRole::Viewer => vec![
            Permission("*:read".into()),
            Permission("report:generate".into()),
        ],
    }
}
```

## Procedural Macros

```rust
#[require_role(UserRole::Manager)]
pub fn execute(&mut self, dto: &CreatePurchaseDealDto) -> Result<CreatePurchaseDealReturnDto> { }

#[require_any_role(UserRole::Manager, UserRole::Operator)]
pub fn execute(&mut self, dto: &RecordStockMovementDto) -> Result<RecordStockMovementResultDto> { }

#[require_permission("budget:write")]
pub fn execute(&mut self, dto: &RecordBudgetEntryDto) -> Result<RecordBudgetEntryResultDto> { }
```

Expansion adds a guard before the function body. Admin always passes (superuser).

## First-Run Bootstrap

On first launch (no users exist), the app creates a default admin:
```
username: admin
password: admin (force change on first login)
role: Admin
```

## Person vs User

`Person` = external business contact (supplier, manager as a business role). It's data you manage.
`User` = someone who logs into the app. It's identity with access control.

A `User` optionally links to a `Person` via `many_to_one`. This lets you say "user jdoe is the person John Doe (Manager)" without forcing every user to be a person. Not all persons are users. Not all users map to a person.

## Crate Structure

```
crates/inventory_auth/
├── Cargo.toml
└── src/
    ├── lib.rs
    ├── password.rs          # argon2 hash/verify
    ├── session.rs           # Token generation, validation, expiry
    ├── session_store.rs     # SessionStore trait + InMemory/File impls
    └── bootstrap.rs         # First-run default admin creation

crates/inventory_security/
├── Cargo.toml
└── src/
    ├── lib.rs
    ├── context.rs           # SecurityContext
    ├── permission.rs        # Permission type + role→permission mapping
    └── error.rs             # AuthError type

crates/inventory_security_macros/
├── Cargo.toml              # proc-macro = true
└── src/
    └── lib.rs              # #[require_role], #[require_any_role], #[require_permission]
```
