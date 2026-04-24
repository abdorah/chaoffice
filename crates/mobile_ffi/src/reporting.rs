//! Report generation FFI functions: inventory, stock movement, budget, purchasing.
//!
//! Each function delegates to `reporting_commands`, which starts a long-running
//! operation. We poll for the result synchronously, then build an
//! `FfiReportResult` with the file path and size on disk.

use std::path::Path;
use std::thread;
use std::time::Duration;

use crate::dtos::{FfiReportFormat, FfiReportResult};
use crate::error::FfiError;
use crate::get_app_context;
use frontend::commands::reporting_commands;

/// Poll a long operation until it completes, returning the file path from the
/// result DTO. `get_result` should return `Ok(Some(_))` when the operation is
/// done, `Ok(None)` while still running, and `Err(_)` on failure.
fn poll_for_file_path<F>(mut get_result: F) -> Result<String, FfiError>
where
    F: FnMut() -> Result<Option<String>, FfiError>,
{
    // Long operations are typically fast (in-process file generation).
    // Poll with a short sleep to avoid busy-waiting.
    for _ in 0..600 {
        if let Some(path) = get_result()? {
            return Ok(path);
        }
        thread::sleep(Duration::from_millis(100));
    }
    Err(FfiError::ReportError {
        message: "Report generation timed out after 60 seconds".into(),
    })
}

/// Helper: given a file path, stat the file and build an `FfiReportResult`.
fn build_report_result(file_path: String, format: FfiReportFormat) -> FfiReportResult {
    let size_bytes = Path::new(&file_path)
        .metadata()
        .map(|m| m.len())
        .unwrap_or(0);
    FfiReportResult {
        file_path,
        format,
        size_bytes,
    }
}

/// Generate an inventory report in the requested format.
///
/// The report file is written to `output_dir` and the result includes the
/// full file path and size in bytes.
#[uniffi::export]
pub fn mobile_generate_inventory_report(
    format: FfiReportFormat,
    output_dir: String,
) -> Result<FfiReportResult, FfiError> {
    let ctx = get_app_context()?;
    let fmt_clone = format.clone();

    let dto = reporting::GenerateInventoryReportDto {
        output_path: output_dir,
        format: format.into(),
        include_zero_stock: false,
    };

    let operation_id = reporting_commands::generate_inventory_report(ctx, &dto)
        .map_err(FfiError::from)?;

    let file_path = poll_for_file_path(|| {
        let result = reporting_commands::get_generate_inventory_report_result(ctx, &operation_id)
            .map_err(FfiError::from)?;
        Ok(result.map(|r| r.file_path))
    })?;

    Ok(build_report_result(file_path, fmt_clone))
}

/// Generate a stock movement report for a date range.
///
/// Uses a wide date range (epoch to now) to include all movements.
/// The report file is written to `output_dir`.
#[uniffi::export]
pub fn mobile_generate_stock_movement_report(
    format: FfiReportFormat,
    output_dir: String,
    from_date: String,
    to_date: String,
) -> Result<FfiReportResult, FfiError> {
    let ctx = get_app_context()?;
    let fmt_clone = format.clone();

    let dto = reporting::GenerateStockMovementReportDto {
        output_path: output_dir,
        format: format.into(),
        from_date: chrono::DateTime::parse_from_rfc3339(&from_date)
            .map(|dt| dt.with_timezone(&chrono::Utc))
            .unwrap_or_else(|_| chrono::DateTime::<chrono::Utc>::MIN_UTC),
        to_date: chrono::DateTime::parse_from_rfc3339(&to_date)
            .map(|dt| dt.with_timezone(&chrono::Utc))
            .unwrap_or_else(|_| chrono::Utc::now()),
    };

    let operation_id = reporting_commands::generate_stock_movement_report(ctx, &dto)
        .map_err(FfiError::from)?;

    let file_path = poll_for_file_path(|| {
        let result =
            reporting_commands::get_generate_stock_movement_report_result(ctx, &operation_id)
                .map_err(FfiError::from)?;
        Ok(result.map(|r| r.file_path))
    })?;

    Ok(build_report_result(file_path, fmt_clone))
}

/// Generate a budget report for a date range.
///
/// The report file is written to `output_dir`.
#[uniffi::export]
pub fn mobile_generate_budget_report(
    format: FfiReportFormat,
    output_dir: String,
    from_date: String,
    to_date: String,
    include_projections: bool,
) -> Result<FfiReportResult, FfiError> {
    let ctx = get_app_context()?;
    let fmt_clone = format.clone();

    let dto = reporting::GenerateBudgetReportDto {
        output_path: output_dir,
        format: format.into(),
        from_date: chrono::DateTime::parse_from_rfc3339(&from_date)
            .map(|dt| dt.with_timezone(&chrono::Utc))
            .unwrap_or_else(|_| chrono::DateTime::<chrono::Utc>::MIN_UTC),
        to_date: chrono::DateTime::parse_from_rfc3339(&to_date)
            .map(|dt| dt.with_timezone(&chrono::Utc))
            .unwrap_or_else(|_| chrono::Utc::now()),
        include_projections,
    };

    let operation_id = reporting_commands::generate_budget_report(ctx, &dto)
        .map_err(FfiError::from)?;

    let file_path = poll_for_file_path(|| {
        let result = reporting_commands::get_generate_budget_report_result(ctx, &operation_id)
            .map_err(FfiError::from)?;
        Ok(result.map(|r| r.file_path))
    })?;

    Ok(build_report_result(file_path, fmt_clone))
}

/// Generate a purchasing report.
///
/// The report file is written to `output_dir`.
#[uniffi::export]
pub fn mobile_generate_purchasing_report(
    format: FfiReportFormat,
    output_dir: String,
) -> Result<FfiReportResult, FfiError> {
    let ctx = get_app_context()?;
    let fmt_clone = format.clone();

    let dto = reporting::GeneratePurchasingReportDto {
        output_path: output_dir,
        format: format.into(),
        status_filter: reporting::dtos::DealStatusFilter::All,
    };

    let operation_id = reporting_commands::generate_purchasing_report(ctx, &dto)
        .map_err(FfiError::from)?;

    let file_path = poll_for_file_path(|| {
        let result =
            reporting_commands::get_generate_purchasing_report_result(ctx, &operation_id)
                .map_err(FfiError::from)?;
        Ok(result.map(|r| r.file_path))
    })?;

    Ok(build_report_result(file_path, fmt_clone))
}
