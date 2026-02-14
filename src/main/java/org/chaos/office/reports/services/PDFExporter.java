package org.chaos.office.reports.services;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.chaos.office.reports.models.*;
import org.chaos.office.service.BrandingService;
import org.chaos.office.util.ArabicTextProcessor;
import org.chaos.office.util.CurrencyFormatter;
import org.chaos.office.util.EnumLocalizer;
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
     * Uses Identity-H encoding for proper Unicode support.
     */
    private Font createUnicodeFont(int size, int style) {
        try {
            // Try multiple font options that support Arabic
            String[] fontOptions = {
                "c:/windows/fonts/arial.ttf",           // Windows Arial
                "c:/windows/fonts/arialuni.ttf",        // Windows Arial Unicode MS
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",  // Linux DejaVu Sans
                "/System/Library/Fonts/Supplemental/Arial Unicode.ttf"  // macOS Arial Unicode
            };
            
            for (String fontPath : fontOptions) {
                try {
                    BaseFont bf = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                    return new Font(bf, size, style);
                } catch (Exception ignored) {
                    // Try next font
                }
            }
            
            // If no file-based fonts work, try system fonts
            BaseFont bf = BaseFont.createFont("Arial", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            return new Font(bf, size, style);
        } catch (Exception e) {
            logger.warn("Failed to create Unicode font with Arabic support, using Helvetica", e);
            try {
                // Last resort: use Helvetica (won't render Arabic correctly)
                BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
                return new Font(bf, size, style);
            } catch (Exception e2) {
                logger.error("Failed to create any font", e2);
                return new Font(Font.HELVETICA, size, style);
            }
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
        // Check if current locale is Arabic (RTL)
        boolean isRTL = LocaleManager.getCurrentLocale().getLanguage().equals("ar");
        int alignment = isRTL ? Element.ALIGN_RIGHT : Element.ALIGN_CENTER;
        
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
        
        if (isRTL) {
            // Only shape, don't reorder - let the table's RTL direction handle ordering
            companyName = ArabicTextProcessor.shapeOnly(companyName);
        }
        
        if (isRTL) {
            // Use a single-cell table for proper Arabic rendering
            PdfPTable companyTable = new PdfPTable(1);
            companyTable.setWidthPercentage(100);
            companyTable.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
            PdfPCell companyCell = new PdfPCell(new Phrase(companyName, getTitleFont()));
            companyCell.setBorder(Rectangle.NO_BORDER);
            companyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            companyTable.addCell(companyCell);
            document.add(companyTable);
        } else {
            Paragraph company = new Paragraph(companyName, getTitleFont());
            company.setAlignment(alignment);
            document.add(company);
        }
        
        document.add(new Paragraph(" ")); // Spacing
        
        // Report title
        String titleText = reportData.getReportTitle();
        if (isRTL) {
            // Only shape, don't reorder - let the table's RTL direction handle ordering
            titleText = ArabicTextProcessor.shapeOnly(titleText);
        }
        
        if (isRTL) {
            // Use a single-cell table for proper Arabic rendering
            PdfPTable titleTable = new PdfPTable(1);
            titleTable.setWidthPercentage(100);
            titleTable.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
            PdfPCell titleCell = new PdfPCell(new Phrase(titleText, getHeaderFont()));
            titleCell.setBorder(Rectangle.NO_BORDER);
            titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            titleTable.addCell(titleCell);
            document.add(titleTable);
        } else {
            Paragraph title = new Paragraph(titleText, getHeaderFont());
            title.setAlignment(alignment);
            document.add(title);
        }
        
        // Generation timestamp
        String timestampLabel = LocaleManager.getString("report.generated.on");
        String timestampValue = reportData.getGeneratedAt().format(DISPLAY_FORMATTER);
        String timestampText = timestampLabel + ": " + timestampValue;
        
        if (isRTL) {
            // Only shape, don't reorder - let the table's RTL direction handle ordering
            timestampText = ArabicTextProcessor.shapeOnly(timestampText);
        }
        
        if (isRTL) {
            PdfPTable timestampTable = new PdfPTable(1);
            timestampTable.setWidthPercentage(100);
            timestampTable.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
            PdfPCell timestampCell = new PdfPCell(new Phrase(timestampText, getNormalFont()));
            timestampCell.setBorder(Rectangle.NO_BORDER);
            timestampCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            timestampTable.addCell(timestampCell);
            document.add(timestampTable);
        } else {
            Paragraph timestamp = new Paragraph(timestampText, getNormalFont());
            timestamp.setAlignment(alignment);
            document.add(timestamp);
        }
        
        // Report parameters
        Map<String, String> params = reportData.getParameters();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String paramLabel = entry.getKey();
            String paramValue = entry.getValue();
            String paramText = paramLabel + ": " + paramValue;
            
            if (isRTL) {
                // Only shape, don't reorder - let the table's RTL direction handle ordering
                paramText = ArabicTextProcessor.shapeOnly(paramText);
            }
            
            if (isRTL) {
                PdfPTable paramTable = new PdfPTable(1);
                paramTable.setWidthPercentage(100);
                paramTable.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
                PdfPCell paramCell = new PdfPCell(new Phrase(paramText, getNormalFont()));
                paramCell.setBorder(Rectangle.NO_BORDER);
                paramCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                paramTable.addCell(paramCell);
                document.add(paramTable);
            } else {
                Paragraph param = new Paragraph(paramText, getNormalFont());
                param.setAlignment(alignment);
                document.add(param);
            }
        }
        
        document.add(new Paragraph(" ")); // Spacing
        document.add(new Paragraph(" ")); // Spacing
    }
    
    /**
     * Adds a title/heading to the document with proper RTL support.
     * For Arabic, uses a borderless table to ensure proper text rendering.
     */
    private void addTitle(Document document, String text, Font font, int alignment, boolean isRTL) throws DocumentException {
        if (isRTL) {
            // Only shape, don't reorder - let the table's RTL direction handle ordering
            text = ArabicTextProcessor.shapeOnly(text);
            // Use a single-cell table for proper Arabic rendering
            PdfPTable titleTable = new PdfPTable(1);
            titleTable.setWidthPercentage(100);
            titleTable.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
            PdfPCell titleCell = new PdfPCell(new Phrase(text, font));
            titleCell.setBorder(Rectangle.NO_BORDER);
            titleCell.setHorizontalAlignment(alignment);
            titleTable.addCell(titleCell);
            document.add(titleTable);
        } else {
            Paragraph title = new Paragraph(text, font);
            title.setAlignment(alignment);
            document.add(title);
        }
    }
    
    /**
     * Adds sales report content to the PDF document.
     */
    private void addSalesContent(Document document, SalesReportData data) throws DocumentException {
        boolean isRTL = LocaleManager.getCurrentLocale().getLanguage().equals("ar");
        int alignment = isRTL ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT;
        
        // Summary metrics
        addTitle(document, LocaleManager.getString("report.summary.metrics"), getHeaderFont(), alignment, isRTL);
        document.add(new Paragraph(" "));
        
        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(100);
        if (isRTL) {
            summaryTable.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        }
        addTableRow(summaryTable, LocaleManager.getString("report.total.revenue"), formatCurrency(data.getTotalRevenue()), true, isRTL);
        addTableRow(summaryTable, LocaleManager.getString("report.number.of.sales"), String.valueOf(data.getSalesCount()), false, isRTL);
        addTableRow(summaryTable, LocaleManager.getString("report.average.sale.value"), formatCurrency(data.getAverageSaleValue()), true, isRTL);
        formatTable(summaryTable);
        document.add(summaryTable);
        
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));
        
        // Payment method breakdown
        addTitle(document, LocaleManager.getString("report.payment.method.breakdown"), getHeaderFont(), alignment, isRTL);
        document.add(new Paragraph(" "));
        
        PdfPTable paymentTable = new PdfPTable(2);
        paymentTable.setWidthPercentage(100);
        if (isRTL) {
            paymentTable.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        }
        addTableHeader(paymentTable, LocaleManager.getString("report.payment.method"), isRTL);
        addTableHeader(paymentTable, LocaleManager.getString("report.total.revenue"), isRTL);
        
        for (Map.Entry<PaymentMethod, BigDecimal> entry : data.getPaymentMethodBreakdown().entrySet()) {
            String localizedMethod = EnumLocalizer.getLocalizedPaymentMethod(entry.getKey());
            addTableRow(paymentTable, localizedMethod, formatCurrency(entry.getValue()), false, isRTL);
        }
        formatTable(paymentTable);
        document.add(paymentTable);
        
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));
        
        // Discount analysis
        addTitle(document, LocaleManager.getString("report.discount.analysis"), getHeaderFont(), alignment, isRTL);
        document.add(new Paragraph(" "));
        
        PdfPTable discountTable = new PdfPTable(2);
        discountTable.setWidthPercentage(100);
        if (isRTL) {
            discountTable.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        }
        addTableRow(discountTable, LocaleManager.getString("report.total.discounts.given"), formatCurrency(data.getTotalDiscounts()), true, isRTL);
        addTableRow(discountTable, LocaleManager.getString("report.average.discount.percentage"), data.getAverageDiscountPercentage() + "%", false, isRTL);
        formatTable(discountTable);
        document.add(discountTable);
        
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));
        
        // Top selling parts
        addTitle(document, LocaleManager.getString("report.top.selling.parts"), getHeaderFont(), alignment, isRTL);
        document.add(new Paragraph(" "));
        
        PdfPTable partsTable = new PdfPTable(3);
        partsTable.setWidthPercentage(100);
        if (isRTL) {
            partsTable.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        }
        addTableHeader(partsTable, LocaleManager.getString("report.column.part.name"), isRTL);
        addTableHeader(partsTable, LocaleManager.getString("report.quantity.sold"), isRTL);
        addTableHeader(partsTable, LocaleManager.getString("report.column.revenue"), isRTL);
        
        for (TopSellingPart part : data.getTopSellingParts()) {
            addTableCell(partsTable, part.getPartName(), isRTL);
            addTableCell(partsTable, String.valueOf(part.getQuantitySold()), isRTL);
            addTableCell(partsTable, formatCurrency(part.getRevenue()), isRTL);
        }
        formatTable(partsTable);
        document.add(partsTable);
    }
    
    /**
     * Adds inventory report content to the PDF document.
     */
    private void addInventoryContent(Document document, InventoryReportData data) throws DocumentException {
        boolean isRTL = LocaleManager.getCurrentLocale().getLanguage().equals("ar");
        int alignment = isRTL ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT;
        
        // Summary metrics
        addTitle(document, LocaleManager.getString("report.summary.metrics"), getHeaderFont(), alignment, isRTL);
        document.add(new Paragraph(" "));
        
        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(100);
        if (isRTL) {
            summaryTable.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        }
        addTableRow(summaryTable, LocaleManager.getString("report.total.inventory.value"), formatCurrency(data.getTotalInventoryValue()), true, isRTL);
        addTableRow(summaryTable, LocaleManager.getString("report.low.stock.items"), String.valueOf(data.getLowStockParts().size()), false, isRTL);
        addTableRow(summaryTable, LocaleManager.getString("report.out.of.stock.items"), String.valueOf(data.getOutOfStockParts().size()), true, isRTL);
        formatTable(summaryTable);
        document.add(summaryTable);
        
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));
        
        // Inventory details by category
        addTitle(document, LocaleManager.getString("report.inventory.details"), getHeaderFont(), alignment, isRTL);
        document.add(new Paragraph(" "));
        
        PdfPTable inventoryTable = new PdfPTable(6);
        inventoryTable.setWidthPercentage(100);
        if (isRTL) {
            inventoryTable.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        }
        addTableHeader(inventoryTable, LocaleManager.getString("report.column.category"), isRTL);
        addTableHeader(inventoryTable, LocaleManager.getString("report.column.part.name"), isRTL);
        addTableHeader(inventoryTable, LocaleManager.getString("report.column.quantity"), isRTL);
        addTableHeader(inventoryTable, LocaleManager.getString("report.column.price"), isRTL);
        addTableHeader(inventoryTable, LocaleManager.getString("report.stock.value"), isRTL);
        addTableHeader(inventoryTable, LocaleManager.getString("report.status"), isRTL);
        
        for (Map.Entry<String, List<PartInventoryItem>> entry : data.getPartsByCategory().entrySet()) {
            for (PartInventoryItem item : entry.getValue()) {
                addTableCell(inventoryTable, entry.getKey(), isRTL);
                addTableCell(inventoryTable, item.getPartName(), isRTL);
                addTableCell(inventoryTable, String.valueOf(item.getQuantity()), isRTL);
                addTableCell(inventoryTable, formatCurrency(item.getPrice()), isRTL);
                addTableCell(inventoryTable, formatCurrency(item.getStockValue()), isRTL);
                
                // Highlight low stock and out of stock items
                String statusText = EnumLocalizer.getLocalizedStockStatus(item.getStatus());
                PdfPCell statusCell = new PdfPCell(new Phrase(statusText, getNormalFont()));
                statusCell.setPadding(5);
                if (isRTL) {
                    statusCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                }
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
     * Adds a header cell to a table with RTL support.
     */
    private void addTableHeader(PdfPTable table, String text, boolean isRTL) {
        if (isRTL) {
            text = ArabicTextProcessor.shapeOnly(text);
        }
        PdfPCell cell = new PdfPCell(new Phrase(text, getTableHeaderFont()));
        cell.setBackgroundColor(new Color(200, 200, 200));
        cell.setPadding(5);
        if (isRTL) {
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        }
        table.addCell(cell);
    }
    
    /**
     * Adds a regular cell to a table with RTL support.
     */
    private void addTableCell(PdfPTable table, String text, boolean isRTL) {
        if (isRTL && ArabicTextProcessor.containsArabic(text)) {
            text = ArabicTextProcessor.shapeOnly(text);
        }
        PdfPCell cell = new PdfPCell(new Phrase(text, getNormalFont()));
        cell.setPadding(5);
        if (isRTL) {
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        }
        table.addCell(cell);
    }
    
    /**
     * Adds a two-column row to a table with RTL support.
     */
    private void addTableRow(PdfPTable table, String label, String value, boolean highlight, boolean isRTL) {
        if (isRTL && ArabicTextProcessor.containsArabic(label)) {
            label = ArabicTextProcessor.shapeOnly(label);
        }
        if (isRTL && ArabicTextProcessor.containsArabic(value)) {
            value = ArabicTextProcessor.shapeOnly(value);
        }
        
        PdfPCell labelCell = new PdfPCell(new Phrase(label, getTableHeaderFont()));
        labelCell.setPadding(5);
        if (highlight) {
            labelCell.setBackgroundColor(new Color(240, 240, 240));
        }
        if (isRTL) {
            labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        }
        table.addCell(labelCell);
        
        PdfPCell valueCell = new PdfPCell(new Phrase(value, getNormalFont()));
        valueCell.setPadding(5);
        if (highlight) {
            valueCell.setBackgroundColor(new Color(240, 240, 240));
        }
        if (isRTL) {
            valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
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
        // Only replace characters that are invalid in filenames (keep Unicode characters like Arabic)
        String sanitizedType = reportType.replaceAll("[\\\\/:*?\"<>|]", "_").replace(" ", "_");
        return sanitizedType + "_" + timestamp + ".pdf";
    }
}
