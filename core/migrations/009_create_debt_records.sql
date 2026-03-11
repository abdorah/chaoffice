CREATE TABLE IF NOT EXISTS debt_records (
    id TEXT PRIMARY KEY NOT NULL,
    customer_id TEXT NOT NULL REFERENCES customers(id),
    sale_id TEXT NOT NULL REFERENCES sales(id),
    original_amount REAL NOT NULL CHECK (original_amount > 0),
    remaining_amount REAL NOT NULL CHECK (remaining_amount >= 0),
    sale_date TEXT NOT NULL,
    is_settled INTEGER NOT NULL DEFAULT 0,
    sync_status TEXT NOT NULL DEFAULT 'Synced',
    updated_at TEXT NOT NULL
);
