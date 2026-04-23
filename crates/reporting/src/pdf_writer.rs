use crate::error::ReportError;
use crate::writer::{ReportRow, ReportWriter};
use typst_as_lib::TypstEngine;

static TEMPLATE: &str = include_str!("../templates/report.typ");

// Embed Liberation Sans for guaranteed font availability
static FONT_REGULAR: &[u8] = include_bytes!("../fonts/LiberationSans-Regular.ttf");
static FONT_BOLD: &[u8] = include_bytes!("../fonts/LiberationSans-Bold.ttf");
static FONT_ITALIC: &[u8] = include_bytes!("../fonts/LiberationSans-Italic.ttf");
static FONT_BOLD_ITALIC: &[u8] = include_bytes!("../fonts/LiberationSans-BoldItalic.ttf");

/// Convert a Rust string to a typst Value::Str
fn tstr(s: &str) -> typst_library::foundations::Value {
    typst_library::foundations::Value::Str(s.into())
}

/// Convert a Vec<String> to a typst Value::Array
fn tarray(items: &[String]) -> typst_library::foundations::Value {
    let arr: typst_library::foundations::Array = items.iter()
        .map(|s| typst_library::foundations::Value::Str(s.as_str().into()))
        .collect();
    typst_library::foundations::Value::Array(arr)
}

/// Convert a Vec<Vec<String>> (rows) to a typst Value::Array of Arrays
fn trows(rows: &[ReportRow]) -> typst_library::foundations::Value {
    let arr: typst_library::foundations::Array = rows.iter()
        .map(|row| {
            let inner: typst_library::foundations::Array = row.iter()
                .map(|s| typst_library::foundations::Value::Str(s.as_str().into()))
                .collect();
            typst_library::foundations::Value::Array(inner)
        })
        .collect();
    typst_library::foundations::Value::Array(arr)
}

pub struct PdfWriter {
    output_path: String,
    title: String,
    headers: Vec<String>,
    rows: Vec<ReportRow>,
    appendix_title: Option<String>,
    appendix_headers: Vec<String>,
    appendix_rows: Vec<ReportRow>,
    in_appendix: bool,
}

impl PdfWriter {
    pub fn new(output_path: &str) -> Self {
        PdfWriter {
            output_path: output_path.to_string(),
            title: String::new(),
            headers: Vec::new(),
            rows: Vec::new(),
            appendix_title: None,
            appendix_headers: Vec::new(),
            appendix_rows: Vec::new(),
            in_appendix: false,
        }
    }
}

impl ReportWriter for PdfWriter {
    fn begin(&mut self, title: &str, headers: &[&str]) -> Result<(), ReportError> {
        self.title = title.to_string();
        self.headers = headers.iter().map(|h| h.to_string()).collect();
        self.rows.clear();
        Ok(())
    }

    fn write_row(&mut self, row: &ReportRow) -> Result<(), ReportError> {
        if self.in_appendix {
            self.appendix_rows.push(row.clone());
        } else {
            self.rows.push(row.clone());
        }
        Ok(())
    }

    fn begin_section(&mut self, title: &str, headers: &[&str]) -> Result<(), ReportError> {
        self.appendix_title = Some(title.to_string());
        self.appendix_headers = headers.iter().map(|h| h.to_string()).collect();
        self.appendix_rows.clear();
        self.in_appendix = true;
        Ok(())
    }

    fn finish(&mut self) -> Result<(), ReportError> {
        use typst_library::foundations::{Dict, Value};

        let now = chrono::Local::now().format("%Y-%m-%d %H:%M").to_string();

        let mut input = Dict::new();
        input.insert("title".into(), tstr(&self.title));
        input.insert("generated".into(), tstr(&now));
        input.insert("headers".into(), tarray(&self.headers));
        input.insert("rows".into(), trows(&self.rows));

        if let Some(ref app_title) = self.appendix_title {
            input.insert("appendix_title".into(), tstr(app_title));
            input.insert("appendix_headers".into(), tarray(&self.appendix_headers));
            input.insert("appendix_rows".into(), trows(&self.appendix_rows));
        } else {
            // Provide empty defaults so the template doesn't error
            input.insert("appendix_title".into(), Value::None);
            input.insert("appendix_headers".into(), Value::Array(Default::default()));
            input.insert("appendix_rows".into(), Value::Array(Default::default()));
        }

        let engine = TypstEngine::builder()
            .main_file(TEMPLATE)
            .fonts([FONT_REGULAR, FONT_BOLD, FONT_ITALIC, FONT_BOLD_ITALIC])
            .build();

        let warned = engine.compile_with_input::<Dict, typst_library::layout::PagedDocument>(input);

        // Log any warnings from typst compilation
        for w in &warned.warnings {
            eprintln!("Typst warning: {}", w.message);
        }

        let doc = warned.output.map_err(|e| ReportError::PdfError {
            details: format!("{:?}", e),
        })?;

        eprintln!("PDF: compiled {} rows, {} pages", self.rows.len(), doc.pages.len());

        let options = Default::default();
        let pdf = typst_pdf::pdf(&doc, &options)
            .map_err(|errs| ReportError::PdfError {
                details: format!("{:?}", errs),
            })?;

        std::fs::write(&self.output_path, pdf)
            .map_err(|e| ReportError::PdfError {
                details: format!("Write file: {}", e),
            })?;

        Ok(())
    }
}
