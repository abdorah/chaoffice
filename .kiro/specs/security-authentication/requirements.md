# Requirements Document

## Introduction

This document specifies the security and authentication layer for the Inventory Management Application built with Rust, Qleany, and Slint UI. The feature encompasses user authentication (login, logout, password management), user management (admin-only CRUD), role-based access control (RBAC) with four roles and granular permissions, session storage abstraction for multiple frontends, first-run bootstrap, and Slint UI integration for login and user management screens.

## Glossary

- **Auth_Service**: The authentication service responsible for login, logout, password change, and session lifecycle operations, implemented in the `inventory_auth` crate.
- **Security_Context**: A per-operation structure containing the authenticated user's identity, role, and resolved permissions, extracted from a valid session token. Implemented in the `inventory_security` crate.
- **Session**: A database entity representing an active user session, containing a token (UUID v4) and an expiration timestamp.
- **Session_Store**: A trait abstraction for frontend-specific session token persistence (in-memory for Slint, file-based for CLI).
- **RBAC_Engine**: The role-based access control enforcement layer that maps roles to permissions and gates use-case execution via proc macros.
- **User**: An application identity entity with username, password hash, display name, role, active status, and optional Person link.
- **UserRole**: An enum with values Admin, Manager, Operator, Viewer defining the access level of a User.
- **Permission**: A string-based capability in the format `resource:action` (e.g., `product:read`, `stock:*`).
- **Bootstrap_Service**: The first-run initialization service that creates a default admin account when no users exist.
- **Password_Hasher**: The component responsible for hashing and verifying passwords using the argon2 algorithm.
- **Login_Screen**: The Slint UI page that gates application access, requiring valid credentials before showing the main application.
- **Users_Page**: The Slint UI admin-only page for managing application users.

## Requirements

### Requirement 1: User Login

**User Story:** As a user, I want to log in with my username and password, so that I can access the inventory management application according to my role.

#### Acceptance Criteria

1. WHEN a user submits valid credentials (username and password), THE Auth_Service SHALL verify the password against the stored argon2 hash and return a successful login result containing a session token, user ID, role, and display name.
2. WHEN a user submits an incorrect password for an existing username, THE Auth_Service SHALL reject the login attempt and return an error message without revealing whether the username exists.
3. WHEN a user submits a username that does not exist, THE Auth_Service SHALL reject the login attempt and return the same error message as for an incorrect password.
4. WHEN a user account has `is_active` set to false, THE Auth_Service SHALL reject the login attempt and return an error indicating the account is deactivated.
5. WHEN a login succeeds, THE Auth_Service SHALL generate a UUID v4 session token with a 24-hour expiration and persist it as a Session entity linked to the User.
6. WHEN a user already has an active session and logs in again, THE Auth_Service SHALL invalidate the previous session and create a new one.

### Requirement 2: User Logout

**User Story:** As a logged-in user, I want to log out, so that my session is terminated and the application is secured.

#### Acceptance Criteria

1. WHEN a user submits a valid session token for logout, THE Auth_Service SHALL delete the associated Session entity and confirm the logout.
2. WHEN a user submits an invalid or expired session token for logout, THE Auth_Service SHALL treat the operation as successful without error (idempotent logout).
3. WHEN a logout completes, THE Session_Store SHALL clear the locally persisted token.

### Requirement 3: Change Password

**User Story:** As a logged-in user, I want to change my password, so that I can maintain the security of my account.

#### Acceptance Criteria

1. WHEN a user provides a correct current password and a valid new password, THE Auth_Service SHALL update the stored password hash using argon2 and return a success result.
2. WHEN a user provides an incorrect current password, THE Auth_Service SHALL reject the password change and return an error message.
3. WHEN a user provides a new password shorter than 8 characters, THE Auth_Service SHALL reject the password change and return a validation error.
4. WHEN a user provides a new password identical to the current password, THE Auth_Service SHALL reject the password change and return an error indicating the new password must differ.

### Requirement 4: First-Run Bootstrap

**User Story:** As a system deployer, I want the application to create a default admin account on first launch, so that initial access is possible without manual database setup.

#### Acceptance Criteria

1. WHEN the application starts and no User entities exist in the database, THE Bootstrap_Service SHALL create a default User with username "admin", password "admin" (hashed with argon2), role Admin, and `is_active` set to true.
2. WHEN the application starts and at least one User entity exists, THE Bootstrap_Service SHALL skip the bootstrap process.
3. WHEN the default admin logs in for the first time, THE Auth_Service SHALL flag the login result to indicate a password change is required.

### Requirement 5: Session Management

**User Story:** As the system, I want to manage session lifecycle, so that only authenticated users with valid sessions can perform operations.

#### Acceptance Criteria

1. WHEN a session token is presented for validation, THE Security_Context SHALL verify the token exists and has not expired (expires_at is in the future).
2. WHEN a session token has expired, THE Security_Context SHALL reject the token and return a session-expired error.
3. WHEN a session token does not match any stored Session entity, THE Security_Context SHALL reject the token and return an invalid-session error.
4. WHEN a valid session token is presented, THE Security_Context SHALL construct a context containing the user ID, role, and resolved permissions for that role.

### Requirement 6: Session Storage Abstraction

**User Story:** As a developer, I want a pluggable session storage mechanism, so that different frontends (Slint desktop, CLI) can persist session tokens appropriately.

#### Acceptance Criteria

1. THE Session_Store SHALL define a trait with `save_token`, `load_token`, and `clear_token` operations.
2. WHEN the Slint desktop frontend is used, THE Session_Store SHALL provide an InMemorySessionStore implementation that stores the token in a Mutex-protected field.
3. WHEN the CLI frontend is used, THE Session_Store SHALL provide a FileSessionStore implementation that persists the token to `~/.inventory_manager/session`.
4. WHEN `load_token` is called and no token is stored, THE Session_Store SHALL return None without error.

### Requirement 7: Role-Based Access Control

**User Story:** As a system administrator, I want to enforce role-based permissions on all use cases, so that users can only perform actions authorized for their role.

#### Acceptance Criteria

1. THE RBAC_Engine SHALL map each UserRole to a set of Permissions according to the permission matrix: Admin gets `*:*`; Manager gets `product:*`, `category:*`, `person:*`, `deal:*`, `location:*`, `stock:*`, `budget:*`, `report:*`, `import:*`, `export:*`, `sync:trigger`; Operator gets `product:read`, `category:read`, `location:read`, `stock:*`, `dashboard:read`; Viewer gets `*:read`, `report:generate`.
2. WHEN a use case is annotated with `#[require_role(UserRole::X)]`, THE RBAC_Engine SHALL reject execution if the Security_Context role does not match X, unless the role is Admin.
3. WHEN a use case is annotated with `#[require_any_role(UserRole::X, UserRole::Y)]`, THE RBAC_Engine SHALL reject execution if the Security_Context role is not in the specified set, unless the role is Admin.
4. WHEN a use case is annotated with `#[require_permission("resource:action")]`, THE RBAC_Engine SHALL reject execution if the Security_Context does not contain the specified permission, unless the role is Admin.
5. WHEN a permission check involves a wildcard (`*:*` or `resource:*`), THE RBAC_Engine SHALL match the wildcard against the requested permission pattern.
6. WHEN an unauthorized use case execution is attempted, THE RBAC_Engine SHALL return an AccessDenied error containing the required permission or role.

### Requirement 8: User Management

**User Story:** As an administrator, I want to create, deactivate, and list users, so that I can control who has access to the application.

#### Acceptance Criteria

1. WHEN an Admin creates a new user with a username, password, display name, role, and optional person ID, THE Auth_Service SHALL hash the password with argon2, create the User entity, and return the new user ID.
2. WHEN an Admin attempts to create a user with a username that already exists, THE Auth_Service SHALL reject the creation and return a duplicate-username error.
3. WHEN an Admin deactivates a user, THE Auth_Service SHALL set `is_active` to false on the User entity and delete any active Session for that user (force logout).
4. WHEN an Admin requests the user list, THE Auth_Service SHALL return all User entities with their IDs, usernames, display names, roles, and active statuses.
5. WHEN a non-Admin user attempts any user management operation, THE RBAC_Engine SHALL reject the operation with an AccessDenied error.

### Requirement 9: Password Hashing

**User Story:** As a security-conscious system, I want all passwords hashed with argon2, so that plaintext passwords are never stored.

#### Acceptance Criteria

1. THE Password_Hasher SHALL hash passwords using the argon2 algorithm with a randomly generated salt.
2. THE Password_Hasher SHALL verify a plaintext password against a stored argon2 hash and return a boolean result.
3. THE Password_Hasher SHALL produce different hashes for the same password on successive calls (due to random salt).

### Requirement 10: Slint UI Login Screen

**User Story:** As a desktop user, I want a login screen that gates the application, so that I must authenticate before accessing any functionality.

#### Acceptance Criteria

1. WHEN the Slint application starts, THE Login_Screen SHALL be displayed as the initial view, blocking access to all other application pages.
2. WHEN a user enters valid credentials and submits the login form, THE Login_Screen SHALL call the Auth_Service login, store the token via Session_Store, and navigate to the main application view.
3. WHEN a login attempt fails, THE Login_Screen SHALL display the error message returned by the Auth_Service without revealing internal details.
4. WHEN the session expires or the user logs out, THE Login_Screen SHALL be displayed again.

### Requirement 11: Slint UI Users Page

**User Story:** As an administrator using the desktop application, I want a users management page, so that I can create and manage user accounts through the UI.

#### Acceptance Criteria

1. WHEN an Admin navigates to the Users_Page, THE Users_Page SHALL display a list of all users with their username, display name, role, and active status.
2. WHEN an Admin submits the create-user form with valid data, THE Users_Page SHALL call the Auth_Service to create the user and refresh the user list.
3. WHEN an Admin clicks deactivate on a user, THE Users_Page SHALL call the Auth_Service to deactivate the user and update the list to reflect the change.
4. WHEN a non-Admin user attempts to navigate to the Users_Page, THE application SHALL deny access and display an appropriate message.
