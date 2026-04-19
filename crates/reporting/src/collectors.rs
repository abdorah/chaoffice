use crate::dtos::DealStatusFilter;
use crate::error::ReportError;
use crate::writer::ReportRow;
use chrono::{DateTime, Utc};
use common::entities::DealStatus;

/// Inventory report columns:
/// name, reference, description, quantity, price_unit, status, category_name, location_name, supplier_name
pub fn collect_inventory_rows(
    uow: &dyn crate::use_cases::generate_inventory_report_uc::GenerateInventoryReportUnitOfWorkTrait,
    include_zero_stock: bool,
) -> Result<(Vec<ReportRow>, usize), ReportError> {
    let products = uow
        .get_all_product()
        .map_err(|e| ReportError::Internal(e.to_string()))?;

    let mut rows = Vec::new();
    for p in &products {
        if !include_zero_stock && p.quantity == 0 {
            continue;
        }

        let category_name = if let Some(cat_id) = p.category {
            uow.get_category(&cat_id)
                .map_err(|e| ReportError::Internal(e.to_string()))?
                .map(|c| c.name.clone())
                .unwrap_or_default()
        } else {
            String::new()
        };

        let location_name = if let Some(loc_id) = p.location {
            uow.get_location(&loc_id)
                .map_err(|e| ReportError::Internal(e.to_string()))?
                .map(|l| l.name.clone())
                .unwrap_or_default()
        } else {
            String::new()
        };

        let supplier_name = if let Some(sup_id) = p.supplier {
            uow.get_person(&sup_id)
                .map_err(|e| ReportError::Internal(e.to_string()))?
                .map(|s| s.name.clone())
                .unwrap_or_default()
        } else {
            String::new()
        };

        rows.push(vec![
            p.name.clone(),
            p.reference.clone(),
            p.description.clone(),
            p.quantity.to_string(),
            format!("{:.2}", p.price_unit),
            format!("{:?}", p.status),
            category_name,
            location_name,
            supplier_name,
        ]);
    }

    let count = rows.len();
    Ok((rows, count))
}

/// Stock movement report columns:
/// date, movement_type, product_name, product_reference, quantity, from_location, to_location, performed_by, note
pub fn collect_stock_movement_rows(
    uow: &dyn crate::use_cases::generate_stock_movement_report_uc::GenerateStockMovementReportUnitOfWorkTrait,
    from_date: &DateTime<Utc>,
    to_date: &DateTime<Utc>,
) -> Result<(Vec<ReportRow>, usize), ReportError> {
    let mut movements = uow
        .get_all_stock_movement()
        .map_err(|e| ReportError::Internal(e.to_string()))?;

    // Filter by date range [from_date, to_date] inclusive
    movements.retain(|m| m.created_at >= *from_date && m.created_at <= *to_date);

    // Sort chronologically
    movements.sort_by(|a, b| a.created_at.cmp(&b.created_at));

    let mut rows = Vec::new();
    for m in &movements {
        let product_name;
        let product_reference;
        if let Some(prod_id) = m.product {
            let prod = uow
                .get_product(&prod_id)
                .map_err(|e| ReportError::Internal(e.to_string()))?;
            product_name = prod.as_ref().map(|p| p.name.clone()).unwrap_or_default();
            product_reference = prod.as_ref().map(|p| p.reference.clone()).unwrap_or_default();
        } else {
            product_name = String::new();
            product_reference = String::new();
        }

        let from_loc = if let Some(loc_id) = m.from_location {
            uow.get_location(&loc_id)
                .map_err(|e| ReportError::Internal(e.to_string()))?
                .map(|l| l.name.clone())
                .unwrap_or_default()
        } else {
            String::new()
        };

        let to_loc = if let Some(loc_id) = m.to_location {
            uow.get_location(&loc_id)
                .map_err(|e| ReportError::Internal(e.to_string()))?
                .map(|l| l.name.clone())
                .unwrap_or_default()
        } else {
            String::new()
        };

        let performed_by = if let Some(user_id) = m.performed_by {
            uow.get_user(&user_id)
                .map_err(|e| ReportError::Internal(e.to_string()))?
                .map(|u| u.display_name.clone())
                .unwrap_or_default()
        } else {
            String::new()
        };

        rows.push(vec![
            m.created_at.format("%Y-%m-%d %H:%M:%S").to_string(),
            format!("{:?}", m.movement_type),
            product_name,
            product_reference,
            m.quantity.to_string(),
            from_loc,
            to_loc,
            performed_by,
            m.note.clone(),
        ]);
    }

    let count = rows.len();
    Ok((rows, count))
}

/// Budget report columns:
/// entry_date, entry_type, amount, description, product_name, deal_title, recorded_by
pub fn collect_budget_rows(
    uow: &dyn crate::use_cases::generate_budget_report_uc::GenerateBudgetReportUnitOfWorkTrait,
    from_date: &DateTime<Utc>,
    to_date: &DateTime<Utc>,
) -> Result<(Vec<ReportRow>, usize), ReportError> {
    let entries = uow
        .get_all_budget_entry()
        .map_err(|e| ReportError::Internal(e.to_string()))?;

    // Filter by date range [from_date, to_date] inclusive
    let filtered: Vec<_> = entries
        .into_iter()
        .filter(|e| e.entry_date >= *from_date && e.entry_date <= *to_date)
        .collect();

    let mut rows = Vec::new();
    for entry in &filtered {
        let product_name = if let Some(prod_id) = entry.product {
            uow.get_product(&prod_id)
                .map_err(|e| ReportError::Internal(e.to_string()))?
                .map(|p| p.name.clone())
                .unwrap_or_default()
        } else {
            String::new()
        };

        let deal_title = if let Some(deal_id) = entry.deal {
            uow.get_deal(&deal_id)
                .map_err(|e| ReportError::Internal(e.to_string()))?
                .map(|d| d.title.clone())
                .unwrap_or_default()
        } else {
            String::new()
        };

        let recorded_by = if let Some(user_id) = entry.recorded_by {
            uow.get_user(&user_id)
                .map_err(|e| ReportError::Internal(e.to_string()))?
                .map(|u| u.display_name.clone())
                .unwrap_or_default()
        } else {
            String::new()
        };

        rows.push(vec![
            entry.entry_date.format("%Y-%m-%d %H:%M:%S").to_string(),
            format!("{:?}", entry.entry_type),
            format!("{:.2}", entry.amount),
            entry.description.clone(),
            product_name,
            deal_title,
            recorded_by,
        ]);
    }

    let count = rows.len();
    Ok((rows, count))
}

/// Budget projection rows (for appendix):
/// month, projected_income, projected_expenses, projected_balance, recurring_deal_costs
pub fn collect_budget_projection_rows(
    uow: &dyn crate::use_cases::generate_budget_report_uc::GenerateBudgetReportUnitOfWorkTrait,
) -> Result<Vec<ReportRow>, ReportError> {
    use common::entities::BudgetEntryType;

    let entries = uow
        .get_all_budget_entry()
        .map_err(|e| ReportError::Internal(e.to_string()))?;
    let deals = uow
        .get_all_deal()
        .map_err(|e| ReportError::Internal(e.to_string()))?;

    // Calculate historical monthly averages
    let total_income: f64 = entries
        .iter()
        .filter(|e| e.entry_type == BudgetEntryType::Sale)
        .map(|e| e.amount)
        .sum();
    let total_expenses: f64 = entries
        .iter()
        .filter(|e| e.entry_type == BudgetEntryType::Purchase || e.entry_type == BudgetEntryType::Expense)
        .map(|e| e.amount)
        .sum();

    let months_of_data = if entries.is_empty() {
        1.0
    } else {
        let min_date = entries.iter().map(|e| e.entry_date).min().unwrap();
        let max_date = entries.iter().map(|e| e.entry_date).max().unwrap();
        let diff = max_date.signed_duration_since(min_date);
        (diff.num_days() as f64 / 30.0).max(1.0)
    };

    let avg_monthly_income = total_income / months_of_data;
    let avg_monthly_expenses = total_expenses / months_of_data;

    // Calculate recurring deal costs (active deals)
    let recurring_cost: f64 = deals
        .iter()
        .filter(|d| d.status == DealStatus::Active)
        .map(|d| d.total_value)
        .sum();
    let monthly_recurring = recurring_cost / 12.0;

    let now = Utc::now();
    let mut rows = Vec::new();
    for i in 1..=6 {
        let month = now + chrono::Duration::days(30 * i);
        let projected_income = avg_monthly_income;
        let projected_expenses = avg_monthly_expenses + monthly_recurring;
        let projected_balance = projected_income - projected_expenses;

        rows.push(vec![
            month.format("%Y-%m").to_string(),
            format!("{:.2}", projected_income),
            format!("{:.2}", projected_expenses),
            format!("{:.2}", projected_balance),
            format!("{:.2}", monthly_recurring),
        ]);
    }

    Ok(rows)
}

/// Purchasing report columns:
/// title, description, supplier_name, product_name, status, frequency, unit_cost, total_value, start_date, end_date
pub fn collect_purchasing_rows(
    uow: &dyn crate::use_cases::generate_purchasing_report_uc::GeneratePurchasingReportUnitOfWorkTrait,
    status_filter: &DealStatusFilter,
) -> Result<(Vec<ReportRow>, usize), ReportError> {
    let deals = uow
        .get_all_deal()
        .map_err(|e| ReportError::Internal(e.to_string()))?;

    // Filter by status
    let filtered: Vec<_> = deals
        .into_iter()
        .filter(|d| match status_filter {
            DealStatusFilter::All => true,
            DealStatusFilter::ActiveOnly => d.status == DealStatus::Active,
            DealStatusFilter::CompletedOnly => d.status == DealStatus::Completed,
        })
        .collect();

    let mut rows = Vec::new();
    for deal in &filtered {
        let supplier_name = if let Some(sup_id) = deal.supplier {
            uow.get_person(&sup_id)
                .map_err(|e| ReportError::Internal(e.to_string()))?
                .map(|p| p.name.clone())
                .unwrap_or_default()
        } else {
            String::new()
        };

        let product_name = if let Some(prod_id) = deal.product {
            uow.get_product(&prod_id)
                .map_err(|e| ReportError::Internal(e.to_string()))?
                .map(|p| p.name.clone())
                .unwrap_or_default()
        } else {
            String::new()
        };

        rows.push(vec![
            deal.title.clone(),
            deal.description.clone(),
            supplier_name,
            product_name,
            format!("{:?}", deal.status),
            format!("{:?}", deal.frequency),
            format!("{:.2}", deal.unit_cost),
            format!("{:.2}", deal.total_value),
            deal.start_date.format("%Y-%m-%d").to_string(),
            deal.end_date.format("%Y-%m-%d").to_string(),
        ]);
    }

    let count = rows.len();
    Ok((rows, count))
}
