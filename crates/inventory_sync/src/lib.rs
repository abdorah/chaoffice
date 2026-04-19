pub mod bridge;
pub mod config;
pub mod conflict;
pub mod engine;
pub mod error;
pub mod schema;
pub mod tracker;
pub mod types;

// Public re-exports
pub use config::{SyncConfig, SyncStrategy};
pub use conflict::{ConflictResolver, ResolvedEntity};
pub use engine::SyncEngine;
pub use error::SyncError;
pub use tracker::ChangeTracker;
pub use types::{DehydrateResult, EntityRow, HydrateResult, SyncResult};
