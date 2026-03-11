use casbin::{CoreApi, DefaultModel, Enforcer, FileAdapter};

use crate::error::{AppError, AppResult};
use crate::models::domain::UserRole;

/// Initialize a Casbin enforcer from model.conf and policy.csv file paths.
pub async fn init_enforcer(model_path: &str, policy_path: &str) -> AppResult<Enforcer> {
    let model = DefaultModel::from_file(model_path)
        .await
        .map_err(|e| AppError::Unknown(format!("Failed to load Casbin model: {e}")))?;

    let policy_owned = policy_path.to_owned();
    let adapter = FileAdapter::new(policy_owned);

    Enforcer::new(model, adapter)
        .await
        .map_err(|e| AppError::Unknown(format!("Failed to initialize Casbin enforcer: {e}")))
}

/// Check whether a given role has permission to perform `action` on `resource`.
///
/// Converts the `UserRole` enum to a lowercase string for Casbin matching
/// (e.g. `UserRole::Admin` → `"admin"`).
pub fn check_permission(
    enforcer: &mut Enforcer,
    role: &UserRole,
    resource: &str,
    action: &str,
) -> AppResult<bool> {
    let role_str = match role {
        UserRole::Admin => "admin",
        UserRole::Chef => "chef",
        UserRole::Representative => "representative",
    };

    enforcer
        .enforce((role_str, resource, action))
        .map_err(|e| AppError::Unknown(format!("Casbin enforcement error: {e}")))
}

#[cfg(test)]
mod tests {
    use super::*;

    /// Helper: build an enforcer from the project's policy files.
    async fn test_enforcer() -> Enforcer {
        init_enforcer("policies/model.conf", "policies/policy.csv")
            .await
            .expect("Failed to load Casbin policies")
    }

    #[tokio::test]
    async fn admin_can_view_admin_dashboard() {
        let mut e = test_enforcer().await;
        let allowed = check_permission(&mut e, &UserRole::Admin, "admin_dashboard", "view")
            .expect("enforce failed");
        assert!(allowed);
    }

    #[tokio::test]
    async fn chef_can_execute_production() {
        let mut e = test_enforcer().await;
        let allowed = check_permission(&mut e, &UserRole::Chef, "production", "execute")
            .expect("enforce failed");
        assert!(allowed);
    }

    #[tokio::test]
    async fn representative_can_create_sales() {
        let mut e = test_enforcer().await;
        let allowed =
            check_permission(&mut e, &UserRole::Representative, "sales", "create")
                .expect("enforce failed");
        assert!(allowed);
    }

    #[tokio::test]
    async fn chef_cannot_manage_users() {
        let mut e = test_enforcer().await;
        let allowed = check_permission(&mut e, &UserRole::Chef, "users", "manage")
            .expect("enforce failed");
        assert!(!allowed);
    }

    #[tokio::test]
    async fn representative_cannot_view_admin_dashboard() {
        let mut e = test_enforcer().await;
        let allowed =
            check_permission(&mut e, &UserRole::Representative, "admin_dashboard", "view")
                .expect("enforce failed");
        assert!(!allowed);
    }
}
