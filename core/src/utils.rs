use chrono::{DateTime, Utc};
use uuid::Uuid;

use crate::error::{AppError, AppResult};
use crate::models::domain::WalletType;

/// Parse a UUID string, returning NotFound on failure.
pub fn parse_uuid(entity_type: &str, id_str: &str) -> AppResult<Uuid> {
    Uuid::parse_str(id_str).map_err(|_| AppError::NotFound {
        entity_type: entity_type.to_string(),
        entity_id: id_str.to_string(),
    })
}

/// Parse a wallet type string from the database.
pub fn parse_wallet_type(s: &str) -> AppResult<WalletType> {
    match s {
        "Bank" => Ok(WalletType::Bank),
        "Cash" => Ok(WalletType::Cash),
        "Representative" => Ok(WalletType::Representative),
        other => Err(AppError::Unknown(format!("Invalid wallet type: {other}"))),
    }
}

/// Parse an RFC3339 timestamp string.
pub fn parse_timestamp(s: &str) -> AppResult<DateTime<Utc>> {
    s.parse()
        .map_err(|e| AppError::Unknown(format!("Invalid timestamp: {e}")))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parse_uuid_valid() {
        let id = Uuid::new_v4();
        let result = parse_uuid("user", &id.to_string());
        assert_eq!(result.unwrap(), id);
    }

    #[test]
    fn parse_uuid_invalid_returns_not_found() {
        let result = parse_uuid("user", "not-a-uuid");
        match result {
            Err(AppError::NotFound { entity_type, entity_id }) => {
                assert_eq!(entity_type, "user");
                assert_eq!(entity_id, "not-a-uuid");
            }
            other => panic!("Expected NotFound, got {:?}", other),
        }
    }

    #[test]
    fn parse_wallet_type_valid_variants() {
        assert_eq!(parse_wallet_type("Bank").unwrap(), WalletType::Bank);
        assert_eq!(parse_wallet_type("Cash").unwrap(), WalletType::Cash);
        assert_eq!(
            parse_wallet_type("Representative").unwrap(),
            WalletType::Representative
        );
    }

    #[test]
    fn parse_wallet_type_invalid_returns_unknown() {
        let result = parse_wallet_type("Crypto");
        assert!(matches!(result, Err(AppError::Unknown(_))));
    }

    #[test]
    fn parse_timestamp_valid_rfc3339() {
        let ts = "2024-01-15T10:30:00Z";
        let result = parse_timestamp(ts).unwrap();
        assert_eq!(result.to_rfc3339().starts_with("2024-01-15T10:30:00"), true);
    }

    #[test]
    fn parse_timestamp_invalid_returns_unknown() {
        let result = parse_timestamp("not-a-timestamp");
        assert!(matches!(result, Err(AppError::Unknown(_))));
    }
}
