use crate::error::ReportError;
use crate::types::ReportFormat;
use crate::writer::{ReportRow, create_writer};
use common::long_operation::OperationProgress;

/// Shared generation logic: create writer, write rows with progress, finish.
/// Returns (file_path, row_count) where row_count = number of data rows written.
pub fn generate_report(
    title: &str,
    headers: &[&str],
    rows: Vec<ReportRow>,
    format: &ReportFormat,
    output_path: &str,
    progress: &dyn Fn(OperationProgress),
) -> Result<(String, usize), ReportError> {
    let mut writer = create_writer(format, output_path)?;
    writer.begin(title, headers)?;

    let total = rows.len();
    for (i, row) in rows.iter().enumerate() {
        writer.write_row(row)?;
        let pct = if total > 0 {
            ((i + 1) as f32 / total as f32) * 100.0
        } else {
            100.0
        };
        progress(OperationProgress::new(pct, Some(format!("Writing row {}/{}", i + 1, total))));
    }

    writer.finish()?;
    Ok((output_path.to_string(), total))
}

/// Extended generation with an optional appendix section (for budget projections).
/// Row count only counts main data rows (not appendix).
pub fn generate_report_with_appendix(
    title: &str,
    headers: &[&str],
    rows: Vec<ReportRow>,
    appendix_title: &str,
    appendix_headers: &[&str],
    appendix_rows: Vec<ReportRow>,
    format: &ReportFormat,
    output_path: &str,
    progress: &dyn Fn(OperationProgress),
) -> Result<(String, usize), ReportError> {
    let mut writer = create_writer(format, output_path)?;
    writer.begin(title, headers)?;

    let main_count = rows.len();
    let total_work = main_count + appendix_rows.len();

    for (i, row) in rows.iter().enumerate() {
        writer.write_row(row)?;
        let pct = if total_work > 0 {
            ((i + 1) as f32 / total_work as f32) * 100.0
        } else {
            50.0
        };
        progress(OperationProgress::new(pct, Some(format!("Writing row {}/{}", i + 1, main_count))));
    }

    // Write appendix section
    writer.begin_section(appendix_title, appendix_headers)?;
    for (i, row) in appendix_rows.iter().enumerate() {
        writer.write_row(row)?;
        let pct = if total_work > 0 {
            ((main_count + i + 1) as f32 / total_work as f32) * 100.0
        } else {
            100.0
        };
        progress(OperationProgress::new(pct, Some(format!("Writing projection {}/{}", i + 1, appendix_rows.len()))));
    }

    writer.finish()?;
    // Row count only counts main data rows
    Ok((output_path.to_string(), main_count))
}
