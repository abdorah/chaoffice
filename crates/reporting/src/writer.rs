use crate::csv_writer::CsvReportWriter;
use crate::error::ReportError;
use crate::excel_writer::ExcelWriter;
use crate::pdf_writer::PdfWriter;
use crate::types::ReportFormat;

/// A row of string values to write to the report.
pub type ReportRow = Vec<String>;

/// Trait for writing tabular report data to a specific file format.
pub trait ReportWriter {
    /// Initialize the writer with a report title and column headers.
    fn begin(&mut self, title: &str, headers: &[&str]) -> Result<(), ReportError>;

    /// Write a single data row.
    fn write_row(&mut self, row: &ReportRow) -> Result<(), ReportError>;

    /// Begin a new section (used for projection appendix in budget reports).
    fn begin_section(&mut self, title: &str, headers: &[&str]) -> Result<(), ReportError>;

    /// Finalize and flush the report to disk.
    fn finish(&mut self) -> Result<(), ReportError>;
}

/// Create the appropriate writer for the given format and output path.
pub fn create_writer(
    format: &ReportFormat,
    output_path: &str,
) -> Result<Box<dyn ReportWriter>, ReportError> {
    // Validate output path is writable by checking parent directory
    let path = std::path::Path::new(output_path);
    if let Some(parent) = path.parent() {
        if !parent.as_os_str().is_empty() && !parent.exists() {
            return Err(ReportError::NotWritable {
                path: output_path.to_string(),
            });
        }
    }

    match format {
        ReportFormat::Excel => Ok(Box::new(ExcelWriter::new(output_path))),
        ReportFormat::Pdf => Ok(Box::new(PdfWriter::new(output_path))),
        ReportFormat::Csv => CsvReportWriter::new(output_path).map(|w| Box::new(w) as Box<dyn ReportWriter>),
    }
}
