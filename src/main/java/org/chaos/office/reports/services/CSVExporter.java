package org.chaos.office.reports.services;

import org.chaos.office.reports.models.*;
import org.chaos.office.util.CurrencyFormatter;
import org.chaos.office.util.EnumLocalizer;
import org.chaos.office.util.LocaleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Exports report data to CSV format for spreadsheet analysis.
 * Uses LocaleManager for internationalized column headers and labels.
 * Uses CurrencyFormatter for currency values.
 * 
 * Requirements: 5.5, 7.2
 */
public class CSVExporter {
    private static final Logger logger = LoggerFactory.getLogger(CSVExporter.class);
    private static final DateTimeFormatter FILENAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Exports report data to a CSV file with UTF-8 encoding and BOM for Excel compatibility.
     * 
     * @param reportData The report data to export
     * @param filePath The destination file path
     * @throws IOException if file writing fails
     */
    public void export(ReportData reportData, String filePath) throws IOException {
        logger.info("Exporting report to CSV: {}", filePath);
        
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            // Write UTF-8 BOM for Excel compatibility
            fos.write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF});
            
            // Use UTF-8 encoding for the writer
            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8))) {
                if (reportData instanceof SalesReportData) {
                    writeSalesCSV((SalesReportData) reportData, writer);
                } else if (reportData instanceof InventoryReportData) {
                    writeInventoryCSV((InventoryReportData) reportData, writer);
                } else {
                    throw new IllegalArgumentException("Unsupported report type: " + reportData.getClass().getName());
                }
                
                logger.info("CSV export completed successfully with UTF-8 encoding");
            }
        } catch (IOException e) {
            logger.error("Failed to export CSV: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Writes sales report data to CSV format.
     * Uses LocaleManager for internationalized labels and CurrencyFormatter for prices.
     */
    private void writeSalesCSV(SalesReportData data, PrintWriter writer) {
        // Header section
        String separator = LocaleManager.getString("date.range.separator");
        writer.println(LocaleManager.getString("report.type") + "," + LocaleManager.getString("report.sales.title"));
        writer.println(LocaleManager.getString("report.period") + "," + data.getStartDate() + separator + data.getEndDate());
        writer.println(LocaleManager.getString("report.generated.on") + "," + data.getGeneratedAt().format(DISPLAY_FORMATTER));
        writer.println();
        
        // Summary metrics
        writer.println(LocaleManager.getString("report.summary.metrics"));
        writer.println(LocaleManager.getString("report.total.revenue") + "," + formatCurrency(data.getTotalRevenue()));
        writer.println(LocaleManager.getString("report.number.of.sales") + "," + data.getSalesCount());
        writer.println(LocaleManager.getString("report.average.sale.value") + "," + formatCurrency(data.getAverageSaleValue()));
        writer.println();
        
        // Payment method breakdown
        writer.println(LocaleManager.getString("report.payment.method.breakdown"));
        writer.println(LocaleManager.getString("report.payment.method") + "," + LocaleManager.getString("report.total.revenue"));
        for (Map.Entry<PaymentMethod, BigDecimal> entry : data.getPaymentMethodBreakdown().entrySet()) {
            writer.println(EnumLocalizer.getLocalizedPaymentMethod(entry.getKey()) + "," + formatCurrency(entry.getValue()));
        }
        writer.println();
        
        // Discount analysis
        writer.println(LocaleManager.getString("report.discount.analysis"));
        writer.println(LocaleManager.getString("report.total.discounts.given") + "," + formatCurrency(data.getTotalDiscounts()));
        writer.println(LocaleManager.getString("report.average.discount.percentage") + "," + data.getAverageDiscountPercentage() + "%");
        writer.println();
        
        // Top selling parts
        writer.println(LocaleManager.getString("report.top.selling.parts"));
        writer.println(LocaleManager.getString("report.column.part.name") + "," + 
                      LocaleManager.getString("report.quantity.sold") + "," + 
                      LocaleManager.getString("report.column.revenue"));
        for (TopSellingPart part : data.getTopSellingParts()) {
            writer.println(escapeCSVField(part.getPartName()) + "," + 
                          part.getQuantitySold() + "," + 
                          formatCurrency(part.getRevenue()));
        }
    }
    
    /**
     * Writes inventory report data to CSV format.
     * Uses LocaleManager for internationalized labels and CurrencyFormatter for prices.
     */
    private void writeInventoryCSV(InventoryReportData data, PrintWriter writer) {
        // Header section
        writer.println(LocaleManager.getString("report.type") + "," + LocaleManager.getString("report.inventory.title"));
        writer.println(LocaleManager.getString("report.stock.threshold") + "," + data.getStockThreshold() + " " + LocaleManager.getString("report.units"));
        writer.println(LocaleManager.getString("report.generated.on") + "," + data.getGeneratedAt().format(DISPLAY_FORMATTER));
        writer.println();
        
        // Summary metrics
        writer.println(LocaleManager.getString("report.summary.metrics"));
        writer.println(LocaleManager.getString("report.total.inventory.value") + "," + formatCurrency(data.getTotalInventoryValue()));
        writer.println(LocaleManager.getString("report.low.stock.items") + "," + data.getLowStockParts().size());
        writer.println(LocaleManager.getString("report.out.of.stock.items") + "," + data.getOutOfStockParts().size());
        writer.println();
        
        // Inventory details
        writer.println(LocaleManager.getString("report.inventory.details"));
        writer.println(LocaleManager.getString("report.column.category") + "," +
                      LocaleManager.getString("report.column.part.name") + "," +
                      LocaleManager.getString("report.column.quantity") + "," +
                      LocaleManager.getString("report.column.price") + "," +
                      LocaleManager.getString("report.stock.value") + "," +
                      LocaleManager.getString("report.status"));
        
        for (Map.Entry<String, List<PartInventoryItem>> entry : data.getPartsByCategory().entrySet()) {
            for (PartInventoryItem item : entry.getValue()) {
                writer.println(escapeCSVField(entry.getKey()) + "," +
                              escapeCSVField(item.getPartName()) + "," +
                              item.getQuantity() + "," +
                              formatCurrency(item.getPrice()) + "," +
                              formatCurrency(item.getStockValue()) + "," +
                              EnumLocalizer.getLocalizedStockStatus(item.getStatus()));
            }
        }
    }
    
    /**
     * Escapes CSV field values containing commas or quotes.
     */
    private String escapeCSVField(String field) {
        if (field == null) {
            return "";
        }
        
        // If field contains comma, quote, or newline, wrap in quotes and escape quotes
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        
        return field;
    }
    
    /**
     * Formats currency values using CurrencyFormatter with configured currency symbol.
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
        return sanitizedType + "_" + timestamp + ".csv";
    }
}
