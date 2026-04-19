use thiserror::Error;

#[derive(Debug, Error)]
pub enum ReportError {
    #[error("Output path not writable: {path}")]
    NotWritable { path: String },
    #[error("from_date must not be later than to_date")]
    InvalidDateRange,
    #[error("Excel write error: {details}")]
    ExcelError { details: String },
    #[error("PDF write error: {details}")]
    PdfError { details: String },
    #[error("CSV write error: {details}")]
    CsvError { details: String },
    #[error("Internal error: {0}")]
    Internal(String),
}
