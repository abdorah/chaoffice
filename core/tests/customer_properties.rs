// Property tests for Customer Management (Properties 16, 17, 18, 19)
//
// **Validates: Requirements 7.1, 7.2, 7.3, 7.4**

mod generators;

use proptest::prelude::*;
use tokio::runtime::Runtime;

use sweet_lab_core::error::AppError;
use sweet_lab_core::persistence::db;
use sweet_lab_core::sales::customers::CustomerService;

// ── Helpers ────────────────────────────────────────────────────────────────

async fn setup() -> (sqlx::SqlitePool, CustomerService) {
    let pool = db::init_db(":memory:").await.expect("DB init failed");
    let svc = CustomerService::new(pool.clone());
    (pool, svc)
}

// ── Property 16: Customer initial reliability rating ───────────────────────
//
// For any newly created customer, the reliability_rating SHALL be 0, and the
// stored customer SHALL contain the provided name, city, and mobile number.
//
// **Validates: Requirements 7.1**

proptest! {
    #![proptest_config(ProptestConfig::with_cases(50))]

    #[test]
    fn prop16_customer_initial_reliability_rating(
        name in generators::arb_name(),
        city in generators::arb_name(),
        mobile in generators::arb_mobile(),
    ) {
        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let (_pool, svc) = setup().await;

            let customer = svc.create_customer(&name, &city, &mobile).await;
            prop_assert!(customer.is_ok(), "Customer creation should succeed, got: {:?}", customer.unwrap_err());
            let customer = customer.unwrap();

            // Initial rating must be 0
            prop_assert_eq!(customer.reliability_rating, 0, "New customer should have rating 0");

            // Stored fields must match provided values
            prop_assert_eq!(&customer.name, &name, "Name mismatch");
            prop_assert_eq!(&customer.city, &city, "City mismatch");
            prop_assert_eq!(&customer.mobile, &mobile, "Mobile mismatch");

            Ok(())
        })?;
    }
}

// ── Property 17: Customer reliability rating bounds ────────────────────────
//
// For any reliability rating update, the Sales_Service SHALL accept values
// in [1, 5] and return AppError::Validation for values outside that range.
//
// **Validates: Requirements 7.2**

proptest! {
    #![proptest_config(ProptestConfig::with_cases(50))]

    #[test]
    fn prop17_accept_valid_rating(rating in 1i32..=5) {
        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let (_pool, svc) = setup().await;

            let customer = svc.create_customer("Test", "City", &format!("+96391{:07}", rating))
                .await
                .expect("Customer creation failed");

            let result = svc.update_reliability_rating(customer.id, rating).await;
            prop_assert!(result.is_ok(), "Rating {} should be accepted, got: {:?}", rating, result.unwrap_err());

            Ok(())
        })?;
    }

    #[test]
    fn prop17_reject_rating_below_range(rating in (i32::MIN..1i32)) {
        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let (_pool, svc) = setup().await;

            let customer = svc.create_customer("Test", "City", "+963910000001")
                .await
                .expect("Customer creation failed");

            let result = svc.update_reliability_rating(customer.id, rating).await;
            prop_assert!(result.is_err(), "Rating {} should be rejected", rating);
            match result.unwrap_err() {
                AppError::Validation { field, .. } => {
                    prop_assert_eq!(field, "reliability_rating");
                }
                other => prop_assert!(false, "Expected Validation error, got: {:?}", other),
            }

            Ok(())
        })?;
    }

    #[test]
    fn prop17_reject_rating_above_range(rating in 6i32..=1000) {
        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let (_pool, svc) = setup().await;

            let customer = svc.create_customer("Test", "City", "+963910000002")
                .await
                .expect("Customer creation failed");

            let result = svc.update_reliability_rating(customer.id, rating).await;
            prop_assert!(result.is_err(), "Rating {} should be rejected", rating);
            match result.unwrap_err() {
                AppError::Validation { field, .. } => {
                    prop_assert_eq!(field, "reliability_rating");
                }
                other => prop_assert!(false, "Expected Validation error, got: {:?}", other),
            }

            Ok(())
        })?;
    }
}


// ── Property 18: Customer mobile uniqueness ────────────────────────────────
//
// For any two customer creation requests with the same mobile number, the
// second request SHALL return AppError::Duplicate.
//
// **Validates: Requirements 7.3**

proptest! {
    #![proptest_config(ProptestConfig::with_cases(50))]

    #[test]
    fn prop18_duplicate_mobile_rejected(
        name1 in generators::arb_name(),
        city1 in generators::arb_name(),
        name2 in generators::arb_name(),
        city2 in generators::arb_name(),
        mobile in generators::arb_mobile(),
    ) {
        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let (_pool, svc) = setup().await;

            // First creation should succeed
            let first = svc.create_customer(&name1, &city1, &mobile).await;
            prop_assert!(first.is_ok(), "First customer creation should succeed, got: {:?}", first.unwrap_err());

            // Second creation with same mobile should fail
            let second = svc.create_customer(&name2, &city2, &mobile).await;
            prop_assert!(second.is_err(), "Second customer with same mobile should be rejected");
            match second.unwrap_err() {
                AppError::Duplicate { field, value } => {
                    prop_assert_eq!(field, "mobile");
                    prop_assert_eq!(value, mobile);
                }
                other => prop_assert!(false, "Expected Duplicate error, got: {:?}", other),
            }

            Ok(())
        })?;
    }
}

// ── Property 19: Customer search correctness ───────────────────────────────
//
// For any customer in the system and for any search query that matches the
// customer's name, city, or mobile number, the customer SHALL appear in the
// search results.
//
// **Validates: Requirements 7.4**

proptest! {
    #![proptest_config(ProptestConfig::with_cases(50))]

    #[test]
    fn prop19_search_by_name_returns_customer(
        name in generators::arb_name(),
        city in generators::arb_name(),
        mobile in generators::arb_mobile(),
    ) {
        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let (_pool, svc) = setup().await;

            let customer = svc.create_customer(&name, &city, &mobile)
                .await
                .expect("Customer creation failed");

            // Search by full name
            let results = svc.search_customers(&name).await.expect("Search failed");
            prop_assert!(
                results.iter().any(|c| c.id == customer.id),
                "Customer should appear in search results for name '{}', got {} results",
                name, results.len()
            );

            Ok(())
        })?;
    }

    #[test]
    fn prop19_search_by_city_returns_customer(
        name in generators::arb_name(),
        city in generators::arb_name(),
        mobile in generators::arb_mobile(),
    ) {
        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let (_pool, svc) = setup().await;

            let customer = svc.create_customer(&name, &city, &mobile)
                .await
                .expect("Customer creation failed");

            // Search by full city
            let results = svc.search_customers(&city).await.expect("Search failed");
            prop_assert!(
                results.iter().any(|c| c.id == customer.id),
                "Customer should appear in search results for city '{}', got {} results",
                city, results.len()
            );

            Ok(())
        })?;
    }

    #[test]
    fn prop19_search_by_mobile_returns_customer(
        name in generators::arb_name(),
        city in generators::arb_name(),
        mobile in generators::arb_mobile(),
    ) {
        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let (_pool, svc) = setup().await;

            let customer = svc.create_customer(&name, &city, &mobile)
                .await
                .expect("Customer creation failed");

            // Search by full mobile
            let results = svc.search_customers(&mobile).await.expect("Search failed");
            prop_assert!(
                results.iter().any(|c| c.id == customer.id),
                "Customer should appear in search results for mobile '{}', got {} results",
                mobile, results.len()
            );

            Ok(())
        })?;
    }
}
