pub mod api;
pub mod auth;
pub mod debt;
pub mod error;
pub mod expenses;
pub mod inventory;
pub mod models;
pub mod persistence;
pub mod recipes;
pub mod reports;
pub mod sales;
pub mod sync;
pub mod wallet;

uniffi::include_scaffolding!("sweet_lab_core");
