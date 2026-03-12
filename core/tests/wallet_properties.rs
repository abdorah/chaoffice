// Property tests for Wallet Management (Properties 23, 24)
//
// **Validates: Requirements 9.2, 9.3, 11.3**

mod generators;

use proptest::prelude::*;
use tokio::runtime::Runtime;

use sweet_lab_core::error::AppError;
use sweet_lab_core::persistence::db;
use sweet_lab_core::persistence::queries::wallets as queries;
use sweet_lab_core::wallet::service::WalletServiceImpl;

// ── Helpers ────────────────────────────────────────────────────────────────

async fn setup() -> (sqlx::SqlitePool, WalletServiceImpl) {
    let pool = db::init_db(":memory:").await.expect("DB init failed");
    let svc = WalletServiceImpl::new(pool.clone());
    (pool, svc)
}

async fn seed_wallet(pool: &sqlx::SqlitePool, id: &str, name: &str, wallet_type: &str, balance: f64) {
    let now = chrono::Utc::now().to_rfc3339();
    queries::insert_wallet(pool, id, name, wallet_type, balance, &now)
        .await
        .expect("seed wallet failed");
}

// ── Property 23: Wallet transfer conservation ──────────────────────────────
//
// For any fund transfer of amount A between source wallet (balance S) and
// destination wallet (balance D) where A ≤ S, after the transfer:
// source balance SHALL equal S - A, destination balance SHALL equal D + A.
// The total balance across both wallets SHALL be conserved.
//
// **Validates: Requirements 9.2**

proptest! {
    #![proptest_config(ProptestConfig::with_cases(50))]

    #[test]
    fn prop23_transfer_conservation(
        source_balance in (1u64..100_000u64).prop_map(|v| v as f64 / 100.0),
        dest_balance in (0u64..100_000u64).prop_map(|v| v as f64 / 100.0),
        // transfer_pct picks a fraction of source_balance so amount <= source_balance
        transfer_pct in (1u64..=100u64),
    ) {
        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let (pool, svc) = setup().await;

            let src_id = uuid::Uuid::new_v4();
            let dst_id = uuid::Uuid::new_v4();
            seed_wallet(&pool, &src_id.to_string(), "Source", "Bank", source_balance).await;
            seed_wallet(&pool, &dst_id.to_string(), "Dest", "Cash", dest_balance).await;

            // Amount is a percentage of source_balance, ensuring amount <= source_balance
            let amount = (source_balance * transfer_pct as f64 / 100.0 * 100.0).floor() / 100.0;
            // Clamp to source_balance to avoid floating-point edge cases
            let amount = if amount > source_balance { source_balance } else { amount };
            // Skip trivially zero amounts
            if amount <= 0.0 {
                return Ok(());
            }

            let total_before = source_balance + dest_balance;

            let result = svc.transfer_funds(src_id, dst_id, amount).await;
            prop_assert!(result.is_ok(), "Transfer should succeed for amount {} with source balance {}, got: {:?}", amount, source_balance, result.unwrap_err());

            let wallets = svc.get_wallets().await.unwrap();
            let src = wallets.iter().find(|w| w.id == src_id).unwrap();
            let dst = wallets.iter().find(|w| w.id == dst_id).unwrap();

            // Source decreases by amount
            let expected_src = source_balance - amount;
            prop_assert!(
                (src.current_balance - expected_src).abs() < 1e-9,
                "Source balance should be {} but was {}",
                expected_src, src.current_balance
            );

            // Destination increases by amount
            let expected_dst = dest_balance + amount;
            prop_assert!(
                (dst.current_balance - expected_dst).abs() < 1e-9,
                "Dest balance should be {} but was {}",
                expected_dst, dst.current_balance
            );

            // Total conserved
            let total_after = src.current_balance + dst.current_balance;
            prop_assert!(
                (total_before - total_after).abs() < 1e-9,
                "Total should be conserved: before={}, after={}",
                total_before, total_after
            );

            Ok(())
        })?;
    }
}

// ── Property 24: Wallet insufficient funds rejection ───────────────────────
//
// For any wallet with balance B and for any debit operation with amount A
// where A > B, the operation SHALL return AppError::InsufficientFunds and
// the wallet balance SHALL remain B.
//
// **Validates: Requirements 9.3, 11.3**

proptest! {
    #![proptest_config(ProptestConfig::with_cases(50))]

    #[test]
    fn prop24_debit_insufficient_funds_rejected(
        balance in (0u64..50_000u64).prop_map(|v| v as f64 / 100.0),
        excess in (1u64..50_000u64).prop_map(|v| v as f64 / 100.0),
    ) {
        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let (pool, svc) = setup().await;

            let wallet_id = uuid::Uuid::new_v4();
            seed_wallet(&pool, &wallet_id.to_string(), "Test Wallet", "Cash", balance).await;

            // Amount exceeds balance
            let amount = balance + excess;

            let result = svc.debit_wallet(wallet_id, amount, "Over-debit", None).await;
            prop_assert!(result.is_err(), "Debit of {} should fail with balance {}", amount, balance);

            match result.unwrap_err() {
                AppError::InsufficientFunds { wallet_name, available, requested } => {
                    prop_assert_eq!(wallet_name, "Test Wallet");
                    prop_assert!((available - balance).abs() < 1e-9, "Available should be {}, got {}", balance, available);
                    prop_assert!((requested - amount).abs() < 1e-9, "Requested should be {}, got {}", amount, requested);
                }
                other => prop_assert!(false, "Expected InsufficientFunds, got: {:?}", other),
            }

            // Balance unchanged
            let wallets = svc.get_wallets().await.unwrap();
            let wallet = wallets.iter().find(|w| w.id == wallet_id).unwrap();
            prop_assert!(
                (wallet.current_balance - balance).abs() < 1e-9,
                "Balance should remain {} but was {}",
                balance, wallet.current_balance
            );

            Ok(())
        })?;
    }

    #[test]
    fn prop24_transfer_insufficient_funds_rejected(
        source_balance in (0u64..50_000u64).prop_map(|v| v as f64 / 100.0),
        excess in (1u64..50_000u64).prop_map(|v| v as f64 / 100.0),
        dest_balance in (0u64..50_000u64).prop_map(|v| v as f64 / 100.0),
    ) {
        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let (pool, svc) = setup().await;

            let src_id = uuid::Uuid::new_v4();
            let dst_id = uuid::Uuid::new_v4();
            seed_wallet(&pool, &src_id.to_string(), "Source", "Bank", source_balance).await;
            seed_wallet(&pool, &dst_id.to_string(), "Dest", "Cash", dest_balance).await;

            // Amount exceeds source balance
            let amount = source_balance + excess;

            let result = svc.transfer_funds(src_id, dst_id, amount).await;
            prop_assert!(result.is_err(), "Transfer of {} should fail with source balance {}", amount, source_balance);

            match result.unwrap_err() {
                AppError::InsufficientFunds { wallet_name, available, requested } => {
                    prop_assert_eq!(wallet_name, "Source");
                    prop_assert!((available - source_balance).abs() < 1e-9, "Available should be {}, got {}", source_balance, available);
                    prop_assert!((requested - amount).abs() < 1e-9, "Requested should be {}, got {}", amount, requested);
                }
                other => prop_assert!(false, "Expected InsufficientFunds, got: {:?}", other),
            }

            // Both balances unchanged
            let wallets = svc.get_wallets().await.unwrap();
            let src = wallets.iter().find(|w| w.id == src_id).unwrap();
            let dst = wallets.iter().find(|w| w.id == dst_id).unwrap();
            prop_assert!(
                (src.current_balance - source_balance).abs() < 1e-9,
                "Source balance should remain {} but was {}",
                source_balance, src.current_balance
            );
            prop_assert!(
                (dst.current_balance - dest_balance).abs() < 1e-9,
                "Dest balance should remain {} but was {}",
                dest_balance, dst.current_balance
            );

            Ok(())
        })?;
    }
}
