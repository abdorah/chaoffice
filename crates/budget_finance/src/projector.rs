use chrono::{Datelike, Months, Utc};
use common::entities::{BudgetEntry, BudgetEntryType, Deal, DealFrequency, DealStatus};
use std::collections::HashSet;

/// Frequency multiplier: how many times per month a deal recurs.
pub fn frequency_to_monthly_factor(freq: &DealFrequency) -> f64 {
    match freq {
        DealFrequency::OneTime => 0.0,
        DealFrequency::Weekly => 52.0 / 12.0,
        DealFrequency::Monthly => 1.0,
        DealFrequency::Quarterly => 1.0 / 3.0,
        DealFrequency::Yearly => 1.0 / 12.0,
    }
}

/// Compute recurring deal costs per month from active deals.
pub fn compute_recurring_costs(active_deals: &[Deal]) -> f64 {
    active_deals
        .iter()
        .filter(|d| d.status == DealStatus::Active)
        .map(|d| d.unit_cost * frequency_to_monthly_factor(&d.frequency))
        .sum()
}

/// Compute historical average monthly income (Sale) and expense (Expense).
/// Returns (avg_monthly_income, avg_monthly_expense).
pub fn compute_historical_averages(entries: &[BudgetEntry]) -> (f64, f64) {
    let mut total_income = 0.0;
    let mut total_expense = 0.0;
    let mut months_with_data: HashSet<String> = HashSet::new();

    for entry in entries {
        match entry.entry_type {
            BudgetEntryType::Sale => {
                total_income += entry.amount;
                months_with_data.insert(entry.entry_date.format("%Y-%m").to_string());
            }
            BudgetEntryType::Expense => {
                total_expense += entry.amount;
                months_with_data.insert(entry.entry_date.format("%Y-%m").to_string());
            }
            _ => {}
        }
    }

    let num_months = months_with_data.len().max(1) as f64;
    (total_income / num_months, total_expense / num_months)
}

/// Full projection output.
pub struct Projection {
    pub month_labels: Vec<String>,
    pub projected_income: Vec<f64>,
    pub projected_expenses: Vec<f64>,
    pub projected_balance: Vec<f64>,
    pub recurring_deal_costs: Vec<f64>,
}

/// Build the full projection for months_ahead months.
pub fn project_budget(
    active_deals: &[Deal],
    historical_entries: &[BudgetEntry],
    months_ahead: u32,
) -> Projection {
    let recurring = compute_recurring_costs(active_deals);
    let (avg_income, avg_expense) = compute_historical_averages(historical_entries);

    let now = Utc::now();
    let mut month_labels = Vec::with_capacity(months_ahead as usize);
    let mut projected_income = Vec::with_capacity(months_ahead as usize);
    let mut projected_expenses = Vec::with_capacity(months_ahead as usize);
    let mut projected_balance = Vec::with_capacity(months_ahead as usize);
    let mut recurring_deal_costs = Vec::with_capacity(months_ahead as usize);

    for i in 1..=months_ahead {
        let future = now
            .checked_add_months(Months::new(i))
            .unwrap_or(now);
        let label = format!("{:04}-{:02}", future.year(), future.month());
        month_labels.push(label);

        let income_i = avg_income;
        let expense_i = recurring + avg_expense;
        let balance_i = income_i - expense_i;

        projected_income.push(income_i);
        projected_expenses.push(expense_i);
        projected_balance.push(balance_i);
        recurring_deal_costs.push(recurring);
    }

    Projection {
        month_labels,
        projected_income,
        projected_expenses,
        projected_balance,
        recurring_deal_costs,
    }
}
