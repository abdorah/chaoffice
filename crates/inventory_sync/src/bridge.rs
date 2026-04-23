//! EntityBridge trait and stub implementations for all 10 entity types.
//!
//! Since we're stubbing LibSQL, `to_row` serializes entities to serde_json::Value
//! and `from_row` deserializes from serde_json::Value. This preserves the real
//! trait shape while avoiding the libsql dependency.

use crate::error::SyncError;
use common::entities::*;
use serde_json::Value;

/// Trait for converting Qleany entities to/from a generic row representation.
///
/// In the real implementation, `to_row` would produce `Vec<libsql::Value>` and
/// `from_row` would consume a `libsql::Row`. For the stub, we use `serde_json::Value`.
pub trait EntityBridge {
    type Entity;

    /// Convert a Qleany entity to a JSON value (stub for LibSQL column values).
    fn to_row(entity: &Self::Entity) -> Result<Value, SyncError>;

    /// Convert a JSON value back to a Qleany entity (stub for LibSQL row).
    fn from_row(row: &Value) -> Result<Self::Entity, SyncError>;

    /// Table name in LibSQL.
    fn table_name() -> &'static str;

    /// Column definitions for CREATE TABLE.
    fn column_defs() -> &'static str;

    /// Generate an upsert SQL statement.
    fn upsert_sql() -> String;
}

// ---------------------------------------------------------------------------
// Helper: serialize/deserialize via serde_json for all bridges
// ---------------------------------------------------------------------------

fn entity_to_row<T: serde::Serialize>(entity: &T, entity_type: &str) -> Result<Value, SyncError> {
    serde_json::to_value(entity).map_err(|e| SyncError::Serialization {
        entity_type: entity_type.to_string(),
        entity_id: 0,
        message: e.to_string(),
    })
}

fn entity_from_row<T: serde::de::DeserializeOwned>(
    row: &Value,
    entity_type: &str,
) -> Result<T, SyncError> {
    serde_json::from_value(row.clone()).map_err(|e| SyncError::Serialization {
        entity_type: entity_type.to_string(),
        entity_id: 0,
        message: e.to_string(),
    })
}

// ---------------------------------------------------------------------------
// ProductBridge
// ---------------------------------------------------------------------------

pub struct ProductBridge;

impl EntityBridge for ProductBridge {
    type Entity = Product;

    fn to_row(entity: &Product) -> Result<Value, SyncError> {
        entity_to_row(entity, "Product")
    }

    fn from_row(row: &Value) -> Result<Product, SyncError> {
        entity_from_row(row, "Product")
    }

    fn table_name() -> &'static str {
        "products"
    }

    fn column_defs() -> &'static str {
        "id INTEGER PRIMARY KEY,
        name TEXT NOT NULL,
        reference TEXT,
        description TEXT,
        quantity INTEGER DEFAULT 0,
        price_unit REAL DEFAULT 0.0,
        status TEXT DEFAULT 'Available',
        category_id INTEGER,
        supplier_id INTEGER,
        location_id INTEGER,
        created_at TEXT NOT NULL,
        updated_at TEXT NOT NULL,
        deleted_at TEXT"
    }

    fn upsert_sql() -> String {
        format!(
            "INSERT INTO {} (id, name, reference, description, quantity, price_unit, status, category_id, supplier_id, location_id, created_at, updated_at) \
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) \
             ON CONFLICT(id) DO UPDATE SET name=excluded.name, reference=excluded.reference, \
             description=excluded.description, quantity=excluded.quantity, price_unit=excluded.price_unit, \
             status=excluded.status, category_id=excluded.category_id, supplier_id=excluded.supplier_id, \
             location_id=excluded.location_id, updated_at=excluded.updated_at",
            Self::table_name()
        )
    }
}

// ---------------------------------------------------------------------------
// CategoryBridge
// ---------------------------------------------------------------------------

pub struct CategoryBridge;

impl EntityBridge for CategoryBridge {
    type Entity = Category;

    fn to_row(entity: &Category) -> Result<Value, SyncError> {
        entity_to_row(entity, "Category")
    }

    fn from_row(row: &Value) -> Result<Category, SyncError> {
        entity_from_row(row, "Category")
    }

    fn table_name() -> &'static str {
        "categories"
    }

    fn column_defs() -> &'static str {
        "id INTEGER PRIMARY KEY,
        name TEXT NOT NULL,
        description TEXT,
        parent_category_id INTEGER,
        created_at TEXT NOT NULL,
        updated_at TEXT NOT NULL,
        deleted_at TEXT"
    }

    fn upsert_sql() -> String {
        format!(
            "INSERT INTO {} (id, name, description, parent_category_id, created_at, updated_at) \
             VALUES (?, ?, ?, ?, ?, ?) \
             ON CONFLICT(id) DO UPDATE SET name=excluded.name, description=excluded.description, \
             parent_category_id=excluded.parent_category_id, updated_at=excluded.updated_at",
            Self::table_name()
        )
    }
}

// ---------------------------------------------------------------------------
// PersonBridge
// ---------------------------------------------------------------------------

pub struct PersonBridge;

impl EntityBridge for PersonBridge {
    type Entity = Person;

    fn to_row(entity: &Person) -> Result<Value, SyncError> {
        entity_to_row(entity, "Person")
    }

    fn from_row(row: &Value) -> Result<Person, SyncError> {
        entity_from_row(row, "Person")
    }

    fn table_name() -> &'static str {
        "persons"
    }

    fn column_defs() -> &'static str {
        "id INTEGER PRIMARY KEY,
        name TEXT NOT NULL,
        role TEXT NOT NULL,
        contact_id INTEGER,
        created_at TEXT NOT NULL,
        updated_at TEXT NOT NULL,
        deleted_at TEXT"
    }

    fn upsert_sql() -> String {
        format!(
            "INSERT INTO {} (id, name, role, contact_id, created_at, updated_at) \
             VALUES (?, ?, ?, ?, ?, ?) \
             ON CONFLICT(id) DO UPDATE SET name=excluded.name, role=excluded.role, \
             contact_id=excluded.contact_id, updated_at=excluded.updated_at",
            Self::table_name()
        )
    }
}

// ---------------------------------------------------------------------------
// ContactBridge
// ---------------------------------------------------------------------------

pub struct ContactBridge;

impl EntityBridge for ContactBridge {
    type Entity = Contact;

    fn to_row(entity: &Contact) -> Result<Value, SyncError> {
        entity_to_row(entity, "Contact")
    }

    fn from_row(row: &Value) -> Result<Contact, SyncError> {
        entity_from_row(row, "Contact")
    }

    fn table_name() -> &'static str {
        "contacts"
    }

    fn column_defs() -> &'static str {
        "id INTEGER PRIMARY KEY,
        phone TEXT,
        email TEXT,
        created_at TEXT NOT NULL,
        updated_at TEXT NOT NULL,
        deleted_at TEXT"
    }

    fn upsert_sql() -> String {
        format!(
            "INSERT INTO {} (id, phone, email, created_at, updated_at) \
             VALUES (?, ?, ?, ?, ?) \
             ON CONFLICT(id) DO UPDATE SET phone=excluded.phone, email=excluded.email, \
             updated_at=excluded.updated_at",
            Self::table_name()
        )
    }
}

// ---------------------------------------------------------------------------
// DealBridge
// ---------------------------------------------------------------------------

pub struct DealBridge;

impl EntityBridge for DealBridge {
    type Entity = Deal;

    fn to_row(entity: &Deal) -> Result<Value, SyncError> {
        entity_to_row(entity, "Deal")
    }

    fn from_row(row: &Value) -> Result<Deal, SyncError> {
        entity_from_row(row, "Deal")
    }

    fn table_name() -> &'static str {
        "deals"
    }

    fn column_defs() -> &'static str {
        "id INTEGER PRIMARY KEY,
        title TEXT NOT NULL,
        description TEXT,
        unit_cost REAL DEFAULT 0.0,
        total_value REAL DEFAULT 0.0,
        start_date TEXT,
        end_date TEXT,
        frequency TEXT DEFAULT 'OneTime',
        status TEXT DEFAULT 'Draft',
        product_id INTEGER,
        supplier_id INTEGER,
        manager_id INTEGER,
        created_at TEXT NOT NULL,
        updated_at TEXT NOT NULL,
        deleted_at TEXT"
    }

    fn upsert_sql() -> String {
        format!(
            "INSERT INTO {} (id, title, description, unit_cost, total_value, start_date, end_date, \
             frequency, status, product_id, supplier_id, manager_id, created_at, updated_at) \
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) \
             ON CONFLICT(id) DO UPDATE SET title=excluded.title, description=excluded.description, \
             unit_cost=excluded.unit_cost, total_value=excluded.total_value, start_date=excluded.start_date, \
             end_date=excluded.end_date, frequency=excluded.frequency, status=excluded.status, \
             product_id=excluded.product_id, supplier_id=excluded.supplier_id, \
             manager_id=excluded.manager_id, updated_at=excluded.updated_at",
            Self::table_name()
        )
    }
}

// ---------------------------------------------------------------------------
// LocationBridge
// ---------------------------------------------------------------------------

pub struct LocationBridge;

impl EntityBridge for LocationBridge {
    type Entity = Location;

    fn to_row(entity: &Location) -> Result<Value, SyncError> {
        entity_to_row(entity, "Location")
    }

    fn from_row(row: &Value) -> Result<Location, SyncError> {
        entity_from_row(row, "Location")
    }

    fn table_name() -> &'static str {
        "locations"
    }

    fn column_defs() -> &'static str {
        "id INTEGER PRIMARY KEY,
        name TEXT NOT NULL,
        address TEXT,
        latitude REAL DEFAULT 0.0,
        longitude REAL DEFAULT 0.0,
        capacity INTEGER DEFAULT 0,
        created_at TEXT NOT NULL,
        updated_at TEXT NOT NULL,
        deleted_at TEXT"
    }

    fn upsert_sql() -> String {
        format!(
            "INSERT INTO {} (id, name, address, latitude, longitude, capacity, manager_id, created_at, updated_at) \
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) \
             ON CONFLICT(id) DO UPDATE SET name=excluded.name, address=excluded.address, \
             latitude=excluded.latitude, longitude=excluded.longitude, capacity=excluded.capacity, \
             manager_id=excluded.manager_id, updated_at=excluded.updated_at",
            Self::table_name()
        )
    }
}

// ---------------------------------------------------------------------------
// UserBridge
// ---------------------------------------------------------------------------

pub struct UserBridge;

impl EntityBridge for UserBridge {
    type Entity = User;

    fn to_row(entity: &User) -> Result<Value, SyncError> {
        entity_to_row(entity, "User")
    }

    fn from_row(row: &Value) -> Result<User, SyncError> {
        entity_from_row(row, "User")
    }

    fn table_name() -> &'static str {
        "users"
    }

    fn column_defs() -> &'static str {
        "id INTEGER PRIMARY KEY,
        username TEXT NOT NULL,
        password_hash TEXT NOT NULL,
        display_name TEXT,
        role TEXT NOT NULL,
        is_active INTEGER DEFAULT 1,
        person_id INTEGER,
        created_at TEXT NOT NULL,
        updated_at TEXT NOT NULL,
        deleted_at TEXT"
    }

    fn upsert_sql() -> String {
        format!(
            "INSERT INTO {} (id, username, password_hash, display_name, role, is_active, person_id, created_at, updated_at) \
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) \
             ON CONFLICT(id) DO UPDATE SET username=excluded.username, password_hash=excluded.password_hash, \
             display_name=excluded.display_name, role=excluded.role, is_active=excluded.is_active, \
             person_id=excluded.person_id, updated_at=excluded.updated_at",
            Self::table_name()
        )
    }
}

// ---------------------------------------------------------------------------
// SessionBridge
// ---------------------------------------------------------------------------

pub struct SessionBridge;

impl EntityBridge for SessionBridge {
    type Entity = Session;

    fn to_row(entity: &Session) -> Result<Value, SyncError> {
        entity_to_row(entity, "Session")
    }

    fn from_row(row: &Value) -> Result<Session, SyncError> {
        entity_from_row(row, "Session")
    }

    fn table_name() -> &'static str {
        "sessions"
    }

    fn column_defs() -> &'static str {
        "id INTEGER PRIMARY KEY,
        token TEXT NOT NULL,
        expires_at TEXT NOT NULL,
        created_at TEXT NOT NULL,
        updated_at TEXT NOT NULL,
        deleted_at TEXT"
    }

    fn upsert_sql() -> String {
        format!(
            "INSERT INTO {} (id, token, expires_at, created_at, updated_at) \
             VALUES (?, ?, ?, ?, ?) \
             ON CONFLICT(id) DO UPDATE SET token=excluded.token, expires_at=excluded.expires_at, \
             updated_at=excluded.updated_at",
            Self::table_name()
        )
    }
}

// ---------------------------------------------------------------------------
// StockMovementBridge
// ---------------------------------------------------------------------------

pub struct StockMovementBridge;

impl EntityBridge for StockMovementBridge {
    type Entity = StockMovement;

    fn to_row(entity: &StockMovement) -> Result<Value, SyncError> {
        entity_to_row(entity, "StockMovement")
    }

    fn from_row(row: &Value) -> Result<StockMovement, SyncError> {
        entity_from_row(row, "StockMovement")
    }

    fn table_name() -> &'static str {
        "stock_movements"
    }

    fn column_defs() -> &'static str {
        "id INTEGER PRIMARY KEY,
        movement_type TEXT NOT NULL,
        quantity INTEGER NOT NULL,
        note TEXT,
        product_id INTEGER,
        from_location_id INTEGER,
        to_location_id INTEGER,
        performed_by_id INTEGER,
        created_at TEXT NOT NULL,
        updated_at TEXT NOT NULL,
        deleted_at TEXT"
    }

    fn upsert_sql() -> String {
        format!(
            "INSERT INTO {} (id, movement_type, quantity, note, product_id, from_location_id, \
             to_location_id, performed_by_id, created_at, updated_at) \
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) \
             ON CONFLICT(id) DO UPDATE SET movement_type=excluded.movement_type, quantity=excluded.quantity, \
             note=excluded.note, product_id=excluded.product_id, from_location_id=excluded.from_location_id, \
             to_location_id=excluded.to_location_id, performed_by_id=excluded.performed_by_id, \
             updated_at=excluded.updated_at",
            Self::table_name()
        )
    }
}

// ---------------------------------------------------------------------------
// BudgetEntryBridge
// ---------------------------------------------------------------------------

pub struct BudgetEntryBridge;

impl EntityBridge for BudgetEntryBridge {
    type Entity = BudgetEntry;

    fn to_row(entity: &BudgetEntry) -> Result<Value, SyncError> {
        entity_to_row(entity, "BudgetEntry")
    }

    fn from_row(row: &Value) -> Result<BudgetEntry, SyncError> {
        entity_from_row(row, "BudgetEntry")
    }

    fn table_name() -> &'static str {
        "budget_entries"
    }

    fn column_defs() -> &'static str {
        "id INTEGER PRIMARY KEY,
        entry_type TEXT NOT NULL,
        amount REAL NOT NULL,
        description TEXT,
        entry_date TEXT,
        product_id INTEGER,
        deal_id INTEGER,
        recorded_by_id INTEGER,
        created_at TEXT NOT NULL,
        updated_at TEXT NOT NULL,
        deleted_at TEXT"
    }

    fn upsert_sql() -> String {
        format!(
            "INSERT INTO {} (id, entry_type, amount, description, entry_date, product_id, deal_id, \
             recorded_by_id, created_at, updated_at) \
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) \
             ON CONFLICT(id) DO UPDATE SET entry_type=excluded.entry_type, amount=excluded.amount, \
             description=excluded.description, entry_date=excluded.entry_date, product_id=excluded.product_id, \
             deal_id=excluded.deal_id, recorded_by_id=excluded.recorded_by_id, updated_at=excluded.updated_at",
            Self::table_name()
        )
    }
}
