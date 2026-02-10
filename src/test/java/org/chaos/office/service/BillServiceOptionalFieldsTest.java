package org.chaos.office.service;

import org.chaos.office.model.Bill;
import org.chaos.office.model.Command;
import org.chaos.office.model.Part;
import org.chaos.office.util.DatabaseConnection;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for optional client name and phone fields in BillService.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BillServiceOptionalFieldsTest {
    
    private static final Logger logger = LoggerFactory.getLogger(BillServiceOptionalFieldsTest.class);
    private BillService billService;
    private PartService partService;
    
    @BeforeEach
    void setUp() {
        billService = new BillService();
        partService = new PartService();
    }
    
    @Test
    @Order(1)
    void testSaveBillWithEmptyClientName() {
        Bill bill = new Bill();
        bill.setClientName(""); // Empty client name
        bill.setClientPhone("1234567890");
        bill.setSubtotal(100.0f);
        bill.setTotalPrice(100.0f);
        bill.setDate(LocalDate.now());
        
        Part part = partService.getAllParts().get(0);
        Command command = new Command();
        command.setPartId(part.getId());
        command.setQuantity(1);
        command.setPriceConsidered(100.0f);
        
        List<Command> commands = new ArrayList<>();
        commands.add(command);
        
        // Should save successfully with empty client name
        assertDoesNotThrow(() -> billService.saveBill(bill, commands));
        assertTrue(bill.getId() > 0);
        
        // Verify it was saved
        Optional<Bill> retrieved = billService.getBillById(bill.getId());
        assertTrue(retrieved.isPresent());
        assertEquals("", retrieved.get().getClientName());
        
        logger.info("Successfully saved bill with empty client name");
    }
    
    @Test
    @Order(2)
    void testSaveBillWithEmptyClientPhone() {
        Bill bill = new Bill();
        bill.setClientName("Test Client");
        bill.setClientPhone(""); // Empty client phone
        bill.setSubtotal(100.0f);
        bill.setTotalPrice(100.0f);
        bill.setDate(LocalDate.now());
        
        Part part = partService.getAllParts().get(0);
        Command command = new Command();
        command.setPartId(part.getId());
        command.setQuantity(1);
        command.setPriceConsidered(100.0f);
        
        List<Command> commands = new ArrayList<>();
        commands.add(command);
        
        // Should save successfully with empty client phone
        assertDoesNotThrow(() -> billService.saveBill(bill, commands));
        assertTrue(bill.getId() > 0);
        
        // Verify it was saved
        Optional<Bill> retrieved = billService.getBillById(bill.getId());
        assertTrue(retrieved.isPresent());
        assertEquals("", retrieved.get().getClientPhone());
        
        logger.info("Successfully saved bill with empty client phone");
    }
    
    @Test
    @Order(3)
    void testSaveBillWithBothFieldsEmpty() {
        Bill bill = new Bill();
        bill.setClientName(""); // Empty client name
        bill.setClientPhone(""); // Empty client phone
        bill.setSubtotal(100.0f);
        bill.setTotalPrice(100.0f);
        bill.setDate(LocalDate.now());
        
        Part part = partService.getAllParts().get(0);
        Command command = new Command();
        command.setPartId(part.getId());
        command.setQuantity(1);
        command.setPriceConsidered(100.0f);
        
        List<Command> commands = new ArrayList<>();
        commands.add(command);
        
        // Should save successfully with both fields empty
        assertDoesNotThrow(() -> billService.saveBill(bill, commands));
        assertTrue(bill.getId() > 0);
        
        // Verify it was saved
        Optional<Bill> retrieved = billService.getBillById(bill.getId());
        assertTrue(retrieved.isPresent());
        assertEquals("", retrieved.get().getClientName());
        assertEquals("", retrieved.get().getClientPhone());
        
        logger.info("Successfully saved bill with both client name and phone empty");
    }
    
    @AfterAll
    static void cleanup() {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            // Delete test commands first
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM commands WHERE billid IN (SELECT id FROM bills WHERE clientname = '' OR clientname = 'Test Client')")) {
                stmt.executeUpdate();
            }
            
            // Delete test bills
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM bills WHERE clientname = '' OR clientname = 'Test Client'")) {
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            logger.error("Error cleaning up test data", e);
        }
    }
}
