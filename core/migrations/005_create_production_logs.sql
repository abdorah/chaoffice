CREATE TABLE IF NOT EXISTS production_logs (
    id TEXT PRIMARY KEY NOT NULL,
    recipe_id TEXT NOT NULL REFERENCES recipes(id),
    chef_id TEXT NOT NULL REFERENCES users(id),
    production_quantity INTEGER NOT NULL CHECK (production_quantity > 0),
    materials_consumed_json TEXT NOT NULL,
    finished_good_id TEXT NOT NULL REFERENCES finished_goods(id),
    timestamp TEXT NOT NULL,
    sync_status TEXT NOT NULL DEFAULT 'Synced',
    updated_at TEXT NOT NULL
);
