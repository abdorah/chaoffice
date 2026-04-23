//! SchemaManager — SQL schema definitions for all entity tables.
//!
//! SQL strings are fully defined. Execution is stubbed (would call LibSQL).

use crate::error::SyncError;

pub struct SchemaManager;

impl SchemaManager {
    /// All CREATE TABLE statements for entity tables and sync_metadata.
    pub fn create_table_statements() -> Vec<&'static str> {
        vec![
            "CREATE TABLE IF NOT EXISTS products (
                id INTEGER PRIMARY KEY,
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
                deleted_at TEXT
            )",
            "CREATE TABLE IF NOT EXISTS categories (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT,
                parent_category_id INTEGER,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                deleted_at TEXT
            )",
            "CREATE TABLE IF NOT EXISTS persons (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL,
                role TEXT NOT NULL,
                contact_id INTEGER,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                deleted_at TEXT
            )",
            "CREATE TABLE IF NOT EXISTS contacts (
                id INTEGER PRIMARY KEY,
                phone TEXT,
                email TEXT,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                deleted_at TEXT
            )",
            "CREATE TABLE IF NOT EXISTS deals (
                id INTEGER PRIMARY KEY,
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
                deleted_at TEXT
            )",
            "CREATE TABLE IF NOT EXISTS locations (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL,
                address TEXT,
                latitude REAL DEFAULT 0.0,
                longitude REAL DEFAULT 0.0,
                capacity INTEGER DEFAULT 0,
                manager_id INTEGER,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                deleted_at TEXT
            )",
            "CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY,
                username TEXT NOT NULL,
                password_hash TEXT NOT NULL,
                display_name TEXT,
                role TEXT NOT NULL,
                is_active INTEGER DEFAULT 1,
                person_id INTEGER,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                deleted_at TEXT
            )",
            "CREATE TABLE IF NOT EXISTS sessions (
                id INTEGER PRIMARY KEY,
                token TEXT NOT NULL,
                expires_at TEXT NOT NULL,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                deleted_at TEXT
            )",
            "CREATE TABLE IF NOT EXISTS stock_movements (
                id INTEGER PRIMARY KEY,
                movement_type TEXT NOT NULL,
                quantity INTEGER NOT NULL,
                note TEXT,
                product_id INTEGER,
                from_location_id INTEGER,
                to_location_id INTEGER,
                performed_by_id INTEGER,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                deleted_at TEXT
            )",
            "CREATE TABLE IF NOT EXISTS budget_entries (
                id INTEGER PRIMARY KEY,
                entry_type TEXT NOT NULL,
                amount REAL NOT NULL,
                description TEXT,
                entry_date TEXT,
                product_id INTEGER,
                deal_id INTEGER,
                recorded_by_id INTEGER,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                deleted_at TEXT
            )",
            "CREATE TABLE IF NOT EXISTS sync_metadata (
                entity_type TEXT PRIMARY KEY,
                last_push_at TEXT,
                last_pull_at TEXT,
                last_push_count INTEGER DEFAULT 0,
                last_pull_count INTEGER DEFAULT 0
            )",
        ]
    }

    /// Initialize the LibSQL schema by executing all CREATE TABLE statements.
    pub async fn init_schema(conn: &libsql::Connection) -> Result<(), SyncError> {
        let statements = Self::create_table_statements();
        for sql in &statements {
            conn.execute(sql, ())
                .await
                .map_err(|e| SyncError::SchemaInit(e.to_string()))?;
        }
        log::info!(
            "SchemaManager: {} CREATE TABLE statements executed",
            statements.len()
        );
        Ok(())
    }

    /// Returns the list of all synced entity table names.
    pub fn entity_table_names() -> Vec<&'static str> {
        vec![
            "products",
            "categories",
            "persons",
            "contacts",
            "deals",
            "locations",
            "users",
            "sessions",
            "stock_movements",
            "budget_entries",
        ]
    }
}
