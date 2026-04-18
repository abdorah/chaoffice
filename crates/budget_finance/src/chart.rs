use crate::aggregator::AggregatedSummary;
use crate::projector::Projection;

/// Bar chart data passed to Slint.
pub struct BarData {
    pub labels: Vec<String>,
    pub purchases: Vec<f64>,
    pub sales: Vec<f64>,
    pub expenses: Vec<f64>,
}

/// Build BarData from an AggregatedSummary.
pub fn build_bar_data(summary: &AggregatedSummary) -> BarData {
    BarData {
        labels: summary.monthly_labels.clone(),
        purchases: summary.monthly_purchases.clone(),
        sales: summary.monthly_sales.clone(),
        expenses: summary.monthly_expenses.clone(),
    }
}

/// Build SVG path strings from projection data for the line chart.
/// Returns (income_path, expenses_path, balance_path).
/// Each path is "M x0,y0 L x1,y1 ..." scaled to a 0–100 viewbox.
pub fn build_line_chart_paths(
    projection: &Projection,
    chart_width: f64,
    chart_height: f64,
) -> (String, String, String) {
    let n = projection.month_labels.len();
    if n == 0 {
        return (String::new(), String::new(), String::new());
    }

    // Find max value across all three series for vertical scaling
    let max_val = projection
        .projected_income
        .iter()
        .chain(projection.projected_expenses.iter())
        .chain(projection.projected_balance.iter())
        .cloned()
        .fold(0.0_f64, f64::max);

    let income_path = build_path(&projection.projected_income, n, chart_width, chart_height, max_val);
    let expenses_path = build_path(&projection.projected_expenses, n, chart_width, chart_height, max_val);
    let balance_path = build_path(&projection.projected_balance, n, chart_width, chart_height, max_val);

    (income_path, expenses_path, balance_path)
}

fn build_path(values: &[f64], n: usize, width: f64, height: f64, max_val: f64) -> String {
    let mut parts = Vec::with_capacity(n);
    for (i, &v) in values.iter().enumerate() {
        let x = if n == 1 {
            width / 2.0
        } else {
            (i as f64 / (n - 1) as f64) * width
        };
        let y = if max_val == 0.0 {
            height // flat baseline when all values are zero
        } else {
            height - (v / max_val) * height
        };
        let prefix = if i == 0 { "M" } else { "L" };
        parts.push(format!("{} {:.1},{:.1}", prefix, x, y));
    }
    parts.join(" ")
}
