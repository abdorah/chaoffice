use rust_xlsxwriter::{Format, Workbook};

use crate::error::{AppError, AppResult};
use crate::models::domain::Invoice;

/// Render an Invoice to Excel (.xlsx) bytes (Req 12.4).
///
/// Creates a workbook with a single "Invoice" sheet containing:
/// - Business name and invoice metadata
/// - Customer details
/// - Itemized line items with headers
/// - Totals section
pub fn export_to_excel(invoice: &Invoice) -> AppResult<Vec<u8>> {
    let mut workbook = Workbook::new();
    let sheet = workbook.add_worksheet();
    sheet
        .set_name("Invoice")
        .map_err(|e| AppError::Unknown(format!("Excel sheet error: {e}")))?;

    let bold = Format::new().set_bold();
    let header_fmt = Format::new().set_bold().set_font_size(14.0);
    let money_fmt = Format::new().set_num_format("0.00");
    let bold_money = Format::new().set_bold().set_num_format("0.00");

    let mut row: u32 = 0;

    // ── Header ─────────────────────────────────────────────────────
    sheet
        .write_string_with_format(row, 0, &invoice.business_name, &header_fmt)
        .map_err(xlsx_err)?;
    row += 1;
    sheet
        .write_string_with_format(row, 0, &format!("Invoice #: {}", invoice.invoice_number), &bold)
        .map_err(xlsx_err)?;
    row += 1;
    sheet
        .write_string(row, 0, &format!("Date: {}", invoice.date))
        .map_err(xlsx_err)?;
    row += 2;

    // ── Customer details ───────────────────────────────────────────
    sheet
        .write_string_with_format(row, 0, "Customer Details", &bold)
        .map_err(xlsx_err)?;
    row += 1;
    sheet.write_string(row, 0, "Name:").map_err(xlsx_err)?;
    sheet.write_string(row, 1, &invoice.customer_name).map_err(xlsx_err)?;
    row += 1;
    sheet.write_string(row, 0, "City:").map_err(xlsx_err)?;
    sheet.write_string(row, 1, &invoice.customer_city).map_err(xlsx_err)?;
    row += 1;
    sheet.write_string(row, 0, "Mobile:").map_err(xlsx_err)?;
    sheet.write_string(row, 1, &invoice.customer_mobile).map_err(xlsx_err)?;
    row += 2;

    // ── Line items table ───────────────────────────────────────────
    sheet.write_string_with_format(row, 0, "#", &bold).map_err(xlsx_err)?;
    sheet.write_string_with_format(row, 1, "Product", &bold).map_err(xlsx_err)?;
    sheet.write_string_with_format(row, 2, "Qty", &bold).map_err(xlsx_err)?;
    sheet.write_string_with_format(row, 3, "Unit Price", &bold).map_err(xlsx_err)?;
    sheet.write_string_with_format(row, 4, "Subtotal", &bold).map_err(xlsx_err)?;
    row += 1;

    for (i, item) in invoice.line_items.iter().enumerate() {
        let subtotal = item.quantity as f64 * item.unit_price;
        sheet.write_number(row, 0, (i + 1) as f64).map_err(xlsx_err)?;
        sheet.write_string(row, 1, &item.finished_good_name).map_err(xlsx_err)?;
        sheet.write_number(row, 2, item.quantity as f64).map_err(xlsx_err)?;
        sheet.write_number_with_format(row, 3, item.unit_price, &money_fmt).map_err(xlsx_err)?;
        sheet.write_number_with_format(row, 4, subtotal, &money_fmt).map_err(xlsx_err)?;
        row += 1;
    }

    row += 1;

    // ── Totals ─────────────────────────────────────────────────────
    sheet.write_string_with_format(row, 3, "Total:", &bold).map_err(xlsx_err)?;
    sheet.write_number_with_format(row, 4, invoice.total_amount, &bold_money).map_err(xlsx_err)?;
    row += 1;
    sheet.write_string(row, 3, "Amount Paid:").map_err(xlsx_err)?;
    sheet.write_number_with_format(row, 4, invoice.amount_paid, &money_fmt).map_err(xlsx_err)?;
    row += 1;
    sheet.write_string_with_format(row, 3, "Remaining:", &bold).map_err(xlsx_err)?;
    sheet.write_number_with_format(row, 4, invoice.remaining_balance, &bold_money).map_err(xlsx_err)?;

    // Set reasonable column widths
    sheet.set_column_width(0, 5).map_err(xlsx_err)?;
    sheet.set_column_width(1, 25).map_err(xlsx_err)?;
    sheet.set_column_width(2, 8).map_err(xlsx_err)?;
    sheet.set_column_width(3, 12).map_err(xlsx_err)?;
    sheet.set_column_width(4, 12).map_err(xlsx_err)?;

    let buf = workbook
        .save_to_buffer()
        .map_err(|e| AppError::Unknown(format!("Excel save error: {e}")))?;

    Ok(buf)
}

fn xlsx_err(e: rust_xlsxwriter::XlsxError) -> AppError {
    AppError::Unknown(format!("Excel write error: {e}"))
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
    fn export_to_excel_returns_non_empty_bytes() {
        let invoice = sample_invoice();
        let bytes = export_to_excel(&invoice).unwrap();
        assert!(!bytes.is_empty());
        // XLSX files start with PK (ZIP magic bytes)
        assert_eq!(&bytes[0..2], b"PK", "Should be a valid ZIP/XLSX file");
    }

    #[test]
    fn export_to_excel_empty_line_items() {
        let mut invoice = sample_invoice();
        invoice.line_items.clear();
        invoice.total_amount = 0.0;
        invoice.amount_paid = 0.0;
        invoice.remaining_balance = 0.0;

        let bytes = export_to_excel(&invoice).unwrap();
        assert!(!bytes.is_empty());
    }
}
