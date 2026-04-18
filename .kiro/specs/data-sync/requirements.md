# Requirements Document

## Introduction

This document specifies the data synchronization layer for the Inventory Management Application. The feature is implemented as a standalone `inventory_sync` Rust crate (not a Qleany feature) that bridges Qleany's internal redb database with TursoDB Cloud via LibSQL embedded replicas. It provides online/offline capability with configurable sync strategies, change tracking, and conflict resolution. The crate covers the full sync lifecycle: hydration (LibSQL → redb on startup), dehydration (redb → LibSQL on save/periodic), full bidirectional sync with conflict resolution, connectivity detection, and configuration management. The Slint UI exposes sync controls on the SyncPage, gated by RBAC permissions.

## Glossary

- **Sync_Engine**: The core orchestrator responsible for hydrate, dehydrate, full_sync, and connectivity operations. Implemented in the `inventory_sync` crate.
- **LibSQL_Replica**: A local SQLite file managed by the `libsql` crate that acts as a persistent embedded replica of the TursoDB Cloud database. Syncs bidirectionally with TursoDB when online.
- **redb**: Qleany's fast in-memory runtime database supporting undo/redo. Ephemeral — discarded on app close.
- **TursoDB_Cloud**: The remote cloud database service that stores the canonical copy of all synced entities.
- **Change_Tracker**: A component that subscribes to Qleany's EventHub and records which entities have been modified in redb since the last dehydration.
- **Conflict_Resolver**: A component that resolves conflicts when the same entity has been modified both locally and remotely since the last sync.
- **Sync_Config**: A configuration structure containing TursoDB connection credentials, auto-sync settings, and the default conflict resolution strategy. Persisted in a local config file.
- **Sync_Strategy**: An enum defining the sync approach: Full, Incremental, ConflictResolveLocal, or ConflictResolveRemote.
- **Sync_Metadata**: A per-entity-type record tracking last push and pull timestamps and counts, stored in the LibSQL database.
- **Entity_Bridge**: The serialization layer that converts Qleany entity structs to/from LibSQL SQL rows.
- **Sync_Page**: The Slint UI page displaying sync status, push/pull controls, connection indicator, pending changes count, and last sync time.
- **Hydrate**: The operation of loading all entities from LibSQL into redb at application startup.
- **Dehydrate**: The operation of flushing changed entities from redb to LibSQL on save, periodic interval, or shutdown.

## Requirements

### Requirement 1: Sync Engine Initialization

**User Story:** As a developer, I want to initialize the sync engine with TursoDB credentials and a local replica path, so that the application can establish a connection between the local LibSQL replica and TursoDB Cloud.

#### Acceptance Criteria

1. WHEN the Sync_Engine is constructed with a valid Sync_Config, THE Sync_Engine SHALL open a LibSQL embedded replica at the configured local file path connected to the specified TursoDB_Cloud URL and auth token.
2. IF the TursoDB_Cloud URL or auth token is invalid, THEN THE Sync_Engine SHALL return a descriptive connection error without crashing.
3. IF the local LibSQL replica file does not exist, THEN THE Sync_Engine SHALL create the file and initialize the schema (all entity tables and sync_metadata table).
4. WHEN the Sync_Engine initializes the schema, THE Sync_Engine SHALL create tables for all synced entity types (Product, Category, Person, Contact, Deal, Location, User, Session, StockMovement, BudgetEntry) with columns matching the Qleany entity fields, including `created_at`, `updated_at`, and `deleted_at` columns.

### Requirement 2: Hydration (LibSQL to redb)

**User Story:** As a user, I want the application to load all data from the persistent LibSQL replica into the runtime database on startup, so that I can work with the latest available data.

#### Acceptance Criteria

1. WHEN hydrate is called, THE Sync_Engine SHALL first attempt to sync the LibSQL_Replica from TursoDB_Cloud to pull the latest remote changes.
2. IF the TursoDB_Cloud is unreachable during hydrate, THEN THE Sync_Engine SHALL proceed using the existing local LibSQL_Replica data and report the offline status.
3. WHEN hydrate reads entities from LibSQL, THE Sync_Engine SHALL load all non-deleted entities (where `deleted_at` is NULL) into redb via the Qleany repository layer.
4. WHEN hydrate completes, THE Sync_Engine SHALL return a HydrateResult containing the count of entities loaded per entity type.
5. WHEN hydrate loads entities into redb, THE Sync_Engine SHALL preserve all field values including `created_at` and `updated_at` timestamps without modification.

### Requirement 3: Dehydration (redb to LibSQL)

**User Story:** As a user, I want my local changes to be persisted to the LibSQL replica, so that changes survive application restarts and can be synced to the cloud.

#### Acceptance Criteria

1. WHEN dehydrate is called, THE Sync_Engine SHALL read all pending changes from the Change_Tracker and write the corresponding entities from redb to LibSQL using INSERT or UPDATE (upsert) operations.
2. WHEN dehydrate writes entities to LibSQL, THE Sync_Engine SHALL update the `updated_at` column to reflect the current timestamp for each modified entity.
3. WHEN dehydrate processes a deleted entity, THE Sync_Engine SHALL set the `deleted_at` column to the current timestamp in LibSQL instead of removing the row (soft delete).
4. WHEN dehydrate completes writing to LibSQL, THE Sync_Engine SHALL attempt to sync the LibSQL_Replica to TursoDB_Cloud.
5. IF TursoDB_Cloud is unreachable during dehydrate, THEN THE Sync_Engine SHALL complete the local LibSQL write successfully and defer the remote sync until connectivity is restored.
6. WHEN dehydrate completes, THE Sync_Engine SHALL return a DehydrateResult containing the count of entities written per entity type and whether the remote sync succeeded.
7. WHEN dehydrate completes successfully, THE Sync_Engine SHALL drain the Change_Tracker pending changes for the processed entity types.

### Requirement 4: Full Bidirectional Sync

**User Story:** As a user, I want to manually trigger a full push-and-pull sync with conflict resolution, so that my local data and the remote cloud data are reconciled.

#### Acceptance Criteria

1. WHEN full_sync is called with a Sync_Strategy, THE Sync_Engine SHALL first dehydrate all pending local changes to LibSQL.
2. WHEN full_sync performs the remote sync, THE Sync_Engine SHALL push local LibSQL changes to TursoDB_Cloud and pull remote changes from TursoDB_Cloud to the local LibSQL_Replica.
3. WHEN full_sync detects that the same entity has been modified both locally and remotely since the last sync, THE Conflict_Resolver SHALL resolve the conflict according to the specified Sync_Strategy.
4. WHEN the Sync_Strategy is ConflictResolveLocal, THE Conflict_Resolver SHALL keep the local version of the conflicting entity.
5. WHEN the Sync_Strategy is ConflictResolveRemote, THE Conflict_Resolver SHALL keep the remote version of the conflicting entity.
6. WHEN the Sync_Strategy is Full or Incremental, THE Conflict_Resolver SHALL use last-write-wins resolution based on the `updated_at` timestamp.
7. WHEN full_sync completes conflict resolution, THE Sync_Engine SHALL hydrate any new or updated remote entities back into redb.
8. WHEN full_sync completes, THE Sync_Engine SHALL return a SyncResult containing counts of pushed, pulled, and conflicted entities.
9. WHEN full_sync is called with the Incremental strategy, THE Sync_Engine SHALL only process entities with `updated_at` later than the last sync timestamp recorded in Sync_Metadata.

### Requirement 5: Connectivity Detection

**User Story:** As a user, I want the application to detect whether TursoDB Cloud is reachable, so that the UI can display the current connection status.

#### Acceptance Criteria

1. WHEN is_online is called, THE Sync_Engine SHALL attempt a lightweight sync operation against TursoDB_Cloud and return true if the operation succeeds.
2. WHEN is_online is called and TursoDB_Cloud is unreachable, THE Sync_Engine SHALL return false without raising an error.
3. WHEN the connectivity status changes, THE Sync_Engine SHALL report the new status so the UI can update the connection indicator.

### Requirement 6: Sync Configuration

**User Story:** As an administrator, I want to configure TursoDB connection details, auto-sync behavior, and the default sync strategy, so that the sync behavior matches the deployment environment.

#### Acceptance Criteria

1. THE Sync_Config SHALL contain fields for: turso_url (String), turso_auth_token (String), auto_sync_enabled (bool), sync_interval_seconds (u64), and default_strategy (Sync_Strategy).
2. WHEN configure is called with a new Sync_Config, THE Sync_Engine SHALL persist the configuration to a local config file and apply the new settings.
3. WHEN auto_sync_enabled is true and sync_interval_seconds is greater than zero, THE Sync_Engine SHALL automatically trigger dehydrate at the configured interval.
4. WHEN auto_sync_enabled is set to false, THE Sync_Engine SHALL stop any running auto-sync timer.
5. WHEN the application starts, THE Sync_Engine SHALL load the Sync_Config from the persisted config file if it exists.
6. IF no persisted Sync_Config file exists on startup, THEN THE Sync_Engine SHALL use default values: auto_sync_enabled = false, sync_interval_seconds = 300, default_strategy = Incremental.

### Requirement 7: Change Tracking

**User Story:** As the system, I want to track which entities have been modified in redb since the last dehydration, so that only changed data is written during dehydrate.

#### Acceptance Criteria

1. WHEN a Qleany entity event is emitted on the EventHub (create, update, or delete), THE Change_Tracker SHALL record the entity type and entity ID in its pending changes map.
2. THE Change_Tracker SHALL store pending changes as a mapping from entity type name to a set of entity IDs.
3. WHEN the Change_Tracker is drained during dehydrate, THE Change_Tracker SHALL return all pending changes and reset its internal state to empty.
4. WHEN multiple events occur for the same entity ID, THE Change_Tracker SHALL store the entity ID only once per entity type (set semantics).
5. THE Change_Tracker SHALL be thread-safe, allowing concurrent event recording from the EventHub and draining from the dehydrate operation.

### Requirement 8: Sync Metadata Tracking

**User Story:** As the system, I want to track the last push and pull timestamps per entity type, so that incremental sync can determine which entities have changed since the last sync.

#### Acceptance Criteria

1. THE Sync_Metadata table in LibSQL SHALL store one row per entity type with columns: entity_type (TEXT PRIMARY KEY), last_push_at (TEXT), last_pull_at (TEXT), last_push_count (INTEGER), last_pull_count (INTEGER).
2. WHEN a dehydrate or push operation completes for an entity type, THE Sync_Engine SHALL update the corresponding Sync_Metadata row with the current timestamp and count of pushed entities.
3. WHEN a hydrate or pull operation completes for an entity type, THE Sync_Engine SHALL update the corresponding Sync_Metadata row with the current timestamp and count of pulled entities.
4. WHEN the Incremental strategy is used, THE Sync_Engine SHALL query Sync_Metadata to determine the last sync timestamp and only process entities modified after that timestamp.

### Requirement 9: Entity Bridge (redb ↔ LibSQL Serialization)

**User Story:** As a developer, I want a serialization layer that converts Qleany entities between redb structs and LibSQL SQL rows, so that data can flow between the two databases.

#### Acceptance Criteria

1. THE Entity_Bridge SHALL provide a `to_row` function that converts a Qleany entity struct into a set of LibSQL column values for each synced entity type.
2. THE Entity_Bridge SHALL provide a `from_row` function that converts a LibSQL row into a Qleany entity struct for each synced entity type.
3. FOR ALL valid Qleany entity instances, converting with `to_row` then `from_row` SHALL produce an entity equivalent to the original (round-trip property).
4. WHEN an entity contains optional relationship fields (e.g., `category_id`, `supplier_id`), THE Entity_Bridge SHALL map None values to SQL NULL and SQL NULL values back to None.
5. WHEN an entity contains enum fields (e.g., ProductStatus, MovementType), THE Entity_Bridge SHALL serialize enums as TEXT strings and deserialize TEXT strings back to the corresponding enum variant.
6. WHEN an entity contains datetime fields, THE Entity_Bridge SHALL serialize datetimes as ISO 8601 TEXT strings and deserialize ISO 8601 TEXT strings back to datetime values.

### Requirement 10: LibSQL Schema Management

**User Story:** As a developer, I want the LibSQL schema to be automatically created and maintained, so that the sync layer works without manual database setup.

#### Acceptance Criteria

1. WHEN the Sync_Engine initializes, THE Sync_Engine SHALL execute CREATE TABLE IF NOT EXISTS statements for all synced entity tables and the sync_metadata table.
2. THE LibSQL schema SHALL include a `deleted_at` TEXT column on every entity table to support soft-delete tombstones.
3. THE LibSQL schema SHALL use INTEGER PRIMARY KEY for the `id` column on all entity tables, matching the Qleany entity ID type.
4. THE LibSQL schema SHALL include `created_at` TEXT and `updated_at` TEXT columns on all entity tables.

### Requirement 11: Sync Page UI

**User Story:** As a user, I want a sync management page in the Slint UI, so that I can view sync status, trigger push/pull operations, and see the connection indicator.

#### Acceptance Criteria

1. WHEN the Sync_Page is displayed, THE Sync_Page SHALL show the current pending changes count from the Change_Tracker.
2. WHEN the Sync_Page is displayed, THE Sync_Page SHALL show the current online/offline connection status.
3. WHEN the Sync_Page is displayed, THE Sync_Page SHALL show the timestamp of the last successful sync operation.
4. WHEN a user with `sync:trigger` permission clicks "Push to Remote", THE Sync_Page SHALL invoke the Sync_Engine dehydrate operation and display the result.
5. WHEN a user with `sync:trigger` permission clicks "Pull from Remote", THE Sync_Page SHALL invoke the Sync_Engine hydrate operation and display the result.
6. WHILE a sync operation is in progress, THE Sync_Page SHALL display a progress indicator and disable the push/pull buttons.
7. IF a sync operation fails, THEN THE Sync_Page SHALL display the error message to the user.

### Requirement 12: RBAC Integration for Sync Operations

**User Story:** As an administrator, I want sync operations to be gated by role-based permissions, so that only authorized users can trigger sync or modify sync configuration.

#### Acceptance Criteria

1. WHEN a user with the Admin or Manager role triggers a push or pull sync operation, THE Sync_Engine SHALL execute the operation (permission: `sync:trigger`).
2. WHEN a user without `sync:trigger` permission attempts a push or pull operation, THE Sync_Engine SHALL reject the operation with an AccessDenied error.
3. WHEN a user with the Admin role modifies the Sync_Config, THE Sync_Engine SHALL apply the configuration change (permission: `sync:configure` is Admin-only).
4. WHEN a non-Admin user attempts to modify the Sync_Config, THE Sync_Engine SHALL reject the operation with an AccessDenied error.

### Requirement 13: Offline Resilience

**User Story:** As a user, I want the application to work seamlessly when offline, so that I can continue working and have my changes synced when connectivity returns.

#### Acceptance Criteria

1. WHEN the application starts and TursoDB_Cloud is unreachable, THE Sync_Engine SHALL hydrate from the local LibSQL_Replica data without error.
2. WHEN dehydrate is called and TursoDB_Cloud is unreachable, THE Sync_Engine SHALL persist changes to the local LibSQL_Replica and queue them for remote sync.
3. WHEN connectivity is restored after an offline period, THE Sync_Engine SHALL sync accumulated local changes to TursoDB_Cloud on the next sync operation.
4. THE Sync_Engine SHALL accumulate Change_Tracker entries across multiple offline dehydrate cycles until a successful remote sync clears them from LibSQL.

### Requirement 14: Application Lifecycle Integration

**User Story:** As a user, I want sync to integrate with the application startup and shutdown lifecycle, so that data is loaded on start and saved on close.

#### Acceptance Criteria

1. WHEN the application starts, THE application SHALL call Sync_Engine hydrate to load data from LibSQL into redb.
2. WHEN the application shuts down, THE application SHALL call Sync_Engine dehydrate to flush all pending changes from redb to LibSQL.
3. WHEN the application shuts down and TursoDB_Cloud is unreachable, THE application SHALL complete the local dehydrate to LibSQL and exit without blocking on remote sync.
