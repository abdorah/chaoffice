// Property tests for Offline Mode and Sync (Properties 35, 36, 37)
//
// **Validates: Requirements 13.1, 13.2, 13.3**

mod generators;

use proptest::prelude::*;
use tokio::runtime::Runtime;

use sweet_lab_core::models::domain::{Pagination, SyncStatus};
use sweet_lab_core::persistence::db;
use sweet_lab_core::sync::{conflict, queue};

// ── Helpers ────────────────────────────────────────────────────────────────

async fn setup() -> sqlx::SqlitePool {
    db::init_db(":memory:").await.expect("DB init failed")
}

fn arb_entity_type() -> impl Strategy<Value = String> {
    prop_oneof![
        Just("product".to_string()),
        Just("customer".to_string()),
        Just("sale".to_string()),
        Just("wallet".to_string()),
        Just("expense".to_string()),
        Just("raw_material".to_string()),
    ]
}

fn arb_operation() -> impl Strategy<Value = String> {
    prop_oneof![
        Just("CREATE".to_string()),
        Just("UPDATE".to_string()),
        Just("DELETE".to_string()),
    ]
}

fn arb_payload() -> impl Strategy<Value = String> {
    "[a-zA-Z0-9 ]{1,100}".prop_map(|s| format!(r#"{{"data":"{}"}}"#, s.trim()))
}

// ── Property 35: Offline queue persistence ─────────────────────────────────
//
// For any data modification performed while offline, the modification SHALL
// be stored in the sync queue with the correct entity_type, entity_id,
// operation, and payload.
//
// **Validates: Requirements 13.1**

proptest! {
    #![proptest_config(ProptestConfig::with_cases(30))]

    #[test]
    fn prop35_offline_queue_persistence(
        entity_type in arb_entity_type(),
        entity_id in generators::arb_uuid(),
        operation in arb_operation(),
        payload in arb_payload(),
    ) {
        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let pool = setup().await;

            // Queue a modification (simulating offline write)
            let item = queue::enqueue(&pool, &entity_type, entity_id, &operation, &payload)
                .await
                .unwrap();

            // Verify the returned item has correct metadata
            prop_assert_eq!(&item.entity_type, &entity_type);
            prop_assert_eq!(item.entity_id, entity_id);
            prop_assert_eq!(&item.operation, &operation);
            prop_assert_eq!(&item.payload, &payload);
            prop_assert_eq!(item.status, SyncStatus::Pending);

            // Verify the item is persisted and retrievable from the queue
            let pending = queue::get_pending(&pool).await.unwrap();
            prop_assert_eq!(pending.len(), 1);

            let stored = &pending[0];
            prop_assert_eq!(&stored.entity_type, &entity_type);
            prop_assert_eq!(stored.entity_id, entity_id);
            prop_assert_eq!(&stored.operation, &operation);
            prop_assert_eq!(&stored.payload, &payload);
            prop_assert_eq!(stored.status.clone(), SyncStatus::Pending);

            Ok(())
        })?;
    }
}

// ── Property 36: Sync queue FIFO ordering ──────────────────────────────────
//
// For any set of queued modifications, synchronization SHALL process them
// in the order they were created (ascending by created_at timestamp).
//
// **Validates: Requirements 13.2**

proptest! {
    #![proptest_config(ProptestConfig::with_cases(20))]

    #[test]
    fn prop36_sync_queue_fifo_ordering(
        // Generate 2-8 items with varying entity types and operations
        items in prop::collection::vec(
            (arb_entity_type(), generators::arb_uuid(), arb_operation(), arb_payload()),
            2..=8
        ),
    ) {
        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let pool = setup().await;

            // Enqueue items in order, recording their IDs
            let mut enqueued_ids = Vec::new();
            for (entity_type, entity_id, operation, payload) in &items {
                let item = queue::enqueue(&pool, entity_type, *entity_id, operation, payload)
                    .await
                    .unwrap();
                enqueued_ids.push(item.id);
            }

            // Fetch pending items — should be in FIFO order (created_at ASC)
            let pending = queue::get_pending(&pool).await.unwrap();
            prop_assert_eq!(pending.len(), items.len());

            // Verify the order matches insertion order
            for (i, item) in pending.iter().enumerate() {
                prop_assert_eq!(
                    item.id, enqueued_ids[i],
                    "Item at position {} should have id {} but had {}",
                    i, enqueued_ids[i], item.id
                );
            }

            // Verify created_at timestamps are in ascending order
            for i in 1..pending.len() {
                prop_assert!(
                    pending[i].created_at >= pending[i - 1].created_at,
                    "Item {} created_at ({}) should be >= item {} created_at ({})",
                    i, pending[i].created_at, i - 1, pending[i - 1].created_at
                );
            }

            Ok(())
        })?;
    }
}

// ── Property 37: Conflict resolution logging ───────────────────────────────
//
// For any synchronization conflict, the Sync_Engine SHALL apply
// last-write-wins and create a ConflictLog entry containing the
// entity_type, entity_id, local_version, remote_version, and
// resolved_with strategy.
//
// **Validates: Requirements 13.3**

proptest! {
    #![proptest_config(ProptestConfig::with_cases(30))]

    #[test]
    fn prop37_conflict_resolution_logging(
        entity_type in arb_entity_type(),
        entity_id in generators::arb_uuid(),
        local_version in arb_payload(),
        remote_version in arb_payload(),
        use_local in any::<bool>(),
    ) {
        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let pool = setup().await;

            let resolved_with = if use_local { "LOCAL" } else { "REMOTE" };

            // Log a conflict (simulating last-write-wins resolution)
            let entry = conflict::log_conflict(
                &pool,
                &entity_type,
                entity_id,
                &local_version,
                &remote_version,
                resolved_with,
            )
            .await
            .unwrap();

            // Verify the returned ConflictLog has all required fields
            prop_assert_eq!(&entry.entity_type, &entity_type);
            prop_assert_eq!(entry.entity_id, entity_id);
            prop_assert_eq!(&entry.local_version, &local_version);
            prop_assert_eq!(&entry.remote_version, &remote_version);
            prop_assert_eq!(&entry.resolved_with, resolved_with);

            // Verify the conflict is persisted and retrievable
            let logs = conflict::get_all(&pool, &Pagination::default()).await.unwrap();
            prop_assert_eq!(logs.len(), 1);

            let stored = &logs[0];
            prop_assert_eq!(&stored.entity_type, &entity_type);
            prop_assert_eq!(stored.entity_id, entity_id);
            prop_assert_eq!(&stored.local_version, &local_version);
            prop_assert_eq!(&stored.remote_version, &remote_version);
            prop_assert_eq!(&stored.resolved_with, resolved_with);

            Ok(())
        })?;
    }
}
