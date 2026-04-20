# LibSQL Integration Tasks — DONE

- [x] 1. Add `libsql` dependency to inventory_sync Cargo.toml
- [x] 2. Implement SyncEngine::new with real LibSQL connection (local-only or remote replica)
- [x] 3. Implement SchemaManager::init_schema with real SQL execution
- [x] 4. Implement SyncEngine::hydrate with real LibSQL queries (SELECT per table)
- [x] 5. Implement SyncEngine::dehydrate with real LibSQL connection verification
- [x] 6. Implement SyncEngine::is_online (SELECT 1 for local, db.sync() for remote)
- [x] 7. Wire hydrate/dehydrate in main.rs lifecycle (dehydrate on shutdown)
- [x] 8. Verify build and test — all pass, zero STUB comments remain
