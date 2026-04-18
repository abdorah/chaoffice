use chrono::DateTime;
use chrono::Utc;
use common::entities::{BudgetEntry, BudgetEntryType};
use std::collections::BTreeMap;

/// Aggregated budget summary for a date range.
pub struct AggregatedSummary {
    pub total_purchases: f64,
    pub total_sales: f64,
    pub total_expenses: f64,
    pub net_balance: f64,
    pub monthly_labels: Vec<String>,
    pub monthly_purchases: Vec<f64>,
    pub monthly_sales: Vec<f64>,
    pub monthly_expenses: Vec<f64>,
}

/// Aggregate BudgetEntry entities within [from_date, to_date] into totals
/// and monthly breakdowns. Entries with entry_type Forecast are excluded.
pub fn aggregate_entries(
    entries: &[BudgetEntry],
    from_date: &DateTime<Utc>,
    to_date: &DateTime<Utc>,
) -> AggregatedSummary {
    // BTreeMap keeps months in chronological order
    let mut monthly: BTreeMap<String, (f64, f64, f64)> = BTreeMap::new();

    for entry in entries {
        // Filter by date range [from_date, to_date] inclusive
        if entry.entry_date < *from_date || entry.entry_date > *to_date {
            continue;
        }
        // Exclude Forecast entries from totals
        if entry.entry_type == BudgetEntryType::Forecast {
            continue;
        }

        let label = entry.entry_date.format("%Y-%m").to_string();
        let bucket = monthly.entry(label).or_insert((0.0, 0.0, 0.0));

        match entry.entry_type {
            BudgetEntryType::Purchase => bucket.0 += entry.amount,
            BudgetEntryType::Sale => bucket.1 += entry.amount,
            BudgetEntryType::Expense => bucket.2 += entry.amount,
            BudgetEntryType::Forecast => {} // already filtered above
        }
    }

    let mut monthly_labels = Vec::new();
    let mut monthly_purchases = Vec::new();
    let mut monthly_sales = Vec::new();
    let mut monthly_expenses = Vec::new();
    let mut total_purchases = 0.0;
    let mut total_sales = 0.0;
    let mut total_expenses = 0.0;

    for (label, (p, s, e)) in &monthly {
        monthly_labels.push(label.clone());
        monthly_purchases.push(*p);
        monthly_sales.push(*s);
        monthly_expenses.push(*e);
        total_purchases += p;
        total_sales += s;
        total_expenses += e;
    }

    let net_balance = total_sales - total_purchases - total_expenses;

    AggregatedSummary {
        total_purchases,
        total_sales,
        total_expenses,
        net_balance,
        monthly_labels,
        monthly_purchases,
        monthly_sales,
        monthly_expenses,
    }
}
