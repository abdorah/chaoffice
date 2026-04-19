use crate::error::ReportError;
use crate::writer::{ReportRow, ReportWriter};
use rust_xlsxwriter::{Format, Workbook, Worksheet};

pub struct ExcelWriter {
    workbook: Workbook,
    output_path: String,
    current_sheet_index: usize,
    row_index: u32,
    header_format: Format,
    col_count: u16,
}

impl ExcelWriter {
    pub fn new(output_path: &str) -> Self {
        let header_format = Format::new().set_bold();
        ExcelWriter {
            workbook: Workbook::new(),
            output_path: output_path.to_string(),
            current_sheet_index: 0,
            row_index: 0,
            header_format,
            col_count: 0,
        }
    }

    fn write_headers_to_sheet(
        sheet: &mut Worksheet,
        headers: &[&str],
        format: &Format,
    ) -> Result<(), ReportError> {
        for (col, header) in headers.iter().enumerate() {
            sheet
                .write_string_with_format(0, col as u16, *header, format)
                .map_err(|e| ReportError::ExcelError {
                    details: e.to_string(),
                })?;
        }
        Ok(())
    }
}

impl ReportWriter for ExcelWriter {
    fn begin(&mut self, title: &str, headers: &[&str]) -> Result<(), ReportError> {
        self.col_count = headers.len() as u16;
        let sheet = self
            .workbook
            .add_worksheet()
            .set_name(title)
            .map_err(|e| ReportError::ExcelError {
                details: e.to_string(),
            })?;

        Self::write_headers_to_sheet(sheet, headers, &self.header_format.clone())?;
        self.row_index = 1;
        Ok(())
    }

    fn write_row(&mut self, row: &ReportRow) -> Result<(), ReportError> {
        let sheet = self
            .workbook
            .worksheet_from_index(self.current_sheet_index)
            .map_err(|e| ReportError::ExcelError {
                details: e.to_string(),
            })?;

        for (col, value) in row.iter().enumerate() {
            sheet
                .write_string(self.row_index, col as u16, value)
                .map_err(|e| ReportError::ExcelError {
                    details: e.to_string(),
                })?;
        }
        self.row_index += 1;
        Ok(())
    }

    fn begin_section(&mut self, title: &str, headers: &[&str]) -> Result<(), ReportError> {
        self.current_sheet_index += 1;
        self.col_count = headers.len() as u16;
        let sheet = self
            .workbook
            .add_worksheet()
            .set_name(title)
            .map_err(|e| ReportError::ExcelError {
                details: e.to_string(),
            })?;

        Self::write_headers_to_sheet(sheet, headers, &self.header_format.clone())?;
        self.row_index = 1;
        Ok(())
    }

    fn finish(&mut self) -> Result<(), ReportError> {
        // Set auto-width on all sheets
        let sheet_count = self.current_sheet_index + 1;
        for i in 0..sheet_count {
            if let Ok(sheet) = self.workbook.worksheet_from_index(i) {
                let _ = sheet.autofit();
            }
        }

        self.workbook
            .save(&self.output_path)
            .map_err(|e| ReportError::ExcelError {
                details: e.to_string(),
            })
    }
}
