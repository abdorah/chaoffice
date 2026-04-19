//! Integration test: verifies role-based permission resolution and the
//! guard functions from inventory_security_macros.

use common::entities::UserRole;
use inventory_security::SecurityContext;
use inventory_security_macros::{check_any_role, check_permission, check_role};

// ─── Helper ──────────────────────────────────────────────────────────
fn ctx(role: UserRole) -> SecurityContext {
    SecurityContext::from_user(1, role, "test-token".to_string())
}

// ─── 1. Admin has permission for everything ──────────────────────────
#[test]
fn admin_has_all_permissions() {
    let admin = ctx(UserRole::Admin);

    let perms = [
        "product:read", "product:write", "product:delete",
        "deal:read", "deal:write",
        "stock:read", "stock:write",
        "budget:read", "budget:write",
        "report:generate", "report:read",
        "import:execute", "export:execute",
        "sync:trigger",
        "category:read", "location:write",
        "anything:anything",
    ];

    for p in &perms {
        assert!(admin.has_permission(p), "Admin should have permission {p}");
    }
}

// ─── 2. Manager permissions ──────────────────────────────────────────
#[test]
fn manager_has_expected_permissions() {
    let mgr = ctx(UserRole::Manager);

    let allowed = [
        "product:read", "product:write", "product:delete",
        "deal:read", "deal:write",
        "stock:read", "stock:write",
        "budget:read", "budget:write",
        "report:generate", "report:read",
        "import:execute", "export:execute",
        "sync:trigger",
    ];

    for p in &allowed {
        assert!(mgr.has_permission(p), "Manager should have permission {p}");
    }
}

// ─── 3. Operator permissions ─────────────────────────────────────────
#[test]
fn operator_has_stock_but_not_deal_or_budget() {
    let op = ctx(UserRole::Operator);

    // Allowed
    assert!(op.has_permission("stock:read"), "Operator should have stock:read");
    assert!(op.has_permission("stock:write"), "Operator should have stock:write");
    assert!(op.has_permission("product:read"), "Operator should have product:read");
    assert!(op.has_permission("category:read"), "Operator should have category:read");
    assert!(op.has_permission("dashboard:read"), "Operator should have dashboard:read");

    // Denied
    assert!(!op.has_permission("deal:read"), "Operator should NOT have deal:read");
    assert!(!op.has_permission("deal:write"), "Operator should NOT have deal:write");
    assert!(!op.has_permission("budget:read"), "Operator should NOT have budget:read");
    assert!(!op.has_permission("budget:write"), "Operator should NOT have budget:write");
}

// ─── 4. Viewer permissions ───────────────────────────────────────────
#[test]
fn viewer_has_read_and_report_but_not_write() {
    let viewer = ctx(UserRole::Viewer);

    // Allowed: *:read covers all resources
    assert!(viewer.has_permission("product:read"), "Viewer should have product:read");
    assert!(viewer.has_permission("deal:read"), "Viewer should have deal:read");
    assert!(viewer.has_permission("stock:read"), "Viewer should have stock:read");
    assert!(viewer.has_permission("budget:read"), "Viewer should have budget:read");
    assert!(viewer.has_permission("report:read"), "Viewer should have report:read");
    assert!(viewer.has_permission("report:generate"), "Viewer should have report:generate");

    // Denied
    assert!(!viewer.has_permission("product:write"), "Viewer should NOT have product:write");
    assert!(!viewer.has_permission("deal:write"), "Viewer should NOT have deal:write");
    assert!(!viewer.has_permission("product:delete"), "Viewer should NOT have product:delete");
}

// ─── 5. Guard functions from inventory_security_macros ───────────────
#[test]
fn check_role_works() {
    let admin = ctx(UserRole::Admin);
    let operator = ctx(UserRole::Operator);

    // Admin passes any role check
    assert!(check_role(&admin, UserRole::Manager).is_ok());
    assert!(check_role(&admin, UserRole::Operator).is_ok());

    // Operator passes own role
    assert!(check_role(&operator, UserRole::Operator).is_ok());

    // Operator fails Manager role
    assert!(check_role(&operator, UserRole::Manager).is_err());
}

#[test]
fn check_any_role_works() {
    let admin = ctx(UserRole::Admin);
    let viewer = ctx(UserRole::Viewer);

    // Admin passes any combination
    assert!(check_any_role(&admin, &[UserRole::Manager, UserRole::Operator]).is_ok());

    // Viewer passes when Viewer is in the list
    assert!(check_any_role(&viewer, &[UserRole::Viewer, UserRole::Operator]).is_ok());

    // Viewer fails when not in the list
    assert!(check_any_role(&viewer, &[UserRole::Manager, UserRole::Operator]).is_err());
}

#[test]
fn check_permission_works() {
    let admin = ctx(UserRole::Admin);
    let operator = ctx(UserRole::Operator);

    // Admin passes any permission
    assert!(check_permission(&admin, "anything:anything").is_ok());

    // Operator passes stock:write
    assert!(check_permission(&operator, "stock:write").is_ok());

    // Operator fails budget:write
    assert!(check_permission(&operator, "budget:write").is_err());
}
