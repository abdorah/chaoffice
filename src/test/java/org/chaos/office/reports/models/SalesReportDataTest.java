package org.chaos.office.reports.models;

import static org.junit.jupiter.api.Assertions.*;

import org.chaos.office.util.LocaleManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for the SalesReportData class.
 * Tests initialization, getters/setters, and report-specific functionality.
 */
class SalesReportDataTest {
    
    private SalesReportData salesReportData;
    
    @BeforeEach
    void setUp() {
        // Set locale to English for consistent test results
        LocaleManager.setLocale(LocaleManager.LOCALE_ENGLISH);
        salesReportData = new SalesReportData();
    }
    
    @Test
    void testDefaultConstructor() {
        assertNotNull(salesReportData);
        assertEquals("Sales Report", salesReportData.getReportType());
        assertNotNull(salesReportData.getGeneratedAt());
        assertEquals("ChaOffice Parts Inventory", salesReportData.getCompanyName());
        
        // Verify default values
        assertNull(salesReportData.getStartDate());
        assertNull(salesReportData.getEndDate());
        assertEquals(BigDecimal.ZERO, salesReportData.getTotalRevenue());
        assertEquals(0, salesReportData.getSalesCount());
        assertEquals(BigDecimal.ZERO, salesReportData.getAverageSaleValue());
        assertNotNull(salesReportData.getPaymentMethodBreakdown());
        assertTrue(salesReportData.getPaymentMethodBreakdown().isEmpty());
        assertEquals(BigDecimal.ZERO, salesReportData.getTotalDiscounts());
        assertEquals(BigDecimal.ZERO, salesReportData.getAverageDiscountPercentage());
        assertNotNull(salesReportData.getTopSellingParts());
        assertTrue(salesReportData.getTopSellingParts().isEmpty());
    }
    
    @Test
    void testConstructorWithDateRange() {
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        
        SalesReportData report = new SalesReportData(startDate, endDate);
        
        assertEquals(startDate, report.getStartDate());
        assertEquals(endDate, report.getEndDate());
        assertEquals("Sales Report", report.getReportType());
        
        // Verify other fields are still initialized
        assertEquals(BigDecimal.ZERO, report.getTotalRevenue());
        assertNotNull(report.getPaymentMethodBreakdown());
        assertNotNull(report.getTopSellingParts());
    }
    
    @Test
    void testGetReportTitle() {
        assertEquals("Sales Report", salesReportData.getReportTitle());
    }
    
    @Test
    void testGetParametersWithDateRange() {
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        
        salesReportData.setStartDate(startDate);
        salesReportData.setEndDate(endDate);
        
        Map<String, String> params = salesReportData.getParameters();
        
        assertNotNull(params);
        assertEquals(1, params.size());
        assertEquals("2024-01-01 to 2024-01-31", params.get("Date Range"));
    }
    
    @Test
    void testGetParametersWithoutDateRange() {
        Map<String, String> params = salesReportData.getParameters();
        
        assertNotNull(params);
        assertTrue(params.isEmpty());
    }
    
    @Test
    void testSetAndGetStartDate() {
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        salesReportData.setStartDate(startDate);
        
        assertEquals(startDate, salesReportData.getStartDate());
    }
    
    @Test
    void testSetAndGetEndDate() {
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        salesReportData.setEndDate(endDate);
        
        assertEquals(endDate, salesReportData.getEndDate());
    }
    
    @Test
    void testSetAndGetTotalRevenue() {
        BigDecimal revenue = new BigDecimal("15000.50");
        salesReportData.setTotalRevenue(revenue);
        
        assertEquals(revenue, salesReportData.getTotalRevenue());
    }
    
    @Test
    void testSetAndGetSalesCount() {
        salesReportData.setSalesCount(42);
        
        assertEquals(42, salesReportData.getSalesCount());
    }
    
    @Test
    void testSetAndGetAverageSaleValue() {
        BigDecimal average = new BigDecimal("357.15");
        salesReportData.setAverageSaleValue(average);
        
        assertEquals(average, salesReportData.getAverageSaleValue());
    }
    
    @Test
    void testSetAndGetPaymentMethodBreakdown() {
        Map<PaymentMethod, BigDecimal> breakdown = new HashMap<>();
        breakdown.put(PaymentMethod.CASH, new BigDecimal("5000.00"));
        breakdown.put(PaymentMethod.CARD, new BigDecimal("8000.00"));
        breakdown.put(PaymentMethod.CHECK, new BigDecimal("2000.50"));
        
        salesReportData.setPaymentMethodBreakdown(breakdown);
        
        Map<PaymentMethod, BigDecimal> result = salesReportData.getPaymentMethodBreakdown();
        assertEquals(3, result.size());
        assertEquals(new BigDecimal("5000.00"), result.get(PaymentMethod.CASH));
        assertEquals(new BigDecimal("8000.00"), result.get(PaymentMethod.CARD));
        assertEquals(new BigDecimal("2000.50"), result.get(PaymentMethod.CHECK));
    }
    
    @Test
    void testSetAndGetTotalDiscounts() {
        BigDecimal discounts = new BigDecimal("1250.75");
        salesReportData.setTotalDiscounts(discounts);
        
        assertEquals(discounts, salesReportData.getTotalDiscounts());
    }
    
    @Test
    void testSetAndGetAverageDiscountPercentage() {
        BigDecimal percentage = new BigDecimal("12.50");
        salesReportData.setAverageDiscountPercentage(percentage);
        
        assertEquals(percentage, salesReportData.getAverageDiscountPercentage());
    }
    
    @Test
    void testSetAndGetTopSellingParts() {
        List<TopSellingPart> topParts = new ArrayList<>();
        topParts.add(new TopSellingPart("Part A", 100, new BigDecimal("5000.00")));
        topParts.add(new TopSellingPart("Part B", 75, new BigDecimal("3750.00")));
        
        salesReportData.setTopSellingParts(topParts);
        
        List<TopSellingPart> result = salesReportData.getTopSellingParts();
        assertEquals(2, result.size());
        assertEquals("Part A", result.get(0).getPartName());
        assertEquals(100, result.get(0).getQuantitySold());
    }
    
    @Test
    void testToString() {
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        
        salesReportData.setStartDate(startDate);
        salesReportData.setEndDate(endDate);
        salesReportData.setTotalRevenue(new BigDecimal("10000.00"));
        salesReportData.setSalesCount(50);
        
        String result = salesReportData.toString();
        
        assertNotNull(result);
        assertTrue(result.contains("SalesReportData"));
        assertTrue(result.contains("2024-01-01"));
        assertTrue(result.contains("2024-01-31"));
        assertTrue(result.contains("10000.00"));
        assertTrue(result.contains("salesCount=50"));
    }
    
    @Test
    void testZeroSalesScenario() {
        // Verify that a report with no sales has zero values
        assertEquals(0, salesReportData.getSalesCount());
        assertEquals(BigDecimal.ZERO, salesReportData.getTotalRevenue());
        assertEquals(BigDecimal.ZERO, salesReportData.getAverageSaleValue());
        assertEquals(BigDecimal.ZERO, salesReportData.getTotalDiscounts());
        assertEquals(BigDecimal.ZERO, salesReportData.getAverageDiscountPercentage());
        assertTrue(salesReportData.getTopSellingParts().isEmpty());
    }
    
    @Test
    void testEmptyPaymentMethodBreakdown() {
        Map<PaymentMethod, BigDecimal> breakdown = salesReportData.getPaymentMethodBreakdown();
        
        assertNotNull(breakdown);
        assertTrue(breakdown.isEmpty());
    }
    
    @Test
    void testSinglePaymentMethod() {
        Map<PaymentMethod, BigDecimal> breakdown = new HashMap<>();
        breakdown.put(PaymentMethod.CASH, new BigDecimal("15000.00"));
        
        salesReportData.setPaymentMethodBreakdown(breakdown);
        
        assertEquals(1, salesReportData.getPaymentMethodBreakdown().size());
        assertEquals(new BigDecimal("15000.00"), 
                     salesReportData.getPaymentMethodBreakdown().get(PaymentMethod.CASH));
    }
    
    @Test
    void testFewerThanTenTopSellingParts() {
        List<TopSellingPart> topParts = new ArrayList<>();
        topParts.add(new TopSellingPart("Part A", 50, new BigDecimal("2500.00")));
        topParts.add(new TopSellingPart("Part B", 30, new BigDecimal("1500.00")));
        topParts.add(new TopSellingPart("Part C", 20, new BigDecimal("1000.00")));
        
        salesReportData.setTopSellingParts(topParts);
        
        assertEquals(3, salesReportData.getTopSellingParts().size());
    }
    
    @Test
    void testNullDateRange() {
        salesReportData.setStartDate(null);
        salesReportData.setEndDate(null);
        
        assertNull(salesReportData.getStartDate());
        assertNull(salesReportData.getEndDate());
        
        Map<String, String> params = salesReportData.getParameters();
        assertTrue(params.isEmpty());
    }
}
