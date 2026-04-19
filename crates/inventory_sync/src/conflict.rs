//! ConflictResolver — strategy-based conflict resolution (pure logic).
//!
//! Fully implemented. No external dependencies.

use crate::config::SyncStrategy;
use crate::types::EntityRow;

/// The outcome of a conflict resolution.
#[derive(Debug, Clone)]
pub enum ResolvedEntity {
    KeepLocal(EntityRow),
    KeepRemote(EntityRow),
}

/// Resolves conflicts between local and remote entity versions.
pub struct ConflictResolver;

impl ConflictResolver {
    /// Resolve a conflict between local and remote versions of an entity.
    ///
    /// - `ConflictResolveLocal` → always keep local
    /// - `ConflictResolveRemote` → always keep remote
    /// - `Full` / `Incremental` → last-write-wins based on `updated_at`
    pub fn resolve(
        strategy: &SyncStrategy,
        local: &EntityRow,
        remote: &EntityRow,
    ) -> ResolvedEntity {
        match strategy {
            SyncStrategy::ConflictResolveLocal => ResolvedEntity::KeepLocal(local.clone()),
            SyncStrategy::ConflictResolveRemote => ResolvedEntity::KeepRemote(remote.clone()),
            SyncStrategy::Full | SyncStrategy::Incremental => {
                // Last-write-wins: the entity with the later updated_at wins.
                // If timestamps are equal, prefer local (tie-break).
                if local.updated_at >= remote.updated_at {
                    ResolvedEntity::KeepLocal(local.clone())
                } else {
                    ResolvedEntity::KeepRemote(remote.clone())
                }
            }
        }
    }
}
