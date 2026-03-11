//! Conversion functions between prost-generated protobuf structs and domain models.
//!
//! Proto models use `String` for UUIDs, `i32` for enums, and `prost_types::Timestamp`
//! for timestamps. Domain models use `uuid::Uuid`, Rust enums, and `chrono::DateTime<Utc>`.

use chrono::{DateTime, TimeZone, Utc};
use std::collections::HashMap;
use uuid::Uuid;

use crate::error::{AppError, AppResult};
use crate::models::domain;
use crate::models::generated::models as proto;

// ── Timestamp helpers ──────────────────────────────────────────────────────

fn timestamp_to_datetime(ts: Option<prost_types::Timestamp>) -> DateTime<Utc> {
    match ts {
        Some(t) => Utc
            .timestamp_opt(t.seconds, t.nanos as u32)
            .single()
            .unwrap_or_else(Utc::now),
        None => Utc::now(),
    }
}

fn datetime_to_timestamp(dt: &DateTime<Utc>) -> prost_types::Timestamp {
    prost_types::Timestamp {
        seconds: dt.timestamp(),
        nanos: dt.timestamp_subsec_nanos() as i32,
    }
}

fn parse_uuid(s: &str) -> AppResult<Uuid> {
    Uuid::parse_str(s).map_err(|e| AppError::Serialization(format!("Invalid UUID '{}': {}", s, e)))
}

// ── Enum conversions ───────────────────────────────────────────────────────

impl From<&domain::UserRole> for proto::UserRole {
    fn from(role: &domain::UserRole) -> Self {
        match role {
            domain::UserRole::Admin => proto::UserRole::Admin,
            domain::UserRole::Chef => proto::UserRole::Chef,
            domain::UserRole::Representative => proto::UserRole::Representative,
        }
    }
}

fn proto_user_role_to_domain(value: i32) -> AppResult<domain::UserRole> {
    match proto::UserRole::try_from(value) {
        Ok(proto::UserRole::Admin) => Ok(domain::UserRole::Admin),
        Ok(proto::UserRole::Chef) => Ok(domain::UserRole::Chef),
        Ok(proto::UserRole::Representative) => Ok(domain::UserRole::Representative),
        _ => Err(AppError::Serialization(format!(
            "Invalid UserRole value: {}",
            value
        ))),
    }
}

impl From<&domain::WalletType> for proto::WalletType {
    fn from(wt: &domain::WalletType) -> Self {
        match wt {
            domain::WalletType::Bank => proto::WalletType::Bank,
            domain::WalletType::Cash => proto::WalletType::Cash,
            domain::WalletType::Representative => proto::WalletType::Representative,
        }
    }
}

fn proto_wallet_type_to_domain(value: i32) -> AppResult<domain::WalletType> {
    match proto::WalletType::try_from(value) {
        Ok(proto::WalletType::Bank) => Ok(domain::WalletType::Bank),
        Ok(proto::WalletType::Cash) => Ok(domain::WalletType::Cash),
        Ok(proto::WalletType::Representative) => Ok(domain::WalletType::Representative),
        _ => Err(AppError::Serialization(format!(
            "Invalid WalletType value: {}",
            value
        ))),
    }
}

impl From<&domain::ExpenseCategory> for proto::ExpenseCategory {
    fn from(cat: &domain::ExpenseCategory) -> Self {
        match cat {
            domain::ExpenseCategory::Purchase => proto::ExpenseCategory::Purchase,
            domain::ExpenseCategory::OperatingCost => proto::ExpenseCategory::OperatingCost,
        }
    }
}

fn proto_expense_category_to_domain(value: i32) -> AppResult<domain::ExpenseCategory> {
    match proto::ExpenseCategory::try_from(value) {
        Ok(proto::ExpenseCategory::Purchase) => Ok(domain::ExpenseCategory::Purchase),
        Ok(proto::ExpenseCategory::OperatingCost) => Ok(domain::ExpenseCategory::OperatingCost),
        _ => Err(AppError::Serialization(format!(
            "Invalid ExpenseCategory value: {}",
            value
        ))),
    }
}

impl From<&domain::SyncStatus> for proto::SyncStatus {
    fn from(s: &domain::SyncStatus) -> Self {
        match s {
            domain::SyncStatus::Synced => proto::SyncStatus::Synced,
            domain::SyncStatus::Pending => proto::SyncStatus::Pending,
            domain::SyncStatus::Conflict => proto::SyncStatus::Conflict,
        }
    }
}

fn proto_sync_status_to_domain(value: i32) -> AppResult<domain::SyncStatus> {
    match proto::SyncStatus::try_from(value) {
        Ok(proto::SyncStatus::Synced) => Ok(domain::SyncStatus::Synced),
        Ok(proto::SyncStatus::Pending) => Ok(domain::SyncStatus::Pending),
        Ok(proto::SyncStatus::Conflict) => Ok(domain::SyncStatus::Conflict),
        _ => Err(AppError::Serialization(format!(
            "Invalid SyncStatus value: {}",
            value
        ))),
    }
}

// ── Domain → Proto conversions ─────────────────────────────────────────────

impl From<&domain::AppUser> for proto::AppUser {
    fn from(u: &domain::AppUser) -> Self {
        proto::AppUser {
            id: u.id.to_string(),
            username: u.username.clone(),
            full_name: u.full_name.clone(),
            role: proto::UserRole::from(&u.role) as i32,
            password_hash: u.password_hash.clone(),
        }
    }
}

impl From<&domain::RawMaterial> for proto::RawMaterial {
    fn from(m: &domain::RawMaterial) -> Self {
        proto::RawMaterial {
            id: m.id.to_string(),
            name: m.name.clone(),
            unit: m.unit.clone(),
            current_quantity: m.current_quantity,
            last_updated: Some(datetime_to_timestamp(&m.last_updated)),
        }
    }
}

impl From<&domain::FinishedGood> for proto::FinishedGood {
    fn from(g: &domain::FinishedGood) -> Self {
        proto::FinishedGood {
            id: g.id.to_string(),
            name: g.name.clone(),
            current_quantity: g.current_quantity,
            unit_price: g.unit_price,
            last_updated: Some(datetime_to_timestamp(&g.last_updated)),
        }
    }
}

impl From<&domain::RecipeIngredient> for proto::RecipeIngredient {
    fn from(i: &domain::RecipeIngredient) -> Self {
        proto::RecipeIngredient {
            raw_material_id: i.raw_material_id.to_string(),
            raw_material_name: i.raw_material_name.clone(),
            required_quantity: i.required_quantity,
        }
    }
}

impl From<&domain::Recipe> for proto::Recipe {
    fn from(r: &domain::Recipe) -> Self {
        proto::Recipe {
            id: r.id.to_string(),
            name: r.name.clone(),
            finished_good_id: r.finished_good_id.to_string(),
            finished_good_name: r.finished_good_name.clone(),
            ingredients: r.ingredients.iter().map(proto::RecipeIngredient::from).collect(),
        }
    }
}

impl From<&domain::ProductionLog> for proto::ProductionLog {
    fn from(p: &domain::ProductionLog) -> Self {
        proto::ProductionLog {
            id: p.id.to_string(),
            recipe_id: p.recipe_id.to_string(),
            recipe_name: p.recipe_name.clone(),
            chef_id: p.chef_id.to_string(),
            chef_name: p.chef_name.clone(),
            production_quantity: p.production_quantity,
            materials_consumed: p
                .materials_consumed
                .iter()
                .map(|(k, v)| (k.to_string(), *v))
                .collect(),
            finished_good_id: p.finished_good_id.to_string(),
            timestamp: Some(datetime_to_timestamp(&p.timestamp)),
        }
    }
}

impl From<&domain::Customer> for proto::Customer {
    fn from(c: &domain::Customer) -> Self {
        proto::Customer {
            id: c.id.to_string(),
            name: c.name.clone(),
            city: c.city.clone(),
            mobile: c.mobile.clone(),
            reliability_rating: c.reliability_rating,
            total_debt: c.total_debt,
            overdue_days: c.overdue_days,
        }
    }
}

impl From<&domain::SaleLineItem> for proto::SaleLineItem {
    fn from(li: &domain::SaleLineItem) -> Self {
        proto::SaleLineItem {
            finished_good_id: li.finished_good_id.to_string(),
            finished_good_name: li.finished_good_name.clone(),
            quantity: li.quantity,
            unit_price: li.unit_price,
        }
    }
}

impl From<&domain::Sale> for proto::Sale {
    fn from(s: &domain::Sale) -> Self {
        proto::Sale {
            id: s.id.to_string(),
            customer_id: s.customer_id.to_string(),
            customer_name: s.customer_name.clone(),
            line_items: s.line_items.iter().map(proto::SaleLineItem::from).collect(),
            total_amount: s.total_amount,
            amount_paid: s.amount_paid,
            payment_wallet_id: s.payment_wallet_id.to_string(),
            timestamp: Some(datetime_to_timestamp(&s.timestamp)),
        }
    }
}

impl From<&domain::Wallet> for proto::Wallet {
    fn from(w: &domain::Wallet) -> Self {
        proto::Wallet {
            id: w.id.to_string(),
            name: w.name.clone(),
            wallet_type: proto::WalletType::from(&w.wallet_type) as i32,
            current_balance: w.current_balance,
        }
    }
}

impl From<&domain::WalletTransaction> for proto::WalletTransaction {
    fn from(t: &domain::WalletTransaction) -> Self {
        proto::WalletTransaction {
            id: t.id.to_string(),
            wallet_id: t.wallet_id.to_string(),
            amount: t.amount,
            description: t.description.clone(),
            related_entity_id: t.related_entity_id.map(|id| id.to_string()).unwrap_or_default(),
            timestamp: Some(datetime_to_timestamp(&t.timestamp)),
        }
    }
}

impl From<&domain::DebtRecord> for proto::DebtRecord {
    fn from(d: &domain::DebtRecord) -> Self {
        proto::DebtRecord {
            id: d.id.to_string(),
            customer_id: d.customer_id.to_string(),
            customer_name: d.customer_name.clone(),
            sale_id: d.sale_id.to_string(),
            original_amount: d.original_amount,
            remaining_amount: d.remaining_amount,
            sale_date: Some(datetime_to_timestamp(&d.sale_date)),
            overdue_days: d.overdue_days,
            is_critical: d.is_critical,
            is_settled: d.is_settled,
        }
    }
}

impl From<&domain::Expense> for proto::Expense {
    fn from(e: &domain::Expense) -> Self {
        proto::Expense {
            id: e.id.to_string(),
            description: e.description.clone(),
            amount: e.amount,
            category: proto::ExpenseCategory::from(&e.category) as i32,
            wallet_id: e.wallet_id.to_string(),
            wallet_name: e.wallet_name.clone(),
            recorded_by: e.recorded_by.to_string(),
            timestamp: Some(datetime_to_timestamp(&e.timestamp)),
        }
    }
}

impl From<&domain::SyncQueueItem> for proto::SyncQueueItem {
    fn from(s: &domain::SyncQueueItem) -> Self {
        proto::SyncQueueItem {
            id: s.id.to_string(),
            entity_type: s.entity_type.clone(),
            entity_id: s.entity_id.to_string(),
            operation: s.operation.clone(),
            payload: s.payload.clone(),
            created_at: Some(datetime_to_timestamp(&s.created_at)),
            status: proto::SyncStatus::from(&s.status) as i32,
        }
    }
}

impl From<&domain::ConflictLog> for proto::ConflictLog {
    fn from(c: &domain::ConflictLog) -> Self {
        proto::ConflictLog {
            id: c.id.to_string(),
            entity_type: c.entity_type.clone(),
            entity_id: c.entity_id.to_string(),
            local_version: c.local_version.clone(),
            remote_version: c.remote_version.clone(),
            resolved_with: c.resolved_with.clone(),
            timestamp: Some(datetime_to_timestamp(&c.timestamp)),
        }
    }
}

// ── Proto → Domain conversions ─────────────────────────────────────────────

impl TryFrom<&proto::AppUser> for domain::AppUser {
    type Error = AppError;
    fn try_from(u: &proto::AppUser) -> AppResult<Self> {
        Ok(domain::AppUser {
            id: parse_uuid(&u.id)?,
            username: u.username.clone(),
            full_name: u.full_name.clone(),
            role: proto_user_role_to_domain(u.role)?,
            password_hash: u.password_hash.clone(),
        })
    }
}

impl TryFrom<&proto::RawMaterial> for domain::RawMaterial {
    type Error = AppError;
    fn try_from(m: &proto::RawMaterial) -> AppResult<Self> {
        Ok(domain::RawMaterial {
            id: parse_uuid(&m.id)?,
            name: m.name.clone(),
            unit: m.unit.clone(),
            current_quantity: m.current_quantity,
            last_updated: timestamp_to_datetime(m.last_updated),
        })
    }
}

impl TryFrom<&proto::FinishedGood> for domain::FinishedGood {
    type Error = AppError;
    fn try_from(g: &proto::FinishedGood) -> AppResult<Self> {
        Ok(domain::FinishedGood {
            id: parse_uuid(&g.id)?,
            name: g.name.clone(),
            current_quantity: g.current_quantity,
            unit_price: g.unit_price,
            last_updated: timestamp_to_datetime(g.last_updated),
        })
    }
}

impl TryFrom<&proto::RecipeIngredient> for domain::RecipeIngredient {
    type Error = AppError;
    fn try_from(i: &proto::RecipeIngredient) -> AppResult<Self> {
        Ok(domain::RecipeIngredient {
            raw_material_id: parse_uuid(&i.raw_material_id)?,
            raw_material_name: i.raw_material_name.clone(),
            required_quantity: i.required_quantity,
        })
    }
}

impl TryFrom<&proto::Recipe> for domain::Recipe {
    type Error = AppError;
    fn try_from(r: &proto::Recipe) -> AppResult<Self> {
        let ingredients: AppResult<Vec<domain::RecipeIngredient>> = r
            .ingredients
            .iter()
            .map(domain::RecipeIngredient::try_from)
            .collect();
        Ok(domain::Recipe {
            id: parse_uuid(&r.id)?,
            name: r.name.clone(),
            finished_good_id: parse_uuid(&r.finished_good_id)?,
            finished_good_name: r.finished_good_name.clone(),
            ingredients: ingredients?,
        })
    }
}

impl TryFrom<&proto::ProductionLog> for domain::ProductionLog {
    type Error = AppError;
    fn try_from(p: &proto::ProductionLog) -> AppResult<Self> {
        let mut materials_consumed = HashMap::new();
        for (k, v) in &p.materials_consumed {
            materials_consumed.insert(parse_uuid(k)?, *v);
        }
        Ok(domain::ProductionLog {
            id: parse_uuid(&p.id)?,
            recipe_id: parse_uuid(&p.recipe_id)?,
            recipe_name: p.recipe_name.clone(),
            chef_id: parse_uuid(&p.chef_id)?,
            chef_name: p.chef_name.clone(),
            production_quantity: p.production_quantity,
            materials_consumed,
            finished_good_id: parse_uuid(&p.finished_good_id)?,
            timestamp: timestamp_to_datetime(p.timestamp),
        })
    }
}

impl TryFrom<&proto::Customer> for domain::Customer {
    type Error = AppError;
    fn try_from(c: &proto::Customer) -> AppResult<Self> {
        Ok(domain::Customer {
            id: parse_uuid(&c.id)?,
            name: c.name.clone(),
            city: c.city.clone(),
            mobile: c.mobile.clone(),
            reliability_rating: c.reliability_rating,
            total_debt: c.total_debt,
            overdue_days: c.overdue_days,
        })
    }
}

impl TryFrom<&proto::SaleLineItem> for domain::SaleLineItem {
    type Error = AppError;
    fn try_from(li: &proto::SaleLineItem) -> AppResult<Self> {
        Ok(domain::SaleLineItem {
            finished_good_id: parse_uuid(&li.finished_good_id)?,
            finished_good_name: li.finished_good_name.clone(),
            quantity: li.quantity,
            unit_price: li.unit_price,
        })
    }
}

impl TryFrom<&proto::Sale> for domain::Sale {
    type Error = AppError;
    fn try_from(s: &proto::Sale) -> AppResult<Self> {
        let line_items: AppResult<Vec<domain::SaleLineItem>> = s
            .line_items
            .iter()
            .map(domain::SaleLineItem::try_from)
            .collect();
        Ok(domain::Sale {
            id: parse_uuid(&s.id)?,
            customer_id: parse_uuid(&s.customer_id)?,
            customer_name: s.customer_name.clone(),
            line_items: line_items?,
            total_amount: s.total_amount,
            amount_paid: s.amount_paid,
            payment_wallet_id: parse_uuid(&s.payment_wallet_id)?,
            timestamp: timestamp_to_datetime(s.timestamp),
        })
    }
}

impl TryFrom<&proto::Wallet> for domain::Wallet {
    type Error = AppError;
    fn try_from(w: &proto::Wallet) -> AppResult<Self> {
        Ok(domain::Wallet {
            id: parse_uuid(&w.id)?,
            name: w.name.clone(),
            wallet_type: proto_wallet_type_to_domain(w.wallet_type)?,
            current_balance: w.current_balance,
        })
    }
}

impl TryFrom<&proto::WalletTransaction> for domain::WalletTransaction {
    type Error = AppError;
    fn try_from(t: &proto::WalletTransaction) -> AppResult<Self> {
        let related_entity_id = if t.related_entity_id.is_empty() {
            None
        } else {
            Some(parse_uuid(&t.related_entity_id)?)
        };
        Ok(domain::WalletTransaction {
            id: parse_uuid(&t.id)?,
            wallet_id: parse_uuid(&t.wallet_id)?,
            amount: t.amount,
            description: t.description.clone(),
            related_entity_id,
            timestamp: timestamp_to_datetime(t.timestamp),
        })
    }
}

impl TryFrom<&proto::DebtRecord> for domain::DebtRecord {
    type Error = AppError;
    fn try_from(d: &proto::DebtRecord) -> AppResult<Self> {
        Ok(domain::DebtRecord {
            id: parse_uuid(&d.id)?,
            customer_id: parse_uuid(&d.customer_id)?,
            customer_name: d.customer_name.clone(),
            sale_id: parse_uuid(&d.sale_id)?,
            original_amount: d.original_amount,
            remaining_amount: d.remaining_amount,
            sale_date: timestamp_to_datetime(d.sale_date),
            overdue_days: d.overdue_days,
            is_critical: d.is_critical,
            is_settled: d.is_settled,
        })
    }
}

impl TryFrom<&proto::Expense> for domain::Expense {
    type Error = AppError;
    fn try_from(e: &proto::Expense) -> AppResult<Self> {
        Ok(domain::Expense {
            id: parse_uuid(&e.id)?,
            description: e.description.clone(),
            amount: e.amount,
            category: proto_expense_category_to_domain(e.category)?,
            wallet_id: parse_uuid(&e.wallet_id)?,
            wallet_name: e.wallet_name.clone(),
            recorded_by: parse_uuid(&e.recorded_by)?,
            timestamp: timestamp_to_datetime(e.timestamp),
        })
    }
}

impl TryFrom<&proto::SyncQueueItem> for domain::SyncQueueItem {
    type Error = AppError;
    fn try_from(s: &proto::SyncQueueItem) -> AppResult<Self> {
        Ok(domain::SyncQueueItem {
            id: parse_uuid(&s.id)?,
            entity_type: s.entity_type.clone(),
            entity_id: parse_uuid(&s.entity_id)?,
            operation: s.operation.clone(),
            payload: s.payload.clone(),
            created_at: timestamp_to_datetime(s.created_at),
            status: proto_sync_status_to_domain(s.status)?,
        })
    }
}

impl TryFrom<&proto::ConflictLog> for domain::ConflictLog {
    type Error = AppError;
    fn try_from(c: &proto::ConflictLog) -> AppResult<Self> {
        Ok(domain::ConflictLog {
            id: parse_uuid(&c.id)?,
            entity_type: c.entity_type.clone(),
            entity_id: parse_uuid(&c.entity_id)?,
            local_version: c.local_version.clone(),
            remote_version: c.remote_version.clone(),
            resolved_with: c.resolved_with.clone(),
            timestamp: timestamp_to_datetime(c.timestamp),
        })
    }
}

// ── JSON schema validation ─────────────────────────────────────────────────

/// Validates a JSON string by deserializing it into the specified domain type
/// and checking that required fields are non-empty and numeric values are valid.
/// Returns `AppError::Serialization` for invalid data.
pub fn validate_json<T>(json: &str) -> AppResult<T>
where
    T: serde::de::DeserializeOwned,
{
    serde_json::from_str::<T>(json)
        .map_err(|e| AppError::Serialization(format!("JSON deserialization failed: {}", e)))
}

/// Validates that a string field is non-empty.
pub fn validate_non_empty(field_name: &str, value: &str) -> AppResult<()> {
    if value.trim().is_empty() {
        return Err(AppError::Serialization(format!(
            "Field '{}' must not be empty",
            field_name
        )));
    }
    Ok(())
}

/// Validates that a numeric value is non-negative.
pub fn validate_non_negative(field_name: &str, value: f64) -> AppResult<()> {
    if value < 0.0 {
        return Err(AppError::Serialization(format!(
            "Field '{}' must be non-negative, got {}",
            field_name, value
        )));
    }
    Ok(())
}

/// Validates that a numeric value is positive (> 0).
pub fn validate_positive(field_name: &str, value: f64) -> AppResult<()> {
    if value <= 0.0 {
        return Err(AppError::Serialization(format!(
            "Field '{}' must be positive, got {}",
            field_name, value
        )));
    }
    Ok(())
}

/// Validates that an integer is within a given inclusive range.
pub fn validate_range(field_name: &str, value: i32, min: i32, max: i32) -> AppResult<()> {
    if value < min || value > max {
        return Err(AppError::Serialization(format!(
            "Field '{}' must be between {} and {}, got {}",
            field_name, min, max, value
        )));
    }
    Ok(())
}

/// Validates a deserialized `RawMaterial` has valid field values.
pub fn validate_raw_material(m: &domain::RawMaterial) -> AppResult<()> {
    validate_non_empty("name", &m.name)?;
    validate_non_empty("unit", &m.unit)?;
    validate_non_negative("current_quantity", m.current_quantity)?;
    Ok(())
}

/// Validates a deserialized `FinishedGood` has valid field values.
pub fn validate_finished_good(g: &domain::FinishedGood) -> AppResult<()> {
    validate_non_empty("name", &g.name)?;
    validate_non_negative("current_quantity", g.current_quantity)?;
    validate_non_negative("unit_price", g.unit_price)?;
    Ok(())
}

/// Validates a deserialized `Customer` has valid field values.
pub fn validate_customer(c: &domain::Customer) -> AppResult<()> {
    validate_non_empty("name", &c.name)?;
    validate_non_empty("city", &c.city)?;
    validate_non_empty("mobile", &c.mobile)?;
    validate_range("reliability_rating", c.reliability_rating, 0, 5)?;
    validate_non_negative("total_debt", c.total_debt)?;
    Ok(())
}

/// Validates a deserialized `AppUser` has valid field values.
pub fn validate_app_user(u: &domain::AppUser) -> AppResult<()> {
    validate_non_empty("username", &u.username)?;
    validate_non_empty("full_name", &u.full_name)?;
    validate_non_empty("password_hash", &u.password_hash)?;
    Ok(())
}

/// Validates a deserialized `Expense` has valid field values.
pub fn validate_expense(e: &domain::Expense) -> AppResult<()> {
    validate_non_empty("description", &e.description)?;
    validate_positive("amount", e.amount)?;
    Ok(())
}

/// Validates a deserialized `DebtRecord` has valid field values.
pub fn validate_debt_record(d: &domain::DebtRecord) -> AppResult<()> {
    validate_positive("original_amount", d.original_amount)?;
    validate_non_negative("remaining_amount", d.remaining_amount)?;
    Ok(())
}

/// Validates a deserialized `Sale` has valid field values.
pub fn validate_sale(s: &domain::Sale) -> AppResult<()> {
    validate_non_negative("total_amount", s.total_amount)?;
    validate_non_negative("amount_paid", s.amount_paid)?;
    if s.line_items.is_empty() {
        return Err(AppError::Serialization(
            "Sale must have at least one line item".to_string(),
        ));
    }
    for li in &s.line_items {
        validate_non_empty("finished_good_name", &li.finished_good_name)?;
        validate_non_negative("unit_price", li.unit_price)?;
        if li.quantity <= 0 {
            return Err(AppError::Serialization(
                "SaleLineItem quantity must be positive".to_string(),
            ));
        }
    }
    Ok(())
}

/// Generic: deserialize JSON and validate using a provided validator function.
pub fn validate_and_deserialize<T, F>(json: &str, validator: F) -> AppResult<T>
where
    T: serde::de::DeserializeOwned,
    F: FnOnce(&T) -> AppResult<()>,
{
    let obj: T = validate_json(json)?;
    validator(&obj)?;
    Ok(obj)
}

#[cfg(test)]
mod tests {
    use super::*;
    use uuid::Uuid;

    #[test]
    fn test_domain_to_proto_and_back_app_user() {
        let user = domain::AppUser {
            id: Uuid::new_v4(),
            username: "admin".to_string(),
            full_name: "Admin User".to_string(),
            role: domain::UserRole::Admin,
            password_hash: "hash123".to_string(),
        };
        let proto_user = proto::AppUser::from(&user);
        let roundtrip = domain::AppUser::try_from(&proto_user).unwrap();
        assert_eq!(user.id, roundtrip.id);
        assert_eq!(user.username, roundtrip.username);
        assert_eq!(user.role, roundtrip.role);
    }

    #[test]
    fn test_domain_to_proto_and_back_raw_material() {
        let mat = domain::RawMaterial {
            id: Uuid::new_v4(),
            name: "Sugar".to_string(),
            unit: "kg".to_string(),
            current_quantity: 100.5,
            last_updated: Utc::now(),
        };
        let proto_mat = proto::RawMaterial::from(&mat);
        let roundtrip = domain::RawMaterial::try_from(&proto_mat).unwrap();
        assert_eq!(mat.id, roundtrip.id);
        assert_eq!(mat.name, roundtrip.name);
        assert_eq!(mat.current_quantity, roundtrip.current_quantity);
    }

    #[test]
    fn test_invalid_uuid_returns_serialization_error() {
        let proto_user = proto::AppUser {
            id: "not-a-uuid".to_string(),
            username: "test".to_string(),
            full_name: "Test".to_string(),
            role: proto::UserRole::Admin as i32,
            password_hash: "hash".to_string(),
        };
        let result = domain::AppUser::try_from(&proto_user);
        assert!(result.is_err());
        assert!(matches!(result.unwrap_err(), AppError::Serialization(_)));
    }

    #[test]
    fn test_invalid_enum_value_returns_error() {
        let result = proto_user_role_to_domain(99);
        assert!(result.is_err());
        assert!(matches!(result.unwrap_err(), AppError::Serialization(_)));
    }

    #[test]
    fn test_validate_json_valid() {
        let json = r#"{"id":"550e8400-e29b-41d4-a716-446655440000","name":"Sugar","unit":"kg","current_quantity":50.0,"last_updated":"2024-01-01T00:00:00Z"}"#;
        let result: AppResult<domain::RawMaterial> = validate_json(json);
        assert!(result.is_ok());
    }

    #[test]
    fn test_validate_json_invalid() {
        let json = r#"{"bad": "data"}"#;
        let result: AppResult<domain::RawMaterial> = validate_json(json);
        assert!(result.is_err());
        assert!(matches!(result.unwrap_err(), AppError::Serialization(_)));
    }

    #[test]
    fn test_validate_non_empty_rejects_blank() {
        assert!(validate_non_empty("name", "").is_err());
        assert!(validate_non_empty("name", "  ").is_err());
        assert!(validate_non_empty("name", "valid").is_ok());
    }

    #[test]
    fn test_validate_non_negative_rejects_negative() {
        assert!(validate_non_negative("qty", -1.0).is_err());
        assert!(validate_non_negative("qty", 0.0).is_ok());
        assert!(validate_non_negative("qty", 5.0).is_ok());
    }

    #[test]
    fn test_validate_range() {
        assert!(validate_range("rating", 0, 0, 5).is_ok());
        assert!(validate_range("rating", 5, 0, 5).is_ok());
        assert!(validate_range("rating", -1, 0, 5).is_err());
        assert!(validate_range("rating", 6, 0, 5).is_err());
    }

    #[test]
    fn test_validate_and_deserialize_rejects_invalid_data() {
        let json = r#"{"id":"550e8400-e29b-41d4-a716-446655440000","name":"","unit":"kg","current_quantity":50.0,"last_updated":"2024-01-01T00:00:00Z"}"#;
        let result = validate_and_deserialize::<domain::RawMaterial, _>(json, validate_raw_material);
        assert!(result.is_err());
    }
}
