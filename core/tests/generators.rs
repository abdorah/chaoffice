// Shared proptest strategies for all Sweet Lab ERP domain objects.
// These generators produce valid, constrained instances for property-based testing.

use chrono::{DateTime, TimeZone, Utc};
use proptest::prelude::*;
use uuid::Uuid;

use sweet_lab_core::models::domain::*;
use sweet_lab_core::models::Money;

// ── Helper strategies ──────────────────────────────────────────────────────

/// Generate a valid UUID (v4).
pub fn arb_uuid() -> impl Strategy<Value = Uuid> {
    any::<[u8; 16]>().prop_map(|bytes| {
        uuid::Builder::from_random_bytes(bytes).into_uuid()
    })
}

/// Generate a DateTime<Utc> within a reasonable range (2020–2030).
pub fn arb_datetime() -> impl Strategy<Value = DateTime<Utc>> {
    // Timestamp range: 2020-01-01 to 2030-01-01
    (1577836800i64..1893456000i64).prop_map(|secs| {
        Utc.timestamp_opt(secs, 0).unwrap()
    })
}

/// Generate a non-empty alphanumeric string (1–50 chars).
pub fn arb_name() -> impl Strategy<Value = String> {
    "[a-zA-Z0-9 ]{1,50}".prop_map(|s| s.trim().to_string())
        .prop_filter("name must not be empty", |s| !s.is_empty())
}

/// Generate a non-negative f64 (0.0 to 100_000.0), rounded to 2 decimal places
/// to avoid floating-point serialization edge cases.
pub fn arb_positive_f64() -> impl Strategy<Value = f64> {
    (0u64..10_000_000u64).prop_map(|v| (v as f64) / 100.0)
}

/// Generate a non-negative Money value (0 to 100_000.00).
pub fn arb_money() -> impl Strategy<Value = Money> {
    (0i64..10_000_000i64).prop_map(Money)
}

/// Generate a phone-like string.
pub fn arb_mobile() -> impl Strategy<Value = String> {
    "[0-9]{10,15}"
}

/// Generate a short description string.
pub fn arb_description() -> impl Strategy<Value = String> {
    "[a-zA-Z0-9 ]{1,100}".prop_map(|s| s.trim().to_string())
        .prop_filter("description must not be empty", |s| !s.is_empty())
}

// ── Enum strategies ────────────────────────────────────────────────────────

pub fn arb_user_role() -> impl Strategy<Value = UserRole> {
    prop_oneof![
        Just(UserRole::Admin),
        Just(UserRole::Chef),
        Just(UserRole::Representative),
    ]
}

pub fn arb_wallet_type() -> impl Strategy<Value = WalletType> {
    prop_oneof![
        Just(WalletType::Bank),
        Just(WalletType::Cash),
        Just(WalletType::Representative),
    ]
}

pub fn arb_expense_category() -> impl Strategy<Value = ExpenseCategory> {
    prop_oneof![
        Just(ExpenseCategory::Purchase),
        Just(ExpenseCategory::OperatingCost),
    ]
}

pub fn arb_sync_status() -> impl Strategy<Value = SyncStatus> {
    prop_oneof![
        Just(SyncStatus::Synced),
        Just(SyncStatus::Pending),
        Just(SyncStatus::Conflict),
    ]
}

// ── Domain object strategies ───────────────────────────────────────────────

pub fn arb_app_user() -> impl Strategy<Value = AppUser> {
    (arb_uuid(), arb_name(), arb_name(), arb_user_role(), arb_name())
        .prop_map(|(id, username, full_name, role, password_hash)| AppUser {
            id,
            username,
            full_name,
            role,
            password_hash,
        })
}

pub fn arb_raw_material() -> impl Strategy<Value = RawMaterial> {
    (arb_uuid(), arb_name(), arb_name(), arb_positive_f64(), arb_datetime())
        .prop_map(|(id, name, unit, current_quantity, last_updated)| RawMaterial {
            id,
            name,
            unit,
            current_quantity,
            last_updated,
        })
}

pub fn arb_finished_good() -> impl Strategy<Value = FinishedGood> {
    (arb_uuid(), arb_name(), arb_positive_f64(), arb_money(), arb_datetime())
        .prop_map(|(id, name, current_quantity, unit_price, last_updated)| FinishedGood {
            id,
            name,
            current_quantity,
            unit_price,
            last_updated,
        })
}

pub fn arb_recipe_ingredient() -> impl Strategy<Value = RecipeIngredient> {
    (arb_uuid(), arb_name(), arb_positive_f64())
        .prop_filter("required_quantity must be > 0", |(_, _, qty)| *qty > 0.0)
        .prop_map(|(raw_material_id, raw_material_name, required_quantity)| RecipeIngredient {
            raw_material_id,
            raw_material_name,
            required_quantity,
        })
}

pub fn arb_recipe() -> impl Strategy<Value = Recipe> {
    (
        arb_uuid(),
        arb_name(),
        arb_uuid(),
        arb_name(),
        prop::collection::vec(arb_recipe_ingredient(), 1..=10),
    )
        .prop_map(|(id, name, finished_good_id, finished_good_name, ingredients)| Recipe {
            id,
            name,
            finished_good_id,
            finished_good_name,
            ingredients,
        })
}

pub fn arb_production_log() -> impl Strategy<Value = ProductionLog> {
    (
        arb_uuid(),
        arb_uuid(),
        arb_name(),
        arb_uuid(),
        arb_name(),
        1..100i32,
        prop::collection::hash_map(arb_uuid(), arb_positive_f64(), 1..5),
        arb_uuid(),
        arb_datetime(),
    )
        .prop_map(
            |(id, recipe_id, recipe_name, chef_id, chef_name, production_quantity, materials_consumed, finished_good_id, timestamp)| {
                ProductionLog {
                    id,
                    recipe_id,
                    recipe_name,
                    chef_id,
                    chef_name,
                    production_quantity,
                    materials_consumed,
                    finished_good_id,
                    timestamp,
                }
            },
        )
}

pub fn arb_customer() -> impl Strategy<Value = Customer> {
    (
        arb_uuid(),
        arb_name(),
        arb_name(),
        arb_mobile(),
        0..=5i32,
        arb_money(),
        0..365i32,
    )
        .prop_map(|(id, name, city, mobile, reliability_rating, total_debt, overdue_days)| {
            Customer {
                id,
                name,
                city,
                mobile,
                reliability_rating,
                total_debt,
                overdue_days,
            }
        })
}

pub fn arb_sale_line_item() -> impl Strategy<Value = SaleLineItem> {
    (arb_uuid(), arb_name(), 1..100i32, arb_money())
        .prop_map(|(finished_good_id, finished_good_name, quantity, unit_price)| SaleLineItem {
            finished_good_id,
            finished_good_name,
            quantity,
            unit_price,
        })
}

pub fn arb_sale() -> impl Strategy<Value = Sale> {
    (
        arb_uuid(),
        arb_uuid(),
        arb_name(),
        prop::collection::vec(arb_sale_line_item(), 1..=5),
        arb_money(),
        arb_money(),
        arb_uuid(),
        arb_datetime(),
    )
        .prop_map(
            |(id, customer_id, customer_name, line_items, total_amount, amount_paid, payment_wallet_id, timestamp)| {
                Sale {
                    id,
                    customer_id,
                    customer_name,
                    line_items,
                    total_amount,
                    amount_paid,
                    payment_wallet_id,
                    timestamp,
                }
            },
        )
}

pub fn arb_receipt() -> impl Strategy<Value = Receipt> {
    (
        arb_sale(),
        arb_name(),
        arb_name(),
        arb_mobile(),
        arb_name(),
        arb_money(),
        arb_name(),
    )
        .prop_map(
            |(sale, customer_name, customer_city, customer_mobile, business_name, remaining_balance, formatted_date)| {
                Receipt {
                    sale,
                    customer_name,
                    customer_city,
                    customer_mobile,
                    business_name,
                    remaining_balance,
                    formatted_date,
                }
            },
        )
}

pub fn arb_wallet() -> impl Strategy<Value = Wallet> {
    (arb_uuid(), arb_name(), arb_wallet_type(), arb_money())
        .prop_map(|(id, name, wallet_type, current_balance)| Wallet {
            id,
            name,
            wallet_type,
            current_balance,
        })
}

pub fn arb_wallet_transaction() -> impl Strategy<Value = WalletTransaction> {
    (
        arb_uuid(),
        arb_uuid(),
        arb_money(),
        arb_description(),
        prop::option::of(arb_uuid()),
        arb_datetime(),
    )
        .prop_map(|(id, wallet_id, amount, description, related_entity_id, timestamp)| {
            WalletTransaction {
                id,
                wallet_id,
                amount,
                description,
                related_entity_id,
                timestamp,
            }
        })
}

pub fn arb_fund_transfer() -> impl Strategy<Value = FundTransfer> {
    (arb_uuid(), arb_uuid(), arb_uuid(), arb_money(), arb_datetime())
        .prop_map(|(id, source_wallet_id, destination_wallet_id, amount, timestamp)| {
            FundTransfer {
                id,
                source_wallet_id,
                destination_wallet_id,
                amount,
                timestamp,
            }
        })
}

pub fn arb_debt_record() -> impl Strategy<Value = DebtRecord> {
    (
        arb_uuid(),
        arb_uuid(),
        arb_name(),
        arb_uuid(),
        arb_money(),
        arb_money(),
        arb_datetime(),
        0..365i32,
        any::<bool>(),
        any::<bool>(),
    )
        .prop_map(
            |(id, customer_id, customer_name, sale_id, original_amount, remaining_amount, sale_date, overdue_days, is_critical, is_settled)| {
                DebtRecord {
                    id,
                    customer_id,
                    customer_name,
                    sale_id,
                    original_amount,
                    remaining_amount,
                    sale_date,
                    overdue_days,
                    is_critical,
                    is_settled,
                }
            },
        )
}

pub fn arb_debt_allocation() -> impl Strategy<Value = DebtAllocation> {
    (arb_uuid(), arb_money())
        .prop_map(|(debt_record_id, amount_applied)| DebtAllocation {
            debt_record_id,
            amount_applied,
        })
}

pub fn arb_debt_payment() -> impl Strategy<Value = DebtPayment> {
    (
        arb_uuid(),
        arb_uuid(),
        arb_money(),
        arb_money(),
        arb_uuid(),
        prop::collection::vec(arb_debt_allocation(), 1..=5),
        arb_datetime(),
    )
        .prop_map(|(id, customer_id, amount, unallocated, wallet_id, allocations, timestamp)| DebtPayment {
            id,
            customer_id,
            amount,
            unallocated,
            wallet_id,
            allocations,
            timestamp,
        })
}

pub fn arb_expense() -> impl Strategy<Value = Expense> {
    (
        arb_uuid(),
        arb_description(),
        arb_money(),
        arb_expense_category(),
        arb_uuid(),
        arb_name(),
        arb_uuid(),
        arb_datetime(),
    )
        .prop_map(
            |(id, description, amount, category, wallet_id, wallet_name, recorded_by, timestamp)| {
                Expense {
                    id,
                    description,
                    amount,
                    category,
                    wallet_id,
                    wallet_name,
                    recorded_by,
                    timestamp,
                }
            },
        )
}

pub fn arb_financial_summary() -> impl Strategy<Value = FinancialSummary> {
    (
        arb_money(),
        arb_money(),
        arb_money(),
        prop::collection::vec(arb_wallet(), 0..=3),
        arb_datetime(),
        arb_datetime(),
    )
        .prop_map(
            |(total_revenue, total_expenses, net_profit, wallet_balances, start_date, end_date)| {
                FinancialSummary {
                    total_revenue,
                    total_expenses,
                    net_profit,
                    wallet_balances,
                    start_date,
                    end_date,
                }
            },
        )
}

pub fn arb_inventory_report() -> impl Strategy<Value = InventoryReport> {
    (
        prop::collection::vec(
            (arb_raw_material(), any::<bool>()).prop_map(|(material, is_low_stock)| {
                RawMaterialReport { material, is_low_stock }
            }),
            0..=5,
        ),
        prop::collection::vec(
            (arb_finished_good(), any::<bool>()).prop_map(|(good, is_low_stock)| {
                FinishedGoodReport { good, is_low_stock }
            }),
            0..=5,
        ),
    )
        .prop_map(|(raw_materials, finished_goods)| InventoryReport {
            raw_materials,
            finished_goods,
        })
}

pub fn arb_invoice() -> impl Strategy<Value = Invoice> {
    (
        arb_name(),
        arb_name(),
        arb_name(),
        arb_mobile(),
        prop::collection::vec(arb_sale_line_item(), 1..=5),
        arb_money(),
        arb_money(),
        arb_money(),
        arb_name(),
        arb_name(),
    )
        .prop_map(
            |(business_name, customer_name, customer_city, customer_mobile, line_items, total_amount, amount_paid, remaining_balance, date, invoice_number)| {
                Invoice {
                    business_name,
                    customer_name,
                    customer_city,
                    customer_mobile,
                    line_items,
                    total_amount,
                    amount_paid,
                    remaining_balance,
                    date,
                    invoice_number,
                }
            },
        )
}

pub fn arb_sync_queue_item() -> impl Strategy<Value = SyncQueueItem> {
    (
        arb_uuid(),
        arb_name(),
        arb_uuid(),
        prop_oneof![Just("CREATE".to_string()), Just("UPDATE".to_string()), Just("DELETE".to_string())],
        arb_description(),
        arb_datetime(),
        arb_sync_status(),
    )
        .prop_map(|(id, entity_type, entity_id, operation, payload, created_at, status)| {
            SyncQueueItem {
                id,
                entity_type,
                entity_id,
                operation,
                payload,
                created_at,
                status,
            }
        })
}

pub fn arb_conflict_log() -> impl Strategy<Value = ConflictLog> {
    (
        arb_uuid(),
        arb_name(),
        arb_uuid(),
        arb_description(),
        arb_description(),
        prop_oneof![Just("LOCAL".to_string()), Just("REMOTE".to_string())],
        arb_datetime(),
    )
        .prop_map(|(id, entity_type, entity_id, local_version, remote_version, resolved_with, timestamp)| {
            ConflictLog {
                id,
                entity_type,
                entity_id,
                local_version,
                remote_version,
                resolved_with,
                timestamp,
            }
        })
}
