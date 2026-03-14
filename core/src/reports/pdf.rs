use genpdf::elements::{Paragraph, TableLayout};
use genpdf::style::Style;
use genpdf::{Document, Element, SimplePageDecorator};

use crate::error::{AppError, AppResult};
use crate::models::domain::Invoice;

/// Default font family name for PDF generation.
const DEFAULT_FONT_FAMILY: &str = "LiberationSans";

/// Render an Invoice to PDF bytes (Req 12.4, 22.1, 22.2).
///
/// Uses genpdf with LiberationSans fonts from the given `font_dir`.
/// Returns `AppError::Validation` if the font directory does not exist.
pub fn export_to_pdf(invoice: &Invoice, font_dir: &str) -> AppResult<Vec<u8>> {
    // Req 22.2: Return a descriptive error instead of panicking if font dir is missing
    if !std::path::Path::new(font_dir).is_dir() {
        return Err(AppError::Validation {
            field: "font_dir".to_string(),
            message: format!("Font directory not found: {font_dir}"),
        });
    }

    let font_family =
        genpdf::fonts::from_files(font_dir, DEFAULT_FONT_FAMILY, None).map_err(|e| {
            AppError::Unknown(format!(
                "Failed to load fonts from '{font_dir}/{DEFAULT_FONT_FAMILY}': {e}. \
                 Place LiberationSans-Regular.ttf, LiberationSans-Bold.ttf, \
                 LiberationSans-Italic.ttf, and LiberationSans-BoldItalic.ttf in the fonts/ directory."
            ))
        })?;

    let mut doc = Document::new(font_family);
    doc.set_title("Invoice");

    let mut decorator = SimplePageDecorator::new();
    decorator.set_margins(15);
    doc.set_page_decorator(decorator);

    // ── Header ─────────────────────────────────────────────────────
    let bold = Style::new().bold();

    doc.push(Paragraph::new(&invoice.business_name).styled(Style::new().bold().with_font_size(18)));
    doc.push(Paragraph::new(" "));
    doc.push(Paragraph::new(format!("Invoice #: {}", invoice.invoice_number)).styled(bold));
    doc.push(Paragraph::new(format!("Date: {}", invoice.date)));
    doc.push(Paragraph::new(" "));

    // ── Customer details ───────────────────────────────────────────
    doc.push(Paragraph::new("Customer Details").styled(Style::new().bold().with_font_size(14)));
    doc.push(Paragraph::new(format!("Name: {}", invoice.customer_name)));
    doc.push(Paragraph::new(format!("City: {}", invoice.customer_city)));
    doc.push(Paragraph::new(format!("Mobile: {}", invoice.customer_mobile)));
    doc.push(Paragraph::new(" "));

    // ── Line items table ───────────────────────────────────────────
    doc.push(Paragraph::new("Items").styled(Style::new().bold().with_font_size(14)));

    let mut table = TableLayout::new(vec![1, 3, 1, 1, 1]);
    table.set_cell_decorator(genpdf::elements::FrameCellDecorator::new(true, true, false));

    // Header row
    let header_style = Style::new().bold();
    let mut row = table.row();
    row.push_element(Paragraph::new("#").styled(header_style));
    row.push_element(Paragraph::new("Product").styled(header_style));
    row.push_element(Paragraph::new("Qty").styled(header_style));
    row.push_element(Paragraph::new("Price").styled(header_style));
    row.push_element(Paragraph::new("Subtotal").styled(header_style));
    row.push().map_err(|e| AppError::Unknown(format!("PDF table error: {e}")))?;

    // Data rows
    for (i, item) in invoice.line_items.iter().enumerate() {
        let subtotal = item.unit_price * item.quantity as i64;
        let mut row = table.row();
        row.push_element(Paragraph::new(format!("{}", i + 1)));
        row.push_element(Paragraph::new(&item.finished_good_name));
        row.push_element(Paragraph::new(format!("{}", item.quantity)));
        row.push_element(Paragraph::new(item.unit_price.to_display()));
        row.push_element(Paragraph::new(subtotal.to_display()));
        row.push().map_err(|e| AppError::Unknown(format!("PDF table error: {e}")))?;
    }

    doc.push(table);
    doc.push(Paragraph::new(" "));

    // ── Totals ─────────────────────────────────────────────────────
    doc.push(Paragraph::new(format!("Total: {}", invoice.total_amount.to_display())).styled(bold));
    doc.push(Paragraph::new(format!("Amount Paid: {}", invoice.amount_paid.to_display())));
    doc.push(
        Paragraph::new(format!("Remaining Balance: {}", invoice.remaining_balance.to_display()))
            .styled(bold),
    );

    // Render to bytes
    let mut buf = Vec::new();
    doc.render(&mut buf)
        .map_err(|e| AppError::Unknown(format!("PDF render error: {e}")))?;

    Ok(buf)
}


#[cfg(test)]
mod tests {
    use super::*;
    use crate::models::domain::SaleLineItem;
    use crate::models::Money;
    use uuid::Uuid;

    fn sample_invoice() -> Invoice {
        Invoice {
            business_name: "Sweet Lab".to_string(),
            customer_name: "Ahmad".to_string(),
            customer_city: "Damascus".to_string(),
            customer_mobile: "+963911111111".to_string(),
            line_items: vec![
                SaleLineItem {
                    finished_good_id: Uuid::new_v4(),
                    finished_good_name: "Sweet Box".to_string(),
                    quantity: 3,
                    unit_price: Money::from_f64(25.0),
                },
                SaleLineItem {
                    finished_good_id: Uuid::new_v4(),
                    finished_good_name: "Chocolate Bar".to_string(),
                    quantity: 5,
                    unit_price: Money::from_f64(10.0),
                },
            ],
            total_amount: Money::from_f64(125.0),
            amount_paid: Money::from_f64(100.0),
            remaining_balance: Money::from_f64(25.0),
            date: "2025-01-15".to_string(),
            invoice_number: "INV-abcd1234".to_string(),
        }
    }

    #[test]
    fn export_to_pdf_returns_validation_error_for_missing_font_dir() {
        let invoice = sample_invoice();
        let result = export_to_pdf(&invoice, "./nonexistent_fonts_dir");
        match result {
            Err(AppError::Validation { field, message }) => {
                assert_eq!(field, "font_dir");
                assert!(
                    message.contains("Font directory not found"),
                    "Error should mention font directory: {message}"
                );
            }
            other => panic!("Expected Validation error, got: {other:?}"),
        }
    }

    #[test]
    fn export_to_pdf_with_default_font_dir() {
        // With the default font dir, this may succeed or fail depending on environment
        let invoice = sample_invoice();
        let result = export_to_pdf(&invoice, "./fonts");
        match result {
            Ok(bytes) => assert!(!bytes.is_empty(), "PDF bytes should not be empty"),
            Err(e) => {
                let msg = format!("{e}");
                assert!(
                    msg.contains("font") || msg.contains("Font"),
                    "Error should mention fonts: {msg}"
                );
            }
        }
    }
}
