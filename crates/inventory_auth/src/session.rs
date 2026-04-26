use chrono::Utc;
use common::entities::Session;
use common::types::EntityId;
use uuid::Uuid;

/// Generate a new UUID v4 session token.
pub fn generate_token() -> String {
    Uuid::new_v4().to_string()
}

/// Create a new Session struct with a UUID v4 token and 24-hour expiry.
///
/// The `_user_id` is accepted for context but not stored on the Session entity
/// itself (the User→Session link is managed by the Qleany controller layer).
pub fn create_session(_user_id: EntityId) -> Session {
    let now = Utc::now();
    Session {
        id: 0, // assigned by DB on persist
        created_at: now,
        updated_at: now,
        token: generate_token(),
        expires_at: now + chrono::Duration::hours(24),
    }
}

/// Check whether a session is still valid (not expired).
pub fn is_session_valid(session: &Session) -> bool {
    session.expires_at > Utc::now()
}
