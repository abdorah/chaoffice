use crate::error::ReportError;
use crate::writer::{ReportRow, ReportWriter};
use csv::Writer;
use std::fs::File;

pub struct CsvReportWriter {
    writer: Writer<File>,
}

impl CsvReportWriter {
    pub fn new(output_path: &str) -> Result<Self, ReportError> {
        let file = File::create(output_path).map_err(|e| ReportError::CsvError {
            details: format!("Failed to create file: {}", e),
        })?;
        let writer = Writer::from_writer(file);
        Ok(CsvReportWriter { writer })
    }
}

impl ReportWriter for CsvReportWriter {
    fn begin(&mut self, _title: &str, headers: &[&str]) -> Result<(), ReportError> {
        self.writer
            .write_record(headers)
            .map_err(|e| ReportError::CsvError {
                details: e.to_string(),
            })
    }

    fn write_row(&mut self, row: &ReportRow) -> Result<(), ReportError> {
        self.writer
            .write_record(row)
            .map_err(|e| ReportError::CsvError {
                details: e.to_string(),
            })
    }

    fn begin_section(&mut self, title: &str, headers: &[&str]) -> Result<(), ReportError> {
        // Write empty separator row
        self.writer
            .write_record(&[""])
            .map_err(|e| ReportError::CsvError {
                details: e.to_string(),
            })?;
        // Write section title row
        self.writer
            .write_record(&[title])
            .map_err(|e| ReportError::CsvError {
                details: e.to_string(),
            })?;
        // Write section headers
        self.writer
            .write_record(headers)
            .map_err(|e| ReportError::CsvError {
                details: e.to_string(),
            })
    }

    fn finish(&mut self) -> Result<(), ReportError> {
        self.writer
            .flush()
            .map_err(|e| ReportError::CsvError {
                details: e.to_string(),
            })
    }
}
