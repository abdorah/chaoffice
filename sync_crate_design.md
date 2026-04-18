# Sync Crate Design — TursoDB + LibSQL

## Overview

A standalone `inventory_sync` crate (NOT a Qleany feature) that bridges the Qleany internal database (redb) with TursoDB Cloud via LibSQL embedded replicas. Provides online/offline capability with configurable sync strategies.

## Why Not a Qleany Feature?

Sync is infrastructure, not business logic. It doesn't need:
- Undo/redo (you don't "undo" a sync)
- Event buffering (sync results are reported directly)
- DTOs through the controller pipeline

It directly accesses `common` (entity types, db_context) and reads/writes redb, then mirrors to LibSQL. The frontend calls it directly.

## Two-Database Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        App Lifecycle                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  STARTUP                                                    │
│  ┌─────────────┐    sync()     ┌──────────────────┐        │
│  │ LibSQL      │◄─────────────►│ TursoDB Cloud    │        │
│  │ (embedded   │               │ (remote)         │        │
│  │  replica)   │               └──────────────────┘        │
│  └──────┬──────┘                                           │
│         │ hydrate: read all entities                        │
│         ▼                                                   │
│  ┌─────────────┐                                           │
│  │ redb        │  ← Qleany runtime (undo/redo, fast ops)  │
│  │ (in-memory) │                                           │
│  └─────────────┘                                           │
│                                                             │
│  RUNTIME (user works normally through Qleany)               │
│  All CRUD → redb via Qleany controllers                     │
│  Qleany events track what changed                           │
│                                                             │
│  ON SAVE / PERIODIC / MANUAL SYNC                           │
│  ┌─────────────┐   dehydrate    ┌─────────────┐           │
│  │ redb        │───────────────►│ LibSQL      │           │
│  │ (changed    │  (delta only)  │ (embedded   │           │
│  │  entities)  │                │  replica)   │           │
│  └─────────────┘                └──────┬──────┘           │
│                                        │ sync()           │
│                                        ▼                   │
│                                 ┌──────────────────┐       │
│                                 │ TursoDB Cloud    │       │
│                                 └──────────────────┘       │
│                                                             │
│  SHUTDOWN                                                   │
│  Final dehydrate redb → LibSQL. redb discarded.             │
│  LibSQL file persists on disk.                              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

Key insight: LibSQL embedded replica IS your persistent database. It's a local SQLite file that syncs to TursoDB Cloud when online. redb is a fast working cache for Qleany's undo/redo system.

## SyncConfig

```rust
/// Persisted in a local config file (not in redb)
pub struct SyncConfig {
    /// TursoDB Cloud URL, e.g. "libsql://mydb-myorg.turso.io"
    pub turso_url: String,
    /// Auth token from TursoDB dashboard
    pub turso_auth_token: String,
    /// Background auto-sync enabled
    pub auto_sync_enabled: bool,
    /// Seconds between auto-syncs (0 = manual only)
    pub sync_interval_seconds: u64,
    /// Default conflict resolution
    pub default_strategy: SyncStrategy,
}

pub enum SyncStrategy {
    Full,                   // Re-sync everything
    Incremental,            // Only changed since last sync (via updated_at)
    ConflictResolveLocal,   // Local wins on conflict
    ConflictResolveRemote,  // Remote wins on conflict
}
```

## SyncEngine

```rust
pub struct SyncEngine {
    config: SyncConfig,
    db: libsql::Database,       // embedded replica
    change_tracker: ChangeTracker,
}

impl SyncEngine {
    /// Open embedded replica + connect to TursoDB
    pub async fn new(config: SyncConfig) -> Result<Self> {
        let db = libsql::Builder::new_remote_replica(
            "inventory_data.db",        // local file
            config.turso_url.clone(),
            config.turso_auth_token.clone(),
        )
        .build()
        .await?;
        Ok(Self { config, db, change_tracker: ChangeTracker::new() })
    }

    /// Startup: sync remote → local replica, then load into redb
    pub async fn hydrate(&self, db_context: &DbContext) -> Result<HydrateResult> {
        self.db.sync().await?;  // pull latest from TursoDB
        // Read all entities from LibSQL → create in redb via repositories
        // Returns count of loaded entities
    }

    /// Save: flush changed entities from redb → LibSQL, then sync to remote
    pub async fn dehydrate(&self, db_context: &DbContext) -> Result<DehydrateResult> {
        // Read changed entities from redb (tracked by change_tracker)
        // Write to LibSQL via INSERT/UPDATE/DELETE
        // Sync to remote
        self.db.sync().await?;
    }

    /// Manual sync trigger (push + pull)
    pub async fn full_sync(&self, db_context: &DbContext, strategy: SyncStrategy) -> Result<SyncResult> {
        self.dehydrate(db_context).await?;
        self.db.sync().await?;
        // Check for remote changes, apply conflict resolution
        // Hydrate new/changed entities back into redb
    }

    pub async fn is_online(&self) -> bool {
        self.db.sync().await.is_ok()
    }
}
```

## Change Tracking

The sync crate subscribes to Qleany's EventHub to know what changed in redb:

```rust
pub struct ChangeTracker {
    pending: Mutex<HashMap<String, HashSet<u32>>>,  // entity_type → set of changed ids
}

impl ChangeTracker {
    /// Called when Qleany emits entity events
    pub fn on_entity_event(&self, event: &Event) {
        match &event.origin {
            Origin::DirectAccess(entity_event) => {
                let entity_type = entity_event.entity_name();
                let mut pending = self.pending.lock().unwrap();
                let ids = pending.entry(entity_type).or_default();
                for id in &event.ids {
                    ids.insert(*id);
                }
            }
            _ => {} // ignore feature events
        }
    }

    /// Drain pending changes (called during dehydrate)
    pub fn drain(&self) -> HashMap<String, HashSet<u32>> {
        let mut pending = self.pending.lock().unwrap();
        std::mem::take(&mut *pending)
    }
}
```

## Conflict Resolution

```rust
pub struct ConflictResolver {
    strategy: SyncStrategy,
}

impl ConflictResolver {
    pub fn resolve(&self, local: &EntityRow, remote: &EntityRow) -> EntityRow {
        match self.strategy {
            SyncStrategy::ConflictResolveLocal => local.clone(),
            SyncStrategy::ConflictResolveRemote => remote.clone(),
            _ => {
                // Incremental/Full: last-write-wins via updated_at
                if local.updated_at >= remote.updated_at {
                    local.clone()
                } else {
                    remote.clone()
                }
            }
        }
    }
}
```

## LibSQL Schema

Auto-created on first run, mirrors Qleany entities:

```sql
CREATE TABLE IF NOT EXISTS products (
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
    deleted_at TEXT  -- soft delete tombstone for sync
);

-- Similar for all entities...

CREATE TABLE IF NOT EXISTS sync_metadata (
    entity_type TEXT PRIMARY KEY,
    last_push_at TEXT,
    last_pull_at TEXT,
    last_push_count INTEGER DEFAULT 0,
    last_pull_count INTEGER DEFAULT 0
);
```

## Offline Behavior

1. App starts → `SyncEngine::new()` opens embedded replica
2. If online → `db.sync()` pulls latest from TursoDB Cloud
3. `hydrate()` loads LibSQL data into redb
4. User works normally (all ops through Qleany → redb)
5. `ChangeTracker` records what changed via EventHub subscription
6. On save/periodic → `dehydrate()` flushes delta to LibSQL
7. If online → `db.sync()` pushes to TursoDB Cloud
8. If offline → changes accumulate in LibSQL, sync when connectivity returns
9. On app close → final `dehydrate()`, redb discarded

LibSQL's embedded replica handles the hard part: it maintains a local SQLite file that syncs bidirectionally with TursoDB Cloud. We just need to bridge redb ↔ LibSQL.

## Crate Structure

```
crates/inventory_sync/
├── Cargo.toml
└── src/
    ├── lib.rs
    ├── config.rs        # SyncConfig, SyncStrategy, config file I/O
    ├── engine.rs        # SyncEngine (hydrate/dehydrate/full_sync)
    ├── tracker.rs       # ChangeTracker (EventHub subscriber)
    ├── conflict.rs      # ConflictResolver
    ├── schema.rs        # LibSQL table creation/migration
    └── bridge.rs        # redb ↔ LibSQL entity serialization
```

## Dependencies

```toml
[dependencies]
libsql = "0.6"
serde = { version = "1", features = ["derive"] }
serde_json = "1"
chrono = { version = "0.4", features = ["serde"] }
anyhow = "1"
tokio = { version = "1", features = ["rt-multi-thread"] }
```
