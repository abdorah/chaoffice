use std::collections::HashSet;

use crate::error::AppResult;
use crate::models::domain::UserRole;

/// A simple in-memory permission table that replaces Casbin.
///
/// Each role maps to a set of (resource, action) pairs.
/// This is equivalent to the old `policies/policy.csv` but expressed in code.
pub struct PermissionTable {
    admin: HashSet<(&'static str, &'static str)>,
    chef: HashSet<(&'static str, &'static str)>,
    representative: HashSet<(&'static str, &'static str)>,
}

impl PermissionTable {
    /// Build the permission table with all role→(resource, action) mappings.
    pub fn new() -> Self {
        Self {
            admin: HashSet::from([
                ("admin_dashboard", "view"),
                ("users", "manage"),
                ("recipes", "manage"),
                ("inventory", "view"),
                ("inventory", "manage"),
                ("production", "view"),
                ("sales", "view"),
                ("sales", "create"),
                ("customers", "manage"),
                ("wallets", "manage"),
                ("wallets", "view"),
                ("funds", "transfer"),
                ("expenses", "view"),
                ("expenses", "record"),
                ("debts", "view"),
                ("debts", "collect"),
                ("reports", "view"),
                ("reports", "export"),
            ]),
            chef: HashSet::from([
                ("production", "execute"),
                ("production", "view"),
                ("inventory", "view"),
            ]),
            representative: HashSet::from([
                ("sales", "create"),
                ("sales", "view"),
                ("customers", "manage"),
                ("wallets", "view"),
                ("expenses", "record"),
                ("expenses", "view"),
                ("debts", "view"),
                ("debts", "collect"),
                ("inventory", "view"),
                ("inventory", "manage"),
            ]),
        }
    }

    /// Check whether a given role has permission to perform `action` on `resource`.
    pub fn check_permission(&self, role: &UserRole, resource: &str, action: &str) -> AppResult<bool> {
        let permissions = match role {
            UserRole::Admin => &self.admin,
            UserRole::Chef => &self.chef,
            UserRole::Representative => &self.representative,
        };
        Ok(permissions.iter().any(|(r, a)| *r == resource && *a == action))
    }
}

impl Default for PermissionTable {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn admin_can_view_admin_dashboard() {
        let table = PermissionTable::new();
        assert!(table.check_permission(&UserRole::Admin, "admin_dashboard", "view").unwrap());
    }

    #[test]
    fn chef_can_execute_production() {
        let table = PermissionTable::new();
        assert!(table.check_permission(&UserRole::Chef, "production", "execute").unwrap());
    }

    #[test]
    fn representative_can_create_sales() {
        let table = PermissionTable::new();
        assert!(table.check_permission(&UserRole::Representative, "sales", "create").unwrap());
    }

    #[test]
    fn chef_cannot_manage_users() {
        let table = PermissionTable::new();
        assert!(!table.check_permission(&UserRole::Chef, "users", "manage").unwrap());
    }

    #[test]
    fn representative_cannot_view_admin_dashboard() {
        let table = PermissionTable::new();
        assert!(!table.check_permission(&UserRole::Representative, "admin_dashboard", "view").unwrap());
    }
}
