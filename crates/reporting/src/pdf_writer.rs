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
        // Try the standard genpdf from_files approach first (works on Linux with Liberation fonts)
        let font_family = fonts::from_files("/usr/share/fonts/truetype/liberation", "LiberationSans", None)
            .or_else(|_| fonts::from_files("/usr/share/fonts/truetype/dejavu", "DejaVuSans", None))
            .or_else(|_| fonts::from_files("/System/Library/Fonts", "Helvetica", None))
            .or_else(|_| fonts::from_files(".", "LiberationSans", None));

        let font_family = match font_family {
            Ok(f) => f,
            Err(_) => {
                // Windows: load individual font files with non-standard naming
                let fonts_dir = std::path::Path::new("C:\\Windows\\Fonts");
                let try_load = |regular: &str, bold: &str, italic: &str, bold_italic: &str| -> Result<genpdf::fonts::FontFamily<genpdf::fonts::FontData>, String> {
                    let r = genpdf::fonts::FontData::load(fonts_dir.join(regular), None).map_err(|e| e.to_string())?;
                    let b = genpdf::fonts::FontData::load(fonts_dir.join(bold), None).map_err(|e| e.to_string())?;
                    let i = genpdf::fonts::FontData::load(fonts_dir.join(italic), None).map_err(|e| e.to_string())?;
                    let bi = genpdf::fonts::FontData::load(fonts_dir.join(bold_italic), None).map_err(|e| e.to_string())?;
                    Ok(genpdf::fonts::FontFamily { regular: r, bold: b, italic: i, bold_italic: bi })
                };

                // Try Arial first, then Times New Roman, then Courier New
                try_load("arial.ttf", "arialbd.ttf", "ariali.ttf", "arialbi.ttf")
                    .or_else(|_| try_load("times.ttf", "timesbd.ttf", "timesi.ttf", "timesbi.ttf"))
                    .or_else(|_| try_load("cour.ttf", "courbd.ttf", "couri.ttf", "courbi.ttf"))
                    .map_err(|_| ReportError::PdfError {
                        details: "No suitable fonts found. Ensure Arial, Times New Roman, or Courier New are installed in C:\\Windows\\Fonts".to_string(),
                    })?
            }
        };

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
