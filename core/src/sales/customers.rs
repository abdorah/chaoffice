use chrono::Utc;
use sqlx::SqlitePool;
use uuid::Uuid;

use crate::error::{AppError, AppResult};
use crate::models::domain::{Customer, Pagination};
use crate::models::Money;
use crate::persistence::queries::customers as queries;

/// Service for managing customer profiles.
pub struct CustomerService {
    pool: SqlitePool,
}

impl CustomerService {
    pub fn new(pool: SqlitePool) -> Self {
        Self { pool }
    }

    /// Fetch all customers. total_debt and overdue_days default to 0
    /// (will be computed from debt records later).
    pub async fn get_customers(&self, pagination: Option<Pagination>) -> AppResult<Vec<Customer>> {
        let pg = pagination.unwrap_or_default();
        let rows = queries::list_all(&self.pool, &pg).await?;
        rows.into_iter().map(row_to_domain).collect()
    }

    /// Create a new customer with initial reliability_rating = 0 (Req 7.1).
    /// Rejects duplicate mobile numbers (Req 7.3).
    pub async fn create_customer(
        &self,
        name: &str,
        city: &str,
        mobile: &str,
    ) -> AppResult<Customer> {
        let id = Uuid::new_v4();
        let now = Utc::now().to_rfc3339();

        match queries::insert(&self.pool, &id.to_string(), name, city, mobile, &now).await {
            Ok(()) => {}
            Err(AppError::Database(ref db_err)) if is_unique_violation(db_err) => {
                return Err(AppError::Duplicate {
                    field: "mobile".to_string(),
                    value: mobile.to_string(),
                });
            }
            Err(e) => return Err(e),
        }

        Ok(Customer {
            id,
            name: name.to_string(),
            city: city.to_string(),
            mobile: mobile.to_string(),
            reliability_rating: 0,
            total_debt: Money::ZERO,
            overdue_days: 0,
        })
    }

    /// Update a customer's reliability rating (Req 7.2).
    /// Accepts values in [1, 5] inclusive; rejects anything outside.
    pub async fn update_reliability_rating(
        &self,
        customer_id: Uuid,
        rating: i32,
    ) -> AppResult<()> {
        if !(1..=5).contains(&rating) {
            return Err(AppError::Validation {
                field: "reliability_rating".to_string(),
                message: format!("Rating must be between 1 and 5, got {rating}"),
            });
        }

        let now = Utc::now().to_rfc3339();
        let rows_affected =
            queries::update_rating(&self.pool, &customer_id.to_string(), rating, &now).await?;

        if rows_affected == 0 {
            return Err(AppError::Validation {
                field: "customer_id".to_string(),
                message: format!("Customer {customer_id} not found"),
            });
        }

        Ok(())
    }

    /// Search customers by name, city, or mobile (Req 7.4).
    pub async fn search_customers(&self, query: &str, pagination: Option<Pagination>) -> AppResult<Vec<Customer>> {
        let pg = pagination.unwrap_or_default();
        let rows = queries::search(&self.pool, query, &pg).await?;
        rows.into_iter().map(row_to_domain).collect()
    }
}

/// Check whether a sqlx::Error is a UNIQUE constraint violation.
fn is_unique_violation(err: &sqlx::Error) -> bool {
    match err {
        sqlx::Error::Database(db_err) => {
            db_err.message().contains("UNIQUE constraint failed")
        }
        _ => false,
    }
}

/// Convert a persistence row to a domain model.
fn row_to_domain(row: queries::CustomerRow) -> AppResult<Customer> {
    let id = crate::utils::parse_uuid("customer", &row.id)?;

    Ok(Customer {
        id,
        name: row.name,
        city: row.city,
        mobile: row.mobile,
        reliability_rating: row.reliability_rating,
        total_debt: Money(row.total_debt),
        overdue_days: row.overdue_days,
    })
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::persistence::db;

    async fn setup() -> (SqlitePool, CustomerService) {
        let pool = db::init_db(":memory:").await.expect("DB init failed");
        let svc = CustomerService::new(pool.clone());
        (pool, svc)
    }

    #[tokio::test]
    async fn create_customer_has_initial_rating_zero() {
        let (_pool, svc) = setup().await;
        let customer = svc
            .create_customer("Ahmad", "Damascus", "+963911111111")
            .await
            .unwrap();

        assert_eq!(customer.name, "Ahmad");
        assert_eq!(customer.city, "Damascus");
        assert_eq!(customer.mobile, "+963911111111");
        assert_eq!(customer.reliability_rating, 0);
        assert_eq!(customer.total_debt, Money::ZERO);
        assert_eq!(customer.overdue_days, 0);
    }

    #[tokio::test]
    async fn duplicate_mobile_rejected() {
        let (_pool, svc) = setup().await;
        svc.create_customer("Ahmad", "Damascus", "+963911111111")
            .await
            .unwrap();

        let err = svc
            .create_customer("Khaled", "Aleppo", "+963911111111")
            .await
            .unwrap_err();

        match err {
            AppError::Duplicate { field, value } => {
                assert_eq!(field, "mobile");
                assert_eq!(value, "+963911111111");
            }
            other => panic!("Expected Duplicate, got: {other:?}"),
        }
    }

    #[tokio::test]
    async fn update_rating_within_bounds() {
        let (_pool, svc) = setup().await;
        let customer = svc
            .create_customer("Ahmad", "Damascus", "+963911111111")
            .await
            .unwrap();

        for rating in 1..=5 {
            svc.update_reliability_rating(customer.id, rating)
                .await
                .unwrap();
        }

        let customers = svc.get_customers(None).await.unwrap();
        assert_eq!(customers[0].reliability_rating, 5);
    }

    #[tokio::test]
    async fn update_rating_out_of_bounds_rejected() {
        let (_pool, svc) = setup().await;
        let customer = svc
            .create_customer("Ahmad", "Damascus", "+963911111111")
            .await
            .unwrap();

        for invalid_rating in [0, -1, 6, 100] {
            let err = svc
                .update_reliability_rating(customer.id, invalid_rating)
                .await
                .unwrap_err();
            assert!(
                matches!(err, AppError::Validation { .. }),
                "Expected Validation error for rating {invalid_rating}, got: {err:?}"
            );
        }
    }

    #[tokio::test]
    async fn update_rating_nonexistent_customer_fails() {
        let (_pool, svc) = setup().await;
        let err = svc
            .update_reliability_rating(Uuid::new_v4(), 3)
            .await
            .unwrap_err();
        assert!(matches!(err, AppError::Validation { .. }));
    }

    #[tokio::test]
    async fn search_by_name() {
        let (_pool, svc) = setup().await;
        svc.create_customer("Ahmad Hassan", "Damascus", "+963911111111")
            .await
            .unwrap();
        svc.create_customer("Khaled Omar", "Aleppo", "+963922222222")
            .await
            .unwrap();

        let results = svc.search_customers("Ahmad", None).await.unwrap();
        assert_eq!(results.len(), 1);
        assert_eq!(results[0].name, "Ahmad Hassan");
    }

    #[tokio::test]
    async fn search_by_city() {
        let (_pool, svc) = setup().await;
        svc.create_customer("Ahmad", "Damascus", "+963911111111")
            .await
            .unwrap();
        svc.create_customer("Khaled", "Aleppo", "+963922222222")
            .await
            .unwrap();

        let results = svc.search_customers("Aleppo", None).await.unwrap();
        assert_eq!(results.len(), 1);
        assert_eq!(results[0].name, "Khaled");
    }

    #[tokio::test]
    async fn search_by_mobile() {
        let (_pool, svc) = setup().await;
        svc.create_customer("Ahmad", "Damascus", "+963911111111")
            .await
            .unwrap();
        svc.create_customer("Khaled", "Aleppo", "+963922222222")
            .await
            .unwrap();

        let results = svc.search_customers("9222", None).await.unwrap();
        assert_eq!(results.len(), 1);
        assert_eq!(results[0].name, "Khaled");
    }

    #[tokio::test]
    async fn search_no_matches_returns_empty() {
        let (_pool, svc) = setup().await;
        svc.create_customer("Ahmad", "Damascus", "+963911111111")
            .await
            .unwrap();

        let results = svc.search_customers("nonexistent", None).await.unwrap();
        assert!(results.is_empty());
    }

    #[tokio::test]
    async fn get_customers_returns_all() {
        let (_pool, svc) = setup().await;
        svc.create_customer("Ahmad", "Damascus", "+963911111111")
            .await
            .unwrap();
        svc.create_customer("Khaled", "Aleppo", "+963922222222")
            .await
            .unwrap();

        let customers = svc.get_customers(None).await.unwrap();
        assert_eq!(customers.len(), 2);
    }
}
