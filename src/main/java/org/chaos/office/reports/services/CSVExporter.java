package org.chaos.office.reports.services;

import org.chaos.office.reports.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Exports report data to CSV format for spreadsheet analysis.
 */
public class CSVExporter {
    private static final Logger logger = LoggerFactory.getLogger(CSVExporter.class);
    private static final DateTimeFormatter FILENAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Exports report data to a CSV file.
     * 
     * @param reportData The report data to export
     * @param filePath The destination file path
     * @throws IOException if file writing fails
     */
    public void export(ReportData reportData, String filePath) throws IOException {
        logger.info("Exporting report to CSV: {}", filePath);
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            if (reportData instanceof SalesReportData) {
                writeSalesCSV((SalesReportData) reportData, writer);
            } else if (reportData instanceof InventoryReportData) {
                writeInventoryCSV((InventoryReportData) reportData, writer);
            } else {
                throw new IllegalArgumentException("Unsupported report type: " + reportData.getClass().getName());
            }
            
            logger.info("CSV export completed successfully");
        } catch (IOException e) {
            logger.error("Failed to export CSV: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Writes sales report data to CSV format.
     */
    private void writeSalesCSV(SalesReportData data, PrintWriter writer) {
        // Header section
        writer.println("Report Type,Sales Report");
        writer.println("Date Range," + data.getStartDate() + " to " + data.getEndDate());
        writer.println("Generated On," + data.getGeneratedAt().format(DISPLAY_FORMATTER));
        writer.println();
        
        // Summary metrics
        writer.println("Summary Metrics");
        writer.println("Total Revenue," + formatCurrency(data.getTotalRevenue()));
        writer.println("Number of Sales," + data.getSalesCount());
        writer.println("Average Sale Value," + formatCurrency(data.getAverageSaleValue()));
        writer.println();
        
        // Payment method breakdown
        writer.println("Payment Method Breakdown");
        writer.println("Payment Method,Total Revenue");
        for (Map.Entry<PaymentMethod, BigDecimal> entry : data.getPaymentMethodBreakdown().entrySet()) {
            writer.println(entry.getKey() + "," + formatCurrency(entry.getValue()));
        }
        writer.println();
        
        // Discount analysis
        writer.println("Discount Analysis");
        writer.println("Total Discounts Given," + formatCurrency(data.getTotalDiscounts()));
        writer.println("Average Discount Percentage," + data.getAverageDiscountPercentage() + "%");
        writer.println();
        
        // Top selling parts
        writer.println("Top Selling Parts");
        writer.println("Part Name,Quantity Sold,Revenue");
        for (TopSellingPart part : data.getTopSellingParts()) {
            writer.println(escapeCSVField(part.getPartName()) + "," + 
                          part.getQuantitySold() + "," + 
                          formatCurrency(part.getRevenue()));
        }
    }
    
    /**
     * Writes inventory report data to CSV format.
     */
    private void writeInventoryCSV(InventoryReportData data, PrintWriter writer) {
        // Header section
        writer.println("Report Type,Inventory Report");
        writer.println("Stock Threshold," + data.getStockThreshold() + " units");
        writer.println("Generated On," + data.getGeneratedAt().format(DISPLAY_FORMATTER));
        writer.println();
        
        // Summary metrics
        writer.println("Summary Metrics");
        writer.println("Total Inventory Value," + formatCurrency(data.getTotalInventoryValue()));
        writer.println("Low Stock Items," + data.getLowStockParts().size());
        writer.println("Out of Stock Items," + data.getOutOfStockParts().size());
        writer.println();
        
        // Inventory details
        writer.println("Inventory Details");
        writer.println("Category,Part Name,Quantity,Price,Stock Value,Status");
        
        for (Map.Entry<String, List<PartInventoryItem>> entry : data.getPartsByCategory().entrySet()) {
            for (PartInventoryItem item : entry.getValue()) {
                writer.println(escapeCSVField(entry.getKey()) + "," +
                              escapeCSVField(item.getPartName()) + "," +
                              item.getQuantity() + "," +
                              formatCurrency(item.getPrice()) + "," +
                              formatCurrency(item.getStockValue()) + "," +
                              item.getStatus());
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
        return sanitizedType + "_" + timestamp + ".csv";
    }
}
