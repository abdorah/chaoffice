pub use crate::dtos::{
    BudgetReportFormat, DealStatusFilter, PurchasingReportFormat, ReportFormat, StockReportFormat,
};

/// Converts a ReportFormat to a file extension.
pub fn format_extension(format: &ReportFormat) -> &'static str {
    match format {
        ReportFormat::Excel => "xlsx",
        ReportFormat::Pdf => "pdf",
        ReportFormat::Csv => "csv",
    }
}
