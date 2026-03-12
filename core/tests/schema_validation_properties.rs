// Property 39: Schema validation rejects malformed data
//
// For any JSON string that does not conform to the expected schema
// (missing required fields, wrong types, out-of-range values),
// deserialization SHALL fail.
//
// **Validates: Requirements 14.4**

mod generators;

use proptest::prelude::*;
use serde::de::DeserializeOwned;
use serde::Serialize;
use serde_json::Value;

use sweet_lab_core::models::domain::*;

// ── Malformation helpers ───────────────────────────────────────────────────

/// Remove a required field from a JSON object by key name.
fn remove_field(json: &Value, field: &str) -> String {
    let mut obj = json.as_object().expect("expected JSON object").clone();
    obj.remove(field);
    serde_json::to_string(&Value::Object(obj)).unwrap()
}

/// Replace a field's value with a wrong type (string → number, number → string, etc.).
fn wrong_type_field(json: &Value, field: &str) -> String {
    let mut obj = json.as_object().expect("expected JSON object").clone();
    if let Some(val) = obj.get(field) {
        let replacement = match val {
            Value::String(_) => Value::Number(serde_json::Number::from(99999)),
            Value::Number(_) => Value::String("not_a_number".into()),
            Value::Bool(_) => Value::String("not_a_bool".into()),
            Value::Array(_) => Value::String("not_an_array".into()),
            Value::Object(_) => Value::String("not_an_object".into()),
            Value::Null => Value::String("not_null".into()),
        };
        obj.insert(field.to_string(), replacement);
    }
    serde_json::to_string(&Value::Object(obj)).unwrap()
}

/// Assert that deserializing the given JSON string into type T fails.
fn assert_deser_fails<T: DeserializeOwned + std::fmt::Debug>(json_str: &str) {
    let result = serde_json::from_str::<T>(json_str);
    assert!(
        result.is_err(),
        "Expected deserialization to fail for type {}, but got: {:?}\nJSON: {}",
        std::any::type_name::<T>(),
        result.unwrap(),
        json_str
    );
}

/// Serialize a valid object to a JSON Value.
fn to_json_value<T: Serialize>(obj: &T) -> Value {
    serde_json::to_value(obj).expect("serialization should succeed")
}


// ── Property tests: Missing required fields ────────────────────────────────

proptest! {
    #![proptest_config(ProptestConfig::with_cases(50))]

    // ── AppUser: missing fields ────────────────────────────────────────

    #[test]
    fn app_user_missing_id(user in generators::arb_app_user()) {
        let json = to_json_value(&user);
        assert_deser_fails::<AppUser>(&remove_field(&json, "id"));
    }

    #[test]
    fn app_user_missing_username(user in generators::arb_app_user()) {
        let json = to_json_value(&user);
        assert_deser_fails::<AppUser>(&remove_field(&json, "username"));
    }

    #[test]
    fn app_user_missing_role(user in generators::arb_app_user()) {
        let json = to_json_value(&user);
        assert_deser_fails::<AppUser>(&remove_field(&json, "role"));
    }

    // ── RawMaterial: missing fields ────────────────────────────────────

    #[test]
    fn raw_material_missing_name(rm in generators::arb_raw_material()) {
        let json = to_json_value(&rm);
        assert_deser_fails::<RawMaterial>(&remove_field(&json, "name"));
    }

    #[test]
    fn raw_material_missing_current_quantity(rm in generators::arb_raw_material()) {
        let json = to_json_value(&rm);
        assert_deser_fails::<RawMaterial>(&remove_field(&json, "current_quantity"));
    }

    // ── FinishedGood: missing fields ───────────────────────────────────

    #[test]
    fn finished_good_missing_name(fg in generators::arb_finished_good()) {
        let json = to_json_value(&fg);
        assert_deser_fails::<FinishedGood>(&remove_field(&json, "name"));
    }

    #[test]
    fn finished_good_missing_unit_price(fg in generators::arb_finished_good()) {
        let json = to_json_value(&fg);
        assert_deser_fails::<FinishedGood>(&remove_field(&json, "unit_price"));
    }

    // ── Recipe: missing fields ─────────────────────────────────────────

    #[test]
    fn recipe_missing_ingredients(r in generators::arb_recipe()) {
        let json = to_json_value(&r);
        assert_deser_fails::<Recipe>(&remove_field(&json, "ingredients"));
    }

    #[test]
    fn recipe_missing_name(r in generators::arb_recipe()) {
        let json = to_json_value(&r);
        assert_deser_fails::<Recipe>(&remove_field(&json, "name"));
    }

    // ── Customer: missing fields ───────────────────────────────────────

    #[test]
    fn customer_missing_name(c in generators::arb_customer()) {
        let json = to_json_value(&c);
        assert_deser_fails::<Customer>(&remove_field(&json, "name"));
    }

    #[test]
    fn customer_missing_mobile(c in generators::arb_customer()) {
        let json = to_json_value(&c);
        assert_deser_fails::<Customer>(&remove_field(&json, "mobile"));
    }

    // ── Sale: missing fields ───────────────────────────────────────────

    #[test]
    fn sale_missing_line_items(s in generators::arb_sale()) {
        let json = to_json_value(&s);
        assert_deser_fails::<Sale>(&remove_field(&json, "line_items"));
    }

    #[test]
    fn sale_missing_total_amount(s in generators::arb_sale()) {
        let json = to_json_value(&s);
        assert_deser_fails::<Sale>(&remove_field(&json, "total_amount"));
    }

    // ── Wallet: missing fields ─────────────────────────────────────────

    #[test]
    fn wallet_missing_wallet_type(w in generators::arb_wallet()) {
        let json = to_json_value(&w);
        assert_deser_fails::<Wallet>(&remove_field(&json, "wallet_type"));
    }

    #[test]
    fn wallet_missing_current_balance(w in generators::arb_wallet()) {
        let json = to_json_value(&w);
        assert_deser_fails::<Wallet>(&remove_field(&json, "current_balance"));
    }

    // ── DebtRecord: missing fields ─────────────────────────────────────

    #[test]
    fn debt_record_missing_sale_date(dr in generators::arb_debt_record()) {
        let json = to_json_value(&dr);
        assert_deser_fails::<DebtRecord>(&remove_field(&json, "sale_date"));
    }

    #[test]
    fn debt_record_missing_remaining_amount(dr in generators::arb_debt_record()) {
        let json = to_json_value(&dr);
        assert_deser_fails::<DebtRecord>(&remove_field(&json, "remaining_amount"));
    }

    // ── Expense: missing fields ────────────────────────────────────────

    #[test]
    fn expense_missing_category(e in generators::arb_expense()) {
        let json = to_json_value(&e);
        assert_deser_fails::<Expense>(&remove_field(&json, "category"));
    }

    #[test]
    fn expense_missing_amount(e in generators::arb_expense()) {
        let json = to_json_value(&e);
        assert_deser_fails::<Expense>(&remove_field(&json, "amount"));
    }

    // ── ProductionLog: missing fields ──────────────────────────────────

    #[test]
    fn production_log_missing_recipe_id(pl in generators::arb_production_log()) {
        let json = to_json_value(&pl);
        assert_deser_fails::<ProductionLog>(&remove_field(&json, "recipe_id"));
    }

    #[test]
    fn production_log_missing_timestamp(pl in generators::arb_production_log()) {
        let json = to_json_value(&pl);
        assert_deser_fails::<ProductionLog>(&remove_field(&json, "timestamp"));
    }
}


// ── Property tests: Wrong field types ──────────────────────────────────────

proptest! {
    #![proptest_config(ProptestConfig::with_cases(50))]

    // ── AppUser: wrong types ───────────────────────────────────────────

    #[test]
    fn app_user_wrong_type_id(user in generators::arb_app_user()) {
        let json = to_json_value(&user);
        assert_deser_fails::<AppUser>(&wrong_type_field(&json, "id"));
    }

    #[test]
    fn app_user_wrong_type_role(user in generators::arb_app_user()) {
        let json = to_json_value(&user);
        assert_deser_fails::<AppUser>(&wrong_type_field(&json, "role"));
    }

    // ── RawMaterial: wrong types ───────────────────────────────────────

    #[test]
    fn raw_material_wrong_type_current_quantity(rm in generators::arb_raw_material()) {
        let json = to_json_value(&rm);
        assert_deser_fails::<RawMaterial>(&wrong_type_field(&json, "current_quantity"));
    }

    #[test]
    fn raw_material_wrong_type_last_updated(rm in generators::arb_raw_material()) {
        let json = to_json_value(&rm);
        assert_deser_fails::<RawMaterial>(&wrong_type_field(&json, "last_updated"));
    }

    // ── FinishedGood: wrong types ──────────────────────────────────────

    #[test]
    fn finished_good_wrong_type_unit_price(fg in generators::arb_finished_good()) {
        let json = to_json_value(&fg);
        assert_deser_fails::<FinishedGood>(&wrong_type_field(&json, "unit_price"));
    }

    #[test]
    fn finished_good_wrong_type_current_quantity(fg in generators::arb_finished_good()) {
        let json = to_json_value(&fg);
        assert_deser_fails::<FinishedGood>(&wrong_type_field(&json, "current_quantity"));
    }

    // ── Recipe: wrong types ────────────────────────────────────────────

    #[test]
    fn recipe_wrong_type_ingredients(r in generators::arb_recipe()) {
        let json = to_json_value(&r);
        assert_deser_fails::<Recipe>(&wrong_type_field(&json, "ingredients"));
    }

    #[test]
    fn recipe_wrong_type_finished_good_id(r in generators::arb_recipe()) {
        let json = to_json_value(&r);
        assert_deser_fails::<Recipe>(&wrong_type_field(&json, "finished_good_id"));
    }

    // ── Customer: wrong types ──────────────────────────────────────────

    #[test]
    fn customer_wrong_type_reliability_rating(c in generators::arb_customer()) {
        let json = to_json_value(&c);
        assert_deser_fails::<Customer>(&wrong_type_field(&json, "reliability_rating"));
    }

    #[test]
    fn customer_wrong_type_total_debt(c in generators::arb_customer()) {
        let json = to_json_value(&c);
        assert_deser_fails::<Customer>(&wrong_type_field(&json, "total_debt"));
    }

    // ── Sale: wrong types ──────────────────────────────────────────────

    #[test]
    fn sale_wrong_type_line_items(s in generators::arb_sale()) {
        let json = to_json_value(&s);
        assert_deser_fails::<Sale>(&wrong_type_field(&json, "line_items"));
    }

    #[test]
    fn sale_wrong_type_timestamp(s in generators::arb_sale()) {
        let json = to_json_value(&s);
        assert_deser_fails::<Sale>(&wrong_type_field(&json, "timestamp"));
    }

    // ── Wallet: wrong types ────────────────────────────────────────────

    #[test]
    fn wallet_wrong_type_wallet_type(w in generators::arb_wallet()) {
        let json = to_json_value(&w);
        assert_deser_fails::<Wallet>(&wrong_type_field(&json, "wallet_type"));
    }

    #[test]
    fn wallet_wrong_type_current_balance(w in generators::arb_wallet()) {
        let json = to_json_value(&w);
        assert_deser_fails::<Wallet>(&wrong_type_field(&json, "current_balance"));
    }

    // ── DebtRecord: wrong types ────────────────────────────────────────

    #[test]
    fn debt_record_wrong_type_is_critical(dr in generators::arb_debt_record()) {
        let json = to_json_value(&dr);
        assert_deser_fails::<DebtRecord>(&wrong_type_field(&json, "is_critical"));
    }

    #[test]
    fn debt_record_wrong_type_overdue_days(dr in generators::arb_debt_record()) {
        let json = to_json_value(&dr);
        assert_deser_fails::<DebtRecord>(&wrong_type_field(&json, "overdue_days"));
    }

    // ── Expense: wrong types ───────────────────────────────────────────

    #[test]
    fn expense_wrong_type_category(e in generators::arb_expense()) {
        let json = to_json_value(&e);
        assert_deser_fails::<Expense>(&wrong_type_field(&json, "category"));
    }

    #[test]
    fn expense_wrong_type_timestamp(e in generators::arb_expense()) {
        let json = to_json_value(&e);
        assert_deser_fails::<Expense>(&wrong_type_field(&json, "timestamp"));
    }

    // ── ProductionLog: wrong types ─────────────────────────────────────

    #[test]
    fn production_log_wrong_type_materials_consumed(pl in generators::arb_production_log()) {
        let json = to_json_value(&pl);
        assert_deser_fails::<ProductionLog>(&wrong_type_field(&json, "materials_consumed"));
    }

    #[test]
    fn production_log_wrong_type_production_quantity(pl in generators::arb_production_log()) {
        let json = to_json_value(&pl);
        assert_deser_fails::<ProductionLog>(&wrong_type_field(&json, "production_quantity"));
    }
}

// ── Property tests: Garbage / random JSON ──────────────────────────────────

proptest! {
    #![proptest_config(ProptestConfig::with_cases(100))]

    /// Completely random strings should never deserialize into any domain type.
    #[test]
    fn garbage_string_rejects_app_user(s in "[a-zA-Z0-9!@#$%^&*()]{1,200}") {
        assert_deser_fails::<AppUser>(&s);
    }

    #[test]
    fn garbage_string_rejects_raw_material(s in "[a-zA-Z0-9!@#$%^&*()]{1,200}") {
        assert_deser_fails::<RawMaterial>(&s);
    }

    #[test]
    fn garbage_string_rejects_finished_good(s in "[a-zA-Z0-9!@#$%^&*()]{1,200}") {
        assert_deser_fails::<FinishedGood>(&s);
    }

    #[test]
    fn garbage_string_rejects_recipe(s in "[a-zA-Z0-9!@#$%^&*()]{1,200}") {
        assert_deser_fails::<Recipe>(&s);
    }

    #[test]
    fn garbage_string_rejects_customer(s in "[a-zA-Z0-9!@#$%^&*()]{1,200}") {
        assert_deser_fails::<Customer>(&s);
    }

    #[test]
    fn garbage_string_rejects_sale(s in "[a-zA-Z0-9!@#$%^&*()]{1,200}") {
        assert_deser_fails::<Sale>(&s);
    }

    #[test]
    fn garbage_string_rejects_wallet(s in "[a-zA-Z0-9!@#$%^&*()]{1,200}") {
        assert_deser_fails::<Wallet>(&s);
    }

    #[test]
    fn garbage_string_rejects_debt_record(s in "[a-zA-Z0-9!@#$%^&*()]{1,200}") {
        assert_deser_fails::<DebtRecord>(&s);
    }

    #[test]
    fn garbage_string_rejects_expense(s in "[a-zA-Z0-9!@#$%^&*()]{1,200}") {
        assert_deser_fails::<Expense>(&s);
    }

    #[test]
    fn garbage_string_rejects_production_log(s in "[a-zA-Z0-9!@#$%^&*()]{1,200}") {
        assert_deser_fails::<ProductionLog>(&s);
    }

    /// Empty JSON object should fail for all typed structs (missing all required fields).
    #[test]
    fn empty_object_rejects_all_types(_dummy in 0..1i32) {
        let empty = "{}";
        assert_deser_fails::<AppUser>(empty);
        assert_deser_fails::<RawMaterial>(empty);
        assert_deser_fails::<FinishedGood>(empty);
        assert_deser_fails::<Recipe>(empty);
        assert_deser_fails::<Customer>(empty);
        assert_deser_fails::<Sale>(empty);
        assert_deser_fails::<Wallet>(empty);
        assert_deser_fails::<DebtRecord>(empty);
        assert_deser_fails::<Expense>(empty);
        assert_deser_fails::<ProductionLog>(empty);
    }

    /// JSON array should fail for all struct types.
    #[test]
    fn json_array_rejects_all_types(_dummy in 0..1i32) {
        let arr = "[1, 2, 3]";
        assert_deser_fails::<AppUser>(arr);
        assert_deser_fails::<RawMaterial>(arr);
        assert_deser_fails::<FinishedGood>(arr);
        assert_deser_fails::<Recipe>(arr);
        assert_deser_fails::<Customer>(arr);
        assert_deser_fails::<Sale>(arr);
        assert_deser_fails::<Wallet>(arr);
        assert_deser_fails::<DebtRecord>(arr);
        assert_deser_fails::<Expense>(arr);
        assert_deser_fails::<ProductionLog>(arr);
    }

    /// JSON null should fail for all struct types.
    #[test]
    fn json_null_rejects_all_types(_dummy in 0..1i32) {
        let null = "null";
        assert_deser_fails::<AppUser>(null);
        assert_deser_fails::<RawMaterial>(null);
        assert_deser_fails::<FinishedGood>(null);
        assert_deser_fails::<Recipe>(null);
        assert_deser_fails::<Customer>(null);
        assert_deser_fails::<Sale>(null);
        assert_deser_fails::<Wallet>(null);
        assert_deser_fails::<DebtRecord>(null);
        assert_deser_fails::<Expense>(null);
        assert_deser_fails::<ProductionLog>(null);
    }

    /// Invalid enum variant strings should fail deserialization.
    #[test]
    fn invalid_enum_variant_rejects(variant in "[a-zA-Z]{5,20}") {
        // UserRole only accepts Admin, Chef, Representative
        let json = format!("\"{}\"", variant);
        // Only fail if it's not one of the valid variants
        if variant != "Admin" && variant != "Chef" && variant != "Representative" {
            assert_deser_fails::<UserRole>(&json);
        }
        // WalletType only accepts Bank, Cash, Representative
        if variant != "Bank" && variant != "Cash" && variant != "Representative" {
            assert_deser_fails::<WalletType>(&json);
        }
        // ExpenseCategory only accepts Purchase, OperatingCost
        if variant != "Purchase" && variant != "OperatingCost" {
            assert_deser_fails::<ExpenseCategory>(&json);
        }
        // SyncStatus only accepts Synced, Pending, Conflict
        if variant != "Synced" && variant != "Pending" && variant != "Conflict" {
            assert_deser_fails::<SyncStatus>(&json);
        }
    }
}
