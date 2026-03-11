CREATE TABLE IF NOT EXISTS customers (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    city TEXT NOT NULL,
    mobile TEXT NOT NULL UNIQUE,
    reliability_rating INTEGER NOT NULL DEFAULT 0 CHECK (reliability_rating >= 0 AND reliability_rating <= 5),
    sync_status TEXT NOT NULL DEFAULT 'Synced',
    updated_at TEXT NOT NULL
);
