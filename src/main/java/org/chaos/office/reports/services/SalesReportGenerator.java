package org.chaos.office.reports.services;

import org.chaos.office.model.Bill;
import org.chaos.office.model.Command;
import org.chaos.office.reports.models.PaymentMethod;
import org.chaos.office.reports.models.SalesReportData;
import org.chaos.office.reports.models.TopSellingPart;
import org.chaos.office.service.BillService;
import org.chaos.office.service.PartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates sales reports with revenue metrics and analysis.
 * Integrates with BillService to retrieve sales data and calculates various metrics
 * including revenue, payment method breakdown, discount analysis, and top selling parts.
 * 
 * Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4, 2.5, 3.1, 3.2, 3.3, 3.4, 11.1
 */
public class SalesReportGenerator {
    private static final Logger logger = LoggerFactory.getLogger(SalesReportGenerator.class);
    private static final int TOP_PARTS_LIMIT = 10;
    
    private final BillService billService;
    private final PartService partService;
    
    /**
     * Constructor with service dependencies
     * @param billService Service for retrieving bill data
     * @param partService Service for retrieving part information
     */
    public SalesReportGenerator(BillService billService, PartService partService) {
        this.billService = billService;
        this.partService = partService;
    }
    
    /**
     * Generates a sales report for the specified date range.
     * Retrieves all sales transactions within the date range and calculates
     * comprehensive metrics including revenue, payment methods, discounts, and top selling parts.
     * 
     * @param startDate The start date of the report period (inclusive)
     * @param endDate The end date of the report period (inclusive)
     * @return SalesReportData containing all calculated metrics
     */
    public SalesReportData generate(LocalDate startDate, LocalDate endDate) {
        logger.info("Generating sales report for date range: {} to {}", startDate, endDate);
        
        // Create report data object
        SalesReportData reportData = new SalesReportData(startDate, endDate);
        
        // Retrieve bills within date range using BillService
        List<Bill> bills = billService.filterByDateRange(startDate, endDate);
        logger.debug("Retrieved {} bills for date range", bills.size());
        
        // Calculate all metrics
        reportData.setTotalRevenue(calculateTotalRevenue(bills));
        reportData.setSalesCount(calculateSalesCount(bills));
        reportData.setAverageSaleValue(calculateAverageSaleValue(reportData.getTotalRevenue(), reportData.getSalesCount()));
        reportData.setPaymentMethodBreakdown(groupByPaymentMethod(bills));
        
        // Calculate discount analysis
        DiscountAnalysis discountAnalysis = calculateDiscountAnalysis(bills);
        reportData.setTotalDiscounts(discountAnalysis.totalDiscounts);
        reportData.setAverageDiscountPercentage(discountAnalysis.averageDiscountPercentage);
        
        // Calculate top selling parts
        reportData.setTopSellingParts(calculateTopSellingParts(bills, TOP_PARTS_LIMIT));
        
        logger.info("Sales report generated successfully with {} sales and total revenue of {}", 
                    reportData.getSalesCount(), reportData.getTotalRevenue());
        
        return reportData;
    }
    
    /**
     * Calculates total revenue from all bills.
     * Total revenue is the sum of all bill total prices.
     * 
     * @param bills List of bills
     * @return Total revenue as BigDecimal
     */
    private BigDecimal calculateTotalRevenue(List<Bill> bills) {
        BigDecimal total = bills.stream()
            .map(bill -> BigDecimal.valueOf(bill.getTotalPrice()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return total.setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculates the number of sales (count of bills).
     * 
     * @param bills List of bills
     * @return Number of sales
     */
    private int calculateSalesCount(List<Bill> bills) {
        return bills.size();
    }
    
    /**
     * Calculates average sale value.
     * Average sale value = total revenue / sales count.
     * Returns zero if there are no sales.
     * 
     * @param totalRevenue Total revenue
     * @param salesCount Number of sales
     * @return Average sale value as BigDecimal
     */
    private BigDecimal calculateAverageSaleValue(BigDecimal totalRevenue, int salesCount) {
        if (salesCount == 0) {
            return BigDecimal.ZERO;
        }
        
        return totalRevenue.divide(BigDecimal.valueOf(salesCount), 2, RoundingMode.HALF_UP);
    }
    
    /**
     * Groups sales by payment method and calculates total revenue for each.
     * Maps bill payment method strings to PaymentMethod enum values.
     * 
     * @param bills List of bills
     * @return Map of payment methods to their total revenue
     */
    private Map<PaymentMethod, BigDecimal> groupByPaymentMethod(List<Bill> bills) {
        Map<PaymentMethod, BigDecimal> breakdown = new EnumMap<>(PaymentMethod.class);
        
        // Initialize all payment methods with zero
        for (PaymentMethod method : PaymentMethod.values()) {
            breakdown.put(method, BigDecimal.ZERO);
        }
        
        // Aggregate revenue by payment method
        for (Bill bill : bills) {
            PaymentMethod method = PaymentMethod.fromString(bill.getPaymentMethod());
            BigDecimal currentTotal = breakdown.get(method);
            BigDecimal billAmount = BigDecimal.valueOf(bill.getTotalPrice());
            breakdown.put(method, currentTotal.add(billAmount));
        }
        
        // Round all values to 2 decimal places
        breakdown.replaceAll((k, v) -> v.setScale(2, RoundingMode.HALF_UP));
        
        return breakdown;
    }
    
    /**
     * Calculates discount analysis including total discounts and average discount percentage.
     * 
     * @param bills List of bills
     * @return DiscountAnalysis containing total discounts and average percentage
     */
    private DiscountAnalysis calculateDiscountAnalysis(List<Bill> bills) {
        BigDecimal totalDiscounts = BigDecimal.ZERO;
        BigDecimal totalDiscountPercentage = BigDecimal.ZERO;
        int discountedBillsCount = 0;
        
        for (Bill bill : bills) {
            // Calculate discount amount based on discount type
            BigDecimal discountAmount = calculateDiscountAmount(bill);
            totalDiscounts = totalDiscounts.add(discountAmount);
            
            // Calculate discount percentage for this bill if discount was applied
            if (discountAmount.compareTo(BigDecimal.ZERO) > 0 && bill.getSubtotal() > 0) {
                BigDecimal billDiscountPercentage = discountAmount
                    .divide(BigDecimal.valueOf(bill.getSubtotal()), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
                totalDiscountPercentage = totalDiscountPercentage.add(billDiscountPercentage);
                discountedBillsCount++;
            }
        }
        
        // Calculate average discount percentage
        BigDecimal averageDiscountPercentage = BigDecimal.ZERO;
        if (discountedBillsCount > 0) {
            averageDiscountPercentage = totalDiscountPercentage
                .divide(BigDecimal.valueOf(discountedBillsCount), 2, RoundingMode.HALF_UP);
        }
        
        return new DiscountAnalysis(
            totalDiscounts.setScale(2, RoundingMode.HALF_UP),
            averageDiscountPercentage
        );
    }
    
    /**
     * Calculates the discount amount for a bill based on its discount type and value.
     * 
     * @param bill The bill to calculate discount for
     * @return Discount amount as BigDecimal
     */
    private BigDecimal calculateDiscountAmount(Bill bill) {
        String discountType = bill.getDiscountType();
        float discountValue = bill.getDiscountValue();
        float subtotal = bill.getSubtotal();
        
        if ("percentage".equals(discountType)) {
            // Percentage discount: subtotal * (discountValue / 100)
            return BigDecimal.valueOf(subtotal)
                .multiply(BigDecimal.valueOf(discountValue))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else if ("fixed".equals(discountType)) {
            // Fixed discount: direct discount value
            return BigDecimal.valueOf(discountValue);
        }
        
        // No discount
        return BigDecimal.ZERO;
    }
    
    /**
     * Calculates top selling parts by aggregating quantities across all bills.
     * Returns up to the specified limit of parts, ranked by quantity sold in descending order.
     * Handles edge case where fewer than limit parts were sold.
     * 
     * @param bills List of bills
     * @param limit Maximum number of top parts to return
     * @return List of TopSellingPart objects, sorted by quantity descending
     */
    private List<TopSellingPart> calculateTopSellingParts(List<Bill> bills, int limit) {
        // Map to aggregate quantities and revenue by part ID
        Map<Integer, PartSalesData> partSalesMap = new HashMap<>();
        
        // Aggregate data from all bills
        for (Bill bill : bills) {
            List<Command> commands = billService.getCommandsForBill(bill.getId());
            
            for (Command command : commands) {
                int partId = command.getPartId();
                int quantity = command.getQuantity();
                BigDecimal revenue = BigDecimal.valueOf(command.getPriceConsidered())
                    .multiply(BigDecimal.valueOf(quantity));
                
                partSalesMap.merge(partId, 
                    new PartSalesData(partId, quantity, revenue),
                    (existing, newData) -> new PartSalesData(
                        partId,
                        existing.quantity + newData.quantity,
                        existing.revenue.add(newData.revenue)
                    ));
            }
        }
        
        // Convert to TopSellingPart objects with part names
        List<TopSellingPart> topParts = partSalesMap.values().stream()
            .sorted((a, b) -> Integer.compare(b.quantity, a.quantity)) // Sort by quantity descending
            .limit(limit) // Take top N
            .map(data -> {
                String partName = getPartName(data.partId);
                return new TopSellingPart(
                    partName,
                    data.quantity,
                    data.revenue.setScale(2, RoundingMode.HALF_UP)
                );
            })
            .collect(Collectors.toList());
        
        logger.debug("Calculated {} top selling parts from {} unique parts", 
                     topParts.size(), partSalesMap.size());
        
        return topParts;
    }
    
    /**
     * Retrieves the part name for a given part ID.
     * Returns "Unknown Part" if the part is not found.
     * 
     * @param partId The part ID
     * @return The part name
     */
    private String getPartName(int partId) {
        return partService.getPartById(partId)
            .map(part -> part.getName())
            .orElse("Unknown Part (ID: " + partId + ")");
    }
    
    /**
     * Helper class to hold discount analysis results
     */
    private static class DiscountAnalysis {
        final BigDecimal totalDiscounts;
        final BigDecimal averageDiscountPercentage;
        
        DiscountAnalysis(BigDecimal totalDiscounts, BigDecimal averageDiscountPercentage) {
            this.totalDiscounts = totalDiscounts;
            this.averageDiscountPercentage = averageDiscountPercentage;
        }
    }
    
    /**
     * Helper class to aggregate part sales data
     */
    private static class PartSalesData {
        final int partId;
        final int quantity;
        final BigDecimal revenue;
        
        PartSalesData(int partId, int quantity, BigDecimal revenue) {
            this.partId = partId;
            this.quantity = quantity;
            this.revenue = revenue;
        }
    }
}
