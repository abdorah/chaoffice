package org.chaos.office.reports.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sales report data model containing all metrics and analysis for a sales report.
 * Extends ReportData with sales-specific fields including revenue metrics,
 * payment method breakdown, discount analysis, and top selling parts.
 */
public class SalesReportData extends ReportData {
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalRevenue;
    private int salesCount;
    private BigDecimal averageSaleValue;
    private Map<PaymentMethod, BigDecimal> paymentMethodBreakdown;
    private BigDecimal totalDiscounts;
    private BigDecimal averageDiscountPercentage;
    private List<TopSellingPart> topSellingParts;
    
    /**
     * Default constructor initializes collections and BigDecimal fields to zero
     */
    public SalesReportData() {
        super("Sales Report");
        this.totalRevenue = BigDecimal.ZERO;
        this.averageSaleValue = BigDecimal.ZERO;
        this.paymentMethodBreakdown = new HashMap<>();
        this.totalDiscounts = BigDecimal.ZERO;
        this.averageDiscountPercentage = BigDecimal.ZERO;
        this.topSellingParts = new ArrayList<>();
    }
    
    /**
     * Constructor with date range
     * @param startDate The start date of the report period
     * @param endDate The end date of the report period
     */
    public SalesReportData(LocalDate startDate, LocalDate endDate) {
        this();
        this.startDate = startDate;
        this.endDate = endDate;
    }
    
    @Override
    public String getReportTitle() {
        return org.chaos.office.util.LocaleManager.getString("report.sales.title");
    }
    
    @Override
    public Map<String, String> getParameters() {
        Map<String, String> params = new HashMap<>();
        if (startDate != null && endDate != null) {
            String separator = org.chaos.office.util.LocaleManager.getString("date.range.separator");
            params.put(org.chaos.office.util.LocaleManager.getString("report.date.range"), startDate + separator + endDate);
        }
        return params;
    }
    
    // Getters and setters
    
    public LocalDate getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
    
    public LocalDate getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
    
    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }
    
    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }
    
    public int getSalesCount() {
        return salesCount;
    }
    
    public void setSalesCount(int salesCount) {
        this.salesCount = salesCount;
    }
    
    public BigDecimal getAverageSaleValue() {
        return averageSaleValue;
    }
    
    public void setAverageSaleValue(BigDecimal averageSaleValue) {
        this.averageSaleValue = averageSaleValue;
    }
    
    public Map<PaymentMethod, BigDecimal> getPaymentMethodBreakdown() {
        return paymentMethodBreakdown;
    }
    
    public void setPaymentMethodBreakdown(Map<PaymentMethod, BigDecimal> paymentMethodBreakdown) {
        this.paymentMethodBreakdown = paymentMethodBreakdown;
    }
    
    public BigDecimal getTotalDiscounts() {
        return totalDiscounts;
    }
    
    public void setTotalDiscounts(BigDecimal totalDiscounts) {
        this.totalDiscounts = totalDiscounts;
    }
    
    public BigDecimal getAverageDiscountPercentage() {
        return averageDiscountPercentage;
    }
    
    public void setAverageDiscountPercentage(BigDecimal averageDiscountPercentage) {
        this.averageDiscountPercentage = averageDiscountPercentage;
    }
    
    public List<TopSellingPart> getTopSellingParts() {
        return topSellingParts;
    }
    
    public void setTopSellingParts(List<TopSellingPart> topSellingParts) {
        this.topSellingParts = topSellingParts;
    }
    
    @Override
    public String toString() {
        return "SalesReportData{" +
                "startDate=" + startDate +
                ", endDate=" + endDate +
                ", totalRevenue=" + totalRevenue +
                ", salesCount=" + salesCount +
                ", averageSaleValue=" + averageSaleValue +
                ", paymentMethodBreakdown=" + paymentMethodBreakdown +
                ", totalDiscounts=" + totalDiscounts +
                ", averageDiscountPercentage=" + averageDiscountPercentage +
                ", topSellingParts=" + topSellingParts +
                '}';
    }
}
