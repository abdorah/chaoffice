# Implementation Plan: Data Sync

## Overview

Implement the `inventory_sync` crate as a standalone Rust crate that bridges Qleany's redb with TursoDB Cloud via LibSQL embedded replicas. Tasks are ordered to build foundational types first, then core logic, then integration and UI wiring. Each task builds incrementally on the previous.

## Tasks

- [ ] 1. Set up crate structure and foundational types
  - [ ] 1.1 Create `crates/inventory_sync/Cargo.toml` with dependencies (libsql, serde, serde_json, chrono, anyhow, thiserror, tokio, uuid) and dev-dependencies (proptest, tokio-test, tempfile)
    - Create the crate directory structure: `src/lib.rs`, `src/error.rs`, `src/types.rs`, `src/config.rs`
    - _Requirements: 6.1_
  - [ ] 1.2 Implement `SyncError` enum in `src/error.rs`
    - Define all variants: LibSql, ConnectionFailed, SchemaInit, Serialization, Config, RedbAccess, AccessDenied, Offline
    - Derive `thiserror::Error` with display messages
    - Implement `From<libsql::Error>` for SyncError
    - _Requirements: 1.2_
  - [ ] 1.3 Implement result types in `src/types.rs`
    - Define `HydrateResult`, `DehydrateResult`, `SyncResult`, `EntityRow`
    - _Requirements: 2.4, 3.6, 4.8_
  - [ ] 1.4 Implement `SyncConfig` and `SyncStrategy` in `src/config.rs`
    - Define `SyncConfig` struct with serde Serialize/Deserialize
    - Define `SyncStrategy` enum with serde support
    - Implement `SyncConfig::default()`, `load(path)`, `save(path)`
    - _Requirements: 6.1, 6.2, 6.5, 6.6_
  - [ ]* 1.5 Write property test for SyncConfig round-trip
    - **Property 2: SyncConfig round-trip**
    - **Validates: Requirements 6.2, 6.5**
  - [ ] 1.6 Wire up `src/lib.rs` with public re-exports
    - _Requirements: 6.1_

- [ ] 2. Implement EntityBridge serialization layer
  - [ ] 2.1 Define `EntityBridge` trait in `src/bridge.rs`
    - Define trait with `to_row`, `from_row`, `table_name`, `column_defs`, `upsert_sql` methods
    - _Requirements: 9.1, 9.2_
  - [ ] 2.2 Implement `EntityBridge` for all 10 entity types
    - Implement ProductBridge, CategoryBridge, PersonBridge, ContactBridge, DealBridge, LocationBridge, UserBridge, SessionBridge, StockMovementBridge, BudgetEntryBridge
    - Handle optional fields as SQL NULL, enums as TEXT, datetimes as ISO 8601 TEXT
    - _Requirements: 9.1, 9.2, 9.4, 9.5, 9.6_
  - [ ]* 2.3 Write property test for entity bridge round-trip
    - **Property 1: Entity bridge round-trip**
    - Implement `Arbitrary` generators for each entity type covering None optionals, all enum variants, boundary datetimes
    - **Validates: Requirements 9.1, 9.2, 9.3, 9.4, 9.5, 9.6**

- [ ] 3. Implement SchemaManager and ChangeTracker
  - [ ] 3.1 Implement `SchemaManager` in `src/schema.rs`
    - Implement `init_schema` with CREATE TABLE IF NOT EXISTS for all 10 entity tables + sync_metadata
    - Include `deleted_at`, `created_at`, `updated_at` columns on all entity tables
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 1.3, 1.4_
  - [ ] 3.2 Implement `ChangeTracker` in `src/tracker.rs`
    - Implement `new`, `on_entity_event`, `drain`, `pending_count`
    - Use `Mutex<HashMap<String, HashSet<u32>>>` for thread safety
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_
  - [ ]* 3.3 Write property test for ChangeTracker
    - **Property 3: ChangeTracker records events with set semantics and drains correctly**
    - Generate random event sequences with duplicates, verify set semantics and drain behavior
    - **Validates: Requirements 7.1, 7.3, 7.4, 3.7**

- [ ] 4. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 5. Implement ConflictResolver
  - [ ] 5.1 Implement `ConflictResolver` in `src/conflict.rs`
    - Implement `resolve` method with strategy-based resolution
    - ConflictResolveLocal → keep local, ConflictResolveRemote → keep remote, Full/Incremental → last-write-wins via `updated_at`
    - _Requirements: 4.3, 4.4, 4.5, 4.6_
  - [ ]* 5.2 Write property test for conflict resolution
    - **Property 6: Conflict resolution follows configured strategy**
    - Generate random local/remote EntityRow pairs with varying timestamps and strategies
    - **Validates: Requirements 4.3, 4.4, 4.5, 4.6**

- [ ] 6. Implement SyncEngine core operations
  - [ ] 6.1 Implement `SyncEngine::new` in `src/engine.rs`
    - Open LibSQL embedded replica with config
    - Call SchemaManager::init_schema
    - Initialize ChangeTracker and subscribe to EventHub
    - Handle invalid config with SyncError::ConnectionFailed
    - _Requirements: 1.1, 1.2, 1.3, 1.4_
  - [ ] 6.2 Implement `SyncEngine::hydrate`
    - Call `db.sync()` (catch offline errors gracefully)
    - SELECT all non-deleted entities from each LibSQL table
    - Convert via EntityBridge::from_row and write to redb via repositories
    - Update sync_metadata with pull timestamps
    - Return HydrateResult with counts
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 8.3_
  - [ ] 6.3 Implement `SyncEngine::dehydrate`
    - Drain ChangeTracker
    - Read changed entities from redb, convert via EntityBridge::to_row
    - Upsert to LibSQL (soft delete for deleted entities)
    - Call `db.sync()` (catch offline errors gracefully)
    - Update sync_metadata with push timestamps
    - Return DehydrateResult with counts
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 8.2_
  - [ ] 6.4 Implement `SyncEngine::full_sync`
    - Dehydrate first, then sync, then detect conflicts
    - Apply ConflictResolver with specified strategy
    - Hydrate new/updated remote entities back into redb
    - Support Incremental strategy filtering via sync_metadata timestamps
    - Return SyncResult
    - _Requirements: 4.1, 4.2, 4.3, 4.7, 4.8, 4.9, 8.4_
  - [ ] 6.5 Implement `SyncEngine::is_online`
    - Attempt lightweight `db.sync()`, return bool
    - _Requirements: 5.1, 5.2_
  - [ ] 6.6 Implement `SyncEngine::configure`
    - Update config, persist to file, restart auto-sync timer if needed
    - _Requirements: 6.2, 6.3, 6.4_

- [ ] 7. Implement auto-sync and RBAC integration
  - [ ] 7.1 Implement auto-sync timer in `SyncEngine`
    - `start_auto_sync` spawns a tokio task that calls dehydrate at the configured interval
    - `stop_auto_sync` cancels the task
    - Timer respects `auto_sync_enabled` and `sync_interval_seconds`
    - _Requirements: 6.3, 6.4_
  - [ ] 7.2 Add RBAC permission checks to SyncEngine public methods
    - `hydrate`, `dehydrate`, `full_sync` require `sync:trigger` permission (Admin or Manager)
    - `configure` requires Admin role
    - Return SyncError::AccessDenied for unauthorized access
    - _Requirements: 12.1, 12.2, 12.3, 12.4_
  - [ ]* 7.3 Write property test for RBAC enforcement
    - **Property 9: RBAC enforcement for sync operations**
    - Generate random UserRoles, verify Admin/Manager can trigger sync, only Admin can configure
    - **Validates: Requirements 12.1, 12.2, 12.3, 12.4**

- [ ] 8. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 9. Implement hydrate/dehydrate integration tests
  - [ ]* 9.1 Write property test for hydrate
    - **Property 4: Hydrate loads non-deleted entities preserving field values**
    - Seed LibSQL with random entities (some deleted), hydrate, verify redb contents
    - **Validates: Requirements 2.3, 2.4, 2.5**
  - [ ]* 9.2 Write property test for dehydrate
    - **Property 5: Dehydrate writes all tracked changes including soft deletes**
    - Track random entity changes, dehydrate, verify LibSQL contents
    - **Validates: Requirements 3.1, 3.2, 3.3, 3.6**
  - [ ]* 9.3 Write property test for incremental sync filtering
    - **Property 7: Incremental sync filters by last sync timestamp**
    - Seed entities with various timestamps, set sync_metadata, verify only newer entities processed
    - **Validates: Requirements 4.9, 8.4**
  - [ ]* 9.4 Write property test for sync metadata updates
    - **Property 8: Sync metadata updated after sync operations**
    - Run hydrate/dehydrate, verify sync_metadata rows updated correctly
    - **Validates: Requirements 8.2, 8.3**
  - [ ]* 9.5 Write property test for invalid config error handling
    - **Property 10: Invalid config produces error without panic**
    - Generate random malformed URLs/tokens, verify SyncError::ConnectionFailed returned
    - **Validates: Requirements 1.2**

- [ ] 10. Wire SyncEngine to Slint UI SyncPage
  - [ ] 10.1 Create Slint global callbacks for sync operations
    - Add sync callbacks to AppState global: `sync-to-remote`, `sync-from-remote`, `check-online`
    - Add sync status properties: `pending-changes`, `is-online`, `last-sync-at`, `syncing`
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6_
  - [ ] 10.2 Implement Rust-side callback handlers
    - Connect AppState callbacks to SyncEngine methods
    - Handle async operations with tokio, update Slint properties on completion
    - Display errors on the SyncPage
    - _Requirements: 11.4, 11.5, 11.7, 5.3_

- [ ] 11. Wire application lifecycle integration
  - [ ] 11.1 Integrate SyncEngine into application startup
    - Load SyncConfig, construct SyncEngine, call hydrate on startup
    - Subscribe ChangeTracker to Qleany EventHub
    - Start auto-sync timer if configured
    - _Requirements: 14.1, 13.1_
  - [ ] 11.2 Integrate SyncEngine into application shutdown
    - Call dehydrate on shutdown, handle offline gracefully
    - Stop auto-sync timer
    - _Requirements: 14.2, 14.3, 13.2_

- [ ] 12. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties using `proptest`
- Unit tests validate specific examples and edge cases
- All tests use in-memory LibSQL (`:memory:`) — no TursoDB Cloud dependency needed for testing
