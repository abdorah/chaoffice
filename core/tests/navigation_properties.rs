// Property tests for role-based navigation routing (Property 1)
//
// **Validates: Requirements 1.3, 2.4, 2.5, 2.6**

mod generators;

use proptest::prelude::*;
use sweet_lab_core::models::domain::UserRole;

// ── Property 1: Role-based navigation routing ──────────────────────────────
//
// For any user with a valid role (Admin, Chef, or Representative), after login
// the navigation destination SHALL match the role's designated screen:
//   Admin → Admin Dashboard ("admin_dashboard")
//   Chef  → Production Screen ("production_screen")
//   Representative → Sales Screen ("sales_screen")
//
// **Validates: Requirements 1.3, 2.4, 2.5, 2.6**

proptest! {
    #![proptest_config(ProptestConfig::with_cases(300))]

    #[test]
    fn prop1_role_based_navigation_routing(
        role in generators::arb_user_role(),
    ) {
        let destination = role.navigation_destination();

        let expected = match &role {
            UserRole::Admin => "admin_dashboard",
            UserRole::Chef => "production_screen",
            UserRole::Representative => "sales_screen",
        };

        prop_assert_eq!(
            destination, expected,
            "Role {:?} should navigate to {:?}, got {:?}",
            role, expected, destination
        );

        // The destination must be non-empty
        prop_assert!(!destination.is_empty(), "Destination must not be empty");
    }
}

// ── Exhaustive coverage: every role variant maps to a unique screen ─────────

#[test]
fn all_roles_map_to_distinct_screens() {
    let roles = [UserRole::Admin, UserRole::Chef, UserRole::Representative];
    let destinations: Vec<&str> = roles.iter().map(|r| r.navigation_destination()).collect();

    // Each role maps to a unique screen
    let mut unique = destinations.clone();
    unique.sort();
    unique.dedup();
    assert_eq!(
        destinations.len(),
        unique.len(),
        "Each role must map to a distinct screen, got: {:?}",
        destinations
    );
}

#[test]
fn admin_navigates_to_admin_dashboard() {
    assert_eq!(UserRole::Admin.navigation_destination(), "admin_dashboard");
}

#[test]
fn chef_navigates_to_production_screen() {
    assert_eq!(UserRole::Chef.navigation_destination(), "production_screen");
}

#[test]
fn representative_navigates_to_sales_screen() {
    assert_eq!(
        UserRole::Representative.navigation_destination(),
        "sales_screen"
    );
}
