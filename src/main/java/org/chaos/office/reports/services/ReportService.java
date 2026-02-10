package org.chaos.office.reports.services;

import com.lowagie.text.DocumentException;
import org.chaos.office.reports.models.InventoryReportData;
import org.chaos.office.reports.models.ReportData;
import org.chaos.office.reports.models.SalesReportData;
import org.chaos.office.service.BillService;
import org.chaos.office.service.PartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Central orchestration service for report generation and export.
 * Coordinates between report generators and exporters.
 */
public class ReportService {
    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);
    
    private final SalesReportGenerator salesReportGenerator;
    private final InventoryReportGenerator inventoryReportGenerator;
    private final PDFExporter pdfExporter;
    private final CSVExporter csvExporter;
    private final ExcelExporter excelExporter;
    
    /**
     * Default constructor initializes all dependencies.
     */
    public ReportService() {
        BillService billService = new BillService();
        PartService partService = new PartService();
        
        this.salesReportGenerator = new SalesReportGenerator(billService, partService);
        this.inventoryReportGenerator = new InventoryReportGenerator(partService);
        this.pdfExporter = new PDFExporter();
        this.csvExporter = new CSVExporter();
        this.excelExporter = new ExcelExporter();
    }
    
    /**
     * Constructor with dependency injection for testing.
     */
    public ReportService(SalesReportGenerator salesReportGenerator,
                        InventoryReportGenerator inventoryReportGenerator,
                        PDFExporter pdfExporter,
                        CSVExporter csvExporter) {
        this.salesReportGenerator = salesReportGenerator;
        this.inventoryReportGenerator = inventoryReportGenerator;
        this.pdfExporter = pdfExporter;
        this.csvExporter = csvExporter;
        this.excelExporter = new ExcelExporter();
    }
    
    /**
     * Generates a sales report for the specified date range.
     * Queries the database directly to ensure fresh data.
     * 
     * @param startDate Start date of the report period
     * @param endDate End date of the report period
     * @return SalesReportData containing all sales metrics
     */
    public SalesReportData generateSalesReport(LocalDate startDate, LocalDate endDate) {
        try {
            logger.info("Generating sales report for date range: {} to {}", startDate, endDate);
            return salesReportGenerator.generate(startDate, endDate);
        } catch (Exception e) {
            logger.error("Failed to generate sales report", e);
            throw new RuntimeException("Unable to generate sales report. Please try again.", e);
        }
    }
    
    /**
     * Generates an inventory report with the specified stock threshold.
     * Queries the database directly to ensure fresh data.
     * 
     * @param stockThreshold The minimum quantity level for low stock alerts
     * @return InventoryReportData containing all inventory metrics
     */
    public InventoryReportData generateInventoryReport(int stockThreshold) {
        try {
            logger.info("Generating inventory report with stock threshold: {}", stockThreshold);
            return inventoryReportGenerator.generate(stockThreshold);
        } catch (Exception e) {
            logger.error("Failed to generate inventory report", e);
            throw new RuntimeException("Unable to generate inventory report. Please try again.", e);
        }
    }
    
    /**
     * Exports report data to a PDF file.
     * 
     * @param reportData The report data to export
     * @param filePath The destination file path
     * @throws IOException if file writing fails
     * @throws DocumentException if PDF generation fails
     */
    public void exportToPDF(ReportData reportData, String filePath) throws IOException, DocumentException {
        try {
            logger.info("Exporting report to PDF: {}", filePath);
            pdfExporter.export(reportData, filePath);
            logger.info("PDF export completed successfully: {}", filePath);
        } catch (IOException | DocumentException e) {
            logger.error("Failed to export PDF to: {}", filePath, e);
            throw e;
        }
    }
    
    /**
     * Exports report data to a CSV file.
     * 
     * @param reportData The report data to export
     * @param filePath The destination file path
     * @throws IOException if file writing fails
     */
    public void exportToCSV(ReportData reportData, String filePath) throws IOException {
        try {
            logger.info("Exporting report to CSV: {}", filePath);
            csvExporter.export(reportData, filePath);
            logger.info("CSV export completed successfully: {}", filePath);
        } catch (IOException e) {
            logger.error("Failed to export CSV to: {}", filePath, e);
            throw e;
        }
    }
    
    /**
     * Generates a suggested filename for PDF export.
     * 
     * @param reportType The type of report (e.g., "Sales Report")
     * @return Suggested filename with timestamp
     */
    public String generatePDFFilename(String reportType) {
        return pdfExporter.generateFilename(reportType);
    }
    
    /**
     * Generates a suggested filename for CSV export.
     * 
     * @param reportType The type of report (e.g., "Inventory Report")
     * @return Suggested filename with timestamp
     */
    public String generateCSVFilename(String reportType) {
        return csvExporter.generateFilename(reportType);
    }
    
    /**
     * Exports report data to an Excel file.
     * 
     * @param reportData The report data to export
     * @param filePath The destination file path
     * @throws IOException if file writing fails
     */
    public void exportToExcel(ReportData reportData, String filePath) throws IOException {
        try {
            logger.info("Exporting report to Excel: {}", filePath);
            excelExporter.export(reportData, filePath);
            logger.info("Excel export completed successfully: {}", filePath);
        } catch (IOException e) {
            logger.error("Failed to export Excel to: {}", filePath, e);
            throw e;
        }
    }
    
    /**
     * Generates a suggested filename for Excel export.
     * 
     * @param reportType The type of report (e.g., "Sales Report")
     * @return Suggested filename with timestamp
     */
    public String generateExcelFilename(String reportType) {
        return excelExporter.generateFilename(reportType);
    }
}
