//! FFI-safe DTO structs and enums for the Android mobile app.
//!
//! Each struct is annotated with `#[derive(uniffi::Record)]` and each enum
//! with `#[derive(uniffi::Enum)]`. Conversion functions (`From`/`Into`) bridge
//! between these FFI DTOs and the internal crate DTOs, converting
//! `chrono::DateTime` to/from ISO 8601 strings and `EntityId` (u64) directly.

use chrono::{DateTime, Utc};

// ── Helpers ──────────────────────────────────────────────────────────────────

fn dt_to_string(dt: DateTime<Utc>) -> String {
    dt.to_rfc3339()
}

fn string_to_dt(s: &str) -> DateTime<Utc> {
    DateTime::parse_from_rfc3339(s)
        .map(|dt| dt.with_timezone(&Utc))
        .unwrap_or_default()
}

// ═══════════════════════════════════════════════════════════════════════════════
// Product
// ═══════════════════════════════════════════════════════════════════════════════

#[derive(Debug, Clone, uniffi::Enum)]
pub enum FfiProductStatus {
    Available,
    OutOfStock,
    Discontinued,
    Reserved,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiProductDto {
    pub id: u64,
    pub created_at: String,
    pub updated_at: String,
    pub name: String,
    pub reference: String,
    pub description: String,
    pub quantity: i64,
    pub price_unit: f64,
    pub status: FfiProductStatus,
    pub category_id: Option<u64>,
    pub supplier_id: Option<u64>,
    pub location_id: Option<u64>,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiCreateProductDto {
    pub name: String,
    pub reference: String,
    pub description: String,
    pub quantity: i64,
    pub price_unit: f64,
    pub status: FfiProductStatus,
    pub category_id: Option<u64>,
    pub supplier_id: Option<u64>,
    pub location_id: Option<u64>,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiUpdateProductDto {
    pub id: u64,
    pub name: String,
    pub reference: String,
    pub description: String,
    pub quantity: i64,
    pub price_unit: f64,
    pub status: FfiProductStatus,
}

// -- Product conversions --

impl From<common::entities::ProductStatus> for FfiProductStatus {
    fn from(s: common::entities::ProductStatus) -> Self {
        match s {
            common::entities::ProductStatus::Available => FfiProductStatus::Available,
            common::entities::ProductStatus::OutOfStock => FfiProductStatus::OutOfStock,
            common::entities::ProductStatus::Discontinued => FfiProductStatus::Discontinued,
            common::entities::ProductStatus::Reserved => FfiProductStatus::Reserved,
        }
    }
}

impl From<FfiProductStatus> for common::entities::ProductStatus {
    fn from(s: FfiProductStatus) -> Self {
        match s {
            FfiProductStatus::Available => common::entities::ProductStatus::Available,
            FfiProductStatus::OutOfStock => common::entities::ProductStatus::OutOfStock,
            FfiProductStatus::Discontinued => common::entities::ProductStatus::Discontinued,
            FfiProductStatus::Reserved => common::entities::ProductStatus::Reserved,
        }
    }
}

impl From<direct_access::product::dtos::ProductDto> for FfiProductDto {
    fn from(d: direct_access::product::dtos::ProductDto) -> Self {
        FfiProductDto {
            id: d.id,
            created_at: dt_to_string(d.created_at),
            updated_at: dt_to_string(d.updated_at),
            name: d.name,
            reference: d.reference,
            description: d.description,
            quantity: d.quantity,
            price_unit: d.price_unit,
            status: d.status.into(),
            category_id: d.category,
            supplier_id: d.supplier,
            location_id: d.location,
        }
    }
}

impl From<FfiCreateProductDto> for direct_access::product::dtos::CreateProductDto {
    fn from(d: FfiCreateProductDto) -> Self {
        let now = Utc::now();
        direct_access::product::dtos::CreateProductDto {
            created_at: now,
            updated_at: now,
            name: d.name,
            reference: d.reference,
            description: d.description,
            quantity: d.quantity,
            price_unit: d.price_unit,
            status: d.status.into(),
            category: d.category_id,
            supplier: d.supplier_id,
            location: d.location_id,
        }
    }
}

impl From<FfiUpdateProductDto> for direct_access::product::dtos::UpdateProductDto {
    fn from(d: FfiUpdateProductDto) -> Self {
        direct_access::product::dtos::UpdateProductDto {
            id: d.id,
            created_at: Utc::now(),
            updated_at: Utc::now(),
            name: d.name,
            reference: d.reference,
            description: d.description,
            quantity: d.quantity,
            price_unit: d.price_unit,
            status: d.status.into(),
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Category
// ═══════════════════════════════════════════════════════════════════════════════

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiCategoryDto {
    pub id: u64,
    pub created_at: String,
    pub updated_at: String,
    pub name: String,
    pub description: String,
    pub parent_category_id: Option<u64>,
    pub subcategory_ids: Vec<u64>,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiCreateCategoryDto {
    pub name: String,
    pub description: String,
    pub parent_category_id: Option<u64>,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiUpdateCategoryDto {
    pub id: u64,
    pub name: String,
    pub description: String,
}

impl From<direct_access::category::dtos::CategoryDto> for FfiCategoryDto {
    fn from(d: direct_access::category::dtos::CategoryDto) -> Self {
        FfiCategoryDto {
            id: d.id,
            created_at: dt_to_string(d.created_at),
            updated_at: dt_to_string(d.updated_at),
            name: d.name,
            description: d.description,
            parent_category_id: d.parent_category,
            subcategory_ids: d.subcategories,
        }
    }
}

impl From<FfiCreateCategoryDto> for direct_access::category::dtos::CreateCategoryDto {
    fn from(d: FfiCreateCategoryDto) -> Self {
        let now = Utc::now();
        direct_access::category::dtos::CreateCategoryDto {
            created_at: now,
            updated_at: now,
            name: d.name,
            description: d.description,
            parent_category: d.parent_category_id,
            subcategories: Vec::new(),
        }
    }
}

impl From<FfiUpdateCategoryDto> for direct_access::category::dtos::UpdateCategoryDto {
    fn from(d: FfiUpdateCategoryDto) -> Self {
        direct_access::category::dtos::UpdateCategoryDto {
            id: d.id,
            created_at: Utc::now(),
            updated_at: Utc::now(),
            name: d.name,
            description: d.description,
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Person
// ═══════════════════════════════════════════════════════════════════════════════

#[derive(Debug, Clone, uniffi::Enum)]
pub enum FfiPersonRole {
    Manager,
    Supplier,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiPersonDto {
    pub id: u64,
    pub created_at: String,
    pub updated_at: String,
    pub name: String,
    pub role: FfiPersonRole,
    pub contact_id: Option<u64>,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiCreatePersonDto {
    pub name: String,
    pub role: FfiPersonRole,
    pub contact_id: Option<u64>,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiUpdatePersonDto {
    pub id: u64,
    pub name: String,
    pub role: FfiPersonRole,
}

impl From<common::entities::PersonRole> for FfiPersonRole {
    fn from(r: common::entities::PersonRole) -> Self {
        match r {
            common::entities::PersonRole::Manager => FfiPersonRole::Manager,
            common::entities::PersonRole::Supplier => FfiPersonRole::Supplier,
        }
    }
}

impl From<FfiPersonRole> for common::entities::PersonRole {
    fn from(r: FfiPersonRole) -> Self {
        match r {
            FfiPersonRole::Manager => common::entities::PersonRole::Manager,
            FfiPersonRole::Supplier => common::entities::PersonRole::Supplier,
        }
    }
}

impl From<direct_access::person::dtos::PersonDto> for FfiPersonDto {
    fn from(d: direct_access::person::dtos::PersonDto) -> Self {
        FfiPersonDto {
            id: d.id,
            created_at: dt_to_string(d.created_at),
            updated_at: dt_to_string(d.updated_at),
            name: d.name,
            role: d.role.into(),
            contact_id: d.contact,
        }
    }
}

impl From<FfiCreatePersonDto> for direct_access::person::dtos::CreatePersonDto {
    fn from(d: FfiCreatePersonDto) -> Self {
        let now = Utc::now();
        direct_access::person::dtos::CreatePersonDto {
            created_at: now,
            updated_at: now,
            name: d.name,
            role: d.role.into(),
            contact: d.contact_id,
        }
    }
}

impl From<FfiUpdatePersonDto> for direct_access::person::dtos::UpdatePersonDto {
    fn from(d: FfiUpdatePersonDto) -> Self {
        direct_access::person::dtos::UpdatePersonDto {
            id: d.id,
            created_at: Utc::now(),
            updated_at: Utc::now(),
            name: d.name,
            role: d.role.into(),
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Contact
// ═══════════════════════════════════════════════════════════════════════════════

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiContactDto {
    pub id: u64,
    pub created_at: String,
    pub updated_at: String,
    pub phone: String,
    pub email: String,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiCreateContactDto {
    pub phone: String,
    pub email: String,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiUpdateContactDto {
    pub id: u64,
    pub phone: String,
    pub email: String,
}

impl From<direct_access::contact::dtos::ContactDto> for FfiContactDto {
    fn from(d: direct_access::contact::dtos::ContactDto) -> Self {
        FfiContactDto {
            id: d.id,
            created_at: dt_to_string(d.created_at),
            updated_at: dt_to_string(d.updated_at),
            phone: d.phone,
            email: d.email,
        }
    }
}

impl From<FfiCreateContactDto> for direct_access::contact::dtos::CreateContactDto {
    fn from(d: FfiCreateContactDto) -> Self {
        let now = Utc::now();
        direct_access::contact::dtos::CreateContactDto {
            created_at: now,
            updated_at: now,
            phone: d.phone,
            email: d.email,
        }
    }
}

impl From<FfiUpdateContactDto> for direct_access::contact::dtos::UpdateContactDto {
    fn from(d: FfiUpdateContactDto) -> Self {
        direct_access::contact::dtos::UpdateContactDto {
            id: d.id,
            created_at: Utc::now(),
            updated_at: Utc::now(),
            phone: d.phone,
            email: d.email,
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Deal
// ═══════════════════════════════════════════════════════════════════════════════

#[derive(Debug, Clone, uniffi::Enum)]
pub enum FfiDealFrequency {
    OneTime,
    Weekly,
    Monthly,
    Quarterly,
    Yearly,
}

#[derive(Debug, Clone, uniffi::Enum)]
pub enum FfiDealStatus {
    Draft,
    Active,
    Completed,
    Cancelled,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiDealDto {
    pub id: u64,
    pub created_at: String,
    pub updated_at: String,
    pub title: String,
    pub description: String,
    pub unit_cost: f64,
    pub total_value: f64,
    pub start_date: String,
    pub end_date: String,
    pub frequency: FfiDealFrequency,
    pub status: FfiDealStatus,
    pub product_id: Option<u64>,
    pub supplier_id: Option<u64>,
    pub manager_id: Option<u64>,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiCreateDealDto {
    pub title: String,
    pub description: String,
    pub unit_cost: f64,
    pub total_value: f64,
    pub start_date: String,
    pub end_date: String,
    pub frequency: FfiDealFrequency,
    pub status: FfiDealStatus,
    pub product_id: Option<u64>,
    pub supplier_id: Option<u64>,
    pub manager_id: Option<u64>,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiUpdateDealDto {
    pub id: u64,
    pub title: String,
    pub description: String,
    pub unit_cost: f64,
    pub total_value: f64,
    pub start_date: String,
    pub end_date: String,
    pub frequency: FfiDealFrequency,
    pub status: FfiDealStatus,
}

impl From<common::entities::DealFrequency> for FfiDealFrequency {
    fn from(f: common::entities::DealFrequency) -> Self {
        match f {
            common::entities::DealFrequency::OneTime => FfiDealFrequency::OneTime,
            common::entities::DealFrequency::Weekly => FfiDealFrequency::Weekly,
            common::entities::DealFrequency::Monthly => FfiDealFrequency::Monthly,
            common::entities::DealFrequency::Quarterly => FfiDealFrequency::Quarterly,
            common::entities::DealFrequency::Yearly => FfiDealFrequency::Yearly,
        }
    }
}

impl From<FfiDealFrequency> for common::entities::DealFrequency {
    fn from(f: FfiDealFrequency) -> Self {
        match f {
            FfiDealFrequency::OneTime => common::entities::DealFrequency::OneTime,
            FfiDealFrequency::Weekly => common::entities::DealFrequency::Weekly,
            FfiDealFrequency::Monthly => common::entities::DealFrequency::Monthly,
            FfiDealFrequency::Quarterly => common::entities::DealFrequency::Quarterly,
            FfiDealFrequency::Yearly => common::entities::DealFrequency::Yearly,
        }
    }
}

impl From<common::entities::DealStatus> for FfiDealStatus {
    fn from(s: common::entities::DealStatus) -> Self {
        match s {
            common::entities::DealStatus::Draft => FfiDealStatus::Draft,
            common::entities::DealStatus::Active => FfiDealStatus::Active,
            common::entities::DealStatus::Completed => FfiDealStatus::Completed,
            common::entities::DealStatus::Cancelled => FfiDealStatus::Cancelled,
        }
    }
}

impl From<FfiDealStatus> for common::entities::DealStatus {
    fn from(s: FfiDealStatus) -> Self {
        match s {
            FfiDealStatus::Draft => common::entities::DealStatus::Draft,
            FfiDealStatus::Active => common::entities::DealStatus::Active,
            FfiDealStatus::Completed => common::entities::DealStatus::Completed,
            FfiDealStatus::Cancelled => common::entities::DealStatus::Cancelled,
        }
    }
}

impl From<direct_access::deal::dtos::DealDto> for FfiDealDto {
    fn from(d: direct_access::deal::dtos::DealDto) -> Self {
        FfiDealDto {
            id: d.id,
            created_at: dt_to_string(d.created_at),
            updated_at: dt_to_string(d.updated_at),
            title: d.title,
            description: d.description,
            unit_cost: d.unit_cost,
            total_value: d.total_value,
            start_date: dt_to_string(d.start_date),
            end_date: dt_to_string(d.end_date),
            frequency: d.frequency.into(),
            status: d.status.into(),
            product_id: d.product,
            supplier_id: d.supplier,
            manager_id: d.manager,
        }
    }
}

impl From<FfiCreateDealDto> for direct_access::deal::dtos::CreateDealDto {
    fn from(d: FfiCreateDealDto) -> Self {
        let now = Utc::now();
        direct_access::deal::dtos::CreateDealDto {
            created_at: now,
            updated_at: now,
            title: d.title,
            description: d.description,
            unit_cost: d.unit_cost,
            total_value: d.total_value,
            start_date: string_to_dt(&d.start_date),
            end_date: string_to_dt(&d.end_date),
            frequency: d.frequency.into(),
            status: d.status.into(),
            product: d.product_id,
            supplier: d.supplier_id,
            manager: d.manager_id,
        }
    }
}

impl From<FfiUpdateDealDto> for direct_access::deal::dtos::UpdateDealDto {
    fn from(d: FfiUpdateDealDto) -> Self {
        direct_access::deal::dtos::UpdateDealDto {
            id: d.id,
            created_at: Utc::now(),
            updated_at: Utc::now(),
            title: d.title,
            description: d.description,
            unit_cost: d.unit_cost,
            total_value: d.total_value,
            start_date: string_to_dt(&d.start_date),
            end_date: string_to_dt(&d.end_date),
            frequency: d.frequency.into(),
            status: d.status.into(),
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Location
// ═══════════════════════════════════════════════════════════════════════════════

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiLocationDto {
    pub id: u64,
    pub created_at: String,
    pub updated_at: String,
    pub name: String,
    pub address: String,
    pub latitude: f64,
    pub longitude: f64,
    pub capacity: i64,
    pub manager_id: Option<u64>,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiCreateLocationDto {
    pub name: String,
    pub address: String,
    pub latitude: f64,
    pub longitude: f64,
    pub capacity: i64,
    pub manager_id: Option<u64>,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiUpdateLocationDto {
    pub id: u64,
    pub name: String,
    pub address: String,
    pub latitude: f64,
    pub longitude: f64,
    pub capacity: i64,
    pub manager_id: Option<u64>,
}

impl From<direct_access::location::dtos::LocationDto> for FfiLocationDto {
    fn from(d: direct_access::location::dtos::LocationDto) -> Self {
        FfiLocationDto {
            id: d.id,
            created_at: dt_to_string(d.created_at),
            updated_at: dt_to_string(d.updated_at),
            name: d.name,
            address: d.address,
            latitude: d.latitude,
            longitude: d.longitude,
            capacity: d.capacity,
            manager_id: d.manager,
        }
    }
}

impl From<FfiCreateLocationDto> for direct_access::location::dtos::CreateLocationDto {
    fn from(d: FfiCreateLocationDto) -> Self {
        let now = Utc::now();
        direct_access::location::dtos::CreateLocationDto {
            created_at: now,
            updated_at: now,
            name: d.name,
            address: d.address,
            latitude: d.latitude,
            longitude: d.longitude,
            capacity: d.capacity,
            manager: d.manager_id,
        }
    }
}

impl From<FfiUpdateLocationDto> for direct_access::location::dtos::UpdateLocationDto {
    fn from(d: FfiUpdateLocationDto) -> Self {
        direct_access::location::dtos::UpdateLocationDto {
            id: d.id,
            created_at: Utc::now(),
            updated_at: Utc::now(),
            name: d.name,
            address: d.address,
            latitude: d.latitude,
            longitude: d.longitude,
            capacity: d.capacity,
            manager: d.manager_id,
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// User
// ═══════════════════════════════════════════════════════════════════════════════

#[derive(Debug, Clone, uniffi::Enum)]
pub enum FfiUserRole {
    Admin,
    Manager,
    Operator,
    Viewer,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiUserDto {
    pub id: u64,
    pub created_at: String,
    pub updated_at: String,
    pub username: String,
    pub display_name: String,
    pub role: FfiUserRole,
    pub is_active: bool,
    pub person_id: Option<u64>,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiCreateUserDto {
    pub username: String,
    pub password: String,
    pub display_name: String,
    pub role: FfiUserRole,
    pub person_id: Option<u64>,
}

impl From<common::entities::UserRole> for FfiUserRole {
    fn from(r: common::entities::UserRole) -> Self {
        match r {
            common::entities::UserRole::Admin => FfiUserRole::Admin,
            common::entities::UserRole::Manager => FfiUserRole::Manager,
            common::entities::UserRole::Operator => FfiUserRole::Operator,
            common::entities::UserRole::Viewer => FfiUserRole::Viewer,
        }
    }
}

impl From<FfiUserRole> for common::entities::UserRole {
    fn from(r: FfiUserRole) -> Self {
        match r {
            FfiUserRole::Admin => common::entities::UserRole::Admin,
            FfiUserRole::Manager => common::entities::UserRole::Manager,
            FfiUserRole::Operator => common::entities::UserRole::Operator,
            FfiUserRole::Viewer => common::entities::UserRole::Viewer,
        }
    }
}

impl From<FfiUserRole> for user_management::dtos::CreateUserRole {
    fn from(r: FfiUserRole) -> Self {
        match r {
            FfiUserRole::Admin => user_management::dtos::CreateUserRole::Admin,
            FfiUserRole::Manager => user_management::dtos::CreateUserRole::Manager,
            FfiUserRole::Operator => user_management::dtos::CreateUserRole::Operator,
            FfiUserRole::Viewer => user_management::dtos::CreateUserRole::Viewer,
        }
    }
}

impl From<direct_access::user::dtos::UserDto> for FfiUserDto {
    fn from(d: direct_access::user::dtos::UserDto) -> Self {
        FfiUserDto {
            id: d.id,
            created_at: dt_to_string(d.created_at),
            updated_at: dt_to_string(d.updated_at),
            username: d.username,
            display_name: d.display_name,
            role: d.role.into(),
            is_active: d.is_active,
            person_id: d.person,
        }
    }
}

impl From<FfiCreateUserDto> for user_management::dtos::CreateUserDto {
    fn from(d: FfiCreateUserDto) -> Self {
        user_management::dtos::CreateUserDto {
            username: d.username,
            password: d.password,
            display_name: d.display_name,
            role: d.role.into(),
            person_id: d.person_id.map(|id| id as i64).unwrap_or(0),
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// StockMovement
// ═══════════════════════════════════════════════════════════════════════════════

#[derive(Debug, Clone, uniffi::Enum)]
pub enum FfiMovementType {
    Inbound,
    Outbound,
    Transfer,
    Adjustment,
    Return,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiStockMovementDto {
    pub id: u64,
    pub created_at: String,
    pub updated_at: String,
    pub movement_type: FfiMovementType,
    pub quantity: i64,
    pub note: String,
    pub product_id: Option<u64>,
    pub from_location_id: Option<u64>,
    pub to_location_id: Option<u64>,
    pub performed_by_id: Option<u64>,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiRecordStockMovementDto {
    pub product_id: u64,
    pub movement_type: FfiMovementType,
    pub quantity: i64,
    pub from_location_id: u64,
    pub to_location_id: u64,
    pub note: String,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiStockMovementResultDto {
    pub movement_id: i64,
    pub new_product_quantity: i64,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiStockSummaryDto {
    pub product_ids: Vec<i64>,
    pub product_names: Vec<String>,
    pub current_quantities: Vec<i64>,
    pub inbound_30d: Vec<i64>,
    pub outbound_30d: Vec<i64>,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiStockHistoryDto {
    pub movement_ids: Vec<i64>,
    pub movement_types: Vec<String>,
    pub quantities: Vec<i64>,
    pub dates: Vec<String>,
    pub running_totals: Vec<i64>,
}

impl From<common::entities::MovementType> for FfiMovementType {
    fn from(m: common::entities::MovementType) -> Self {
        match m {
            common::entities::MovementType::Inbound => FfiMovementType::Inbound,
            common::entities::MovementType::Outbound => FfiMovementType::Outbound,
            common::entities::MovementType::Transfer => FfiMovementType::Transfer,
            common::entities::MovementType::Adjustment => FfiMovementType::Adjustment,
            common::entities::MovementType::Return => FfiMovementType::Return,
        }
    }
}

impl From<FfiMovementType> for common::entities::MovementType {
    fn from(m: FfiMovementType) -> Self {
        match m {
            FfiMovementType::Inbound => common::entities::MovementType::Inbound,
            FfiMovementType::Outbound => common::entities::MovementType::Outbound,
            FfiMovementType::Transfer => common::entities::MovementType::Transfer,
            FfiMovementType::Adjustment => common::entities::MovementType::Adjustment,
            FfiMovementType::Return => common::entities::MovementType::Return,
        }
    }
}

impl From<FfiMovementType> for stock_tracking::dtos::MovementTypeInput {
    fn from(m: FfiMovementType) -> Self {
        match m {
            FfiMovementType::Inbound => stock_tracking::dtos::MovementTypeInput::Inbound,
            FfiMovementType::Outbound => stock_tracking::dtos::MovementTypeInput::Outbound,
            FfiMovementType::Transfer => stock_tracking::dtos::MovementTypeInput::Transfer,
            FfiMovementType::Adjustment => stock_tracking::dtos::MovementTypeInput::Adjustment,
            FfiMovementType::Return => stock_tracking::dtos::MovementTypeInput::Return,
        }
    }
}

impl From<direct_access::stock_movement::dtos::StockMovementDto> for FfiStockMovementDto {
    fn from(d: direct_access::stock_movement::dtos::StockMovementDto) -> Self {
        FfiStockMovementDto {
            id: d.id,
            created_at: dt_to_string(d.created_at),
            updated_at: dt_to_string(d.updated_at),
            movement_type: d.movement_type.into(),
            quantity: d.quantity,
            note: d.note,
            product_id: d.product,
            from_location_id: d.from_location,
            to_location_id: d.to_location,
            performed_by_id: d.performed_by,
        }
    }
}

impl From<FfiRecordStockMovementDto> for stock_tracking::dtos::RecordStockMovementDto {
    fn from(d: FfiRecordStockMovementDto) -> Self {
        stock_tracking::dtos::RecordStockMovementDto {
            product_id: d.product_id as i64,
            movement_type: d.movement_type.into(),
            quantity: d.quantity,
            from_location_id: d.from_location_id as i64,
            to_location_id: d.to_location_id as i64,
            note: d.note,
        }
    }
}

impl From<stock_tracking::dtos::StockSummaryDto> for FfiStockSummaryDto {
    fn from(d: stock_tracking::dtos::StockSummaryDto) -> Self {
        FfiStockSummaryDto {
            product_ids: d.product_ids,
            product_names: d.product_names,
            current_quantities: d.current_quantities,
            inbound_30d: d.inbound_30d,
            outbound_30d: d.outbound_30d,
        }
    }
}

impl From<stock_tracking::dtos::StockHistoryDto> for FfiStockHistoryDto {
    fn from(d: stock_tracking::dtos::StockHistoryDto) -> Self {
        FfiStockHistoryDto {
            movement_ids: d.movement_ids,
            movement_types: d.movement_types,
            quantities: d.quantities,
            dates: d.dates,
            running_totals: d.running_totals,
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// BudgetEntry
// ═══════════════════════════════════════════════════════════════════════════════

#[derive(Debug, Clone, uniffi::Enum)]
pub enum FfiBudgetEntryType {
    Purchase,
    Sale,
    Expense,
    Forecast,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiBudgetEntryDto {
    pub id: u64,
    pub created_at: String,
    pub updated_at: String,
    pub entry_type: FfiBudgetEntryType,
    pub amount: f64,
    pub description: String,
    pub entry_date: String,
    pub product_id: Option<u64>,
    pub deal_id: Option<u64>,
    pub recorded_by_id: Option<u64>,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiRecordBudgetEntryDto {
    pub entry_type: FfiBudgetEntryType,
    pub amount: f64,
    pub description: String,
    pub entry_date: String,
    pub product_id: u64,
    pub deal_id: u64,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiBudgetSummaryDto {
    pub total_purchases: f64,
    pub total_sales: f64,
    pub total_expenses: f64,
    pub net_balance: f64,
    pub monthly_labels: Vec<String>,
    pub monthly_purchases: Vec<f64>,
    pub monthly_sales: Vec<f64>,
    pub monthly_expenses: Vec<f64>,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiBudgetProjectionDto {
    pub month_labels: Vec<String>,
    pub projected_income: Vec<f64>,
    pub projected_expenses: Vec<f64>,
    pub projected_balance: Vec<f64>,
    pub recurring_deal_costs: Vec<f64>,
}

impl From<common::entities::BudgetEntryType> for FfiBudgetEntryType {
    fn from(t: common::entities::BudgetEntryType) -> Self {
        match t {
            common::entities::BudgetEntryType::Purchase => FfiBudgetEntryType::Purchase,
            common::entities::BudgetEntryType::Sale => FfiBudgetEntryType::Sale,
            common::entities::BudgetEntryType::Expense => FfiBudgetEntryType::Expense,
            common::entities::BudgetEntryType::Forecast => FfiBudgetEntryType::Forecast,
        }
    }
}

impl From<FfiBudgetEntryType> for common::entities::BudgetEntryType {
    fn from(t: FfiBudgetEntryType) -> Self {
        match t {
            FfiBudgetEntryType::Purchase => common::entities::BudgetEntryType::Purchase,
            FfiBudgetEntryType::Sale => common::entities::BudgetEntryType::Sale,
            FfiBudgetEntryType::Expense => common::entities::BudgetEntryType::Expense,
            FfiBudgetEntryType::Forecast => common::entities::BudgetEntryType::Forecast,
        }
    }
}

impl From<FfiBudgetEntryType> for budget_finance::dtos::BudgetEntryTypeInput {
    fn from(t: FfiBudgetEntryType) -> Self {
        match t {
            FfiBudgetEntryType::Purchase => budget_finance::dtos::BudgetEntryTypeInput::Purchase,
            FfiBudgetEntryType::Sale => budget_finance::dtos::BudgetEntryTypeInput::Sale,
            FfiBudgetEntryType::Expense => budget_finance::dtos::BudgetEntryTypeInput::Expense,
            FfiBudgetEntryType::Forecast => budget_finance::dtos::BudgetEntryTypeInput::Forecast,
        }
    }
}

impl From<direct_access::budget_entry::dtos::BudgetEntryDto> for FfiBudgetEntryDto {
    fn from(d: direct_access::budget_entry::dtos::BudgetEntryDto) -> Self {
        FfiBudgetEntryDto {
            id: d.id,
            created_at: dt_to_string(d.created_at),
            updated_at: dt_to_string(d.updated_at),
            entry_type: d.entry_type.into(),
            amount: d.amount,
            description: d.description,
            entry_date: dt_to_string(d.entry_date),
            product_id: d.product,
            deal_id: d.deal,
            recorded_by_id: d.recorded_by,
        }
    }
}

impl From<FfiRecordBudgetEntryDto> for budget_finance::dtos::RecordBudgetEntryDto {
    fn from(d: FfiRecordBudgetEntryDto) -> Self {
        budget_finance::dtos::RecordBudgetEntryDto {
            entry_type: d.entry_type.into(),
            amount: d.amount,
            description: d.description,
            entry_date: string_to_dt(&d.entry_date),
            product_id: d.product_id as i64,
            deal_id: d.deal_id as i64,
        }
    }
}

impl From<budget_finance::dtos::BudgetSummaryDto> for FfiBudgetSummaryDto {
    fn from(d: budget_finance::dtos::BudgetSummaryDto) -> Self {
        FfiBudgetSummaryDto {
            total_purchases: d.total_purchases,
            total_sales: d.total_sales,
            total_expenses: d.total_expenses,
            net_balance: d.net_balance,
            monthly_labels: d.monthly_labels,
            monthly_purchases: d.monthly_purchases,
            monthly_sales: d.monthly_sales,
            monthly_expenses: d.monthly_expenses,
        }
    }
}

impl From<budget_finance::dtos::BudgetProjectionDto> for FfiBudgetProjectionDto {
    fn from(d: budget_finance::dtos::BudgetProjectionDto) -> Self {
        FfiBudgetProjectionDto {
            month_labels: d.month_labels,
            projected_income: d.projected_income,
            projected_expenses: d.projected_expenses,
            projected_balance: d.projected_balance,
            recurring_deal_costs: d.recurring_deal_costs,
        }
    }
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiGetBudgetSummaryDto {
    pub from_date: String,
    pub to_date: String,
}

impl From<FfiGetBudgetSummaryDto> for budget_finance::dtos::GetBudgetSummaryDto {
    fn from(d: FfiGetBudgetSummaryDto) -> Self {
        budget_finance::dtos::GetBudgetSummaryDto {
            from_date: string_to_dt(&d.from_date),
            to_date: string_to_dt(&d.to_date),
        }
    }
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiGetBudgetProjectionDto {
    pub months_ahead: i64,
}

impl From<FfiGetBudgetProjectionDto> for budget_finance::dtos::GetBudgetProjectionDto {
    fn from(d: FfiGetBudgetProjectionDto) -> Self {
        budget_finance::dtos::GetBudgetProjectionDto {
            months_ahead: d.months_ahead,
        }
    }
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiRecordBudgetEntryResultDto {
    pub entry_id: i64,
}

impl From<budget_finance::dtos::RecordBudgetEntryResultDto> for FfiRecordBudgetEntryResultDto {
    fn from(d: budget_finance::dtos::RecordBudgetEntryResultDto) -> Self {
        FfiRecordBudgetEntryResultDto {
            entry_id: d.entry_id,
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Authentication
// ═══════════════════════════════════════════════════════════════════════════════

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiLoginResult {
    pub session_token: String,
    pub user_id: u64,
    pub display_name: String,
    pub role: FfiUserRole,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiChangePasswordResult {
    pub success: bool,
    pub error_message: String,
}

impl From<authentication::dtos::LoginResultDto> for FfiLoginResult {
    fn from(d: authentication::dtos::LoginResultDto) -> Self {
        let role = match d.role.as_str() {
            "Admin" => FfiUserRole::Admin,
            "Manager" => FfiUserRole::Manager,
            "Operator" => FfiUserRole::Operator,
            _ => FfiUserRole::Viewer,
        };
        FfiLoginResult {
            session_token: d.token,
            user_id: d.user_id as u64,
            display_name: d.display_name,
            role,
        }
    }
}

impl From<authentication::dtos::ChangePasswordResultDto> for FfiChangePasswordResult {
    fn from(d: authentication::dtos::ChangePasswordResultDto) -> Self {
        FfiChangePasswordResult {
            success: d.success,
            error_message: d.error_message,
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Sync
// ═══════════════════════════════════════════════════════════════════════════════

#[derive(Debug, Clone, uniffi::Enum)]
pub enum FfiSyncStrategy {
    Full,
    Incremental,
    ConflictResolveLocal,
    ConflictResolveRemote,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiSyncConfig {
    pub turso_url: String,
    pub turso_auth_token: String,
    pub auto_sync_enabled: bool,
    pub sync_interval_seconds: u64,
    pub default_strategy: FfiSyncStrategy,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiSyncResult {
    pub entities_pushed: u64,
    pub entities_pulled: u64,
    pub conflicts: u64,
}

impl From<inventory_sync::config::SyncStrategy> for FfiSyncStrategy {
    fn from(s: inventory_sync::config::SyncStrategy) -> Self {
        match s {
            inventory_sync::config::SyncStrategy::Full => FfiSyncStrategy::Full,
            inventory_sync::config::SyncStrategy::Incremental => FfiSyncStrategy::Incremental,
            inventory_sync::config::SyncStrategy::ConflictResolveLocal => {
                FfiSyncStrategy::ConflictResolveLocal
            }
            inventory_sync::config::SyncStrategy::ConflictResolveRemote => {
                FfiSyncStrategy::ConflictResolveRemote
            }
        }
    }
}

impl From<FfiSyncStrategy> for inventory_sync::config::SyncStrategy {
    fn from(s: FfiSyncStrategy) -> Self {
        match s {
            FfiSyncStrategy::Full => inventory_sync::config::SyncStrategy::Full,
            FfiSyncStrategy::Incremental => inventory_sync::config::SyncStrategy::Incremental,
            FfiSyncStrategy::ConflictResolveLocal => {
                inventory_sync::config::SyncStrategy::ConflictResolveLocal
            }
            FfiSyncStrategy::ConflictResolveRemote => {
                inventory_sync::config::SyncStrategy::ConflictResolveRemote
            }
        }
    }
}

impl From<inventory_sync::config::SyncConfig> for FfiSyncConfig {
    fn from(c: inventory_sync::config::SyncConfig) -> Self {
        FfiSyncConfig {
            turso_url: c.turso_url,
            turso_auth_token: c.turso_auth_token,
            auto_sync_enabled: c.auto_sync_enabled,
            sync_interval_seconds: c.sync_interval_seconds,
            default_strategy: c.default_strategy.into(),
        }
    }
}

impl From<FfiSyncConfig> for inventory_sync::config::SyncConfig {
    fn from(c: FfiSyncConfig) -> Self {
        inventory_sync::config::SyncConfig {
            turso_url: c.turso_url,
            turso_auth_token: c.turso_auth_token,
            auto_sync_enabled: c.auto_sync_enabled,
            sync_interval_seconds: c.sync_interval_seconds,
            default_strategy: c.default_strategy.into(),
        }
    }
}

impl From<inventory_sync::types::SyncResult> for FfiSyncResult {
    fn from(r: inventory_sync::types::SyncResult) -> Self {
        FfiSyncResult {
            entities_pushed: r.pushed.values().sum::<usize>() as u64,
            entities_pulled: r.pulled.values().sum::<usize>() as u64,
            conflicts: r.conflicts_resolved as u64,
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Reporting
// ═══════════════════════════════════════════════════════════════════════════════

#[derive(Debug, Clone, uniffi::Enum)]
pub enum FfiReportFormat {
    Pdf,
    Excel,
    Csv,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiReportResult {
    pub file_path: String,
    pub format: FfiReportFormat,
    pub size_bytes: u64,
}

impl From<FfiReportFormat> for reporting::dtos::ReportFormat {
    fn from(f: FfiReportFormat) -> Self {
        match f {
            FfiReportFormat::Pdf => reporting::dtos::ReportFormat::Pdf,
            FfiReportFormat::Excel => reporting::dtos::ReportFormat::Excel,
            FfiReportFormat::Csv => reporting::dtos::ReportFormat::Csv,
        }
    }
}

impl From<reporting::dtos::ReportFormat> for FfiReportFormat {
    fn from(f: reporting::dtos::ReportFormat) -> Self {
        match f {
            reporting::dtos::ReportFormat::Pdf => FfiReportFormat::Pdf,
            reporting::dtos::ReportFormat::Excel => FfiReportFormat::Excel,
            reporting::dtos::ReportFormat::Csv => FfiReportFormat::Csv,
        }
    }
}

impl From<FfiReportFormat> for reporting::dtos::StockReportFormat {
    fn from(f: FfiReportFormat) -> Self {
        match f {
            FfiReportFormat::Pdf => reporting::dtos::StockReportFormat::Pdf,
            FfiReportFormat::Excel => reporting::dtos::StockReportFormat::Excel,
            FfiReportFormat::Csv => reporting::dtos::StockReportFormat::Csv,
        }
    }
}

impl From<FfiReportFormat> for reporting::dtos::BudgetReportFormat {
    fn from(f: FfiReportFormat) -> Self {
        match f {
            FfiReportFormat::Pdf => reporting::dtos::BudgetReportFormat::Pdf,
            FfiReportFormat::Excel => reporting::dtos::BudgetReportFormat::Excel,
            FfiReportFormat::Csv => reporting::dtos::BudgetReportFormat::Csv,
        }
    }
}

impl From<FfiReportFormat> for reporting::dtos::PurchasingReportFormat {
    fn from(f: FfiReportFormat) -> Self {
        match f {
            FfiReportFormat::Pdf => reporting::dtos::PurchasingReportFormat::Pdf,
            FfiReportFormat::Excel => reporting::dtos::PurchasingReportFormat::Excel,
            FfiReportFormat::Csv => reporting::dtos::PurchasingReportFormat::Csv,
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Purchasing
// ═══════════════════════════════════════════════════════════════════════════════

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiCreatePurchaseDealDto {
    pub title: String,
    pub description: String,
    pub product_id: i64,
    pub supplier_id: i64,
    pub manager_id: i64,
    pub unit_cost: f64,
    pub total_value: f64,
    pub frequency: FfiDealFrequency,
    pub start_date: String,
    pub end_date: String,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiCreatePurchaseDealReturnDto {
    pub deal_id: i64,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiDealListDto {
    pub deal_ids: Vec<i64>,
    pub titles: Vec<String>,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiActiveDealsDto {
    pub deal_ids: Vec<i64>,
    pub titles: Vec<String>,
    pub statuses: Vec<String>,
}

impl From<FfiDealFrequency> for purchasing::DealFrequencyInput {
    fn from(f: FfiDealFrequency) -> Self {
        match f {
            FfiDealFrequency::OneTime => purchasing::DealFrequencyInput::OneTime,
            FfiDealFrequency::Weekly => purchasing::DealFrequencyInput::Weekly,
            FfiDealFrequency::Monthly => purchasing::DealFrequencyInput::Monthly,
            FfiDealFrequency::Quarterly => purchasing::DealFrequencyInput::Quarterly,
            FfiDealFrequency::Yearly => purchasing::DealFrequencyInput::Yearly,
        }
    }
}

impl From<FfiCreatePurchaseDealDto> for purchasing::CreatePurchaseDealDto {
    fn from(d: FfiCreatePurchaseDealDto) -> Self {
        purchasing::CreatePurchaseDealDto {
            title: d.title,
            description: d.description,
            product_id: d.product_id,
            supplier_id: d.supplier_id,
            manager_id: d.manager_id,
            unit_cost: d.unit_cost,
            total_value: d.total_value,
            frequency: d.frequency.into(),
            start_date: string_to_dt(&d.start_date),
            end_date: string_to_dt(&d.end_date),
        }
    }
}

impl From<purchasing::CreatePurchaseDealReturnDto> for FfiCreatePurchaseDealReturnDto {
    fn from(d: purchasing::CreatePurchaseDealReturnDto) -> Self {
        FfiCreatePurchaseDealReturnDto {
            deal_id: d.deal_id,
        }
    }
}

impl From<purchasing::DealListDto> for FfiDealListDto {
    fn from(d: purchasing::DealListDto) -> Self {
        FfiDealListDto {
            deal_ids: d.deal_ids,
            titles: d.titles,
        }
    }
}

impl From<purchasing::ActiveDealsDto> for FfiActiveDealsDto {
    fn from(d: purchasing::ActiveDealsDto) -> Self {
        FfiActiveDealsDto {
            deal_ids: d.deal_ids,
            titles: d.titles,
            statuses: d.statuses,
        }
    }
}
