// Property 38: JSON serialization round-trip
//
// For any valid application object, serializing to JSON via serde_json then
// deserializing SHALL produce an object equivalent to the original.
//
// **Validates: Requirements 14.1, 14.2, 14.3**

mod generators;

use proptest::prelude::*;

/// Helper: serialize to JSON Value, deserialize back, re-serialize to Value,
/// and compare the two Values. Using serde_json::Value for comparison handles
/// HashMap key ordering non-determinism (JSON objects are unordered maps).
fn assert_json_round_trip<T>(original: &T)
where
    T: serde::Serialize + serde::de::DeserializeOwned + std::fmt::Debug,
{
    let json_value = serde_json::to_value(original)
        .expect("serialization to Value should succeed");
    let json_str = serde_json::to_string(original)
        .expect("serialization to String should succeed");
    let deserialized: T = serde_json::from_str(&json_str)
        .expect("deserialization should succeed");
    let round_trip_value = serde_json::to_value(&deserialized)
        .expect("re-serialization to Value should succeed");
    assert_eq!(
        json_value, round_trip_value,
        "JSON round-trip produced different Value:\n  original: {json_value}\n  round-trip: {round_trip_value}"
    );
}

proptest! {
    #![proptest_config(ProptestConfig::with_cases(100))]

    // ── Enums ──────────────────────────────────────────────────────────

    #[test]
    fn user_role_round_trip(role in generators::arb_user_role()) {
        assert_json_round_trip(&role);
    }

    #[test]
    fn wallet_type_round_trip(wt in generators::arb_wallet_type()) {
        assert_json_round_trip(&wt);
    }

    #[test]
    fn expense_category_round_trip(ec in generators::arb_expense_category()) {
        assert_json_round_trip(&ec);
    }

    #[test]
    fn sync_status_round_trip(ss in generators::arb_sync_status()) {
        assert_json_round_trip(&ss);
    }

    // ── Core domain objects ────────────────────────────────────────────

    #[test]
    fn app_user_round_trip(user in generators::arb_app_user()) {
        assert_json_round_trip(&user);
    }

    #[test]
    fn raw_material_round_trip(rm in generators::arb_raw_material()) {
        assert_json_round_trip(&rm);
    }

    #[test]
    fn finished_good_round_trip(fg in generators::arb_finished_good()) {
        assert_json_round_trip(&fg);
    }

    #[test]
    fn recipe_ingredient_round_trip(ri in generators::arb_recipe_ingredient()) {
        assert_json_round_trip(&ri);
    }

    #[test]
    fn recipe_round_trip(r in generators::arb_recipe()) {
        assert_json_round_trip(&r);
    }

    #[test]
    fn production_log_round_trip(pl in generators::arb_production_log()) {
        assert_json_round_trip(&pl);
    }

    #[test]
    fn customer_round_trip(c in generators::arb_customer()) {
        assert_json_round_trip(&c);
    }

    #[test]
    fn sale_line_item_round_trip(sli in generators::arb_sale_line_item()) {
        assert_json_round_trip(&sli);
    }

    #[test]
    fn sale_round_trip(s in generators::arb_sale()) {
        assert_json_round_trip(&s);
    }

    #[test]
    fn receipt_round_trip(r in generators::arb_receipt()) {
        assert_json_round_trip(&r);
    }

    #[test]
    fn wallet_round_trip(w in generators::arb_wallet()) {
        assert_json_round_trip(&w);
    }

    #[test]
    fn wallet_transaction_round_trip(wt in generators::arb_wallet_transaction()) {
        assert_json_round_trip(&wt);
    }

    #[test]
    fn fund_transfer_round_trip(ft in generators::arb_fund_transfer()) {
        assert_json_round_trip(&ft);
    }

    #[test]
    fn debt_record_round_trip(dr in generators::arb_debt_record()) {
        assert_json_round_trip(&dr);
    }

    #[test]
    fn debt_allocation_round_trip(da in generators::arb_debt_allocation()) {
        assert_json_round_trip(&da);
    }

    #[test]
    fn debt_payment_round_trip(dp in generators::arb_debt_payment()) {
        assert_json_round_trip(&dp);
    }

    #[test]
    fn expense_round_trip(e in generators::arb_expense()) {
        assert_json_round_trip(&e);
    }

    #[test]
    fn financial_summary_round_trip(fs in generators::arb_financial_summary()) {
        assert_json_round_trip(&fs);
    }

    #[test]
    fn inventory_report_round_trip(ir in generators::arb_inventory_report()) {
        assert_json_round_trip(&ir);
    }

    #[test]
    fn invoice_round_trip(inv in generators::arb_invoice()) {
        assert_json_round_trip(&inv);
    }

    #[test]
    fn sync_queue_item_round_trip(sqi in generators::arb_sync_queue_item()) {
        assert_json_round_trip(&sqi);
    }

    #[test]
    fn conflict_log_round_trip(cl in generators::arb_conflict_log()) {
        assert_json_round_trip(&cl);
    }
}
