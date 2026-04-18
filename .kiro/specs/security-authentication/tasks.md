# Implementation Plan: Security & Authentication

## Overview

Incremental implementation of the security and authentication layer across three crates (`inventory_security`, `inventory_auth`, `inventory_security_macros`), followed by Slint UI wiring. Each task builds on the previous, with property tests validating correctness at each stage.

## Tasks

- [x] 1. Set up crate structure and core types
  - [x] 1.1 Create `crates/inventory_security/` crate with `Cargo.toml`
    - Add dependencies: `thiserror`
    - Create `src/lib.rs`, `src/error.rs`, `src/permission.rs`, `src/context.rs`
    - _Requirements: 7.1, 5.4_

  - [x] 1.2 Implement `AuthError` enum in `error.rs`
    - Define all variants: `InvalidCredentials`, `AccountDeactivated`, `SessionExpired`, `InvalidSession`, `AccessDeniedRole`, `AccessDeniedPermission`, `DuplicateUsername`, `PasswordTooShort`, `PasswordUnchanged`, `PasswordChangeRequired`, `Internal`
    - Derive `thiserror::Error` with display messages
    - _Requirements: 7.6_

  - [x] 1.3 Implement `Permission` type and `permissions_for_role` in `permission.rs`
    - Define `Permission(pub String)` newtype
    - Implement `permissions_for_role(role: &UserRole) -> Vec<Permission>` with the full permission matrix
    - Implement `permission_matches(held: &Permission, required: &str) -> bool` with wildcard support (`*:*`, `resource:*`, exact match)
    - _Requirements: 7.1, 7.4, 7.5_

  - [ ]* 1.4 Write property tests for permission module
    - **Property 15: Role-to-permission mapping correctness**
    - **Validates: Requirements 7.1**
    - **Property 17: Permission-based access control with wildcards**
    - **Validates: Requirements 7.4, 7.5**

  - [x] 1.5 Implement `SecurityContext` struct in `context.rs`
    - Define struct with `user_id`, `role`, `permissions`, `token` fields
    - Implement `has_role`, `has_any_role`, `has_permission`, `is_admin` methods
    - Stub `from_token` (full implementation after session module)
    - _Requirements: 5.4, 7.2, 7.3, 7.4_

  - [ ]* 1.6 Write property tests for SecurityContext role/permission checks
    - **Property 16: Role-based access control**
    - **Validates: Requirements 7.2, 7.3**

- [x] 2. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 3. Implement password hashing module
  - [x] 3.1 Create `crates/inventory_auth/` crate with `Cargo.toml`
    - Add dependencies: `argon2`, `uuid`, `chrono`, `inventory_security`
    - Create `src/lib.rs`, `src/password.rs`, `src/session.rs`, `src/session_store.rs`, `src/bootstrap.rs`
    - _Requirements: 9.1_

  - [x] 3.2 Implement `hash_password` and `verify_password` in `password.rs`
    - Use `argon2::Argon2::default()` with `SaltString::generate(&mut OsRng)`
    - `hash_password` returns the PHC-formatted hash string
    - `verify_password` parses the hash and verifies against plaintext
    - _Requirements: 9.1, 9.2, 9.3_

  - [ ]* 3.3 Write property tests for password hashing
    - **Property 1: Password hash round-trip**
    - **Validates: Requirements 9.1, 9.2**
    - **Property 2: Password hash uniqueness**
    - **Validates: Requirements 9.3**

- [x] 4. Implement session management
  - [x] 4.1 Implement `create_session`, `validate_session`, `delete_session` in `session.rs`
    - `create_session`: generate UUID v4 token, set `expires_at` to `now + 24h`, persist Session entity, delete any existing session for the user first
    - `validate_session`: look up Session by token, check `expires_at > now`, load associated User
    - `delete_session`: find and delete Session by token, no-op if not found
    - _Requirements: 1.5, 1.6, 2.1, 5.1, 5.2, 5.3_

  - [x] 4.2 Complete `SecurityContext::from_token` implementation in `inventory_security`
    - Call `validate_session`, build context with `user_id`, `role`, `permissions_for_role(role)`, `token`
    - _Requirements: 5.1, 5.4_

  - [ ]* 4.3 Write property tests for session validation and SecurityContext
    - **Property 12: Session token validation correctness**
    - **Validates: Requirements 5.1, 5.2, 5.3**
    - **Property 13: SecurityContext resolves correct permissions**
    - **Validates: Requirements 5.4**

- [x] 5. Implement session store abstraction
  - [x] 5.1 Implement `SessionStore` trait, `InMemorySessionStore`, and `FileSessionStore` in `session_store.rs`
    - Trait: `save_token`, `load_token`, `clear_token`
    - `InMemorySessionStore`: `Mutex<Option<String>>`
    - `FileSessionStore`: read/write `~/.inventory_manager/session` file, create directory if needed
    - _Requirements: 6.1, 6.2, 6.3, 6.4_

  - [ ]* 5.2 Write property tests for session stores
    - **Property 14: SessionStore round-trip**
    - **Validates: Requirements 6.2, 6.3**
    - Unit test: `load_token` on fresh store returns None (Requirement 6.4)

- [x] 6. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. Implement authentication operations (login, logout, change_password)
  - [x] 7.1 Implement `login` function in `inventory_auth/src/lib.rs`
    - Fetch User by username (return `InvalidCredentials` if not found)
    - Check `is_active` (return `AccountDeactivated` if false)
    - Verify password with `verify_password` (return `InvalidCredentials` if wrong)
    - Call `create_session` (invalidates any existing session)
    - Check if password is default "admin" hash for bootstrap user → set `password_change_required`
    - Return `LoginResultDto`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 4.3_

  - [x] 7.2 Implement `logout` function
    - Call `delete_session` with the token (idempotent)
    - _Requirements: 2.1, 2.2_

  - [x] 7.3 Implement `change_password` function
    - Validate new password length >= 8 (return `PasswordTooShort`)
    - Verify old password (return `InvalidCredentials` if wrong)
    - Check new != old via `verify_password` (return `PasswordUnchanged`)
    - Hash new password and update User entity
    - _Requirements: 3.1, 3.2, 3.3, 3.4_

  - [ ]* 7.4 Write property tests for login
    - **Property 3: Successful login contract**
    - **Validates: Requirements 1.1, 1.5**
    - **Property 4: Failed login indistinguishable errors**
    - **Validates: Requirements 1.2, 1.3**
    - **Property 5: Deactivated account login rejection**
    - **Validates: Requirements 1.4**
    - **Property 6: Re-login invalidates previous session**
    - **Validates: Requirements 1.6**

  - [ ]* 7.5 Write property tests for logout
    - **Property 7: Logout invalidates session**
    - **Validates: Requirements 2.1**
    - **Property 8: Logout is idempotent**
    - **Validates: Requirements 2.2**

  - [ ]* 7.6 Write property tests for change_password
    - **Property 9: Password change round-trip**
    - **Validates: Requirements 3.1**
    - **Property 10: Password change rejects wrong old password**
    - **Validates: Requirements 3.2**
    - Unit tests for short password (3.3) and same password (3.4) edge cases

- [x] 8. Implement user management operations
  - [x] 8.1 Implement `create_user`, `deactivate_user`, `list_users` in `inventory_auth/src/lib.rs`
    - `create_user`: check username uniqueness, hash password, create User entity with `is_active=true`
    - `deactivate_user`: set `is_active=false`, delete any active session for the user
    - `list_users`: fetch all Users, map to `UserSummary` structs
    - _Requirements: 8.1, 8.2, 8.3, 8.4_

  - [ ]* 8.2 Write property tests for user management
    - **Property 18: User creation persists correctly**
    - **Validates: Requirements 8.1**
    - **Property 19: Duplicate username rejection**
    - **Validates: Requirements 8.2**
    - **Property 20: Deactivation force-logout**
    - **Validates: Requirements 8.3**
    - **Property 21: List users completeness**
    - **Validates: Requirements 8.4**

- [x] 9. Implement bootstrap module
  - [x] 9.1 Implement `ensure_admin_exists` in `bootstrap.rs`
    - Check if any User entities exist
    - If none: create User with username "admin", hash_password("admin"), role Admin, is_active true
    - If any exist: return false (no-op)
    - _Requirements: 4.1, 4.2_

  - [ ]* 9.2 Write property test and unit tests for bootstrap
    - **Property 11: Bootstrap idempotence**
    - **Validates: Requirements 4.2**
    - Unit test: bootstrap on empty DB creates admin with correct fields (Requirement 4.1)
    - Unit test: default admin login sets `password_change_required` (Requirement 4.3)

- [x] 10. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 11. Implement proc macros
  - [x] 11.1 Create `crates/inventory_security_macros/` crate with `Cargo.toml`
    - Set `proc-macro = true` in `[lib]`
    - Add dependencies: `syn`, `quote`, `proc-macro2`
    - _Requirements: 7.2, 7.3, 7.4_

  - [x] 11.2 Implement `#[require_role]` macro
    - Parse attribute for a single `UserRole::X` variant
    - Inject guard code: extract `SecurityContext`, check `is_admin() || has_role(X)`, return `AccessDeniedRole` on failure
    - _Requirements: 7.2_

  - [x] 11.3 Implement `#[require_any_role]` macro
    - Parse attribute for multiple `UserRole::X, UserRole::Y` variants
    - Inject guard code: extract `SecurityContext`, check `is_admin() || has_any_role(&[X, Y])`, return `AccessDeniedRole` on failure
    - _Requirements: 7.3_

  - [x] 11.4 Implement `#[require_permission]` macro
    - Parse attribute for a permission string literal `"resource:action"`
    - Inject guard code: extract `SecurityContext`, check `has_permission("resource:action")`, return `AccessDeniedPermission` on failure
    - _Requirements: 7.4_

  - [ ]* 11.5 Write unit tests for proc macro expansion
    - Test that `#[require_role(UserRole::Admin)]` allows Admin
    - Test that `#[require_role(UserRole::Manager)]` rejects Operator
    - Test that `#[require_any_role(UserRole::Manager, UserRole::Operator)]` allows both
    - Test that `#[require_permission("stock:write")]` allows Operator (has `stock:*`)
    - Test that `#[require_permission("deal:write")]` rejects Operator
    - _Requirements: 7.2, 7.3, 7.4, 7.6_

- [x] 12. Wire Slint UI login flow
  - [x] 12.1 Wire `AppState.login` callback to `inventory_auth::login`
    - In the Rust Slint bridge code, handle the `login` callback
    - On success: store token via `InMemorySessionStore`, set `AppState.is-authenticated = true`, set `current-user` and `current-role`
    - On failure: set `AppState.login-error` to the error message
    - On `password_change_required`: navigate to password change dialog
    - _Requirements: 10.1, 10.2, 10.3_

  - [x] 12.2 Wire `AppState.logout` callback to `inventory_auth::logout`
    - Call `logout` with the stored token, clear `InMemorySessionStore`
    - Set `AppState.is-authenticated = false`, clear `current-user` and `current-role`
    - _Requirements: 2.3, 10.4_

  - [x] 12.3 Wire `AppState.create-user` and `AppState.deactivate-user` callbacks
    - `create-user`: call `inventory_auth::create_user`, refresh user list
    - `deactivate-user`: call `inventory_auth::deactivate_user`, refresh user list
    - _Requirements: 11.2, 11.3_

  - [x] 12.4 Wire Users_Page data binding
    - On navigation to Users_Page, call `list_users` and populate `UsersPageAdapter.row-data`
    - Fix role ComboBox values from `["Admin", "Manager", "Supplier", "Viewer"]` to `["Admin", "Manager", "Operator", "Viewer"]`
    - Add role-based visibility: hide Users sidebar item for non-Admin users
    - _Requirements: 11.1, 11.4_

- [x] 13. Integrate bootstrap on application startup
  - [x] 13.1 Call `ensure_admin_exists` during app initialization
    - In the Slint app main.rs (or CLI main.rs), call bootstrap before showing the UI
    - Log whether bootstrap was performed
    - _Requirements: 4.1, 4.2_

- [x] 14. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties using `proptest`
- Unit tests validate specific examples and edge cases
- The proc macro crate (`inventory_security_macros`) must be a separate crate due to Rust's proc-macro compilation model
