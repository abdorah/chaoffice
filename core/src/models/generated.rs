//! Re-exports of prost-generated protobuf structs.
//!
//! The build.rs compiles `.proto` files and outputs Rust modules into `src/models/`.
//! This module includes and re-exports them under a clean namespace.

/// Prost-generated structs from `sweetlab/models.proto`
pub mod models {
    include!("sweetlab.models.rs");
}

/// Prost-generated structs from `sweetlab/auth.proto`
pub mod auth {
    include!("sweetlab.auth.rs");
}

/// Prost-generated structs from `sweetlab/services.proto`
pub mod services {
    include!("sweetlab.services.rs");
}
