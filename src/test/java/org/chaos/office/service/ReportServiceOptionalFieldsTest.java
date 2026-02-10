package org.chaos.office.service;

import org.chaos.office.model.Bill;
import org.chaos.office.model.Command;
import org.chaos.office.model.Part;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PDF generation with optional client name and phone fields.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReportServiceOptionalFieldsTest {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportServiceOptionalFieldsTest.class);
    private ReportService reportService;
    private PartService partService;
    
    @BeforeEach
    void setUp() {
        reportService = new ReportService();
        partService = new PartService();
    }
    
    @Test
    @Order(1)
    void testGeneratePDFWithEmptyClientName() {
        Bill bill = new Bill();
        bill.setId(1);
        bill.setClientName(""); // Empty client name
        bill.setClientPhone("1234567890");
        bill.setTotalPrice(100.0f);
        bill.setDate(LocalDate.now());
        
        Part part = partService.getAllParts().get(0);
        Command command = new Command();
        command.setPartId(part.getId());
        command.setQuantity(1);
        command.setPriceConsidered(100.0f);
        
        List<Command> commands = new ArrayList<>();
        commands.add(command);
        
        File outputFile = new File("test_bill_empty_name.pdf");
        
        try {
            // Should generate PDF successfully with empty client name
            assertDoesNotThrow(() -> reportService.generateBillPDF(bill, commands, outputFile));
            assertTrue(outputFile.exists());
            assertTrue(outputFile.length() > 0);
            
            logger.info("Successfully generated PDF with empty client name");
        } finally {
            // Cleanup
            if (outputFile.exists()) {
                outputFile.delete();
            }
        }
    }
    
    @Test
    @Order(2)
    void testGeneratePDFWithEmptyClientPhone() {
        Bill bill = new Bill();
        bill.setId(2);
        bill.setClientName("Test Client");
        bill.setClientPhone(""); // Empty client phone
        bill.setTotalPrice(100.0f);
        bill.setDate(LocalDate.now());
        
        Part part = partService.getAllParts().get(0);
        Command command = new Command();
        command.setPartId(part.getId());
        command.setQuantity(1);
        command.setPriceConsidered(100.0f);
        
        List<Command> commands = new ArrayList<>();
        commands.add(command);
        
        File outputFile = new File("test_bill_empty_phone.pdf");
        
        try {
            // Should generate PDF successfully with empty client phone
            assertDoesNotThrow(() -> reportService.generateBillPDF(bill, commands, outputFile));
            assertTrue(outputFile.exists());
            assertTrue(outputFile.length() > 0);
            
            logger.info("Successfully generated PDF with empty client phone");
        } finally {
            // Cleanup
            if (outputFile.exists()) {
                outputFile.delete();
            }
        }
    }
    
    @Test
    @Order(3)
    void testGeneratePDFWithBothFieldsEmpty() {
        Bill bill = new Bill();
        bill.setId(3);
        bill.setClientName(""); // Empty client name
        bill.setClientPhone(""); // Empty client phone
        bill.setTotalPrice(100.0f);
        bill.setDate(LocalDate.now());
        
        Part part = partService.getAllParts().get(0);
        Command command = new Command();
        command.setPartId(part.getId());
        command.setQuantity(1);
        command.setPriceConsidered(100.0f);
        
        List<Command> commands = new ArrayList<>();
        commands.add(command);
        
        File outputFile = new File("test_bill_both_empty.pdf");
        
        try {
            // Should generate PDF successfully with both fields empty
            assertDoesNotThrow(() -> reportService.generateBillPDF(bill, commands, outputFile));
            assertTrue(outputFile.exists());
            assertTrue(outputFile.length() > 0);
            
            logger.info("Successfully generated PDF with both client name and phone empty");
        } finally {
            // Cleanup
            if (outputFile.exists()) {
                outputFile.delete();
            }
        }
    }
    
    @Test
    @Order(4)
    void testGeneratePDFWithNullClientName() {
        Bill bill = new Bill();
        bill.setId(4);
        bill.setClientName(null); // Null client name
        bill.setClientPhone("1234567890");
        bill.setTotalPrice(100.0f);
        bill.setDate(LocalDate.now());
        
        Part part = partService.getAllParts().get(0);
        Command command = new Command();
        command.setPartId(part.getId());
        command.setQuantity(1);
        command.setPriceConsidered(100.0f);
        
        List<Command> commands = new ArrayList<>();
        commands.add(command);
        
        File outputFile = new File("test_bill_null_name.pdf");
        
        try {
            // Should generate PDF successfully with null client name
            assertDoesNotThrow(() -> reportService.generateBillPDF(bill, commands, outputFile));
            assertTrue(outputFile.exists());
            assertTrue(outputFile.length() > 0);
            
            logger.info("Successfully generated PDF with null client name");
        } finally {
            // Cleanup
            if (outputFile.exists()) {
                outputFile.delete();
            }
        }
    }
}
