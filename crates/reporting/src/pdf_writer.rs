use crate::error::ReportError;
use crate::writer::{ReportRow, ReportWriter};
use genpdf::elements::{Break, Paragraph, TableLayout};
use genpdf::style::Style;
use genpdf::{Document, Element, SimplePageDecorator, fonts};

pub struct PdfWriter {
    doc: Option<Document>,
    output_path: String,
    headers: Vec<String>,
    col_count: usize,
}

impl PdfWriter {
    pub fn new(output_path: &str) -> Self {
        PdfWriter {
            doc: None,
            output_path: output_path.to_string(),
            headers: Vec::new(),
            col_count: 0,
        }
    }

    fn create_document(title: &str) -> Result<Document, ReportError> {
        // Try multiple font locations: Windows (Arial), Linux (LiberationSans),
        // macOS (Helvetica), and current directory as fallback.
        let font_family = fonts::from_files("C:\\Windows\\Fonts", "arial", None)
            .or_else(|_| fonts::from_files("C:\\Windows\\Fonts", "Arial", None))
            .or_else(|_| fonts::from_files("/usr/share/fonts/truetype/liberation", "LiberationSans", None))
            .or_else(|_| fonts::from_files("/usr/share/fonts", "LiberationSans", None))
            .or_else(|_| fonts::from_files("/System/Library/Fonts", "Helvetica", None))
            .or_else(|_| fonts::from_files(".", "LiberationSans", None))
            .map_err(|e| ReportError::PdfError {
                details: format!(
                    "Could not load any font for PDF generation: {}. \
                     Install LiberationSans or ensure Arial is available.",
                    e
                ),
            })?;

        let mut doc = Document::new(font_family);
        doc.set_title(title);
        let mut decorator = SimplePageDecorator::new();
        decorator.set_margins(10);
        doc.set_page_decorator(decorator);

        // Add title
        doc.push(
            Paragraph::new(title)
                .styled(Style::new().bold().with_font_size(16)),
        );
        doc.push(Break::new(1));

        Ok(doc)
    }
}

impl ReportWriter for PdfWriter {
    fn begin(&mut self, title: &str, headers: &[&str]) -> Result<(), ReportError> {
        self.col_count = headers.len();
        self.headers = headers.iter().map(|h| h.to_string()).collect();

        let doc = Self::create_document(title).map_err(|e| ReportError::PdfError {
            details: e.to_string(),
        })?;
        self.doc = Some(doc);

        Ok(())
    }

    fn write_row(&mut self, row: &ReportRow) -> Result<(), ReportError> {
        let doc = self.doc.as_mut().ok_or_else(|| ReportError::PdfError {
            details: "Document not initialized".to_string(),
        })?;

        let mut table = TableLayout::new(vec![1; self.col_count]);
        table.set_cell_decorator(genpdf::elements::FrameCellDecorator::new(true, false, false));
        let table_row = table.row();
        let mut row_handle = table_row;
        for val in row {
            row_handle.push_element(
                Paragraph::new(val.as_str()).styled(Style::new().with_font_size(7)),
            );
        }
        row_handle.push().map_err(|e| ReportError::PdfError {
            details: e.to_string(),
        })?;
        doc.push(table);

        Ok(())
    }

    fn begin_section(&mut self, title: &str, headers: &[&str]) -> Result<(), ReportError> {
        let doc = self.doc.as_mut().ok_or_else(|| ReportError::PdfError {
            details: "Document not initialized".to_string(),
        })?;

        // Page break
        doc.push(Break::new(2));
        doc.push(
            Paragraph::new(title)
                .styled(Style::new().bold().with_font_size(14)),
        );
        doc.push(Break::new(1));

        self.col_count = headers.len();
        self.headers = headers.iter().map(|h| h.to_string()).collect();

        Ok(())
    }

    fn finish(&mut self) -> Result<(), ReportError> {
        let doc = self.doc.take().ok_or_else(|| ReportError::PdfError {
            details: "Document not initialized".to_string(),
        })?;

        doc.render_to_file(&self.output_path)
            .map_err(|e| ReportError::PdfError {
                details: e.to_string(),
            })
    }
}
