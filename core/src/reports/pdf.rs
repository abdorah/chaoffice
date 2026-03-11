use genpdf::elements::{Paragraph, TableLayout};
use genpdf::style::Style;
use genpdf::{Document, Element, SimplePageDecorator};

use crate::error::{AppError, AppResult};
use crate::models::domain::Invoice;

/// Default font directory — can be overridden by placing Liberation fonts here.
const DEFAULT_FONT_DIR: &str = "./fonts";
const DEFAULT_FONT_FAMILY: &str = "LiberationSans";

/// Render an Invoice to PDF bytes (Req 12.4).
///
/// Uses genpdf with LiberationSans fonts from `./fonts/`. If fonts are not
/// available, returns an error with a descriptive message.
pub fn export_to_pdf(invoice: &Invoice) -> AppResult<Vec<u8>> {
    let font_family =
        genpdf::fonts::from_files(DEFAULT_FONT_DIR, DEFAULT_FONT_FAMILY, None).map_err(|e| {
            AppError::Unknown(format!(
                "Failed to load fonts from '{DEFAULT_FONT_DIR}/{DEFAULT_FONT_FAMILY}': {e}. \
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
        let subtotal = item.quantity as f64 * item.unit_price;
        let mut row = table.row();
        row.push_element(Paragraph::new(format!("{}", i + 1)));
        row.push_element(Paragraph::new(&item.finished_good_name));
        row.push_element(Paragraph::new(format!("{}", item.quantity)));
        row.push_element(Paragraph::new(format!("{:.2}", item.unit_price)));
        row.push_element(Paragraph::new(format!("{:.2}", subtotal)));
        row.push().map_err(|e| AppError::Unknown(format!("PDF table error: {e}")))?;
    }

    doc.push(table);
    doc.push(Paragraph::new(" "));

    // ── Totals ─────────────────────────────────────────────────────
    doc.push(Paragraph::new(format!("Total: {:.2}", invoice.total_amount)).styled(bold));
    doc.push(Paragraph::new(format!("Amount Paid: {:.2}", invoice.amount_paid)));
    doc.push(
        Paragraph::new(format!("Remaining Balance: {:.2}", invoice.remaining_balance))
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
                    unit_price: 25.0,
                },
                SaleLineItem {
                    finished_good_id: Uuid::new_v4(),
                    finished_good_name: "Chocolate Bar".to_string(),
                    quantity: 5,
                    unit_price: 10.0,
                },
            ],
            total_amount: 125.0,
            amount_paid: 100.0,
            remaining_balance: 25.0,
            date: "2025-01-15".to_string(),
            invoice_number: "INV-abcd1234".to_string(),
        }
    }

    #[test]
    fn export_to_pdf_returns_error_without_fonts() {
        // Without font files in ./fonts/, this should return a descriptive error
        let invoice = sample_invoice();
        let result = export_to_pdf(&invoice);
        // In CI/test environments without fonts, we expect an error
        // If fonts are present, we expect non-empty bytes
        match result {
            Ok(bytes) => assert!(!bytes.is_empty(), "PDF bytes should not be empty"),
            Err(e) => {
                let msg = format!("{e}");
                assert!(msg.contains("font"), "Error should mention fonts: {msg}");
            }
        }
    }
}
