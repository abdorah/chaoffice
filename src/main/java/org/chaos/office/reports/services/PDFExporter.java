package org.chaos.office.reports.services;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.chaos.office.reports.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Exports report data to professionally formatted PDF documents.
 */
public class PDFExporter {
    private static final Logger logger = LoggerFactory.getLogger(PDFExporter.class);
    private static final DateTimeFormatter FILENAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 18, Font.BOLD);
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 14, Font.BOLD);
    private static final Font NORMAL_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL);
    private static final Font TABLE_HEADER_FONT = new Font(Font.HELVETICA, 10, Font.BOLD);
    
    /**
     * Exports report data to a PDF file.
     */
    public void export(ReportData reportData, String filePath) throws IOException, DocumentException {
        logger.info("Exporting report to PDF: {}", filePath);
        
        Document document = new Document(PageSize.A4);
        
        try {
            PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();
            
            // Add header
            addHeader(document, reportData);
            
            // Add content based on report type
            if (reportData instanceof SalesReportData) {
                addSalesContent(document, (SalesReportData) reportData);
            } else if (reportData instanceof InventoryReportData) {
                addInventoryContent(document, (InventoryReportData) reportData);
            }
            
            logger.info("PDF export completed successfully");
        } catch (DocumentException | IOException e) {
            logger.error("Failed to export PDF: {}", e.getMessage(), e);
            throw e;
        } finally {
            document.close();
        }
    }
    
    /**
     * Adds header section to the PDF document.
     */
    private void addHeader(Document document, ReportData reportData) throws DocumentException {
        // Company name
        Paragraph company = new Paragraph("ChaOffice Parts Inventory", TITLE_FONT);
        company.setAlignment(Element.ALIGN_CENTER);
        document.add(company);
        
        document.add(new Paragraph(" ")); // Spacing
        
        // Report title
        Paragraph title = new Paragraph(reportData.getReportTitle(), HEADER_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        
        // Generation timestamp
        Paragraph timestamp = new Paragraph("Generated on: " + 
                reportData.getGeneratedAt().format(DISPLAY_FORMATTER), NORMAL_FONT);
        timestamp.setAlignment(Element.ALIGN_CENTER);
        document.add(timestamp);
        
        // Report parameters
        Map<String, String> params = reportData.getParameters();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            Paragraph param = new Paragraph(entry.getKey() + ": " + entry.getValue(), NORMAL_FONT);
            param.setAlignment(Element.ALIGN_CENTER);
            document.add(param);
        }
        
        document.add(new Paragraph(" ")); // Spacing
        document.add(new Paragraph(" ")); // Spacing
    }
    
    /**
     * Adds sales report content to the PDF document.
     */
    private void addSalesContent(Document document, SalesReportData data) throws DocumentException {
        // Summary metrics
        document.add(new Paragraph("Summary Metrics", HEADER_FONT));
        document.add(new Paragraph(" "));
        
        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(100);
        addTableRow(summaryTable, "Total Revenue", formatCurrency(data.getTotalRevenue()), true);
        addTableRow(summaryTable, "Number of Sales", String.valueOf(data.getSalesCount()), false);
        addTableRow(summaryTable, "Average Sale Value", formatCurrency(data.getAverageSaleValue()), true);
        formatTable(summaryTable);
        document.add(summaryTable);
        
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));
        
        // Payment method breakdown
        document.add(new Paragraph("Payment Method Breakdown", HEADER_FONT));
        document.add(new Paragraph(" "));
        
        PdfPTable paymentTable = new PdfPTable(2);
        paymentTable.setWidthPercentage(100);
        addTableHeader(paymentTable, "Payment Method");
        addTableHeader(paymentTable, "Total Revenue");
        
        for (Map.Entry<PaymentMethod, BigDecimal> entry : data.getPaymentMethodBreakdown().entrySet()) {
            addTableRow(paymentTable, entry.getKey().toString(), formatCurrency(entry.getValue()), false);
        }
        formatTable(paymentTable);
        document.add(paymentTable);
        
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));
        
        // Discount analysis
        document.add(new Paragraph("Discount Analysis", HEADER_FONT));
        document.add(new Paragraph(" "));
        
        PdfPTable discountTable = new PdfPTable(2);
        discountTable.setWidthPercentage(100);
        addTableRow(discountTable, "Total Discounts Given", formatCurrency(data.getTotalDiscounts()), true);
        addTableRow(discountTable, "Average Discount Percentage", data.getAverageDiscountPercentage() + "%", false);
        formatTable(discountTable);
        document.add(discountTable);
        
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));
        
        // Top selling parts
        document.add(new Paragraph("Top Selling Parts", HEADER_FONT));
        document.add(new Paragraph(" "));
        
        PdfPTable partsTable = new PdfPTable(3);
        partsTable.setWidthPercentage(100);
        addTableHeader(partsTable, "Part Name");
        addTableHeader(partsTable, "Quantity Sold");
        addTableHeader(partsTable, "Revenue");
        
        for (TopSellingPart part : data.getTopSellingParts()) {
            addTableCell(partsTable, part.getPartName());
            addTableCell(partsTable, String.valueOf(part.getQuantitySold()));
            addTableCell(partsTable, formatCurrency(part.getRevenue()));
        }
        formatTable(partsTable);
        document.add(partsTable);
    }
    
    /**
     * Adds inventory report content to the PDF document.
     */
    private void addInventoryContent(Document document, InventoryReportData data) throws DocumentException {
        // Summary metrics
        document.add(new Paragraph("Summary Metrics", HEADER_FONT));
        document.add(new Paragraph(" "));
        
        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(100);
        addTableRow(summaryTable, "Total Inventory Value", formatCurrency(data.getTotalInventoryValue()), true);
        addTableRow(summaryTable, "Low Stock Items", String.valueOf(data.getLowStockParts().size()), false);
        addTableRow(summaryTable, "Out of Stock Items", String.valueOf(data.getOutOfStockParts().size()), true);
        formatTable(summaryTable);
        document.add(summaryTable);
        
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));
        
        // Inventory details by category
        document.add(new Paragraph("Inventory Details", HEADER_FONT));
        document.add(new Paragraph(" "));
        
        PdfPTable inventoryTable = new PdfPTable(6);
        inventoryTable.setWidthPercentage(100);
        addTableHeader(inventoryTable, "Category");
        addTableHeader(inventoryTable, "Part Name");
        addTableHeader(inventoryTable, "Quantity");
        addTableHeader(inventoryTable, "Price");
        addTableHeader(inventoryTable, "Stock Value");
        addTableHeader(inventoryTable, "Status");
        
        for (Map.Entry<String, List<PartInventoryItem>> entry : data.getPartsByCategory().entrySet()) {
            for (PartInventoryItem item : entry.getValue()) {
                addTableCell(inventoryTable, entry.getKey());
                addTableCell(inventoryTable, item.getPartName());
                addTableCell(inventoryTable, String.valueOf(item.getQuantity()));
                addTableCell(inventoryTable, formatCurrency(item.getPrice()));
                addTableCell(inventoryTable, formatCurrency(item.getStockValue()));
                
                // Highlight low stock and out of stock items
                PdfPCell statusCell = new PdfPCell(new Phrase(item.getStatus().toString(), NORMAL_FONT));
                if (item.getStatus() == StockStatus.OUT_OF_STOCK) {
                    statusCell.setBackgroundColor(new Color(255, 200, 200)); // Light red
                } else if (item.getStatus() == StockStatus.LOW_STOCK) {
                    statusCell.setBackgroundColor(new Color(255, 255, 200)); // Light yellow
                }
                inventoryTable.addCell(statusCell);
            }
        }
        formatTable(inventoryTable);
        document.add(inventoryTable);
    }
    
    /**
     * Adds a header cell to a table.
     */
    private void addTableHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TABLE_HEADER_FONT));
        cell.setBackgroundColor(new Color(200, 200, 200));
        cell.setPadding(5);
        table.addCell(cell);
    }
    
    /**
     * Adds a regular cell to a table.
     */
    private void addTableCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, NORMAL_FONT));
        cell.setPadding(5);
        table.addCell(cell);
    }
    
    /**
     * Adds a two-column row to a table.
     */
    private void addTableRow(PdfPTable table, String label, String value, boolean highlight) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, TABLE_HEADER_FONT));
        labelCell.setPadding(5);
        if (highlight) {
            labelCell.setBackgroundColor(new Color(240, 240, 240));
        }
        table.addCell(labelCell);
        
        PdfPCell valueCell = new PdfPCell(new Phrase(value, NORMAL_FONT));
        valueCell.setPadding(5);
        if (highlight) {
            valueCell.setBackgroundColor(new Color(240, 240, 240));
        }
        table.addCell(valueCell);
    }
    
    /**
     * Applies formatting to a table.
     */
    private void formatTable(PdfPTable table) {
        table.setSpacingBefore(10);
        table.setSpacingAfter(10);
    }
    
    /**
     * Formats currency values with dollar sign and two decimal places.
     */
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "$0.00";
        }
        return String.format("$%,.2f", amount);
    }
    
    /**
     * Generates a filename with timestamp for the report.
     */
    public String generateFilename(String reportType) {
        String timestamp = LocalDateTime.now().format(FILENAME_FORMATTER);
        String sanitizedType = reportType.replace(" ", "");
        return sanitizedType + "_" + timestamp + ".pdf";
    }
}
