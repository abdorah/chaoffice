use common::entities::UserRole;

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct Permission(pub String);

/// Return the set of permissions for a given role.
pub fn permissions_for_role(role: &UserRole) -> Vec<Permission> {
    match role {
        UserRole::Admin => vec![Permission("*:*".to_string())],
        UserRole::Manager => vec![
            Permission("product:*".to_string()),
            Permission("category:*".to_string()),
            Permission("person:*".to_string()),
            Permission("deal:*".to_string()),
            Permission("location:*".to_string()),
            Permission("stock:*".to_string()),
            Permission("budget:*".to_string()),
            Permission("report:*".to_string()),
            Permission("import:*".to_string()),
            Permission("export:*".to_string()),
            Permission("sync:trigger".to_string()),
        ],
        UserRole::Operator => vec![
            Permission("product:read".to_string()),
            Permission("category:read".to_string()),
            Permission("location:read".to_string()),
            Permission("stock:*".to_string()),
            Permission("dashboard:read".to_string()),
        ],
        UserRole::Viewer => vec![
            Permission("*:read".to_string()),
            Permission("report:generate".to_string()),
        ],
    }
}

/// Check if a held permission matches a required permission string.
///
/// Wildcard rules:
/// - `*:*` matches everything
/// - `resource:*` matches any `resource:<action>`
/// - Exact match otherwise
pub fn permission_matches(held: &Permission, required: &str) -> bool {
    let held_str = held.0.as_str();

    // Global wildcard matches everything
    if held_str == "*:*" {
        return true;
    }

    // Resource wildcard: "resource:*" matches "resource:<anything>"
    if let Some(held_resource) = held_str.strip_suffix(":*") {
        if let Some(required_resource) = required.split(':').next() {
            return held_resource == required_resource;
        }
    }

    // Action wildcard in held: "*:action" matches "<anything>:action"
    if let Some(held_action) = held_str.strip_prefix("*:") {
        if let Some(required_action) = required.rsplit(':').next() {
            return held_action == required_action;
        }
    }

    // Exact match
    held_str == required
}
