package org.chaos.office.reports.models;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Abstract base class for all report data models.
 * Contains common fields shared across different report types.
 */
public abstract class ReportData {
    private String reportType;
    private LocalDateTime generatedAt;
    private String companyName = "ChaOffice Parts Inventory";
    
    /**
     * Default constructor initializes generatedAt to current time
     */
    public ReportData() {
        this.generatedAt = LocalDateTime.now();
    }
    
    /**
     * Constructor with report type
     * @param reportType The type of report (e.g., "Sales Report", "Inventory Report")
     */
    public ReportData(String reportType) {
        this();
        this.reportType = reportType;
    }
    
    /**
     * Get the report title for display
     * @return The human-readable report title
     */
    public abstract String getReportTitle();
    
    /**
     * Get report-specific parameters as key-value pairs
     * @return Map of parameter names to their string representations
     */
    public abstract Map<String, String> getParameters();
    
    // Getters and setters
    
    public String getReportType() {
        return reportType;
    }
    
    public void setReportType(String reportType) {
        this.reportType = reportType;
    }
    
    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }
    
    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
    
    public String getCompanyName() {
        return companyName;
    }
    
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
}
