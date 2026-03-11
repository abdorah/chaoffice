CREATE TABLE IF NOT EXISTS finished_goods (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    current_quantity REAL NOT NULL DEFAULT 0.0 CHECK (current_quantity >= 0),
    unit_price REAL NOT NULL DEFAULT 0.0 CHECK (unit_price >= 0),
    last_updated TEXT NOT NULL,
    sync_status TEXT NOT NULL DEFAULT 'Synced',
    updated_at TEXT NOT NULL
);
