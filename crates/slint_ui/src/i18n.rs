use std::collections::HashMap;
use std::path::PathBuf;

use crate::{App, TranslationStrings};
use slint::ComponentHandle;

/// Supported locales with display names and RTL flag.
pub const SUPPORTED_LOCALES: &[(&str, &str, bool)] = &[
    ("en", "English", false),
    ("fr", "Français", false),
    ("ar", "العربية", true),
];

/// Embedded English translations (always available as fallback).
const ENGLISH_JSON: &str = include_str!("../i18n/en.json");

/// Core translation manager that owns translation data and provides lookups.
pub struct TranslationManager {
    current_locale: String,
    translations: HashMap<String, String>,
    fallback: HashMap<String, String>,
}

impl TranslationManager {
    /// Create a new manager, loading the given locale with English fallback.
    /// If the locale is unsupported or fails to load, defaults to English.
    pub fn new(locale: &str) -> Self {
        let fallback = Self::parse_json(ENGLISH_JSON);
        let locale = Self::validated_locale(locale);

        let translations = Self::load_locale(&locale);

        Self {
            current_locale: locale,
            translations,
            fallback,
        }
    }

    /// Create a TranslationManager from pre-built maps (for testing).
    #[cfg(test)]
    pub fn from_maps(
        locale: &str,
        translations: HashMap<String, String>,
        fallback: HashMap<String, String>,
    ) -> Self {
        Self {
            current_locale: locale.to_string(),
            translations,
            fallback,
        }
    }

    /// Validate a locale string, returning "en" for unsupported values.
    fn validated_locale(locale: &str) -> String {
        if SUPPORTED_LOCALES.iter().any(|(code, _, _)| *code == locale) {
            locale.to_string()
        } else {
            log::warn!("Unsupported locale '{}', defaulting to 'en'", locale);
            "en".to_string()
        }
    }

    /// Load a locale's translations from JSON.
    /// English is loaded from the embedded string; other locales from disk.
    fn load_locale(locale: &str) -> HashMap<String, String> {
        if locale == "en" {
            Self::parse_json(ENGLISH_JSON)
        } else {
            let path = Self::locale_file_path(locale);
            match std::fs::read_to_string(&path) {
                Ok(contents) => Self::parse_json(&contents),
                Err(e) => {
                    log::warn!(
                        "Failed to load locale file '{}': {}. Falling back to English.",
                        path.display(),
                        e
                    );
                    Self::parse_json(ENGLISH_JSON)
                }
            }
        }
    }

    /// Get the file path for a locale's JSON translation file.
    fn locale_file_path(locale: &str) -> PathBuf {
        // Look for i18n files relative to the executable's directory first,
        // then fall back to the compile-time crate path for development.
        let exe_dir = std::env::current_exe()
            .ok()
            .and_then(|p| p.parent().map(|d| d.to_path_buf()));

        if let Some(dir) = exe_dir {
            let candidate = dir.join("i18n").join(format!("{}.json", locale));
            if candidate.exists() {
                return candidate;
            }
        }

        // Development fallback: relative to crate root
        PathBuf::from(env!("CARGO_MANIFEST_DIR"))
            .join("i18n")
            .join(format!("{}.json", locale))
    }

    /// Parse a JSON string into a translation map.
    fn parse_json(json: &str) -> HashMap<String, String> {
        match serde_json::from_str::<HashMap<String, String>>(json) {
            Ok(map) => map,
            Err(e) => {
                log::error!("Failed to parse translation JSON: {}", e);
                HashMap::new()
            }
        }
    }

    /// Get a translated string by key, falling back to English, then the key itself.
    pub fn get<'a>(&'a self, key: &'a str) -> &'a str {
        if let Some(value) = self.translations.get(key) {
            value.as_str()
        } else if let Some(value) = self.fallback.get(key) {
            value.as_str()
        } else {
            log::warn!("Translation key '{}' missing from all locales", key);
            key
        }
    }

    /// Get a translated string with placeholder substitution.
    /// Replaces `{variable_name}` patterns with values from the args slice.
    pub fn get_with_args(&self, key: &str, args: &[(&str, &str)]) -> String {
        let template = self.get(key);
        Self::substitute_placeholders(template, args)
    }

    /// Perform placeholder substitution on a template string.
    /// Scans for `{variable_name}` patterns and replaces them from the args map.
    /// Unmatched placeholders are left as-is.
    fn substitute_placeholders(template: &str, args: &[(&str, &str)]) -> String {
        let mut result = String::with_capacity(template.len());
        let mut chars = template.chars().peekable();

        while let Some(ch) = chars.next() {
            if ch == '{' {
                // Try to read a placeholder name until '}'
                let mut name = String::new();
                let mut found_close = false;
                for inner in chars.by_ref() {
                    if inner == '}' {
                        found_close = true;
                        break;
                    }
                    name.push(inner);
                }

                if found_close && !name.is_empty() {
                    // Look up the placeholder in args
                    if let Some((_, value)) = args.iter().find(|(k, _)| *k == name) {
                        result.push_str(value);
                    } else {
                        // Placeholder not in args — leave as-is
                        result.push('{');
                        result.push_str(&name);
                        result.push('}');
                    }
                } else {
                    // Malformed placeholder — output what we consumed
                    result.push('{');
                    result.push_str(&name);
                }
            } else {
                result.push(ch);
            }
        }

        result
    }

    /// Switch to a new locale, reloading translations.
    /// Invalid locales default to English.
    pub fn set_locale(&mut self, locale: &str) {
        let locale = Self::validated_locale(locale);
        self.translations = Self::load_locale(&locale);
        self.current_locale = locale;
    }

    /// Returns true if the current locale uses RTL layout (Arabic).
    pub fn is_rtl(&self) -> bool {
        self.current_locale == "ar"
    }

    /// Returns the current locale string.
    pub fn current_locale(&self) -> &str {
        &self.current_locale
    }

    /// Apply all translations to the Slint TranslationStrings global.
    /// Sets every property from the loaded translations and updates metadata.
    pub fn apply(&self, app: &App) {
        let ts = app.global::<TranslationStrings>();

        // ── Metadata ──────────────────────────────────────────────
        ts.set_current_locale(self.current_locale.as_str().into());
        ts.set_is_rtl(self.is_rtl());

        // ── App ───────────────────────────────────────────────────
        ts.set_app_title(self.get("app.title").into());
        ts.set_app_logged_in_as(self.get("app.logged_in_as").into());
        ts.set_app_logged_in_prefix(self.get("app.logged_in_prefix").into());

        // ── Common ────────────────────────────────────────────────
        ts.set_common_cancel(self.get("common.cancel").into());
        ts.set_common_save(self.get("common.save").into());
        ts.set_common_create(self.get("common.create").into());
        ts.set_common_logout(self.get("common.logout").into());
        ts.set_common_edit_selected(self.get("common.edit_selected").into());
        ts.set_common_dark_mode(self.get("common.dark_mode").into());
        ts.set_common_search(self.get("common.search").into());
        ts.set_common_name(self.get("common.name").into());
        ts.set_common_description(self.get("common.description").into());
        ts.set_common_generate(self.get("common.generate").into());
        ts.set_common_format(self.get("common.format").into());
        ts.set_common_record(self.get("common.record").into());
        ts.set_common_apply(self.get("common.apply").into());
        ts.set_common_from(self.get("common.from").into());
        ts.set_common_to(self.get("common.to").into());

        // ── Login ─────────────────────────────────────────────────
        ts.set_login_title(self.get("login.title").into());
        ts.set_login_subtitle(self.get("login.subtitle").into());
        ts.set_login_username(self.get("login.username").into());
        ts.set_login_password(self.get("login.password").into());
        ts.set_login_username_placeholder(self.get("login.username_placeholder").into());
        ts.set_login_password_placeholder(self.get("login.password_placeholder").into());
        ts.set_login_sign_in(self.get("login.sign_in").into());

        // ── Navigation ────────────────────────────────────────────
        ts.set_nav_sidebar_title(self.get("nav.sidebar_title").into());
        ts.set_nav_dashboard(self.get("nav.dashboard").into());
        ts.set_nav_products(self.get("nav.products").into());
        ts.set_nav_categories(self.get("nav.categories").into());
        ts.set_nav_people(self.get("nav.people").into());
        ts.set_nav_purchasing(self.get("nav.purchasing").into());
        ts.set_nav_locations(self.get("nav.locations").into());
        ts.set_nav_stock_tracking(self.get("nav.stock_tracking").into());
        ts.set_nav_budget(self.get("nav.budget").into());
        ts.set_nav_reports(self.get("nav.reports").into());
        ts.set_nav_users(self.get("nav.users").into());
        ts.set_nav_sync(self.get("nav.sync").into());

        // ── Dashboard ─────────────────────────────────────────────
        ts.set_dashboard_title(self.get("dashboard.title").into());
        ts.set_dashboard_description(self.get("dashboard.description").into());
        ts.set_dashboard_total_products(self.get("dashboard.total_products").into());
        ts.set_dashboard_active_deals(self.get("dashboard.active_deals").into());
        ts.set_dashboard_low_stock_items(self.get("dashboard.low_stock_items").into());
        ts.set_dashboard_stock_alerts(self.get("dashboard.stock_alerts").into());
        ts.set_dashboard_qty(self.get("dashboard.qty").into());
        ts.set_dashboard_qty_prefix(self.get("dashboard.qty_prefix").into());
        ts.set_dashboard_sync_status(self.get("dashboard.sync_status").into());
        ts.set_dashboard_status(self.get("dashboard.status").into());
        ts.set_dashboard_online(self.get("dashboard.online").into());
        ts.set_dashboard_local(self.get("dashboard.local").into());
        ts.set_dashboard_last_sync(self.get("dashboard.last_sync").into());
        ts.set_dashboard_pending(self.get("dashboard.pending").into());

        // ── Products ──────────────────────────────────────────────
        ts.set_products_title(self.get("products.title").into());
        ts.set_products_description(self.get("products.description").into());
        ts.set_products_search_placeholder(self.get("products.search_placeholder").into());
        ts.set_products_add_product(self.get("products.add_product").into());
        ts.set_products_new_product(self.get("products.new_product").into());
        ts.set_products_edit_product(self.get("products.edit_product").into());
        ts.set_products_product_list(self.get("products.product_list").into());
        ts.set_products_name(self.get("products.name").into());
        ts.set_products_reference(self.get("products.reference").into());
        ts.set_products_description_label(self.get("products.description_label").into());
        ts.set_products_quantity(self.get("products.quantity").into());
        ts.set_products_price_unit(self.get("products.price_unit").into());
        ts.set_products_category(self.get("products.category").into());
        ts.set_products_supplier(self.get("products.supplier").into());
        ts.set_products_location(self.get("products.location").into());
        ts.set_products_status(self.get("products.status").into());
        ts.set_products_name_placeholder(self.get("products.name_placeholder").into());
        ts.set_products_reference_placeholder(self.get("products.reference_placeholder").into());
        ts.set_products_description_placeholder(self.get("products.description_placeholder").into());
        ts.set_products_price_placeholder(self.get("products.price_placeholder").into());

        // ── Categories ────────────────────────────────────────────
        ts.set_categories_title(self.get("categories.title").into());
        ts.set_categories_description(self.get("categories.description").into());
        ts.set_categories_search_placeholder(self.get("categories.search_placeholder").into());
        ts.set_categories_add_category(self.get("categories.add_category").into());
        ts.set_categories_new_category(self.get("categories.new_category").into());
        ts.set_categories_edit_category(self.get("categories.edit_category").into());
        ts.set_categories_group_title(self.get("categories.group_title").into());
        ts.set_categories_name(self.get("categories.name").into());
        ts.set_categories_description_label(self.get("categories.description_label").into());
        ts.set_categories_name_placeholder(self.get("categories.name_placeholder").into());
        ts.set_categories_description_placeholder(self.get("categories.description_placeholder").into());

        // ── Persons ───────────────────────────────────────────────
        ts.set_persons_title(self.get("persons.title").into());
        ts.set_persons_description(self.get("persons.description").into());
        ts.set_persons_search_placeholder(self.get("persons.search_placeholder").into());
        ts.set_persons_add_person(self.get("persons.add_person").into());
        ts.set_persons_new_person(self.get("persons.new_person").into());
        ts.set_persons_edit_person(self.get("persons.edit_person").into());
        ts.set_persons_group_title(self.get("persons.group_title").into());
        ts.set_persons_name(self.get("persons.name").into());
        ts.set_persons_role(self.get("persons.role").into());
        ts.set_persons_phone(self.get("persons.phone").into());
        ts.set_persons_email(self.get("persons.email").into());
        ts.set_persons_name_placeholder(self.get("persons.name_placeholder").into());
        ts.set_persons_phone_placeholder(self.get("persons.phone_placeholder").into());
        ts.set_persons_email_placeholder(self.get("persons.email_placeholder").into());

        // ── Deals ─────────────────────────────────────────────────
        ts.set_deals_title(self.get("deals.title").into());
        ts.set_deals_description(self.get("deals.description").into());
        ts.set_deals_search_placeholder(self.get("deals.search_placeholder").into());
        ts.set_deals_new_deal(self.get("deals.new_deal").into());
        ts.set_deals_edit_deal(self.get("deals.edit_deal").into());
        ts.set_deals_group_title(self.get("deals.group_title").into());
        ts.set_deals_deal_title(self.get("deals.deal_title").into());
        ts.set_deals_description_label(self.get("deals.description_label").into());
        ts.set_deals_frequency(self.get("deals.frequency").into());
        ts.set_deals_status(self.get("deals.status").into());
        ts.set_deals_start_date(self.get("deals.start_date").into());
        ts.set_deals_end_date(self.get("deals.end_date").into());
        ts.set_deals_supplier(self.get("deals.supplier").into());
        ts.set_deals_product(self.get("deals.product").into());
        ts.set_deals_manager(self.get("deals.manager").into());
        ts.set_deals_title_placeholder(self.get("deals.title_placeholder").into());
        ts.set_deals_description_placeholder(self.get("deals.description_placeholder").into());
        ts.set_deals_select_start_date(self.get("deals.select_start_date").into());
        ts.set_deals_select_end_date(self.get("deals.select_end_date").into());
        ts.set_deals_col_title(self.get("deals.col_title").into());
        ts.set_deals_col_product(self.get("deals.col_product").into());
        ts.set_deals_col_supplier(self.get("deals.col_supplier").into());
        ts.set_deals_col_manager(self.get("deals.col_manager").into());
        ts.set_deals_col_frequency(self.get("deals.col_frequency").into());
        ts.set_deals_col_status(self.get("deals.col_status").into());

        // ── Locations ─────────────────────────────────────────────
        ts.set_locations_title(self.get("locations.title").into());
        ts.set_locations_description(self.get("locations.description").into());
        ts.set_locations_search_placeholder(self.get("locations.search_placeholder").into());
        ts.set_locations_add_location(self.get("locations.add_location").into());
        ts.set_locations_new_location(self.get("locations.new_location").into());
        ts.set_locations_edit_location(self.get("locations.edit_location").into());
        ts.set_locations_group_title(self.get("locations.group_title").into());
        ts.set_locations_name(self.get("locations.name").into());
        ts.set_locations_address(self.get("locations.address").into());
        ts.set_locations_latitude(self.get("locations.latitude").into());
        ts.set_locations_longitude(self.get("locations.longitude").into());
        ts.set_locations_capacity(self.get("locations.capacity").into());
        ts.set_locations_manager(self.get("locations.manager").into());
        ts.set_locations_name_placeholder(self.get("locations.name_placeholder").into());
        ts.set_locations_address_placeholder(self.get("locations.address_placeholder").into());
        ts.set_locations_lat_placeholder(self.get("locations.lat_placeholder").into());
        ts.set_locations_lng_placeholder(self.get("locations.lng_placeholder").into());
        ts.set_locations_col_name(self.get("locations.col_name").into());
        ts.set_locations_col_address(self.get("locations.col_address").into());
        ts.set_locations_col_manager(self.get("locations.col_manager").into());
        ts.set_locations_col_capacity(self.get("locations.col_capacity").into());
        ts.set_locations_col_used(self.get("locations.col_used").into());
        ts.set_locations_col_lat(self.get("locations.col_lat").into());
        ts.set_locations_col_lng(self.get("locations.col_lng").into());

        // ── Stock Tracking ────────────────────────────────────────
        ts.set_stock_title(self.get("stock.title").into());
        ts.set_stock_description(self.get("stock.description").into());
        ts.set_stock_total_inbound_30d(self.get("stock.total_inbound_30d").into());
        ts.set_stock_total_outbound_30d(self.get("stock.total_outbound_30d").into());
        ts.set_stock_net_change_30d(self.get("stock.net_change_30d").into());
        ts.set_stock_stock_level_trend(self.get("stock.stock_level_trend").into());
        ts.set_stock_record_movement(self.get("stock.record_movement").into());
        ts.set_stock_new_movement(self.get("stock.new_movement").into());
        ts.set_stock_movement_history(self.get("stock.movement_history").into());
        ts.set_stock_product(self.get("stock.product").into());
        ts.set_stock_type(self.get("stock.type").into());
        ts.set_stock_quantity(self.get("stock.quantity").into());
        ts.set_stock_from_location(self.get("stock.from_location").into());
        ts.set_stock_to_location(self.get("stock.to_location").into());
        ts.set_stock_note(self.get("stock.note").into());
        ts.set_stock_note_placeholder(self.get("stock.note_placeholder").into());
        ts.set_stock_col_date(self.get("stock.col_date").into());
        ts.set_stock_col_type(self.get("stock.col_type").into());
        ts.set_stock_col_product(self.get("stock.col_product").into());
        ts.set_stock_col_qty(self.get("stock.col_qty").into());
        ts.set_stock_col_from(self.get("stock.col_from").into());
        ts.set_stock_col_to(self.get("stock.col_to").into());
        ts.set_stock_col_user(self.get("stock.col_user").into());
        ts.set_stock_col_note(self.get("stock.col_note").into());

        // ── Budget ────────────────────────────────────────────────
        ts.set_budget_title(self.get("budget.title").into());
        ts.set_budget_description(self.get("budget.description").into());
        ts.set_budget_total_purchases(self.get("budget.total_purchases").into());
        ts.set_budget_total_sales(self.get("budget.total_sales").into());
        ts.set_budget_total_expenses(self.get("budget.total_expenses").into());
        ts.set_budget_net_balance(self.get("budget.net_balance").into());
        ts.set_budget_select_start_date(self.get("budget.select_start_date").into());
        ts.set_budget_select_end_date(self.get("budget.select_end_date").into());
        ts.set_budget_tab_overview(self.get("budget.tab_overview").into());
        ts.set_budget_tab_projection(self.get("budget.tab_projection").into());
        ts.set_budget_tab_trends(self.get("budget.tab_trends").into());
        ts.set_budget_monthly_breakdown(self.get("budget.monthly_breakdown").into());
        ts.set_budget_budget_entries(self.get("budget.budget_entries").into());
        ts.set_budget_col_date(self.get("budget.col_date").into());
        ts.set_budget_col_type(self.get("budget.col_type").into());
        ts.set_budget_col_amount(self.get("budget.col_amount").into());
        ts.set_budget_col_description(self.get("budget.col_description").into());
        ts.set_budget_col_product(self.get("budget.col_product").into());
        ts.set_budget_col_deal(self.get("budget.col_deal").into());
        ts.set_budget_col_recorded_by(self.get("budget.col_recorded_by").into());
        ts.set_budget_months_ahead(self.get("budget.months_ahead").into());
        ts.set_budget_projected_income(self.get("budget.projected_income").into());
        ts.set_budget_projected_expenses(self.get("budget.projected_expenses").into());
        ts.set_budget_projected_balance(self.get("budget.projected_balance").into());
        ts.set_budget_income_trend(self.get("budget.income_trend").into());
        ts.set_budget_expense_trend(self.get("budget.expense_trend").into());
        ts.set_budget_add_entry(self.get("budget.add_entry").into());
        ts.set_budget_new_entry(self.get("budget.new_entry").into());
        ts.set_budget_type(self.get("budget.type").into());
        ts.set_budget_amount(self.get("budget.amount").into());
        ts.set_budget_date(self.get("budget.date").into());
        ts.set_budget_description_label(self.get("budget.description_label").into());
        ts.set_budget_amount_placeholder(self.get("budget.amount_placeholder").into());
        ts.set_budget_description_placeholder(self.get("budget.description_placeholder").into());

        // ── Reports ───────────────────────────────────────────────
        ts.set_reports_title(self.get("reports.title").into());
        ts.set_reports_description(self.get("reports.description").into());
        ts.set_reports_inventory_report(self.get("reports.inventory_report").into());
        ts.set_reports_stock_movement_report(self.get("reports.stock_movement_report").into());
        ts.set_reports_budget_report(self.get("reports.budget_report").into());
        ts.set_reports_purchasing_report(self.get("reports.purchasing_report").into());
        ts.set_reports_format(self.get("reports.format").into());
        ts.set_reports_include_zero_stock(self.get("reports.include_zero_stock").into());
        ts.set_reports_include_projections(self.get("reports.include_projections").into());
        ts.set_reports_status(self.get("reports.status").into());
        ts.set_reports_status_all(self.get("reports.status_all").into());
        ts.set_reports_status_active_only(self.get("reports.status_active_only").into());
        ts.set_reports_status_completed_only(self.get("reports.status_completed_only").into());
        ts.set_reports_output(self.get("reports.output").into());

        // ── Users ─────────────────────────────────────────────────
        ts.set_users_title(self.get("users.title").into());
        ts.set_users_description(self.get("users.description").into());
        ts.set_users_search_placeholder(self.get("users.search_placeholder").into());
        ts.set_users_add_user(self.get("users.add_user").into());
        ts.set_users_new_user(self.get("users.new_user").into());
        ts.set_users_edit_user(self.get("users.edit_user").into());
        ts.set_users_group_title(self.get("users.group_title").into());
        ts.set_users_username(self.get("users.username").into());
        ts.set_users_display_name(self.get("users.display_name").into());
        ts.set_users_password(self.get("users.password").into());
        ts.set_users_role(self.get("users.role").into());
        ts.set_users_username_placeholder(self.get("users.username_placeholder").into());
        ts.set_users_display_name_placeholder(self.get("users.display_name_placeholder").into());
        ts.set_users_password_placeholder(self.get("users.password_placeholder").into());
        ts.set_users_password_edit_placeholder(self.get("users.password_edit_placeholder").into());
        ts.set_users_col_username(self.get("users.col_username").into());
        ts.set_users_col_display_name(self.get("users.col_display_name").into());
        ts.set_users_col_role(self.get("users.col_role").into());
        ts.set_users_col_active(self.get("users.col_active").into());

        // ── Sync ──────────────────────────────────────────────────
        ts.set_sync_title(self.get("sync.title").into());
        ts.set_sync_description(self.get("sync.description").into());
        ts.set_sync_pending_changes(self.get("sync.pending_changes").into());
        ts.set_sync_connection(self.get("sync.connection").into());
        ts.set_sync_online(self.get("sync.online").into());
        ts.set_sync_local(self.get("sync.local").into());
        ts.set_sync_sync_actions(self.get("sync.sync_actions").into());
        ts.set_sync_last_synchronized(self.get("sync.last_synchronized").into());
        ts.set_sync_status(self.get("sync.status").into());
        ts.set_sync_push_to_remote(self.get("sync.push_to_remote").into());
        ts.set_sync_pull_from_remote(self.get("sync.pull_from_remote").into());
        ts.set_sync_database_settings(self.get("sync.database_settings").into());
        ts.set_sync_turso_url(self.get("sync.turso_url").into());
        ts.set_sync_turso_url_placeholder(self.get("sync.turso_url_placeholder").into());
        ts.set_sync_auth_token(self.get("sync.auth_token").into());
        ts.set_sync_auth_token_placeholder(self.get("sync.auth_token_placeholder").into());
        ts.set_sync_auto_sync_enabled(self.get("sync.auto_sync_enabled").into());
        ts.set_sync_interval_seconds(self.get("sync.interval_seconds").into());
        ts.set_sync_test_connection(self.get("sync.test_connection").into());
        ts.set_sync_save_settings(self.get("sync.save_settings").into());
        ts.set_sync_offline_mode(self.get("sync.offline_mode").into());
        ts.set_sync_offline_description(self.get("sync.offline_description").into());

        // ── Confirm Dialog ────────────────────────────────────────
        ts.set_confirm_default_message(self.get("confirm.default_message").into());
        ts.set_confirm_confirm(self.get("confirm.confirm").into());
        ts.set_confirm_cancel(self.get("confirm.cancel").into());
    }
}


#[cfg(test)]
mod tests {
    use super::*;
    use proptest::prelude::*;
    use proptest::collection::hash_map;

    /// Strategy to generate valid translation keys (dot-notation identifiers).
    fn key_strategy() -> impl Strategy<Value = String> {
        "[a-z][a-z0-9_]{0,9}(\\.[a-z][a-z0-9_]{0,9}){0,2}"
    }

    /// Strategy to generate translation values (non-empty printable strings without braces).
    fn value_strategy() -> impl Strategy<Value = String> {
        "[a-zA-Z0-9 _.,!?:;'-]{1,50}"
    }

    /// Strategy to generate a translation map with 1..20 entries.
    fn translation_map_strategy() -> impl Strategy<Value = HashMap<String, String>> {
        hash_map(key_strategy(), value_strategy(), 1..20)
    }

    /// Strategy to generate valid placeholder names.
    fn placeholder_name_strategy() -> impl Strategy<Value = String> {
        "[a-z][a-z0-9_]{0,9}"
    }

    // Feature: internationalization, Property 1: Translation file round-trip
    // **Validates: Requirements 1.5**
    proptest! {
        #![proptest_config(ProptestConfig::with_cases(100))]
        #[test]
        fn prop_translation_file_round_trip(
            map in translation_map_strategy()
        ) {
            // Serialize to JSON
            let json = serde_json::to_string(&map).unwrap();
            // Deserialize back
            let deserialized: HashMap<String, String> = serde_json::from_str(&json).unwrap();
            prop_assert_eq!(map, deserialized);
        }
    }

    // Feature: internationalization, Property 2: Translation file parsing produces accessible keys
    // **Validates: Requirements 1.2**
    proptest! {
        #![proptest_config(ProptestConfig::with_cases(100))]
        #[test]
        fn prop_translation_parsing_produces_accessible_keys(
            map in translation_map_strategy()
        ) {
            let fallback = HashMap::new();
            let tm = TranslationManager::from_maps("en", map.clone(), fallback);
            for (key, value) in &map {
                prop_assert_eq!(tm.get(key), value.as_str());
            }
        }
    }

    // Feature: internationalization, Property 3: Missing key fallback to English
    // **Validates: Requirements 1.3, 6.2**
    proptest! {
        #![proptest_config(ProptestConfig::with_cases(100))]
        #[test]
        fn prop_missing_key_fallback_to_english(
            english_map in translation_map_strategy(),
            locale_map in translation_map_strategy(),
        ) {
            let tm = TranslationManager::from_maps("fr", locale_map.clone(), english_map.clone());

            // For every key in the English fallback that is NOT in the locale map,
            // get() should return the English value.
            for (key, en_value) in &english_map {
                if !locale_map.contains_key(key) {
                    prop_assert_eq!(tm.get(key), en_value.as_str());
                }
            }
        }
    }

    // Feature: internationalization, Property 9: Placeholder substitution with named variables
    // **Validates: Requirements 7.1, 7.2**
    proptest! {
        #![proptest_config(ProptestConfig::with_cases(100))]
        #[test]
        fn prop_placeholder_substitution_with_named_variables(
            placeholders in proptest::collection::hash_map(
                placeholder_name_strategy(),
                "[a-zA-Z0-9 ]{1,20}",
                1..5
            ),
        ) {
            // Build a template string with all placeholders
            let template: String = placeholders.keys()
                .map(|k| format!("{{{}}}", k))
                .collect::<Vec<_>>()
                .join(" ");

            // Build the expected output with all placeholders replaced
            let expected: String = placeholders.keys()
                .map(|k| placeholders[k].clone())
                .collect::<Vec<_>>()
                .join(" ");

            // Store the template as a translation
            let key = "test.template";
            let mut translations = HashMap::new();
            translations.insert(key.to_string(), template);

            let tm = TranslationManager::from_maps("en", translations, HashMap::new());

            let args: Vec<(&str, &str)> = placeholders.iter()
                .map(|(k, v)| (k.as_str(), v.as_str()))
                .collect();

            let result = tm.get_with_args(key, &args);
            prop_assert_eq!(result, expected);
        }
    }

    // Feature: internationalization, Property 5: is-rtl reflects locale correctly
    // **Validates: Requirements 2.5, 5.1**
    proptest! {
        #![proptest_config(ProptestConfig::with_cases(100))]
        #[test]
        fn prop_is_rtl_reflects_locale_correctly(
            locale_index in 0..SUPPORTED_LOCALES.len(),
            arbitrary_locale in "[a-z]{2,5}"
        ) {
            // For each supported locale, is_rtl() must be true iff locale == "ar"
            let (code, _, expected_rtl) = SUPPORTED_LOCALES[locale_index];
            let tm = TranslationManager::from_maps(
                code,
                HashMap::new(),
                HashMap::new(),
            );
            prop_assert_eq!(
                tm.is_rtl(),
                expected_rtl,
                "is_rtl() should be {} for locale '{}'",
                expected_rtl,
                code
            );
            prop_assert_eq!(tm.is_rtl(), code == "ar");

            // Arbitrary non-supported locales should default to "en" (not RTL)
            if !SUPPORTED_LOCALES.iter().any(|(c, _, _)| *c == arbitrary_locale.as_str()) {
                let validated = "en".to_string();
                let tm2 = TranslationManager::from_maps(&validated, HashMap::new(), HashMap::new());
                prop_assert!(
                    !tm2.is_rtl(),
                    "Non-supported locale '{}' should default to 'en' (not RTL)",
                    arbitrary_locale
                );
            }
        }
    }

    // Feature: internationalization, Property 10: Missing placeholder args preserved as-is
    // **Validates: Requirements 7.3**
    proptest! {
        #![proptest_config(ProptestConfig::with_cases(100))]
        #[test]
        fn prop_missing_placeholder_args_preserved(
            present in proptest::collection::hash_map(
                placeholder_name_strategy(),
                "[a-zA-Z0-9 ]{1,20}",
                1..4
            ),
            missing_names in proptest::collection::hash_set(
                placeholder_name_strategy(),
                1..4
            ),
        ) {
            // Ensure missing names don't overlap with present names
            let missing_names: Vec<String> = missing_names.into_iter()
                .filter(|n| !present.contains_key(n))
                .collect();

            // Skip if no truly missing placeholders remain after filtering
            prop_assume!(!missing_names.is_empty());

            // Build template: present placeholders + missing placeholders
            let mut template_parts: Vec<String> = present.keys()
                .map(|k| format!("{{{}}}", k))
                .collect();
            for name in &missing_names {
                template_parts.push(format!("{{{}}}", name));
            }
            let template = template_parts.join(" ");

            // Build expected: present values + missing placeholders as-is
            let mut expected_parts: Vec<String> = present.keys()
                .map(|k| present[k].clone())
                .collect();
            for name in &missing_names {
                expected_parts.push(format!("{{{}}}", name));
            }
            let expected = expected_parts.join(" ");

            let key = "test.partial";
            let mut translations = HashMap::new();
            translations.insert(key.to_string(), template);

            let tm = TranslationManager::from_maps("en", translations, HashMap::new());

            // Only provide args for the "present" placeholders
            let args: Vec<(&str, &str)> = present.iter()
                .map(|(k, v)| (k.as_str(), v.as_str()))
                .collect();

            let result = tm.get_with_args(key, &args);
            prop_assert_eq!(result, expected);
        }
    }

    // Feature: internationalization, Property 6: Locale persistence round-trip
    // **Validates: Requirements 4.1, 4.2, 4.4**
    //
    // Minimal replica of UiSettings from main.rs for testing TOML round-trip,
    // since main.rs is a binary crate and its types cannot be imported.
    #[derive(serde::Serialize, serde::Deserialize, Debug, PartialEq)]
    struct TestUiSettings {
        window_width: f32,
        window_height: f32,
        dark_mode: bool,
        #[serde(default)]
        maximized: bool,
        #[serde(default)]
        window_x: Option<i32>,
        #[serde(default)]
        window_y: Option<i32>,
        #[serde(default = "default_test_locale")]
        locale: String,
    }

    fn default_test_locale() -> String {
        "en".to_string()
    }

    proptest! {
        #![proptest_config(ProptestConfig::with_cases(100))]
        #[test]
        fn prop_locale_persistence_round_trip(
            locale_index in 0..SUPPORTED_LOCALES.len(),
        ) {
            let (locale_code, _, _) = SUPPORTED_LOCALES[locale_index];

            let settings = TestUiSettings {
                window_width: 1024.0,
                window_height: 700.0,
                dark_mode: false,
                maximized: false,
                window_x: None,
                window_y: None,
                locale: locale_code.to_string(),
            };

            // Serialize to TOML (same as UiSettings::save)
            let toml_str = toml::to_string_pretty(&settings).unwrap();

            // Deserialize back (same as UiSettings::load)
            let loaded: TestUiSettings = toml::from_str(&toml_str).unwrap();

            prop_assert_eq!(
                &loaded.locale,
                locale_code,
                "Locale '{}' should survive TOML round-trip, got '{}'",
                locale_code,
                &loaded.locale
            );
        }
    }

    // Feature: internationalization, Property 8: Translation completeness across locales
    // **Validates: Requirements 6.1**
    #[test]
    fn prop_translation_completeness_across_locales() {
        let en_json = include_str!("../i18n/en.json");
        let fr_json = include_str!("../i18n/fr.json");
        let ar_json = include_str!("../i18n/ar.json");

        let en_map: HashMap<String, String> = serde_json::from_str(en_json)
            .expect("Failed to parse en.json");
        let fr_map: HashMap<String, String> = serde_json::from_str(fr_json)
            .expect("Failed to parse fr.json");
        let ar_map: HashMap<String, String> = serde_json::from_str(ar_json)
            .expect("Failed to parse ar.json");

        let mut missing_fr: Vec<&String> = Vec::new();
        let mut missing_ar: Vec<&String> = Vec::new();

        for key in en_map.keys() {
            if !fr_map.contains_key(key) {
                missing_fr.push(key);
            }
            if !ar_map.contains_key(key) {
                missing_ar.push(key);
            }
        }

        assert!(
            missing_fr.is_empty(),
            "French translation is missing {} keys present in English: {:?}",
            missing_fr.len(),
            missing_fr
        );
        assert!(
            missing_ar.is_empty(),
            "Arabic translation is missing {} keys present in English: {:?}",
            missing_ar.len(),
            missing_ar
        );
    }

    // Feature: internationalization, Property 7: Invalid locale defaults to English
    // **Validates: Requirements 4.3**
    proptest! {
        #![proptest_config(ProptestConfig::with_cases(100))]
        #[test]
        fn prop_invalid_locale_defaults_to_english(
            invalid_locale in "[a-zA-Z0-9_-]{1,20}"
                .prop_filter("must not be a supported locale", |s| {
                    !SUPPORTED_LOCALES.iter().any(|(code, _, _)| *code == s.as_str())
                })
        ) {
            let tm = TranslationManager::new(&invalid_locale);

            // current_locale() must be "en"
            prop_assert_eq!(
                tm.current_locale(),
                "en",
                "Invalid locale '{}' should default to 'en', got '{}'",
                invalid_locale,
                tm.current_locale()
            );

            // is_rtl() must be false (English is LTR)
            prop_assert!(
                !tm.is_rtl(),
                "Invalid locale '{}' should not be RTL",
                invalid_locale
            );

            // get() should return English translations
            prop_assert_eq!(
                tm.get("app.title"),
                "Inventory Manager",
                "Invalid locale '{}' should load English translations",
                invalid_locale
            );
        }
    }

    // ── Unit tests for locale switching and RTL ───────────────────────
    // **Validates: Requirements 2.2, 5.1, 5.3**

    #[test]
    fn test_locale_switch_en_to_fr() {
        let mut tm = TranslationManager::new("en");
        assert_eq!(tm.current_locale(), "en");
        assert!(!tm.is_rtl());
        assert_eq!(tm.get("app.title"), "Inventory Manager");
        assert_eq!(tm.get("login.sign_in"), "Sign In");

        tm.set_locale("fr");
        assert_eq!(tm.current_locale(), "fr");
        assert!(!tm.is_rtl());
        assert_eq!(tm.get("app.title"), "Gestionnaire d'inventaire");
        assert_eq!(tm.get("login.sign_in"), "Se connecter");
    }

    #[test]
    fn test_locale_switch_fr_to_ar() {
        let mut tm = TranslationManager::new("fr");
        assert_eq!(tm.current_locale(), "fr");
        assert!(!tm.is_rtl());
        assert_eq!(tm.get("app.title"), "Gestionnaire d'inventaire");
        assert_eq!(tm.get("login.sign_in"), "Se connecter");

        tm.set_locale("ar");
        assert_eq!(tm.current_locale(), "ar");
        assert!(tm.is_rtl());
        assert_eq!(tm.get("app.title"), "مدير المخزون");
        assert_eq!(tm.get("login.sign_in"), "تسجيل الدخول");
    }

    #[test]
    fn test_locale_switch_ar_to_en() {
        let mut tm = TranslationManager::new("ar");
        assert_eq!(tm.current_locale(), "ar");
        assert!(tm.is_rtl());
        assert_eq!(tm.get("app.title"), "مدير المخزون");
        assert_eq!(tm.get("login.sign_in"), "تسجيل الدخول");

        tm.set_locale("en");
        assert_eq!(tm.current_locale(), "en");
        assert!(!tm.is_rtl());
        assert_eq!(tm.get("app.title"), "Inventory Manager");
        assert_eq!(tm.get("login.sign_in"), "Sign In");
    }

    #[test]
    fn test_rtl_flag_toggles_across_all_switches() {
        let mut tm = TranslationManager::new("en");
        assert!(!tm.is_rtl(), "English should be LTR");

        tm.set_locale("ar");
        assert!(tm.is_rtl(), "Arabic should be RTL");

        tm.set_locale("fr");
        assert!(!tm.is_rtl(), "French should be LTR after switching from Arabic");

        tm.set_locale("ar");
        assert!(tm.is_rtl(), "Arabic should be RTL again");

        tm.set_locale("en");
        assert!(!tm.is_rtl(), "English should be LTR after switching from Arabic");
    }
}
