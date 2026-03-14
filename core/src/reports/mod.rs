pub mod excel;
pub mod financial;
pub mod inventory_report;
pub mod pdf;

use chrono::{DateTime, Utc};
use sqlx::SqlitePool;
use uuid::Uuid;

use crate::error::AppResult;
use crate::models::domain::{FinancialSummary, InventoryReport, Invoice};

/// Concrete implementation of the ReportService.
pub struct ReportServiceImpl {
    pool: SqlitePool,
    font_dir: String,
}

impl ReportServiceImpl {
    pub fn new(pool: SqlitePool, font_dir: Option<String>) -> Self {
        Self {
            pool,
            font_dir: font_dir.unwrap_or_else(|| "./fonts".to_string()),
        }
    }

    /// Aggregate sales revenue, expenses, net profit, and wallet balances for a date range (Req 12.1).
    pub async fn get_financial_summary(
        &self,
        start_date: DateTime<Utc>,
        end_date: DateTime<Utc>,
    ) -> AppResult<FinancialSummary> {
        financial::get_financial_summary(&self.pool, start_date, end_date).await
    }

    /// List all raw materials and finished goods with low-stock flags (Req 12.2).
    pub async fn get_inventory_report(
        &self,
        low_stock_threshold: f64,
    ) -> AppResult<InventoryReport> {
        inventory_report::get_inventory_report(&self.pool, low_stock_threshold).await
    }

    /// Build an Invoice from a sale record (Req 12.3).
    pub async fn generate_invoice(&self, sale_id: Uuid, business_name: &str) -> AppResult<Invoice> {
        financial::generate_invoice(&self.pool, sale_id, business_name).await
    }

    /// Render an Invoice to PDF bytes using genpdf (Req 12.4, 22.1, 22.2).
    pub async fn export_to_pdf(&self, invoice: &Invoice) -> AppResult<Vec<u8>> {
        pdf::export_to_pdf(invoice, &self.font_dir)
    }

    /// Render an Invoice to Excel bytes using rust_xlsxwriter (Req 12.4).
    pub async fn export_to_excel(&self, invoice: &Invoice) -> AppResult<Vec<u8>> {
        excel::export_to_excel(invoice)
    }
}
