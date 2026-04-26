//! ChangeTracker — records pending entity changes for incremental dehydration.
//!
//! Fully implemented (pure Rust, no external deps). Thread-safe via Mutex.

use std::collections::{HashMap, HashSet};
use common::types::EntityId;
use std::sync::Mutex;

/// Tracks which entities have been modified since the last dehydration.
///
/// Thread-safe: can be written to from EventHub callbacks and drained
/// from the dehydrate operation concurrently.
pub struct ChangeTracker {
    pending: Mutex<HashMap<String, HashSet<EntityId>>>,
}

impl ChangeTracker {
    pub fn new() -> Self {
        Self {
            pending: Mutex::new(HashMap::new()),
        }
    }

    /// Record one or more entity IDs as changed for a given entity type.
    ///
    /// Called when Qleany emits entity events (create/update/delete).
    /// Uses set semantics — duplicate IDs are stored only once.
    pub fn on_entity_event(&self, entity_type: &str, entity_ids: &[EntityId]) {
        let mut pending = self.pending.lock().unwrap();
        let set = pending.entry(entity_type.to_string()).or_default();
        for &id in entity_ids {
            set.insert(id);
        }
    }

    /// Drain all pending changes, resetting internal state to empty.
    ///
    /// Returns the accumulated changes. After this call, `pending_count()` is 0.
    pub fn drain(&self) -> HashMap<String, HashSet<EntityId>> {
        let mut pending = self.pending.lock().unwrap();
        std::mem::take(&mut *pending)
    }

    /// Get the total count of pending entity changes (for UI display).
    pub fn pending_count(&self) -> usize {
        let pending = self.pending.lock().unwrap();
        pending.values().map(|s| s.len()).sum()
    }
}

impl Default for ChangeTracker {
    fn default() -> Self {
        Self::new()
    }
}
