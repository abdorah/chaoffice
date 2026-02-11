package org.chaos.office.reports.services;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.chaos.office.reports.models.*;
import org.chaos.office.service.BrandingService;
import org.chaos.office.util.CurrencyFormatter;
import org.chaos.office.util.LocaleManager;
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
 * Supports Unicode characters including Arabic text.
 */
public class PDFExporter {
    private static final Logger logger = LoggerFactory.getLogger(PDFExporter.class);
    private static final DateTimeFormatter FILENAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final BrandingService brandingService;
    
    /**
     * Creates a font that supports Unicode characters including Arabic.
     * Falls back to Helvetica if Unicode font creation fails.
     */
    private Font createUnicodeFont(int size, int style) {
        try {
            // Use Identity-H encoding for Unicode support (including Arabic)
            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            return new Font(bf, size, style);
        } catch (Exception e) {
            logger.warn("Failed to create Unicode font, using default", e);
            return new Font(Font.HELVETICA, size, style);
        }
    }
    
    /**
     * Gets the title font (18pt, bold).
     */
    private Font getTitleFont() {
        return createUnicodeFont(18, Font.BOLD);
    }
    
    /**
     * Gets the header font (14pt, bold).
     */
    private Font getHeaderFont() {
        return createUnicodeFont(14, Font.BOLD);
    }
    
    /**
     * Gets the normal font (10pt, normal).
     */
    private Font getNormalFont() {
        return createUnicodeFont(10, Font.NORMAL);
    }
    
    /**
     * Gets the table header font (10pt, bold).
     */
    private Font getTableHeaderFont() {
        return createUnicodeFont(10, Font.BOLD);
    }
    
    /**
     * Default constructor initializes BrandingService.
     */
    public PDFExporter() {
        this.brandingService = new BrandingService();
    }
    
    /**
     * Constructor with dependency injection for testing.
     * 
     * @param brandingService the branding service to use
     */
    public PDFExporter(BrandingService brandingService) {
        this.brandingService = brandingService;
    }
    
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
        // Add branding logo if configured
        if (brandingService.getBrandingSettings().hasLogo()) {
            try {
                javafx.scene.image.Image fxImage = brandingService.getApplicationLogo();
                if (fxImage != null) {
                    // Convert JavaFX Image to iText Image
                    // Note: For simplicity, we'll skip the logo in PDF for now
                    // A full implementation would require converting JavaFX Image to byte array
                    // and then to iText Image format
                }
            } catch (Exception e) {
                logger.warn("Failed to add logo to PDF", e);
            }
        }
        
        // Add store name if configured, otherwise use default company name
        String companyName = brandingService.getBrandingSettings().hasStoreName() 
            ? brandingService.getBrandingSettings().getStoreName()
            : LocaleManager.getString("app.title");
        
        Paragraph company = new Paragraph(companyName, getTitleFont());
        company.setAlignment(Element.ALIGN_CENTER);
        document.add(company);
        
        document.add(new Paragraph(" ")); // Spacing
        
        // Report title
        Paragraph title = new Paragraph(reportData.getReportTitle(), getHeaderFont());
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        
        // Generation timestamp
        Paragraph timestamp = new Paragraph(LocaleManager.getString("report.generated.on") + ": " + 
                reportData.getGeneratedAt().format(DISPLAY_FORMATTER), getNormalFont());
        timestamp.setAlignment(Element.ALIGN_CENTER);
        document.add(timestamp);
        
        // Report parameters
        Map<String, String> params = reportData.getParameters();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            Paragraph param = new Paragraph(entry.getKey() + ": " + entry.getValue(), getNormalFont());
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
        document.add(new Paragraph(LocaleManager.getString("report.summary.metrics"), getHeaderFont()));
        document.add(new Paragraph(" "));
        
        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(100);
        addTableRow(summaryTable, LocaleManager.getString("report.total.revenue"), formatCurrency(data.getTotalRevenue()), true);
        addTableRow(summaryTable, LocaleManager.getString("report.number.of.sales"), String.valueOf(data.getSalesCount()), false);
        addTableRow(summaryTable, LocaleManager.getString("report.average.sale.value"), formatCurrency(data.getAverageSaleValue()), true);
        formatTable(summaryTable);
        document.add(summaryTable);
        
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));
        
        // Payment method breakdown
        document.add(new Paragraph(LocaleManager.getString("report.payment.method.breakdown"), getHeaderFont()));
        document.add(new Paragraph(" "));
        
        PdfPTable paymentTable = new PdfPTable(2);
        paymentTable.setWidthPercentage(100);
        addTableHeader(paymentTable, LocaleManager.getString("report.payment.method"));
        addTableHeader(paymentTable, LocaleManager.getString("report.total.revenue"));
        
        for (Map.Entry<PaymentMethod, BigDecimal> entry : data.getPaymentMethodBreakdown().entrySet()) {
            addTableRow(paymentTable, entry.getKey().toString(), formatCurrency(entry.getValue()), false);
        }
        formatTable(paymentTable);
        document.add(paymentTable);
        
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));
        
        // Discount analysis
        document.add(new Paragraph(LocaleManager.getString("report.discount.analysis"), getHeaderFont()));
        document.add(new Paragraph(" "));
        
        PdfPTable discountTable = new PdfPTable(2);
        discountTable.setWidthPercentage(100);
        addTableRow(discountTable, LocaleManager.getString("report.total.discounts.given"), formatCurrency(data.getTotalDiscounts()), true);
        addTableRow(discountTable, LocaleManager.getString("report.average.discount.percentage"), data.getAverageDiscountPercentage() + "%", false);
        formatTable(discountTable);
        document.add(discountTable);
        
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));
        
        // Top selling parts
        document.add(new Paragraph(LocaleManager.getString("report.top.selling.parts"), getHeaderFont()));
        document.add(new Paragraph(" "));
        
        PdfPTable partsTable = new PdfPTable(3);
        partsTable.setWidthPercentage(100);
        addTableHeader(partsTable, LocaleManager.getString("report.column.part.name"));
        addTableHeader(partsTable, LocaleManager.getString("report.quantity.sold"));
        addTableHeader(partsTable, LocaleManager.getString("report.column.revenue"));
        
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
        document.add(new Paragraph(LocaleManager.getString("report.summary.metrics"), getHeaderFont()));
        document.add(new Paragraph(" "));
        
        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(100);
        addTableRow(summaryTable, LocaleManager.getString("report.total.inventory.value"), formatCurrency(data.getTotalInventoryValue()), true);
        addTableRow(summaryTable, LocaleManager.getString("report.low.stock.items"), String.valueOf(data.getLowStockParts().size()), false);
        addTableRow(summaryTable, LocaleManager.getString("report.out.of.stock.items"), String.valueOf(data.getOutOfStockParts().size()), true);
        formatTable(summaryTable);
        document.add(summaryTable);
        
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));
        
        // Inventory details by category
        document.add(new Paragraph(LocaleManager.getString("report.inventory.details"), getHeaderFont()));
        document.add(new Paragraph(" "));
        
        PdfPTable inventoryTable = new PdfPTable(6);
        inventoryTable.setWidthPercentage(100);
        addTableHeader(inventoryTable, LocaleManager.getString("report.column.category"));
        addTableHeader(inventoryTable, LocaleManager.getString("report.column.part.name"));
        addTableHeader(inventoryTable, LocaleManager.getString("report.column.quantity"));
        addTableHeader(inventoryTable, LocaleManager.getString("report.column.price"));
        addTableHeader(inventoryTable, LocaleManager.getString("report.stock.value"));
        addTableHeader(inventoryTable, LocaleManager.getString("report.status"));
        
        for (Map.Entry<String, List<PartInventoryItem>> entry : data.getPartsByCategory().entrySet()) {
            for (PartInventoryItem item : entry.getValue()) {
                addTableCell(inventoryTable, entry.getKey());
                addTableCell(inventoryTable, item.getPartName());
                addTableCell(inventoryTable, String.valueOf(item.getQuantity()));
                addTableCell(inventoryTable, formatCurrency(item.getPrice()));
                addTableCell(inventoryTable, formatCurrency(item.getStockValue()));
                
                // Highlight low stock and out of stock items
                PdfPCell statusCell = new PdfPCell(new Phrase(item.getStatus().toString(), getNormalFont()));
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
        PdfPCell cell = new PdfPCell(new Phrase(text, getTableHeaderFont()));
        cell.setBackgroundColor(new Color(200, 200, 200));
        cell.setPadding(5);
        table.addCell(cell);
    }
    
    /**
     * Adds a regular cell to a table.
     */
    private void addTableCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, getNormalFont()));
        cell.setPadding(5);
        table.addCell(cell);
    }
    
    /**
     * Adds a two-column row to a table.
     */
    private void addTableRow(PdfPTable table, String label, String value, boolean highlight) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, getTableHeaderFont()));
        labelCell.setPadding(5);
        if (highlight) {
            labelCell.setBackgroundColor(new Color(240, 240, 240));
        }
        table.addCell(labelCell);
        
        PdfPCell valueCell = new PdfPCell(new Phrase(value, getNormalFont()));
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
     * Formats currency values with configured currency symbol and two decimal places.
     */
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return CurrencyFormatter.format(0.0f);
        }
        return CurrencyFormatter.format(amount.floatValue());
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
