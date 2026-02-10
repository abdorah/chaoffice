package org.chaos.office.reports.services;

import org.chaos.office.reports.models.SalesReportData;
import org.chaos.office.service.BillService;
import org.chaos.office.service.PartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for SalesReportGenerator
 * Tests the generator with real BillService and PartService instances
 */
class SalesReportGeneratorTest {
    
    private BillService billService;
    private PartService partService;
    private SalesReportGenerator generator;
    
    @BeforeEach
    void setUp() {
        billService = new BillService();
        partService = new PartService();
        generator = new SalesReportGenerator(billService, partService);
    }
    
    @Test
    void testGenerateWithNoSales() {
        // Arrange - use a date range with no sales
        LocalDate startDate = LocalDate.of(2050, 1, 1);
        LocalDate endDate = LocalDate.of(2050, 1, 31);
        
        // Act
        SalesReportData report = generator.generate(startDate, endDate);
        
        // Assert
        assertNotNull(report);
        assertEquals(BigDecimal.ZERO.setScale(2), report.getTotalRevenue().setScale(2));
        assertEquals(0, report.getSalesCount());
        assertEquals(BigDecimal.ZERO, report.getAverageSaleValue());
        assertEquals(BigDecimal.ZERO.setScale(2), report.getTotalDiscounts().setScale(2));
        assertEquals(BigDecimal.ZERO, report.getAverageDiscountPercentage());
        assertTrue(report.getTopSellingParts().isEmpty());
    }
    
    @Test
    void testGenerateWithValidDateRange() {
        // Arrange - use a date range that should have some sales in the database
        LocalDate startDate = LocalDate.of(2020, 1, 1);
        LocalDate endDate = LocalDate.of(2030, 12, 31);
        
        // Act
        SalesReportData report = generator.generate(startDate, endDate);
        
        // Assert
        assertNotNull(report);
        assertNotNull(report.getTotalRevenue());
        assertTrue(report.getSalesCount() >= 0);
        assertNotNull(report.getAverageSaleValue());
        assertNotNull(report.getPaymentMethodBreakdown());
        assertNotNull(report.getTotalDiscounts());
        assertNotNull(report.getAverageDiscountPercentage());
        assertNotNull(report.getTopSellingParts());
        
        // If there are sales, verify calculations make sense
        if (report.getSalesCount() > 0) {
            assertTrue(report.getTotalRevenue().compareTo(BigDecimal.ZERO) >= 0);
            assertTrue(report.getAverageSaleValue().compareTo(BigDecimal.ZERO) >= 0);
        }
    }
    
    @Test
    void testGenerateReturnsCorrectDateRange() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        
        // Act
        SalesReportData report = generator.generate(startDate, endDate);
        
        // Assert
        assertNotNull(report);
        assertEquals(startDate, report.getStartDate());
        assertEquals(endDate, report.getEndDate());
    }
    
    @Test
    void testGenerateInitializesAllFields() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        
        // Act
        SalesReportData report = generator.generate(startDate, endDate);
        
        // Assert - verify all fields are initialized (not null)
        assertNotNull(report);
        assertNotNull(report.getStartDate());
        assertNotNull(report.getEndDate());
        assertNotNull(report.getTotalRevenue());
        assertNotNull(report.getAverageSaleValue());
        assertNotNull(report.getPaymentMethodBreakdown());
        assertNotNull(report.getTotalDiscounts());
        assertNotNull(report.getAverageDiscountPercentage());
        assertNotNull(report.getTopSellingParts());
        assertNotNull(report.getReportTitle());
        assertNotNull(report.getParameters());
    }
}
