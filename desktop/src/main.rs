// Sweet Lab Desktop — Slint UI shell
// Calls sweet-lab-core directly (no FFI needed, same Rust crate)

use std::sync::{Arc, Mutex};
use slint::{ModelRc, SharedString, VecModel};
use sweet_lab_core::{RecipeIngredient, SweetLabCore, Uuid};
use chrono::Utc;

slint::include_modules!();

/// Map Arabic role display name to the core UserRole enum value.
fn parse_role_from_arabic(role_ar: &str) -> Option<sweet_lab_core::UserRole> {
    match role_ar {
        "مدير" => Some(sweet_lab_core::UserRole::Admin),
        "شيف" => Some(sweet_lab_core::UserRole::Chef),
        "مندوب" => Some(sweet_lab_core::UserRole::Representative),
        _ => None,
    }
}

/// Map core UserRole to Arabic display name.
fn role_to_arabic(role: &sweet_lab_core::UserRole) -> &'static str {
    match role {
        sweet_lab_core::UserRole::Admin => "مدير",
        sweet_lab_core::UserRole::Chef => "شيف",
        sweet_lab_core::UserRole::Representative => "مندوب",
    }
}

/// Fetch dashboard data from the core engine and populate Slint properties.
/// Runs on a background thread to avoid blocking the UI event loop.
fn refresh_dashboard_data(
    app_weak: slint::Weak<AppWindow>,
    core: Arc<SweetLabCore>,
    session_token: Arc<Mutex<Option<Uuid>>>,
    rt_handle: tokio::runtime::Handle,
) {
    let token = {
        let guard = session_token.lock().unwrap();
        match *guard {
            Some(t) => t,
            None => return,
        }
    };

    // Set loading state on the UI thread
    let app_weak_loading = app_weak.clone();
    let _ = slint::invoke_from_event_loop(move || {
        if let Some(app) = app_weak_loading.upgrade() {
            app.set_dashboard_loading(true);
            app.set_dashboard_error(SharedString::default());
        }
    });

    std::thread::spawn(move || {
        // Fetch all three data sources concurrently
        let wallets_result = rt_handle.block_on(core.get_wallets(token, None));
        let debts_result = rt_handle.block_on(core.get_active_debts(token, None));
        let inventory_result = rt_handle.block_on(core.get_inventory_report(token, 10.0));

        let _ = slint::invoke_from_event_loop(move || {
            let Some(app) = app_weak.upgrade() else { return };

            let mut error_parts: Vec<String> = Vec::new();

            // Populate wallet balances
            match wallets_result {
                Ok(wallets) => {
                    let items: Vec<WalletBalanceItem> = wallets
                        .iter()
                        .map(|w| WalletBalanceItem {
                            name: SharedString::from(&w.name),
                            balance: SharedString::from(w.current_balance.to_display()),
                            wallet_type: SharedString::from(format!("{:?}", w.wallet_type)),
                        })
                        .collect();
                    app.set_dashboard_wallet_balances(ModelRc::new(VecModel::from(items)));
                }
                Err(e) => error_parts.push(format!("المحافظ: {e}")),
            }

            // Populate active debts count
            match debts_result {
                Ok(debts) => {
                    app.set_dashboard_active_debts_count(debts.len() as i32);
                }
                Err(e) => error_parts.push(format!("الديون: {e}")),
            }

            // Populate low stock count
            match inventory_result {
                Ok(report) => {
                    let low_stock = report
                        .raw_materials
                        .iter()
                        .filter(|r| r.is_low_stock)
                        .count()
                        + report
                            .finished_goods
                            .iter()
                            .filter(|f| f.is_low_stock)
                            .count();
                    app.set_dashboard_low_stock_count(low_stock as i32);
                }
                Err(e) => error_parts.push(format!("المخزون: {e}")),
            }

            if !error_parts.is_empty() {
                app.set_dashboard_error(SharedString::from(error_parts.join(" | ")));
            }

            app.set_dashboard_loading(false);
        });
    });
}

/// Fetch user list from the core engine and populate Slint properties.
fn refresh_user_list(
    app_weak: slint::Weak<AppWindow>,
    core: Arc<SweetLabCore>,
    session_token: Arc<Mutex<Option<Uuid>>>,
    rt_handle: tokio::runtime::Handle,
) {
    let token = {
        let guard = session_token.lock().unwrap();
        match *guard {
            Some(t) => t,
            None => return,
        }
    };

    // Set loading state
    let app_weak_loading = app_weak.clone();
    let _ = slint::invoke_from_event_loop(move || {
        if let Some(app) = app_weak_loading.upgrade() {
            app.set_user_mgmt_loading(true);
            app.set_user_mgmt_error(SharedString::default());
            app.set_user_mgmt_success(SharedString::default());
        }
    });

    std::thread::spawn(move || {
        let result = rt_handle.block_on(core.list_users(token));

        let _ = slint::invoke_from_event_loop(move || {
            let Some(app) = app_weak.upgrade() else { return };

            match result {
                Ok(users) => {
                    let items: Vec<UserItem> = users
                        .iter()
                        .map(|u| UserItem {
                            id: SharedString::from(u.id.to_string()),
                            username: SharedString::from(&u.username),
                            full_name: SharedString::from(&u.full_name),
                            role: SharedString::from(role_to_arabic(&u.role)),
                        })
                        .collect();
                    app.set_user_list(ModelRc::new(VecModel::from(items)));
                }
                Err(e) => {
                    app.set_user_mgmt_error(SharedString::from(format!("فشل تحميل المستخدمين: {e}")));
                }
            }

            app.set_user_mgmt_loading(false);
        });
    });
}

/// Fetch inventory data (raw materials + finished goods) from the core engine.
fn refresh_inventory_data(
    app_weak: slint::Weak<AppWindow>,
    core: Arc<SweetLabCore>,
    session_token: Arc<Mutex<Option<Uuid>>>,
    rt_handle: tokio::runtime::Handle,
) {
    let token = {
        let guard = session_token.lock().unwrap();
        match *guard {
            Some(t) => t,
            None => return,
        }
    };

    // Set loading state
    let app_weak_loading = app_weak.clone();
    let _ = slint::invoke_from_event_loop(move || {
        if let Some(app) = app_weak_loading.upgrade() {
            app.set_inventory_loading(true);
            app.set_inventory_error(SharedString::default());
        }
    });

    std::thread::spawn(move || {
        let raw_result = rt_handle.block_on(core.get_raw_materials(token, None));
        let fg_result = rt_handle.block_on(core.get_finished_goods(token, None));

        let _ = slint::invoke_from_event_loop(move || {
            let Some(app) = app_weak.upgrade() else { return };

            let mut error_parts: Vec<String> = Vec::new();

            match raw_result {
                Ok(materials) => {
                    let items: Vec<RawMaterialItem> = materials
                        .iter()
                        .map(|m| RawMaterialItem {
                            id: SharedString::from(m.id.to_string()),
                            name: SharedString::from(&m.name),
                            unit: SharedString::from(&m.unit),
                            current_quantity: SharedString::from(format!("{:.2}", m.current_quantity)),
                            last_updated: SharedString::from(m.last_updated.format("%Y-%m-%d %H:%M").to_string()),
                        })
                        .collect();
                    app.set_inventory_raw_materials(ModelRc::new(VecModel::from(items)));
                }
                Err(e) => error_parts.push(format!("المواد الخام: {e}")),
            }

            match fg_result {
                Ok(goods) => {
                    let items: Vec<FinishedGoodItem> = goods
                        .iter()
                        .map(|g| FinishedGoodItem {
                            id: SharedString::from(g.id.to_string()),
                            name: SharedString::from(&g.name),
                            current_quantity: SharedString::from(format!("{:.2}", g.current_quantity)),
                            unit_price: SharedString::from(g.unit_price.to_display()),
                            last_updated: SharedString::from(g.last_updated.format("%Y-%m-%d %H:%M").to_string()),
                        })
                        .collect();
                    app.set_inventory_finished_goods(ModelRc::new(VecModel::from(items)));
                }
                Err(e) => error_parts.push(format!("المنتجات النهائية: {e}")),
            }

            if !error_parts.is_empty() {
                app.set_inventory_error(SharedString::from(error_parts.join(" | ")));
            }

            app.set_inventory_loading(false);
        });
    });
}

/// Fetch recipes from the core engine and populate Slint properties.
fn refresh_recipes_data(
    app_weak: slint::Weak<AppWindow>,
    core: Arc<SweetLabCore>,
    session_token: Arc<Mutex<Option<Uuid>>>,
    rt_handle: tokio::runtime::Handle,
) {
    let token = {
        let guard = session_token.lock().unwrap();
        match *guard {
            Some(t) => t,
            None => return,
        }
    };

    // Set loading state
    let app_weak_loading = app_weak.clone();
    let _ = slint::invoke_from_event_loop(move || {
        if let Some(app) = app_weak_loading.upgrade() {
            app.set_recipes_loading(true);
            app.set_recipes_error(SharedString::default());
            app.set_recipes_success(SharedString::default());
        }
    });

    std::thread::spawn(move || {
        let result = rt_handle.block_on(core.get_recipes(token, None));

        let _ = slint::invoke_from_event_loop(move || {
            let Some(app) = app_weak.upgrade() else { return };

            match result {
                Ok(recipes) => {
                    let items: Vec<RecipeItem> = recipes
                        .iter()
                        .map(|r| RecipeItem {
                            id: SharedString::from(r.id.to_string()),
                            name: SharedString::from(&r.name),
                            finished_good_name: SharedString::from(&r.finished_good_name),
                            ingredient_count: r.ingredients.len() as i32,
                        })
                        .collect();
                    app.set_recipe_list(ModelRc::new(VecModel::from(items)));
                }
                Err(e) => {
                    app.set_recipes_error(SharedString::from(format!("فشل تحميل الوصفات: {e}")));
                }
            }

            app.set_recipes_loading(false);
        });
    });
}

/// Fetch wallets from the core engine and populate Slint properties.
fn refresh_wallets_data(
    app_weak: slint::Weak<AppWindow>,
    core: Arc<SweetLabCore>,
    session_token: Arc<Mutex<Option<Uuid>>>,
    rt_handle: tokio::runtime::Handle,
) {
    let token = {
        let guard = session_token.lock().unwrap();
        match *guard {
            Some(t) => t,
            None => return,
        }
    };

    // Set loading state
    let app_weak_loading = app_weak.clone();
    let _ = slint::invoke_from_event_loop(move || {
        if let Some(app) = app_weak_loading.upgrade() {
            app.set_wallets_loading(true);
            app.set_wallets_error(SharedString::default());
            app.set_wallets_success(SharedString::default());
        }
    });

    std::thread::spawn(move || {
        let result = rt_handle.block_on(core.get_wallets(token, None));

        let _ = slint::invoke_from_event_loop(move || {
            let Some(app) = app_weak.upgrade() else { return };

            match result {
                Ok(wallets) => {
                    let items: Vec<WalletItem> = wallets
                        .iter()
                        .map(|w| WalletItem {
                            id: SharedString::from(w.id.to_string()),
                            name: SharedString::from(&w.name),
                            wallet_type: SharedString::from(format!("{:?}", w.wallet_type)),
                            balance: SharedString::from(w.current_balance.to_display()),
                        })
                        .collect();
                    app.set_wallet_list(ModelRc::new(VecModel::from(items)));
                }
                Err(e) => {
                    app.set_wallets_error(SharedString::from(format!("فشل تحميل المحافظ: {e}")));
                }
            }

            app.set_wallets_loading(false);
        });
    });
}

/// Fetch transaction history for a specific wallet.
fn load_wallet_transactions(
    app_weak: slint::Weak<AppWindow>,
    core: Arc<SweetLabCore>,
    session_token: Arc<Mutex<Option<Uuid>>>,
    rt_handle: tokio::runtime::Handle,
    wallet_id: Uuid,
) {
    let token = {
        let guard = session_token.lock().unwrap();
        match *guard {
            Some(t) => t,
            None => return,
        }
    };

    // Set loading state
    let app_weak_loading = app_weak.clone();
    let _ = slint::invoke_from_event_loop(move || {
        if let Some(app) = app_weak_loading.upgrade() {
            app.set_wallets_loading(true);
            app.set_wallets_error(SharedString::default());
        }
    });

    std::thread::spawn(move || {
        let result = rt_handle.block_on(core.get_transaction_history(token, wallet_id, None));

        let _ = slint::invoke_from_event_loop(move || {
            let Some(app) = app_weak.upgrade() else { return };

            match result {
                Ok(transactions) => {
                    let items: Vec<TransactionItem> = transactions
                        .iter()
                        .map(|t| TransactionItem {
                            id: SharedString::from(t.id.to_string()),
                            wallet_id: SharedString::from(t.wallet_id.to_string()),
                            amount: SharedString::from(t.amount.to_display()),
                            description: SharedString::from(&t.description),
                            timestamp: SharedString::from(t.timestamp.format("%Y-%m-%d %H:%M").to_string()),
                        })
                        .collect();
                    app.set_wallet_transaction_list(ModelRc::new(VecModel::from(items)));
                }
                Err(e) => {
                    app.set_wallets_error(SharedString::from(format!("فشل تحميل المعاملات: {e}")));
                }
            }

            app.set_wallets_loading(false);
        });
    });
}

/// Fetch reports data (financial summary, inventory report, debt aging) from the core engine.
fn refresh_reports_data(
    app_weak: slint::Weak<AppWindow>,
    core: Arc<SweetLabCore>,
    session_token: Arc<Mutex<Option<Uuid>>>,
    rt_handle: tokio::runtime::Handle,
) {
    let token = {
        let guard = session_token.lock().unwrap();
        match *guard {
            Some(t) => t,
            None => return,
        }
    };

    // Set loading state
    let app_weak_loading = app_weak.clone();
    let _ = slint::invoke_from_event_loop(move || {
        if let Some(app) = app_weak_loading.upgrade() {
            app.set_reports_loading(true);
            app.set_reports_error(SharedString::default());
        }
    });

    std::thread::spawn(move || {
        // Use a wide date range to capture all data
        let end_date = Utc::now();
        let start_date = end_date - chrono::Duration::days(365 * 10);

        let financial_result =
            rt_handle.block_on(core.get_financial_summary(token, start_date, end_date));
        let inventory_result = rt_handle.block_on(core.get_inventory_report(token, 10.0));
        let debt_result = rt_handle.block_on(core.get_debt_aging_report(token, None));

        let _ = slint::invoke_from_event_loop(move || {
            let Some(app) = app_weak.upgrade() else { return };

            let mut error_parts: Vec<String> = Vec::new();

            // Populate financial summary
            match financial_result {
                Ok(summary) => {
                    app.set_reports_total_revenue(SharedString::from(
                        summary.total_revenue.to_display(),
                    ));
                    app.set_reports_total_expenses(SharedString::from(
                        summary.total_expenses.to_display(),
                    ));
                    app.set_reports_net_profit(SharedString::from(
                        summary.net_profit.to_display(),
                    ));
                }
                Err(e) => error_parts.push(format!("الملخص المالي: {e}")),
            }

            // Populate inventory report
            match inventory_result {
                Ok(report) => {
                    let raw_items: Vec<InventoryReportItem> = report
                        .raw_materials
                        .iter()
                        .map(|r| InventoryReportItem {
                            name: SharedString::from(&r.material.name),
                            quantity: SharedString::from(format!(
                                "{:.2} {}",
                                r.material.current_quantity, r.material.unit
                            )),
                            is_low_stock: r.is_low_stock,
                        })
                        .collect();
                    app.set_reports_raw_materials(ModelRc::new(VecModel::from(raw_items)));

                    let fg_items: Vec<InventoryReportItem> = report
                        .finished_goods
                        .iter()
                        .map(|f| InventoryReportItem {
                            name: SharedString::from(&f.good.name),
                            quantity: SharedString::from(format!("{:.2}", f.good.current_quantity)),
                            is_low_stock: f.is_low_stock,
                        })
                        .collect();
                    app.set_reports_finished_goods(ModelRc::new(VecModel::from(fg_items)));
                }
                Err(e) => error_parts.push(format!("تقرير المخزون: {e}")),
            }

            // Populate debt aging report
            match debt_result {
                Ok(debts) => {
                    let items: Vec<DebtAgingItem> = debts
                        .iter()
                        .map(|d| DebtAgingItem {
                            customer_name: SharedString::from(&d.customer_name),
                            amount: SharedString::from(d.remaining_amount.to_display()),
                            overdue_days: d.overdue_days,
                            is_critical: d.is_critical,
                        })
                        .collect();
                    app.set_reports_debt_aging(ModelRc::new(VecModel::from(items)));
                }
                Err(e) => error_parts.push(format!("تقرير الديون: {e}")),
            }

            if !error_parts.is_empty() {
                app.set_reports_error(SharedString::from(error_parts.join(" | ")));
            }

            app.set_reports_loading(false);
        });
    });
}

/// Fetch recipe availability from the core engine and populate Slint properties.
fn refresh_production_data(
    app_weak: slint::Weak<AppWindow>,
    core: Arc<SweetLabCore>,
    session_token: Arc<Mutex<Option<Uuid>>>,
    rt_handle: tokio::runtime::Handle,
) {
    let token = {
        let guard = session_token.lock().unwrap();
        match *guard {
            Some(t) => t,
            None => return,
        }
    };

    // Set loading state
    let app_weak_loading = app_weak.clone();
    let _ = slint::invoke_from_event_loop(move || {
        if let Some(app) = app_weak_loading.upgrade() {
            app.set_production_loading(true);
            app.set_production_error(SharedString::default());
        }
    });

    std::thread::spawn(move || {
        let result = rt_handle.block_on(core.get_recipe_availability(token, None));

        let _ = slint::invoke_from_event_loop(move || {
            let Some(app) = app_weak.upgrade() else { return };

            match result {
                Ok(availability) => {
                    let items: Vec<RecipeAvailabilityItem> = availability
                        .iter()
                        .map(|ra| RecipeAvailabilityItem {
                            recipe_id: SharedString::from(ra.recipe.id.to_string()),
                            recipe_name: SharedString::from(&ra.recipe.name),
                            finished_good_name: SharedString::from(&ra.recipe.finished_good_name),
                            max_producible: ra.max_producible,
                            status: SharedString::from(if ra.max_producible > 0 {
                                "متاح"
                            } else {
                                "غير متاح"
                            }),
                            insufficient_materials: SharedString::from(
                                ra.insufficient_materials.join("، "),
                            ),
                        })
                        .collect();
                    app.set_production_recipe_availability(ModelRc::new(VecModel::from(items)));
                }
                Err(e) => {
                    app.set_production_error(SharedString::from(format!(
                        "فشل تحميل الوصفات: {e}"
                    )));
                }
            }

            app.set_production_loading(false);
        });
    });
}

/// Fetch sales data (customers, finished goods, wallets, sales history) for the representative sales screen.
fn refresh_sales_data(
    app_weak: slint::Weak<AppWindow>,
    core: Arc<SweetLabCore>,
    session_token: Arc<Mutex<Option<Uuid>>>,
    rt_handle: tokio::runtime::Handle,
) {
    let token = {
        let guard = session_token.lock().unwrap();
        match *guard {
            Some(t) => t,
            None => return,
        }
    };

    // Set loading state on the UI thread
    let app_weak_loading = app_weak.clone();
    let _ = slint::invoke_from_event_loop(move || {
        if let Some(app) = app_weak_loading.upgrade() {
            app.set_sales_loading(true);
            app.set_sales_error(SharedString::default());
        }
    });

    std::thread::spawn(move || {
        let customers_result = rt_handle.block_on(core.get_customers(token, None));
        let fg_result = rt_handle.block_on(core.get_finished_goods(token, None));
        let wallets_result = rt_handle.block_on(core.get_wallets(token, None));
        let history_result = rt_handle.block_on(core.get_sales_history(token, None));

        let _ = slint::invoke_from_event_loop(move || {
            let Some(app) = app_weak.upgrade() else { return };

            let mut error_parts: Vec<String> = Vec::new();

            match customers_result {
                Ok(customers) => {
                    let items: Vec<SalesCustomerItem> = customers
                        .iter()
                        .map(|c| SalesCustomerItem {
                            id: SharedString::from(c.id.to_string()),
                            name: SharedString::from(&c.name),
                            city: SharedString::from(&c.city),
                        })
                        .collect();
                    app.set_sales_customer_list(ModelRc::new(VecModel::from(items)));
                }
                Err(e) => error_parts.push(format!("العملاء: {e}")),
            }

            match fg_result {
                Ok(goods) => {
                    let items: Vec<SalesFinishedGoodItem> = goods
                        .iter()
                        .map(|g| SalesFinishedGoodItem {
                            id: SharedString::from(g.id.to_string()),
                            name: SharedString::from(&g.name),
                            unit_price: SharedString::from(g.unit_price.to_display()),
                            current_quantity: SharedString::from(format!("{:.2}", g.current_quantity)),
                        })
                        .collect();
                    app.set_sales_finished_goods_list(ModelRc::new(VecModel::from(items)));
                }
                Err(e) => error_parts.push(format!("المنتجات: {e}")),
            }

            match wallets_result {
                Ok(wallets) => {
                    let items: Vec<SalesWalletItem> = wallets
                        .iter()
                        .map(|w| SalesWalletItem {
                            id: SharedString::from(w.id.to_string()),
                            name: SharedString::from(&w.name),
                        })
                        .collect();
                    app.set_sales_wallet_list(ModelRc::new(VecModel::from(items)));
                }
                Err(e) => error_parts.push(format!("المحافظ: {e}")),
            }

            match history_result {
                Ok(sales) => {
                    let items: Vec<SaleHistoryItem> = sales
                        .iter()
                        .map(|s| SaleHistoryItem {
                            id: SharedString::from(s.id.to_string()),
                            customer_name: SharedString::from(&s.customer_name),
                            total_amount: SharedString::from(s.total_amount.to_display()),
                            amount_paid: SharedString::from(s.amount_paid.to_display()),
                            timestamp: SharedString::from(s.timestamp.format("%Y-%m-%d %H:%M").to_string()),
                        })
                        .collect();
                    app.set_sales_history(ModelRc::new(VecModel::from(items)));
                }
                Err(e) => error_parts.push(format!("سجل المبيعات: {e}")),
            }

            if !error_parts.is_empty() {
                app.set_sales_error(SharedString::from(error_parts.join(" | ")));
            }

            app.set_sales_loading(false);
        });
    });
}

// -- Refresh customers data (Req 24.10) --
fn refresh_customers_data(
    app_weak: slint::Weak<AppWindow>,
    core: Arc<SweetLabCore>,
    session_token: Arc<Mutex<Option<Uuid>>>,
    rt_handle: tokio::runtime::Handle,
) {
    let token = {
        let guard = session_token.lock().unwrap();
        match *guard {
            Some(t) => t,
            None => return,
        }
    };

    // Set loading state on the UI thread
    let app_weak_loading = app_weak.clone();
    let _ = slint::invoke_from_event_loop(move || {
        if let Some(app) = app_weak_loading.upgrade() {
            app.set_customers_loading(true);
            app.set_customers_error(SharedString::default());
        }
    });

    std::thread::spawn(move || {
        let customers_result = rt_handle.block_on(core.get_customers(token, None));

        let _ = slint::invoke_from_event_loop(move || {
            let Some(app) = app_weak.upgrade() else { return };

            match customers_result {
                Ok(customers) => {
                    let items: Vec<CustomerItem> = customers
                        .iter()
                        .map(|c| CustomerItem {
                            id: SharedString::from(c.id.to_string()),
                            name: SharedString::from(&c.name),
                            city: SharedString::from(&c.city),
                            phone: SharedString::from(&c.mobile),
                            reliability_rating: c.reliability_rating,
                            total_debt: SharedString::from(c.total_debt.to_display()),
                        })
                        .collect();
                    app.set_customers_list(ModelRc::new(VecModel::from(items)));
                }
                Err(e) => {
                    app.set_customers_error(SharedString::from(format!("فشل تحميل العملاء: {e}")));
                }
            }

            app.set_customers_loading(false);
        });
    });
}

// -- Search customers (Req 24.10) --
fn search_customers_data(
    app_weak: slint::Weak<AppWindow>,
    core: Arc<SweetLabCore>,
    session_token: Arc<Mutex<Option<Uuid>>>,
    rt_handle: tokio::runtime::Handle,
    query: String,
) {
    let token = {
        let guard = session_token.lock().unwrap();
        match *guard {
            Some(t) => t,
            None => return,
        }
    };

    // Set loading state
    let app_weak_loading = app_weak.clone();
    let _ = slint::invoke_from_event_loop(move || {
        if let Some(app) = app_weak_loading.upgrade() {
            app.set_customers_loading(true);
            app.set_customers_error(SharedString::default());
        }
    });

    std::thread::spawn(move || {
        let result = if query.trim().is_empty() {
            rt_handle.block_on(core.get_customers(token, None))
        } else {
            rt_handle.block_on(core.search_customers(token, query, None))
        };

        let _ = slint::invoke_from_event_loop(move || {
            let Some(app) = app_weak.upgrade() else { return };

            match result {
                Ok(customers) => {
                    let items: Vec<CustomerItem> = customers
                        .iter()
                        .map(|c| CustomerItem {
                            id: SharedString::from(c.id.to_string()),
                            name: SharedString::from(&c.name),
                            city: SharedString::from(&c.city),
                            phone: SharedString::from(&c.mobile),
                            reliability_rating: c.reliability_rating,
                            total_debt: SharedString::from(c.total_debt.to_display()),
                        })
                        .collect();
                    app.set_customers_list(ModelRc::new(VecModel::from(items)));
                }
                Err(e) => {
                    app.set_customers_error(SharedString::from(format!("فشل البحث: {e}")));
                }
            }

            app.set_customers_loading(false);
        });
    });
}

fn refresh_expenses_data(
    app_weak: slint::Weak<AppWindow>,
    core: Arc<SweetLabCore>,
    session_token: Arc<Mutex<Option<Uuid>>>,
    rt_handle: tokio::runtime::Handle,
) {
    let token = {
        let guard = session_token.lock().unwrap();
        match *guard {
            Some(t) => t,
            None => return,
        }
    };

    // Set loading state on the UI thread
    let app_weak_loading = app_weak.clone();
    let _ = slint::invoke_from_event_loop(move || {
        if let Some(app) = app_weak_loading.upgrade() {
            app.set_expenses_loading(true);
            app.set_expenses_error(SharedString::default());
        }
    });

    std::thread::spawn(move || {
        // Fetch expenses for the last 90 days
        let now = Utc::now();
        let start = now - chrono::Duration::days(90);
        let expenses_result = rt_handle.block_on(core.get_expenses(token, start, now, None));
        // Also fetch wallets for the wallet selector
        let wallets_result = rt_handle.block_on(core.get_wallets(token, None));

        let _ = slint::invoke_from_event_loop(move || {
            let Some(app) = app_weak.upgrade() else { return };

            match expenses_result {
                Ok(expenses) => {
                    let items: Vec<ExpenseItem> = expenses
                        .iter()
                        .map(|e| {
                            let cat_display = match e.category {
                                sweet_lab_core::ExpenseCategory::Purchase => "مشتريات",
                                sweet_lab_core::ExpenseCategory::OperatingCost => "تكاليف تشغيلية",
                            };
                            ExpenseItem {
                                id: SharedString::from(e.id.to_string()),
                                description: SharedString::from(&e.description),
                                amount: SharedString::from(e.amount.to_display()),
                                category: SharedString::from(cat_display),
                                wallet_name: SharedString::from(&e.wallet_name),
                                timestamp: SharedString::from(e.timestamp.format("%Y-%m-%d %H:%M").to_string()),
                            }
                        })
                        .collect();
                    app.set_expenses_list(ModelRc::new(VecModel::from(items)));
                }
                Err(e) => {
                    app.set_expenses_error(SharedString::from(format!("فشل تحميل المصروفات: {e}")));
                }
            }

            // Populate wallet list for the expense form
            if let Ok(wallets) = wallets_result {
                let wallet_items: Vec<ExpenseWalletItem> = wallets
                    .iter()
                    .map(|w| ExpenseWalletItem {
                        id: SharedString::from(w.id.to_string()),
                        name: SharedString::from(&w.name),
                        balance: SharedString::from(w.current_balance.to_display()),
                    })
                    .collect();
                let wallet_names: Vec<SharedString> = wallets
                    .iter()
                    .map(|w| SharedString::from(&w.name))
                    .collect();
                app.set_expense_wallet_list(ModelRc::new(VecModel::from(wallet_items)));
                app.set_expense_wallet_names(ModelRc::new(VecModel::from(wallet_names)));
            }

            app.set_expenses_loading(false);
        });
    });
}

fn refresh_payments_data(
    app_weak: slint::Weak<AppWindow>,
    core: Arc<SweetLabCore>,
    session_token: Arc<Mutex<Option<Uuid>>>,
    rt_handle: tokio::runtime::Handle,
) {
    let token = {
        let guard = session_token.lock().unwrap();
        match *guard {
            Some(t) => t,
            None => return,
        }
    };

    // Set loading state on the UI thread
    let app_weak_loading = app_weak.clone();
    let _ = slint::invoke_from_event_loop(move || {
        if let Some(app) = app_weak_loading.upgrade() {
            app.set_payments_loading(true);
            app.set_payments_error(SharedString::default());
        }
    });

    std::thread::spawn(move || {
        // Fetch active debts, wallets, and customers with debt info
        let debts_result = rt_handle.block_on(core.get_active_debts(token, None));
        let wallets_result = rt_handle.block_on(core.get_wallets(token, None));
        let customers_result = rt_handle.block_on(core.get_customers(token, None));

        let _ = slint::invoke_from_event_loop(move || {
            let Some(app) = app_weak.upgrade() else { return };

            match debts_result {
                Ok(debts) => {
                    let items: Vec<DebtItem> = debts
                        .iter()
                        .map(|d| DebtItem {
                            id: SharedString::from(d.id.to_string()),
                            customer_id: SharedString::from(d.customer_id.to_string()),
                            customer_name: SharedString::from(&d.customer_name),
                            sale_date: SharedString::from(d.sale_date.format("%Y-%m-%d").to_string()),
                            original_amount: SharedString::from(d.original_amount.to_display()),
                            remaining_amount: SharedString::from(d.remaining_amount.to_display()),
                            overdue_days: d.overdue_days,
                            is_critical: d.is_critical,
                        })
                        .collect();
                    app.set_payments_debts_list(ModelRc::new(VecModel::from(items)));
                }
                Err(e) => {
                    app.set_payments_error(SharedString::from(format!("فشل تحميل الديون: {e}")));
                }
            }

            // Populate wallet list for the payment form
            if let Ok(wallets) = wallets_result {
                let wallet_items: Vec<PaymentWalletItem> = wallets
                    .iter()
                    .map(|w| PaymentWalletItem {
                        id: SharedString::from(w.id.to_string()),
                        name: SharedString::from(&w.name),
                        balance: SharedString::from(w.current_balance.to_display()),
                    })
                    .collect();
                let wallet_names: Vec<SharedString> = wallets
                    .iter()
                    .map(|w| SharedString::from(&w.name))
                    .collect();
                app.set_payment_wallet_list(ModelRc::new(VecModel::from(wallet_items)));
                app.set_payment_wallet_names(ModelRc::new(VecModel::from(wallet_names)));
            }

            // Populate customer list (only those with debt > 0)
            if let Ok(customers) = customers_result {
                let cust_items: Vec<PaymentCustomerItem> = customers
                    .iter()
                    .filter(|c| c.total_debt.0 > 0)
                    .map(|c| PaymentCustomerItem {
                        id: SharedString::from(c.id.to_string()),
                        name: SharedString::from(&c.name),
                        total_debt: SharedString::from(c.total_debt.to_display()),
                        overdue_days: c.overdue_days,
                    })
                    .collect();
                let cust_names: Vec<SharedString> = cust_items
                    .iter()
                    .map(|c| c.name.clone())
                    .collect();
                app.set_payment_customer_list(ModelRc::new(VecModel::from(cust_items)));
                app.set_payment_customer_names(ModelRc::new(VecModel::from(cust_names)));
            }

            app.set_payments_loading(false);
        });
    });
}

fn main() {
    // Create Tokio runtime for async bridging to the core engine
    let rt = tokio::runtime::Runtime::new().unwrap();

    // Initialize SweetLabCore with SQLite DB, business name, no sync URL, no custom font dir
    let core = rt.block_on(async {
        SweetLabCore::new(
            "sweet_lab.db".to_string(),
            "Sweet Lab".to_string(),
            None,
            None,
        )
        .await
        .unwrap()
    });
    let core = Arc::new(core);

    // Shared session token for use across callbacks
    let session_token: Arc<Mutex<Option<Uuid>>> = Arc::new(Mutex::new(None));
    // Shared user ID for use in callbacks that need the logged-in user's ID (e.g., chef_id)
    let user_id: Arc<Mutex<Option<Uuid>>> = Arc::new(Mutex::new(None));

    let app = AppWindow::new().unwrap();

    // -- Login callback --
    // Req 1.1: Authenticate and create session with corresponding role
    // Req 1.2: Generic error message — does not reveal which field is wrong
    // Req 1.3: Role-based redirect after login
    let app_weak = app.as_weak();
    let core_login = core.clone();
    let session_login = session_token.clone();
    let user_id_login = user_id.clone();
    let rt_handle = rt.handle().clone();
    // Extra clones for triggering dashboard refresh after admin login
    let core_login_dash = core.clone();
    let session_login_dash = session_token.clone();
    let rt_handle_login_dash = rt.handle().clone();
    app.on_login(move |username, password| {
        let app = app_weak.unwrap();
        let username = username.to_string();
        let password = password.to_string();

        // Show loading state
        app.set_login_loading(true);
        app.set_login_error("".into());

        let core_ref = core_login.clone();
        let session_ref = session_login.clone();
        let user_id_ref = user_id_login.clone();
        let app_weak2 = app.as_weak();
        let handle = rt_handle.clone();
        // Clones for dashboard auto-refresh after admin login
        let core_dash_ref = core_login_dash.clone();
        let session_dash_ref = session_login_dash.clone();
        let handle_dash_ref = rt_handle_login_dash.clone();

        // Spawn a background thread to run the async login call
        // so we don't block the Slint UI event loop.
        std::thread::spawn(move || {
            let result = handle.block_on(core_ref.login(username, password));

            // Marshal the result back to the Slint UI thread
            let _ = slint::invoke_from_event_loop(move || {
                let app = app_weak2.unwrap();

                match result {
                    Ok(session) => {
                        // Store session token for use in subsequent API calls
                        if let Ok(mut token) = session_ref.lock() {
                            *token = Some(session.session_id);
                        }
                        // Store user ID for callbacks that need it (e.g., chef_id)
                        if let Ok(mut uid) = user_id_ref.lock() {
                            *uid = Some(session.user_id);
                        }

                        // Route to role-appropriate screen
                        match session.role {
                            sweet_lab_core::UserRole::Admin => {
                                app.set_current_role(UserRole::Admin);
                                app.set_current_view(ActiveView::AdminDashboard);
                                // Auto-refresh dashboard data on admin login
                                refresh_dashboard_data(
                                    app.as_weak(),
                                    core_dash_ref.clone(),
                                    session_dash_ref.clone(),
                                    handle_dash_ref.clone(),
                                );
                            }
                            sweet_lab_core::UserRole::Chef => {
                                app.set_current_role(UserRole::Chef);
                                app.set_current_view(ActiveView::ChefProduction);
                                // Auto-refresh production data on chef login
                                refresh_production_data(
                                    app.as_weak(),
                                    core_dash_ref.clone(),
                                    session_dash_ref.clone(),
                                    handle_dash_ref.clone(),
                                );
                            }
                            sweet_lab_core::UserRole::Representative => {
                                app.set_current_role(UserRole::Representative);
                                app.set_current_view(ActiveView::RepresentativeSales);
                                // Auto-refresh sales data on representative login
                                refresh_sales_data(
                                    app.as_weak(),
                                    core_dash_ref.clone(),
                                    session_dash_ref.clone(),
                                    handle_dash_ref.clone(),
                                );
                            }
                        }
                        app.set_login_error("".into());
                    }
                    Err(_) => {
                        // Req 1.2: Generic error — does not reveal which field is incorrect
                        app.set_login_error("بيانات الدخول غير صحيحة".into());
                    }
                }

                app.set_login_loading(false);
            });
        });
    });

    // -- Logout callback --
    let app_weak = app.as_weak();
    let session_logout = session_token.clone();
    let user_id_logout = user_id.clone();
    let core_logout = core.clone();
    let rt_handle_logout = rt.handle().clone();
    app.on_logout(move || {
        // Read and clear the session token
        let token = {
            let mut guard = session_logout.lock().unwrap();
            guard.take()
        };
        // Clear user ID
        if let Ok(mut uid) = user_id_logout.lock() {
            *uid = None;
        }

        if let Some(session_id) = token {
            let core_ref = core_logout.clone();
            let handle = rt_handle_logout.clone();

            // Fire-and-forget: call core.logout() on a background thread
            std::thread::spawn(move || {
                let _ = handle.block_on(core_ref.logout(session_id));
            });
        }

        // Reset UI to login screen immediately
        let app = app_weak.unwrap();
        app.set_current_view(ActiveView::Login);
        app.set_current_role(UserRole::None);
        app.set_login_error("".into());
        app.set_login_loading(false);
        app.set_nav_index(0);
    });

    // -- Navigation callback --
    let app_weak = app.as_weak();
    let core_nav = core.clone();
    let session_nav = session_token.clone();
    let rt_handle_nav = rt.handle().clone();
    let core_nav_users = core.clone();
    let session_nav_users = session_token.clone();
    let rt_handle_nav_users = rt.handle().clone();
    let core_nav_inventory = core.clone();
    let session_nav_inventory = session_token.clone();
    let rt_handle_nav_inventory = rt.handle().clone();
    let core_nav_recipes = core.clone();
    let session_nav_recipes = session_token.clone();
    let rt_handle_nav_recipes = rt.handle().clone();
    let core_nav_wallets = core.clone();
    let session_nav_wallets = session_token.clone();
    let rt_handle_nav_wallets = rt.handle().clone();
    let core_nav_reports = core.clone();
    let session_nav_reports = session_token.clone();
    let rt_handle_nav_reports = rt.handle().clone();
    let core_nav_production = core.clone();
    let session_nav_production = session_token.clone();
    let rt_handle_nav_production = rt.handle().clone();
    let core_nav_sales = core.clone();
    let session_nav_sales = session_token.clone();
    let rt_handle_nav_sales = rt.handle().clone();
    let core_nav_customers = core.clone();
    let session_nav_customers = session_token.clone();
    let rt_handle_nav_customers = rt.handle().clone();
    let core_nav_expenses = core.clone();
    let session_nav_expenses = session_token.clone();
    let rt_handle_nav_expenses = rt.handle().clone();
    let core_nav_payments = core.clone();
    let session_nav_payments = session_token.clone();
    let rt_handle_nav_payments = rt.handle().clone();
    app.on_navigate(move |index| {
        let app = app_weak.unwrap();
        app.set_nav_index(index);

        // Auto-refresh dashboard when admin navigates to it (nav_index 0)
        if app.get_current_view() == ActiveView::AdminDashboard && index == 0 {
            refresh_dashboard_data(
                app.as_weak(),
                core_nav.clone(),
                session_nav.clone(),
                rt_handle_nav.clone(),
            );
        }

        // Auto-refresh user list when admin navigates to user management (nav_index 1)
        if app.get_current_view() == ActiveView::AdminDashboard && index == 1 {
            refresh_user_list(
                app.as_weak(),
                core_nav_users.clone(),
                session_nav_users.clone(),
                rt_handle_nav_users.clone(),
            );
        }

        // Auto-refresh inventory when admin navigates to inventory (nav_index 2)
        // or when chef navigates to inventory (nav_index 1)
        if (app.get_current_view() == ActiveView::AdminDashboard && index == 2)
            || (app.get_current_view() == ActiveView::ChefProduction && index == 1)
        {
            refresh_inventory_data(
                app.as_weak(),
                core_nav_inventory.clone(),
                session_nav_inventory.clone(),
                rt_handle_nav_inventory.clone(),
            );
        }

        // Auto-refresh recipes when admin navigates to recipes (nav_index 3)
        if app.get_current_view() == ActiveView::AdminDashboard && index == 3 {
            refresh_recipes_data(
                app.as_weak(),
                core_nav_recipes.clone(),
                session_nav_recipes.clone(),
                rt_handle_nav_recipes.clone(),
            );
        }

        // Auto-refresh wallets when admin navigates to wallets (nav_index 4)
        if app.get_current_view() == ActiveView::AdminDashboard && index == 4 {
            refresh_wallets_data(
                app.as_weak(),
                core_nav_wallets.clone(),
                session_nav_wallets.clone(),
                rt_handle_nav_wallets.clone(),
            );
        }

        // Auto-refresh reports when admin navigates to reports (nav_index 5)
        if app.get_current_view() == ActiveView::AdminDashboard && index == 5 {
            refresh_reports_data(
                app.as_weak(),
                core_nav_reports.clone(),
                session_nav_reports.clone(),
                rt_handle_nav_reports.clone(),
            );
        }

        // Auto-refresh production data when chef navigates to production (nav_index 0)
        if app.get_current_view() == ActiveView::ChefProduction && index == 0 {
            refresh_production_data(
                app.as_weak(),
                core_nav_production.clone(),
                session_nav_production.clone(),
                rt_handle_nav_production.clone(),
            );
        }

        // Auto-refresh sales data when representative navigates to sales (nav_index 0)
        if app.get_current_view() == ActiveView::RepresentativeSales && index == 0 {
            refresh_sales_data(
                app.as_weak(),
                core_nav_sales.clone(),
                session_nav_sales.clone(),
                rt_handle_nav_sales.clone(),
            );
        }

        // Auto-refresh customers data when representative navigates to customers (nav_index 1)
        if app.get_current_view() == ActiveView::RepresentativeSales && index == 1 {
            refresh_customers_data(
                app.as_weak(),
                core_nav_customers.clone(),
                session_nav_customers.clone(),
                rt_handle_nav_customers.clone(),
            );
        }

        // Auto-refresh expenses data when representative navigates to expenses (nav_index 2)
        if app.get_current_view() == ActiveView::RepresentativeSales && index == 2 {
            refresh_expenses_data(
                app.as_weak(),
                core_nav_expenses.clone(),
                session_nav_expenses.clone(),
                rt_handle_nav_expenses.clone(),
            );
        }

        // Auto-refresh payments data when representative navigates to payments (nav_index 3)
        if app.get_current_view() == ActiveView::RepresentativeSales && index == 3 {
            refresh_payments_data(
                app.as_weak(),
                core_nav_payments.clone(),
                session_nav_payments.clone(),
                rt_handle_nav_payments.clone(),
            );
        }
    });

    // -- Dashboard refresh callback (Req 24.3) --
    let app_weak = app.as_weak();
    let core_dash = core.clone();
    let session_dash = session_token.clone();
    let rt_handle_dash = rt.handle().clone();
    app.on_refresh_dashboard(move || {
        refresh_dashboard_data(
            app_weak.clone(),
            core_dash.clone(),
            session_dash.clone(),
            rt_handle_dash.clone(),
        );
    });

    // -- User management: refresh users callback (Req 24.4) --
    let app_weak = app.as_weak();
    let core_users_refresh = core.clone();
    let session_users_refresh = session_token.clone();
    let rt_handle_users_refresh = rt.handle().clone();
    app.on_refresh_users(move || {
        refresh_user_list(
            app_weak.clone(),
            core_users_refresh.clone(),
            session_users_refresh.clone(),
            rt_handle_users_refresh.clone(),
        );
    });

    // -- Inventory: refresh callback (Req 24.3) --
    let app_weak = app.as_weak();
    let core_inv_refresh = core.clone();
    let session_inv_refresh = session_token.clone();
    let rt_handle_inv_refresh = rt.handle().clone();
    app.on_refresh_inventory(move || {
        refresh_inventory_data(
            app_weak.clone(),
            core_inv_refresh.clone(),
            session_inv_refresh.clone(),
            rt_handle_inv_refresh.clone(),
        );
    });

    // -- User management: create user callback (Req 24.4) --
    let app_weak = app.as_weak();
    let core_create_user = core.clone();
    let session_create_user = session_token.clone();
    let rt_handle_create_user = rt.handle().clone();
    // Extra clones for refreshing user list after creation
    let core_create_user_refresh = core.clone();
    let session_create_user_refresh = session_token.clone();
    let rt_handle_create_user_refresh = rt.handle().clone();
    app.on_create_user(move |username, password, full_name, role_ar| {
        let username = username.to_string();
        let password = password.to_string();
        let full_name = full_name.to_string();
        let role_ar = role_ar.to_string();

        let role = match parse_role_from_arabic(&role_ar) {
            Some(r) => r,
            None => {
                if let Some(app) = app_weak.upgrade() {
                    app.set_user_mgmt_error(SharedString::from("دور غير صالح"));
                }
                return;
            }
        };

        let token = {
            let guard = session_create_user.lock().unwrap();
            match *guard {
                Some(t) => t,
                None => return,
            }
        };

        // Set loading state
        let app_weak_loading = app_weak.clone();
        if let Some(app) = app_weak_loading.upgrade() {
            app.set_user_mgmt_loading(true);
            app.set_user_mgmt_error(SharedString::default());
            app.set_user_mgmt_success(SharedString::default());
        }

        let core_ref = core_create_user.clone();
        let app_weak2 = app_weak.clone();
        let handle = rt_handle_create_user.clone();
        let core_refresh = core_create_user_refresh.clone();
        let session_refresh = session_create_user_refresh.clone();
        let handle_refresh = rt_handle_create_user_refresh.clone();

        std::thread::spawn(move || {
            let result = handle.block_on(core_ref.create_user(
                token,
                username,
                password,
                full_name,
                role,
            ));

            let _ = slint::invoke_from_event_loop(move || {
                let Some(app) = app_weak2.upgrade() else { return };

                match result {
                    Ok(user) => {
                        app.set_user_mgmt_success(SharedString::from(
                            format!("تم إنشاء المستخدم: {}", user.username),
                        ));
                        // Refresh user list after successful creation
                        refresh_user_list(
                            app.as_weak(),
                            core_refresh,
                            session_refresh,
                            handle_refresh,
                        );
                    }
                    Err(e) => {
                        app.set_user_mgmt_error(SharedString::from(format!("فشل إنشاء المستخدم: {e}")));
                    }
                }

                app.set_user_mgmt_loading(false);
            });
        });
    });

    // -- User management: update user role callback (Req 24.4) --
    let app_weak = app.as_weak();
    let core_update_role = core.clone();
    let session_update_role = session_token.clone();
    let rt_handle_update_role = rt.handle().clone();
    // Extra clones for refreshing user list after role update
    let core_update_role_refresh = core.clone();
    let session_update_role_refresh = session_token.clone();
    let rt_handle_update_role_refresh = rt.handle().clone();
    app.on_update_user_role(move |user_id_str, role_ar| {
        let user_id_str = user_id_str.to_string();
        let role_ar = role_ar.to_string();

        let role = match parse_role_from_arabic(&role_ar) {
            Some(r) => r,
            None => {
                if let Some(app) = app_weak.upgrade() {
                    app.set_user_mgmt_error(SharedString::from("دور غير صالح"));
                }
                return;
            }
        };

        let user_id = match Uuid::parse_str(&user_id_str) {
            Ok(id) => id,
            Err(_) => {
                if let Some(app) = app_weak.upgrade() {
                    app.set_user_mgmt_error(SharedString::from("معرف مستخدم غير صالح"));
                }
                return;
            }
        };

        let token = {
            let guard = session_update_role.lock().unwrap();
            match *guard {
                Some(t) => t,
                None => return,
            }
        };

        // Set loading state
        let app_weak_loading = app_weak.clone();
        if let Some(app) = app_weak_loading.upgrade() {
            app.set_user_mgmt_loading(true);
            app.set_user_mgmt_error(SharedString::default());
            app.set_user_mgmt_success(SharedString::default());
        }

        let core_ref = core_update_role.clone();
        let app_weak2 = app_weak.clone();
        let handle = rt_handle_update_role.clone();
        let core_refresh = core_update_role_refresh.clone();
        let session_refresh = session_update_role_refresh.clone();
        let handle_refresh = rt_handle_update_role_refresh.clone();

        std::thread::spawn(move || {
            let result = handle.block_on(core_ref.update_user_role(token, user_id, role));

            let _ = slint::invoke_from_event_loop(move || {
                let Some(app) = app_weak2.upgrade() else { return };

                match result {
                    Ok(()) => {
                        app.set_user_mgmt_success(SharedString::from(
                            format!("تم تحديث دور المستخدم إلى: {}", role_ar),
                        ));
                        // Refresh user list after successful role update
                        refresh_user_list(
                            app.as_weak(),
                            core_refresh,
                            session_refresh,
                            handle_refresh,
                        );
                    }
                    Err(e) => {
                        app.set_user_mgmt_error(SharedString::from(format!("فشل تحديث الدور: {e}")));
                    }
                }

                app.set_user_mgmt_loading(false);
            });
        });
    });

    // -- Recipes: refresh callback (Req 24.6) --
    let app_weak = app.as_weak();
    let core_recipes_refresh = core.clone();
    let session_recipes_refresh = session_token.clone();
    let rt_handle_recipes_refresh = rt.handle().clone();
    app.on_refresh_recipes(move || {
        refresh_recipes_data(
            app_weak.clone(),
            core_recipes_refresh.clone(),
            session_recipes_refresh.clone(),
            rt_handle_recipes_refresh.clone(),
        );
    });

    // -- Recipes: create recipe callback (Req 24.6) --
    let app_weak = app.as_weak();
    let core_create_recipe = core.clone();
    let session_create_recipe = session_token.clone();
    let rt_handle_create_recipe = rt.handle().clone();
    // Extra clones for refreshing recipe list after creation
    let core_create_recipe_refresh = core.clone();
    let session_create_recipe_refresh = session_token.clone();
    let rt_handle_create_recipe_refresh = rt.handle().clone();
    app.on_create_recipe(move |name, finished_good_id_str, ingredients_json| {
        let name = name.to_string();
        let fg_id_str = finished_good_id_str.to_string();
        let ingredients_str = ingredients_json.to_string();

        // Parse finished_good_id
        let finished_good_id = match Uuid::parse_str(&fg_id_str) {
            Ok(id) => id,
            Err(_) => {
                if let Some(app) = app_weak.upgrade() {
                    app.set_recipes_error(SharedString::from("معرف المنتج النهائي غير صالح"));
                }
                return;
            }
        };

        // Parse ingredients JSON
        let ingredients: Vec<RecipeIngredient> = match serde_json::from_str(&ingredients_str) {
            Ok(v) => v,
            Err(_) => {
                if let Some(app) = app_weak.upgrade() {
                    app.set_recipes_error(SharedString::from("صيغة المكونات غير صالحة (JSON)"));
                }
                return;
            }
        };

        let token = {
            let guard = session_create_recipe.lock().unwrap();
            match *guard {
                Some(t) => t,
                None => return,
            }
        };

        // Set loading state
        if let Some(app) = app_weak.upgrade() {
            app.set_recipes_loading(true);
            app.set_recipes_error(SharedString::default());
            app.set_recipes_success(SharedString::default());
        }

        let core_ref = core_create_recipe.clone();
        let app_weak2 = app_weak.clone();
        let handle = rt_handle_create_recipe.clone();
        let core_refresh = core_create_recipe_refresh.clone();
        let session_refresh = session_create_recipe_refresh.clone();
        let handle_refresh = rt_handle_create_recipe_refresh.clone();

        std::thread::spawn(move || {
            let result = handle.block_on(core_ref.create_recipe(
                token,
                name,
                finished_good_id,
                ingredients,
            ));

            let _ = slint::invoke_from_event_loop(move || {
                let Some(app) = app_weak2.upgrade() else { return };

                match result {
                    Ok(recipe) => {
                        app.set_recipes_success(SharedString::from(
                            format!("تم إنشاء الوصفة: {}", recipe.name),
                        ));
                        // Refresh recipe list after successful creation
                        refresh_recipes_data(
                            app.as_weak(),
                            core_refresh,
                            session_refresh,
                            handle_refresh,
                        );
                    }
                    Err(e) => {
                        app.set_recipes_error(SharedString::from(format!("فشل إنشاء الوصفة: {e}")));
                    }
                }

                app.set_recipes_loading(false);
            });
        });
    });

    // -- Recipes: delete recipe callback (Req 24.6) --
    let app_weak = app.as_weak();
    let core_delete_recipe = core.clone();
    let session_delete_recipe = session_token.clone();
    let rt_handle_delete_recipe = rt.handle().clone();
    // Extra clones for refreshing recipe list after deletion
    let core_delete_recipe_refresh = core.clone();
    let session_delete_recipe_refresh = session_token.clone();
    let rt_handle_delete_recipe_refresh = rt.handle().clone();
    app.on_delete_recipe(move |recipe_id_str| {
        let recipe_id_str = recipe_id_str.to_string();

        let recipe_id = match Uuid::parse_str(&recipe_id_str) {
            Ok(id) => id,
            Err(_) => {
                if let Some(app) = app_weak.upgrade() {
                    app.set_recipes_error(SharedString::from("معرف الوصفة غير صالح"));
                }
                return;
            }
        };

        let token = {
            let guard = session_delete_recipe.lock().unwrap();
            match *guard {
                Some(t) => t,
                None => return,
            }
        };

        // Set loading state
        if let Some(app) = app_weak.upgrade() {
            app.set_recipes_loading(true);
            app.set_recipes_error(SharedString::default());
            app.set_recipes_success(SharedString::default());
        }

        let core_ref = core_delete_recipe.clone();
        let app_weak2 = app_weak.clone();
        let handle = rt_handle_delete_recipe.clone();
        let core_refresh = core_delete_recipe_refresh.clone();
        let session_refresh = session_delete_recipe_refresh.clone();
        let handle_refresh = rt_handle_delete_recipe_refresh.clone();

        std::thread::spawn(move || {
            let result = handle.block_on(core_ref.delete_recipe(token, recipe_id));

            let _ = slint::invoke_from_event_loop(move || {
                let Some(app) = app_weak2.upgrade() else { return };

                match result {
                    Ok(()) => {
                        app.set_recipes_success(SharedString::from("تم حذف الوصفة بنجاح"));
                        // Refresh recipe list after successful deletion
                        refresh_recipes_data(
                            app.as_weak(),
                            core_refresh,
                            session_refresh,
                            handle_refresh,
                        );
                    }
                    Err(e) => {
                        app.set_recipes_error(SharedString::from(format!("فشل حذف الوصفة: {e}")));
                    }
                }

                app.set_recipes_loading(false);
            });
        });
    });

    // -- Wallets: refresh callback (Req 24.5) --
    let app_weak = app.as_weak();
    let core_wallets_refresh = core.clone();
    let session_wallets_refresh = session_token.clone();
    let rt_handle_wallets_refresh = rt.handle().clone();
    app.on_refresh_wallets(move || {
        refresh_wallets_data(
            app_weak.clone(),
            core_wallets_refresh.clone(),
            session_wallets_refresh.clone(),
            rt_handle_wallets_refresh.clone(),
        );
    });

    // -- Wallets: load transaction history callback (Req 24.5) --
    let app_weak = app.as_weak();
    let core_load_tx = core.clone();
    let session_load_tx = session_token.clone();
    let rt_handle_load_tx = rt.handle().clone();
    app.on_load_wallet_transactions(move |wallet_id_str| {
        let wallet_id_str = wallet_id_str.to_string();

        let wallet_id = match Uuid::parse_str(&wallet_id_str) {
            Ok(id) => id,
            Err(_) => {
                if let Some(app) = app_weak.upgrade() {
                    app.set_wallets_error(SharedString::from("معرف المحفظة غير صالح"));
                }
                return;
            }
        };

        load_wallet_transactions(
            app_weak.clone(),
            core_load_tx.clone(),
            session_load_tx.clone(),
            rt_handle_load_tx.clone(),
            wallet_id,
        );
    });

    // -- Wallets: transfer funds callback (Req 24.5) --
    let app_weak = app.as_weak();
    let core_transfer = core.clone();
    let session_transfer = session_token.clone();
    let rt_handle_transfer = rt.handle().clone();
    // Extra clones for refreshing wallet list after transfer
    let core_transfer_refresh = core.clone();
    let session_transfer_refresh = session_token.clone();
    let rt_handle_transfer_refresh = rt.handle().clone();
    app.on_transfer_funds(move |source_id_str, dest_id_str, amount_str| {
        let source_str = source_id_str.to_string();
        let dest_str = dest_id_str.to_string();
        let amt_str = amount_str.to_string();

        let source_id = match Uuid::parse_str(&source_str) {
            Ok(id) => id,
            Err(_) => {
                if let Some(app) = app_weak.upgrade() {
                    app.set_wallets_error(SharedString::from("معرف المحفظة المصدر غير صالح"));
                }
                return;
            }
        };

        let dest_id = match Uuid::parse_str(&dest_str) {
            Ok(id) => id,
            Err(_) => {
                if let Some(app) = app_weak.upgrade() {
                    app.set_wallets_error(SharedString::from("معرف المحفظة الهدف غير صالح"));
                }
                return;
            }
        };

        let amount = match sweet_lab_core::Money::parse(&amt_str) {
            Ok(m) => m,
            Err(_) => {
                if let Some(app) = app_weak.upgrade() {
                    app.set_wallets_error(SharedString::from("المبلغ غير صالح"));
                }
                return;
            }
        };

        let token = {
            let guard = session_transfer.lock().unwrap();
            match *guard {
                Some(t) => t,
                None => return,
            }
        };

        // Set loading state
        if let Some(app) = app_weak.upgrade() {
            app.set_wallets_loading(true);
            app.set_wallets_error(SharedString::default());
            app.set_wallets_success(SharedString::default());
        }

        let core_ref = core_transfer.clone();
        let app_weak2 = app_weak.clone();
        let handle = rt_handle_transfer.clone();
        let core_refresh = core_transfer_refresh.clone();
        let session_refresh = session_transfer_refresh.clone();
        let handle_refresh = rt_handle_transfer_refresh.clone();

        std::thread::spawn(move || {
            let result = handle.block_on(core_ref.transfer_funds(token, source_id, dest_id, amount));

            let _ = slint::invoke_from_event_loop(move || {
                let Some(app) = app_weak2.upgrade() else { return };

                match result {
                    Ok(_transfer) => {
                        app.set_wallets_success(SharedString::from(
                            format!("تم التحويل بنجاح: {}", amount.to_display()),
                        ));
                        // Refresh wallet list after successful transfer
                        refresh_wallets_data(
                            app.as_weak(),
                            core_refresh,
                            session_refresh,
                            handle_refresh,
                        );
                    }
                    Err(e) => {
                        app.set_wallets_error(SharedString::from(format!("فشل التحويل: {e}")));
                    }
                }

                app.set_wallets_loading(false);
            });
        });
    });

    // -- Reports: refresh callback (Req 24.7) --
    let app_weak = app.as_weak();
    let core_reports_refresh = core.clone();
    let session_reports_refresh = session_token.clone();
    let rt_handle_reports_refresh = rt.handle().clone();
    app.on_refresh_reports(move || {
        refresh_reports_data(
            app_weak.clone(),
            core_reports_refresh.clone(),
            session_reports_refresh.clone(),
            rt_handle_reports_refresh.clone(),
        );
    });

    // -- Chef production: refresh callback (Req 24.8) --
    let app_weak = app.as_weak();
    let core_prod_refresh = core.clone();
    let session_prod_refresh = session_token.clone();
    let rt_handle_prod_refresh = rt.handle().clone();
    app.on_refresh_production(move || {
        refresh_production_data(
            app_weak.clone(),
            core_prod_refresh.clone(),
            session_prod_refresh.clone(),
            rt_handle_prod_refresh.clone(),
        );
    });

    // -- Chef production: execute production callback (Req 24.8) --
    let app_weak = app.as_weak();
    let core_exec_prod = core.clone();
    let session_exec_prod = session_token.clone();
    let user_id_exec_prod = user_id.clone();
    let rt_handle_exec_prod = rt.handle().clone();
    // Extra clones for refreshing production data after execution
    let core_exec_prod_refresh = core.clone();
    let session_exec_prod_refresh = session_token.clone();
    let rt_handle_exec_prod_refresh = rt.handle().clone();
    app.on_execute_production(move |recipe_id_str, quantity| {
        let recipe_id_str = recipe_id_str.to_string();

        let recipe_id = match Uuid::parse_str(&recipe_id_str) {
            Ok(id) => id,
            Err(_) => {
                if let Some(app) = app_weak.upgrade() {
                    app.set_production_error(SharedString::from("معرف الوصفة غير صالح"));
                }
                return;
            }
        };

        if quantity <= 0 {
            if let Some(app) = app_weak.upgrade() {
                app.set_production_error(SharedString::from("الكمية يجب أن تكون أكبر من صفر"));
            }
            return;
        }

        let token = {
            let guard = session_exec_prod.lock().unwrap();
            match *guard {
                Some(t) => t,
                None => return,
            }
        };

        let chef_id = {
            let guard = user_id_exec_prod.lock().unwrap();
            match *guard {
                Some(id) => id,
                None => return,
            }
        };

        // Set loading state
        if let Some(app) = app_weak.upgrade() {
            app.set_production_loading(true);
            app.set_production_error(SharedString::default());
            app.set_production_success(SharedString::default());
        }

        let core_ref = core_exec_prod.clone();
        let app_weak2 = app_weak.clone();
        let handle = rt_handle_exec_prod.clone();
        let core_refresh = core_exec_prod_refresh.clone();
        let session_refresh = session_exec_prod_refresh.clone();
        let handle_refresh = rt_handle_exec_prod_refresh.clone();

        std::thread::spawn(move || {
            let result = handle.block_on(core_ref.execute_production(token, recipe_id, quantity, chef_id));

            let _ = slint::invoke_from_event_loop(move || {
                let Some(app) = app_weak2.upgrade() else { return };

                match result {
                    Ok(log) => {
                        app.set_production_success(SharedString::from(
                            format!("تم تنفيذ الإنتاج: {} × {}", log.recipe_name, log.production_quantity),
                        ));
                        // Auto-refresh production data after successful execution
                        refresh_production_data(
                            app.as_weak(),
                            core_refresh,
                            session_refresh,
                            handle_refresh,
                        );
                    }
                    Err(e) => {
                        app.set_production_error(SharedString::from(format!("فشل تنفيذ الإنتاج: {e}")));
                    }
                }

                app.set_production_loading(false);
            });
        });
    });

    // -- Representative sales: refresh callback (Req 24.9) --
    let app_weak = app.as_weak();
    let core_sales_refresh = core.clone();
    let session_sales_refresh = session_token.clone();
    let rt_handle_sales_refresh = rt.handle().clone();
    app.on_refresh_sales(move || {
        refresh_sales_data(
            app_weak.clone(),
            core_sales_refresh.clone(),
            session_sales_refresh.clone(),
            rt_handle_sales_refresh.clone(),
        );
    });

    // -- Representative sales: create sale callback (Req 24.9) --
    let app_weak = app.as_weak();
    let core_create_sale = core.clone();
    let session_create_sale = session_token.clone();
    let rt_handle_create_sale = rt.handle().clone();
    // Extra clones for refreshing sales data after creation
    let core_create_sale_refresh = core.clone();
    let session_create_sale_refresh = session_token.clone();
    let rt_handle_create_sale_refresh = rt.handle().clone();
    app.on_create_sale(move |customer_id_str, line_items_json, amount_paid_str, wallet_id_str| {
        let customer_str = customer_id_str.to_string();
        let items_str = line_items_json.to_string();
        let amt_str = amount_paid_str.to_string();
        let wallet_str = wallet_id_str.to_string();

        let customer_id = match Uuid::parse_str(&customer_str) {
            Ok(id) => id,
            Err(_) => {
                if let Some(app) = app_weak.upgrade() {
                    app.set_sales_error(SharedString::from("معرف العميل غير صالح"));
                }
                return;
            }
        };

        let wallet_id = match Uuid::parse_str(&wallet_str) {
            Ok(id) => id,
            Err(_) => {
                if let Some(app) = app_weak.upgrade() {
                    app.set_sales_error(SharedString::from("معرف المحفظة غير صالح"));
                }
                return;
            }
        };

        let amount_paid = match sweet_lab_core::Money::parse(&amt_str) {
            Ok(m) => m,
            Err(_) => {
                if let Some(app) = app_weak.upgrade() {
                    app.set_sales_error(SharedString::from("المبلغ المدفوع غير صالح"));
                }
                return;
            }
        };

        // Parse line items JSON: [{"finished_good_id":"...","finished_good_name":"...","quantity":N,"unit_price":"..."}]
        let line_items: Vec<sweet_lab_core::SaleLineItem> = match serde_json::from_str(&items_str) {
            Ok(v) => v,
            Err(_) => {
                if let Some(app) = app_weak.upgrade() {
                    app.set_sales_error(SharedString::from("صيغة بنود البيع غير صالحة (JSON)"));
                }
                return;
            }
        };

        let token = {
            let guard = session_create_sale.lock().unwrap();
            match *guard {
                Some(t) => t,
                None => return,
            }
        };

        // Set loading state
        if let Some(app) = app_weak.upgrade() {
            app.set_sales_loading(true);
            app.set_sales_error(SharedString::default());
            app.set_sales_success(SharedString::default());
        }

        let core_ref = core_create_sale.clone();
        let app_weak2 = app_weak.clone();
        let handle = rt_handle_create_sale.clone();
        let core_refresh = core_create_sale_refresh.clone();
        let session_refresh = session_create_sale_refresh.clone();
        let handle_refresh = rt_handle_create_sale_refresh.clone();

        std::thread::spawn(move || {
            let result = handle.block_on(core_ref.create_sale(
                token,
                customer_id,
                line_items,
                amount_paid,
                wallet_id,
            ));

            let _ = slint::invoke_from_event_loop(move || {
                let Some(app) = app_weak2.upgrade() else { return };

                match result {
                    Ok(sale) => {
                        app.set_sales_success(SharedString::from(
                            format!("تم تسجيل البيع بنجاح — الإجمالي: {}", sale.total_amount.to_display()),
                        ));
                        // Auto-refresh sales data after successful creation
                        refresh_sales_data(
                            app.as_weak(),
                            core_refresh,
                            session_refresh,
                            handle_refresh,
                        );
                    }
                    Err(e) => {
                        app.set_sales_error(SharedString::from(format!("فشل تسجيل البيع: {e}")));
                    }
                }

                app.set_sales_loading(false);
            });
        });
    });

    // -- Representative customers: refresh callback (Req 24.10) --
    let app_weak = app.as_weak();
    let core_cust_refresh = core.clone();
    let session_cust_refresh = session_token.clone();
    let rt_handle_cust_refresh = rt.handle().clone();
    app.on_refresh_customers(move || {
        refresh_customers_data(
            app_weak.clone(),
            core_cust_refresh.clone(),
            session_cust_refresh.clone(),
            rt_handle_cust_refresh.clone(),
        );
    });

    // -- Representative customers: search callback (Req 24.10) --
    let app_weak = app.as_weak();
    let core_cust_search = core.clone();
    let session_cust_search = session_token.clone();
    let rt_handle_cust_search = rt.handle().clone();
    app.on_search_customers(move |query| {
        let query = query.to_string();
        search_customers_data(
            app_weak.clone(),
            core_cust_search.clone(),
            session_cust_search.clone(),
            rt_handle_cust_search.clone(),
            query,
        );
    });

    // -- Representative customers: create customer callback (Req 24.10) --
    let app_weak = app.as_weak();
    let core_create_cust = core.clone();
    let session_create_cust = session_token.clone();
    let rt_handle_create_cust = rt.handle().clone();
    // Extra clones for refreshing customer list after creation
    let core_create_cust_refresh = core.clone();
    let session_create_cust_refresh = session_token.clone();
    let rt_handle_create_cust_refresh = rt.handle().clone();
    app.on_create_customer(move |name, city, mobile| {
        let name = name.to_string();
        let city = city.to_string();
        let mobile = mobile.to_string();

        if name.trim().is_empty() {
            if let Some(app) = app_weak.upgrade() {
                app.set_customers_error(SharedString::from("اسم العميل مطلوب"));
            }
            return;
        }

        let token = {
            let guard = session_create_cust.lock().unwrap();
            match *guard {
                Some(t) => t,
                None => return,
            }
        };

        // Set loading state
        if let Some(app) = app_weak.upgrade() {
            app.set_customers_loading(true);
            app.set_customers_error(SharedString::default());
            app.set_customers_success(SharedString::default());
        }

        let core_ref = core_create_cust.clone();
        let app_weak2 = app_weak.clone();
        let handle = rt_handle_create_cust.clone();
        let core_refresh = core_create_cust_refresh.clone();
        let session_refresh = session_create_cust_refresh.clone();
        let handle_refresh = rt_handle_create_cust_refresh.clone();

        std::thread::spawn(move || {
            let result = handle.block_on(core_ref.create_customer(
                token,
                name,
                city,
                mobile,
            ));

            let _ = slint::invoke_from_event_loop(move || {
                let Some(app) = app_weak2.upgrade() else { return };

                match result {
                    Ok(customer) => {
                        app.set_customers_success(SharedString::from(
                            format!("تم إنشاء العميل: {}", customer.name),
                        ));
                        // Auto-refresh customer list after successful creation
                        refresh_customers_data(
                            app.as_weak(),
                            core_refresh,
                            session_refresh,
                            handle_refresh,
                        );
                    }
                    Err(e) => {
                        app.set_customers_error(SharedString::from(format!("فشل إنشاء العميل: {e}")));
                    }
                }

                app.set_customers_loading(false);
            });
        });
    });

    // -- Representative customers: update rating callback (Req 24.10) --
    let app_weak = app.as_weak();
    let core_update_rating = core.clone();
    let session_update_rating = session_token.clone();
    let rt_handle_update_rating = rt.handle().clone();
    // Extra clones for refreshing customer list after rating update
    let core_update_rating_refresh = core.clone();
    let session_update_rating_refresh = session_token.clone();
    let rt_handle_update_rating_refresh = rt.handle().clone();
    app.on_update_customer_rating(move |customer_id_str, rating| {
        let customer_id_str = customer_id_str.to_string();

        let customer_id = match Uuid::parse_str(&customer_id_str) {
            Ok(id) => id,
            Err(_) => {
                if let Some(app) = app_weak.upgrade() {
                    app.set_customers_error(SharedString::from("معرف العميل غير صالح"));
                }
                return;
            }
        };

        if rating < 1 || rating > 5 {
            if let Some(app) = app_weak.upgrade() {
                app.set_customers_error(SharedString::from("التقييم يجب أن يكون بين 1 و 5"));
            }
            return;
        }

        let token = {
            let guard = session_update_rating.lock().unwrap();
            match *guard {
                Some(t) => t,
                None => return,
            }
        };

        // Set loading state
        if let Some(app) = app_weak.upgrade() {
            app.set_customers_loading(true);
            app.set_customers_error(SharedString::default());
            app.set_customers_success(SharedString::default());
        }

        let core_ref = core_update_rating.clone();
        let app_weak2 = app_weak.clone();
        let handle = rt_handle_update_rating.clone();
        let core_refresh = core_update_rating_refresh.clone();
        let session_refresh = session_update_rating_refresh.clone();
        let handle_refresh = rt_handle_update_rating_refresh.clone();

        std::thread::spawn(move || {
            let result = handle.block_on(core_ref.update_reliability_rating(token, customer_id, rating));

            let _ = slint::invoke_from_event_loop(move || {
                let Some(app) = app_weak2.upgrade() else { return };

                match result {
                    Ok(()) => {
                        app.set_customers_success(SharedString::from("تم تحديث التقييم بنجاح"));
                        // Auto-refresh customer list after successful rating update
                        refresh_customers_data(
                            app.as_weak(),
                            core_refresh,
                            session_refresh,
                            handle_refresh,
                        );
                    }
                    Err(e) => {
                        app.set_customers_error(SharedString::from(format!("فشل تحديث التقييم: {e}")));
                    }
                }

                app.set_customers_loading(false);
            });
        });
    });

    // -- Representative expenses: refresh callback (Req 24.11) --
    let app_weak = app.as_weak();
    let core_exp_refresh = core.clone();
    let session_exp_refresh = session_token.clone();
    let rt_handle_exp_refresh = rt.handle().clone();
    app.on_refresh_expenses(move || {
        refresh_expenses_data(
            app_weak.clone(),
            core_exp_refresh.clone(),
            session_exp_refresh.clone(),
            rt_handle_exp_refresh.clone(),
        );
    });

    // -- Representative expenses: record expense callback (Req 24.11) --
    let app_weak = app.as_weak();
    let core_record_exp = core.clone();
    let session_record_exp = session_token.clone();
    let user_id_record_exp = user_id.clone();
    let rt_handle_record_exp = rt.handle().clone();
    // Extra clones for refreshing expenses data after recording
    let core_record_exp_refresh = core.clone();
    let session_record_exp_refresh = session_token.clone();
    let rt_handle_record_exp_refresh = rt.handle().clone();
    app.on_record_expense(move |description, amount_str, category_ar, wallet_id_str| {
        let description = description.to_string();
        let amt_str = amount_str.to_string();
        let cat_ar = category_ar.to_string();
        let wallet_str = wallet_id_str.to_string();

        if description.trim().is_empty() {
            if let Some(app) = app_weak.upgrade() {
                app.set_expenses_error(SharedString::from("وصف المصروف مطلوب"));
            }
            return;
        }

        let amount = match sweet_lab_core::Money::parse(&amt_str) {
            Ok(m) => m,
            Err(_) => {
                if let Some(app) = app_weak.upgrade() {
                    app.set_expenses_error(SharedString::from("المبلغ غير صالح"));
                }
                return;
            }
        };

        let category = match cat_ar.as_str() {
            "مشتريات" => sweet_lab_core::ExpenseCategory::Purchase,
            "تكاليف تشغيلية" => sweet_lab_core::ExpenseCategory::OperatingCost,
            _ => {
                if let Some(app) = app_weak.upgrade() {
                    app.set_expenses_error(SharedString::from("فئة غير صالحة"));
                }
                return;
            }
        };

        let wallet_id = match Uuid::parse_str(&wallet_str) {
            Ok(id) => id,
            Err(_) => {
                if let Some(app) = app_weak.upgrade() {
                    app.set_expenses_error(SharedString::from("معرف المحفظة غير صالح"));
                }
                return;
            }
        };

        let token = {
            let guard = session_record_exp.lock().unwrap();
            match *guard {
                Some(t) => t,
                None => return,
            }
        };

        let recorded_by = {
            let guard = user_id_record_exp.lock().unwrap();
            match *guard {
                Some(id) => id,
                None => return,
            }
        };

        // Set loading state
        if let Some(app) = app_weak.upgrade() {
            app.set_expenses_loading(true);
            app.set_expenses_error(SharedString::default());
            app.set_expenses_success(SharedString::default());
        }

        let core_ref = core_record_exp.clone();
        let app_weak2 = app_weak.clone();
        let handle = rt_handle_record_exp.clone();
        let core_refresh = core_record_exp_refresh.clone();
        let session_refresh = session_record_exp_refresh.clone();
        let handle_refresh = rt_handle_record_exp_refresh.clone();

        std::thread::spawn(move || {
            let result = handle.block_on(core_ref.record_expense(
                token,
                description,
                amount,
                category,
                wallet_id,
                recorded_by,
            ));

            let _ = slint::invoke_from_event_loop(move || {
                let Some(app) = app_weak2.upgrade() else { return };

                match result {
                    Ok(expense) => {
                        app.set_expenses_success(SharedString::from(
                            format!("تم تسجيل المصروف: {} — {}", expense.description, expense.amount.to_display()),
                        ));
                        // Auto-refresh expenses data after successful recording
                        refresh_expenses_data(
                            app.as_weak(),
                            core_refresh,
                            session_refresh,
                            handle_refresh,
                        );
                    }
                    Err(e) => {
                        app.set_expenses_error(SharedString::from(format!("فشل تسجيل المصروف: {e}")));
                    }
                }

                app.set_expenses_loading(false);
            });
        });
    });

    // -- Representative payments: refresh callback (Req 24.12) --
    let app_weak = app.as_weak();
    let core_pay_refresh = core.clone();
    let session_pay_refresh = session_token.clone();
    let rt_handle_pay_refresh = rt.handle().clone();
    app.on_refresh_payments(move || {
        refresh_payments_data(
            app_weak.clone(),
            core_pay_refresh.clone(),
            session_pay_refresh.clone(),
            rt_handle_pay_refresh.clone(),
        );
    });

    // -- Representative payments: record payment callback (Req 24.12) --
    let app_weak = app.as_weak();
    let core_record_pay = core.clone();
    let session_record_pay = session_token.clone();
    let rt_handle_record_pay = rt.handle().clone();
    // Extra clones for refreshing payments data after recording
    let core_record_pay_refresh = core.clone();
    let session_record_pay_refresh = session_token.clone();
    let rt_handle_record_pay_refresh = rt.handle().clone();
    app.on_record_payment(move |customer_id_str, amount_str, wallet_id_str| {
        let cust_str = customer_id_str.to_string();
        let amt_str = amount_str.to_string();
        let wallet_str = wallet_id_str.to_string();

        let amount = match sweet_lab_core::Money::parse(&amt_str) {
            Ok(m) => m,
            Err(_) => {
                if let Some(app) = app_weak.upgrade() {
                    app.set_payments_error(SharedString::from("المبلغ غير صالح"));
                }
                return;
            }
        };

        let customer_id = match Uuid::parse_str(&cust_str) {
            Ok(id) => id,
            Err(_) => {
                if let Some(app) = app_weak.upgrade() {
                    app.set_payments_error(SharedString::from("معرف العميل غير صالح"));
                }
                return;
            }
        };

        let wallet_id = match Uuid::parse_str(&wallet_str) {
            Ok(id) => id,
            Err(_) => {
                if let Some(app) = app_weak.upgrade() {
                    app.set_payments_error(SharedString::from("معرف المحفظة غير صالح"));
                }
                return;
            }
        };

        let token = {
            let guard = session_record_pay.lock().unwrap();
            match *guard {
                Some(t) => t,
                None => return,
            }
        };

        // Set loading state
        if let Some(app) = app_weak.upgrade() {
            app.set_payments_loading(true);
            app.set_payments_error(SharedString::default());
            app.set_payments_success(SharedString::default());
        }

        let core_ref = core_record_pay.clone();
        let app_weak2 = app_weak.clone();
        let handle = rt_handle_record_pay.clone();
        let core_refresh = core_record_pay_refresh.clone();
        let session_refresh = session_record_pay_refresh.clone();
        let handle_refresh = rt_handle_record_pay_refresh.clone();

        std::thread::spawn(move || {
            let result = handle.block_on(core_ref.record_debt_payment(
                token,
                customer_id,
                amount,
                wallet_id,
            ));

            let _ = slint::invoke_from_event_loop(move || {
                let Some(app) = app_weak2.upgrade() else { return };

                match result {
                    Ok(payment) => {
                        let msg = if payment.unallocated.0 > 0 {
                            format!(
                                "تم تسجيل الدفعة: {} — المبلغ غير المخصص: {}",
                                payment.amount.to_display(),
                                payment.unallocated.to_display(),
                            )
                        } else {
                            format!("تم تسجيل الدفعة: {}", payment.amount.to_display())
                        };
                        app.set_payments_success(SharedString::from(msg));
                        // Auto-refresh payments data after successful recording
                        refresh_payments_data(
                            app.as_weak(),
                            core_refresh,
                            session_refresh,
                            handle_refresh,
                        );
                    }
                    Err(e) => {
                        app.set_payments_error(SharedString::from(format!("فشل تسجيل الدفعة: {e}")));
                    }
                }

                app.set_payments_loading(false);
            });
        });
    });

    app.run().unwrap();
}
