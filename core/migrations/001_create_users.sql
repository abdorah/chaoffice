CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY NOT NULL,
    username TEXT NOT NULL UNIQUE,
    full_name TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('Admin', 'Chef', 'Representative')),
    password_hash TEXT NOT NULL,
    last_login_at TEXT,
    session_expires_at TEXT,
    sync_status TEXT NOT NULL DEFAULT 'Synced',
    updated_at TEXT NOT NULL,
    created_at TEXT NOT NULL
);
