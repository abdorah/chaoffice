# Security Authorization Crate Design

## Overview

A companion crate (`inventory_security`) for Qleany-generated code that provides role-based access control (RBAC) for use cases and features. Inspired by Spring Security SpEL, `axum_grant`, and `actix_web_grants`.

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                    Slint UI / CLI                         │
├──────────────────────────────────────────────────────────┤
│              SecurityContext (per-request)                │
│         ┌─────────────────────────────────┐              │
│         │  user_id, roles, permissions    │              │
│         └─────────────────────────────────┘              │
├──────────────────────────────────────────────────────────┤
│         #[require_role(Role::Manager)]                   │
│         #[require_any_role(Role::Manager, Role::Admin)]  │
│         #[require_permission("product:write")]           │
│              ↓ Proc macro gate ↓                         │
├──────────────────────────────────────────────────────────┤
│              Qleany Controllers / Use Cases               │
├──────────────────────────────────────────────────────────┤
│              Repositories / Database                      │
└──────────────────────────────────────────────────────────┘
```

## Core Types

```rust
/// Central role enum — generated from manifest or hand-defined
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum Role {
    Admin,
    Manager,
    Supplier,
    Viewer,
}

/// Fine-grained permission string
/// Format: "resource:action" e.g. "product:write", "deal:read"
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct Permission(pub String);

/// Extracted from auth token or session — passed through the call chain
#[derive(Debug, Clone)]
pub struct SecurityContext {
    pub user_id: String,
    pub roles: Vec<Role>,
    pub permissions: Vec<Permission>,
}

impl SecurityContext {
    pub fn has_role(&self, role: Role) -> bool {
        self.roles.contains(&role)
    }

    pub fn has_any_role(&self, roles: &[Role]) -> bool {
        roles.iter().any(|r| self.roles.contains(r))
    }

    pub fn has_permission(&self, perm: &str) -> bool {
        self.permissions.iter().any(|p| p.0 == perm)
    }
}
```

## Procedural Macros

```rust
/// Gate a use case to require a specific role
#[require_role(Role::Manager)]
pub fn execute(&mut self, dto: &CreateDealDto) -> Result<CreateDealReturnDto> {
    // Only reachable if SecurityContext has Manager role
}

/// Gate with any of multiple roles
#[require_any_role(Role::Manager, Role::Admin)]
pub fn execute(&mut self, dto: &TransferStockDto) -> Result<TransferStockReturnDto> {
    // Reachable if Manager OR Admin
}

/// Gate with fine-grained permission
#[require_permission("product:write")]
pub fn execute(&mut self, dto: &ProductDto) -> Result<ProductDto> {
    // Reachable if user has "product:write" permission
}
```

### Macro Expansion

The `#[require_role(Role::Manager)]` macro expands to:

```rust
pub fn execute(&mut self, dto: &CreateDealDto) -> Result<CreateDealReturnDto> {
    let ctx = self.security_context()
        .ok_or_else(|| anyhow::anyhow!("No security context"))?;
    if !ctx.has_role(Role::Manager) {
        return Err(anyhow::anyhow!("Access denied: requires Manager role"));
    }
    // original function body
}
```

## Integration with Qleany

The `SecurityContext` is threaded through the existing Qleany pipeline:

1. UI authenticates user (login form or token)
2. `SecurityContext` is constructed and stored in `AppContext`
3. Controllers pass it to use cases
4. Proc macros check roles/permissions before use case body executes

### Modified Controller Signature

```rust
pub fn create_deal(
    db_context: &DbContext,
    event_hub: &Arc<EventHub>,
    undo_redo_manager: &mut UndoRedoManager,
    security_ctx: &SecurityContext,  // ← added
    stack_id: Option<u64>,
    dto: &CreateDealDto,
) -> Result<CreateDealReturnDto> {
    // ...
}
```

## Role-Permission Mapping

Default mapping (configurable):

| Role     | Permissions                                                    |
|----------|----------------------------------------------------------------|
| Admin    | *:* (all)                                                      |
| Manager  | product:*, category:*, deal:*, location:*, person:read, sync:* |
| Supplier | product:read, deal:read, deal:update_own, location:read        |
| Viewer   | *:read                                                         |

## Crate Structure

```
crates/inventory_security/
├── Cargo.toml
├── src/
│   ├── lib.rs              # Re-exports
│   ├── role.rs             # Role enum
│   ├── permission.rs       # Permission type + defaults
│   ├── context.rs          # SecurityContext
│   └── error.rs            # AuthError type
└── macros/
    ├── Cargo.toml           # proc-macro crate
    └── src/
        └── lib.rs           # #[require_role], #[require_any_role], #[require_permission]
```
