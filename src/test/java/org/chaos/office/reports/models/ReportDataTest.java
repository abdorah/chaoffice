package org.chaos.office.reports.models;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for the ReportData abstract base class.
 * Uses a concrete test implementation to verify base functionality.
 */
class ReportDataTest {

    /**
     * Concrete implementation of ReportData for testing purposes
     */
    private static class TestReportData extends ReportData {
        private Map<String, String> testParameters;
        
        public TestReportData() {
            super();
            this.testParameters = new HashMap<>();
        }
        
        public TestReportData(String reportType) {
            super(reportType);
            this.testParameters = new HashMap<>();
        }
        
        @Override
        public String getReportTitle() {
            return "Test Report";
        }
        
        @Override
        public Map<String, String> getParameters() {
            return testParameters;
        }
        
        public void addParameter(String key, String value) {
            testParameters.put(key, value);
        }
    }
    
    private TestReportData reportData;
    
    @BeforeEach
    void setUp() {
        reportData = new TestReportData();
    }
    
    @Test
    void testDefaultConstructor() {
        assertNotNull(reportData);
        assertNull(reportData.getReportType());
        assertNotNull(reportData.getGeneratedAt());
        assertEquals("ChaOffice Parts Inventory", reportData.getCompanyName());
    }
    
    @Test
    void testConstructorWithReportType() {
        TestReportData report = new TestReportData("Sales Report");
        
        assertEquals("Sales Report", report.getReportType());
        assertNotNull(report.getGeneratedAt());
        assertEquals("ChaOffice Parts Inventory", report.getCompanyName());
    }
    
    @Test
    void testGeneratedAtIsSetAutomatically() {
        LocalDateTime before = LocalDateTime.now();
        TestReportData report = new TestReportData();
        LocalDateTime after = LocalDateTime.now();
        
        assertNotNull(report.getGeneratedAt());
        assertTrue(!report.getGeneratedAt().isBefore(before));
        assertTrue(!report.getGeneratedAt().isAfter(after));
    }
    
    @Test
    void testSetReportType() {
        reportData.setReportType("Inventory Report");
        assertEquals("Inventory Report", reportData.getReportType());
        
        reportData.setReportType("Sales Report");
        assertEquals("Sales Report", reportData.getReportType());
    }
    
    @Test
    void testSetGeneratedAt() {
        LocalDateTime customTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        reportData.setGeneratedAt(customTime);
        
        assertEquals(customTime, reportData.getGeneratedAt());
    }
    
    @Test
    void testSetCompanyName() {
        reportData.setCompanyName("Custom Company Name");
        assertEquals("Custom Company Name", reportData.getCompanyName());
    }
    
    @Test
    void testDefaultCompanyName() {
        assertEquals("ChaOffice Parts Inventory", reportData.getCompanyName());
    }
    
    @Test
    void testGetReportTitle() {
        assertEquals("Test Report", reportData.getReportTitle());
    }
    
    @Test
    void testGetParameters() {
        Map<String, String> params = reportData.getParameters();
        assertNotNull(params);
        assertTrue(params.isEmpty());
        
        reportData.addParameter("key1", "value1");
        reportData.addParameter("key2", "value2");
        
        params = reportData.getParameters();
        assertEquals(2, params.size());
        assertEquals("value1", params.get("key1"));
        assertEquals("value2", params.get("key2"));
    }
    
    @Test
    void testNullReportType() {
        reportData.setReportType(null);
        assertNull(reportData.getReportType());
    }
    
    @Test
    void testEmptyReportType() {
        reportData.setReportType("");
        assertEquals("", reportData.getReportType());
    }
    
    @Test
    void testMultipleInstances() {
        TestReportData report1 = new TestReportData("Report 1");
        TestReportData report2 = new TestReportData("Report 2");
        
        assertNotEquals(report1.getReportType(), report2.getReportType());
        assertEquals("ChaOffice Parts Inventory", report1.getCompanyName());
        assertEquals("ChaOffice Parts Inventory", report2.getCompanyName());
    }
    
    @Test
    void testCompanyNameCanBeChanged() {
        String originalName = reportData.getCompanyName();
        assertEquals("ChaOffice Parts Inventory", originalName);
        
        reportData.setCompanyName("New Company");
        assertEquals("New Company", reportData.getCompanyName());
        assertNotEquals(originalName, reportData.getCompanyName());
    }
}
