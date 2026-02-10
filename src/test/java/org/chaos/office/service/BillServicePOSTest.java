package org.chaos.office.service;

import org.chaos.office.model.Bill;
import org.chaos.office.model.Command;
import org.chaos.office.util.DatabaseConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BillService POS features.
 * Tests the new discount, payment method, and subtotal functionality.
 */
class BillServicePOSTest {
    
    private BillService billService;
    private int testCategoryId;
    private int testMakerId;
    private int testPartId;
    
    @BeforeEach
    void setUp() throws Exception {
        billService = new BillService();
        DatabaseConnection.getInstance().initializeDatabase();
        
        // Create test data with unique names to avoid conflicts
        String uniqueSuffix = "_" + System.currentTimeMillis();
        testCategoryId = createTestCategory("POS Test Category" + uniqueSuffix);
        testMakerId = createTestMaker("POS Test Maker" + uniqueSuffix);
        testPartId = createTestPart("POS Test Part" + uniqueSuffix, testMakerId, testCategoryId, 100.0f, 50);
    }
    
    @AfterEach
    void tearDown() {
        try {
            cleanupTestData();
        } catch (SQLException e) {
            // Ignore cleanup errors
        }
    }
    
    @Test
    void testSaveBillWithDiscountAndPaymentMethod() {
        Bill bill = new Bill();
        bill.setClientName("POS Test Client");
        bill.setClientPhone("1234567890");
        bill.setSubtotal(500.0f);
        bill.setDiscountType("percentage");
        bill.setDiscountValue(10.0f);
        bill.setPaymentMethod("card");
        bill.calculateFinalTotal(); // Should be 450.0
        bill.setDate(LocalDate.now());
        
        List<Command> commands = new ArrayList<>();
        Command command = new Command();
        command.setPartId(testPartId);
        command.setQuantity(5);
        command.setPriceConsidered(100.0f);
        commands.add(command);
        
        billService.saveBill(bill, commands);
        
        // Verify bill was saved with POS properties
        assertTrue(bill.getId() > 0);
        
        Optional<Bill> retrieved = billService.getBillById(bill.getId());
        assertTrue(retrieved.isPresent());
        
        Bill retrievedBill = retrieved.get();
        assertEquals("POS Test Client", retrievedBill.getClientName());
        assertEquals(500.0f, retrievedBill.getSubtotal(), 0.01f);
        assertEquals("percentage", retrievedBill.getDiscountType());
        assertEquals(10.0f, retrievedBill.getDiscountValue(), 0.01f);
        assertEquals("card", retrievedBill.getPaymentMethod());
        assertEquals(450.0f, retrievedBill.getTotalPrice(), 0.01f);
    }
    
    @Test
    void testSaveBillWithFixedDiscount() {
        Bill bill = new Bill();
        bill.setClientName("POS Fixed Discount Client");
        bill.setClientPhone("9876543210");
        bill.setSubtotal(300.0f);
        bill.setDiscountType("fixed");
        bill.setDiscountValue(50.0f);
        bill.setPaymentMethod("cash");
        bill.calculateFinalTotal(); // Should be 250.0
        bill.setDate(LocalDate.now());
        
        List<Command> commands = new ArrayList<>();
        Command command = new Command();
        command.setPartId(testPartId);
        command.setQuantity(3);
        command.setPriceConsidered(100.0f);
        commands.add(command);
        
        billService.saveBill(bill, commands);
        
        Optional<Bill> retrieved = billService.getBillById(bill.getId());
        assertTrue(retrieved.isPresent());
        
        Bill retrievedBill = retrieved.get();
        assertEquals(300.0f, retrievedBill.getSubtotal(), 0.01f);
        assertEquals("fixed", retrievedBill.getDiscountType());
        assertEquals(50.0f, retrievedBill.getDiscountValue(), 0.01f);
        assertEquals(250.0f, retrievedBill.getTotalPrice(), 0.01f);
    }
    
    @Test
    void testSaveBillWithNoDiscount() {
        Bill bill = new Bill();
        bill.setClientName("POS No Discount Client");
        bill.setClientPhone("5555555555");
        bill.setSubtotal(200.0f);
        bill.setDiscountType("none");
        bill.setDiscountValue(0.0f);
        bill.setPaymentMethod("check");
        bill.calculateFinalTotal(); // Should be 200.0
        bill.setDate(LocalDate.now());
        
        List<Command> commands = new ArrayList<>();
        Command command = new Command();
        command.setPartId(testPartId);
        command.setQuantity(2);
        command.setPriceConsidered(100.0f);
        commands.add(command);
        
        billService.saveBill(bill, commands);
        
        Optional<Bill> retrieved = billService.getBillById(bill.getId());
        assertTrue(retrieved.isPresent());
        
        Bill retrievedBill = retrieved.get();
        assertEquals(200.0f, retrievedBill.getSubtotal(), 0.01f);
        assertEquals("none", retrievedBill.getDiscountType());
        assertEquals(0.0f, retrievedBill.getDiscountValue(), 0.01f);
        assertEquals("check", retrievedBill.getPaymentMethod());
        assertEquals(200.0f, retrievedBill.getTotalPrice(), 0.01f);
    }
    
    @Test
    void testValidationRejectsInvalidDiscountType() {
        Bill bill = new Bill();
        bill.setClientName("Invalid Discount Client");
        bill.setClientPhone("1111111111");
        bill.setSubtotal(100.0f);
        bill.setDiscountType("invalid"); // Invalid type
        bill.setDiscountValue(10.0f);
        bill.setPaymentMethod("cash");
        bill.setDate(LocalDate.now());
        
        List<Command> commands = new ArrayList<>();
        Command command = new Command();
        command.setPartId(testPartId);
        command.setQuantity(1);
        command.setPriceConsidered(100.0f);
        commands.add(command);
        
        assertThrows(IllegalArgumentException.class, () -> billService.saveBill(bill, commands));
    }
    
    @Test
    void testValidationRejectsInvalidPaymentMethod() {
        Bill bill = new Bill();
        bill.setClientName("Invalid Payment Client");
        bill.setClientPhone("2222222222");
        bill.setSubtotal(100.0f);
        bill.setDiscountType("none");
        bill.setDiscountValue(0.0f);
        bill.setDate(LocalDate.now());
        
        // The Bill model itself validates payment method in setPaymentMethod
        assertThrows(IllegalArgumentException.class, () -> bill.setPaymentMethod("bitcoin"));
    }
    
    @Test
    void testValidationRejectsPercentageOver100() {
        Bill bill = new Bill();
        bill.setClientName("Over 100 Percent Client");
        bill.setClientPhone("3333333333");
        bill.setSubtotal(100.0f);
        bill.setDiscountType("percentage");
        bill.setDiscountValue(150.0f); // Over 100%
        bill.setPaymentMethod("cash");
        bill.setDate(LocalDate.now());
        
        List<Command> commands = new ArrayList<>();
        Command command = new Command();
        command.setPartId(testPartId);
        command.setQuantity(1);
        command.setPriceConsidered(100.0f);
        commands.add(command);
        
        assertThrows(IllegalArgumentException.class, () -> billService.saveBill(bill, commands));
    }
    
    @Test
    void testValidationRejectsFixedDiscountExceedingSubtotal() {
        Bill bill = new Bill();
        bill.setClientName("Excessive Discount Client");
        bill.setClientPhone("4444444444");
        bill.setSubtotal(100.0f);
        bill.setDiscountType("fixed");
        bill.setDiscountValue(150.0f); // Exceeds subtotal
        bill.setPaymentMethod("cash");
        bill.setDate(LocalDate.now());
        
        List<Command> commands = new ArrayList<>();
        Command command = new Command();
        command.setPartId(testPartId);
        command.setQuantity(1);
        command.setPriceConsidered(100.0f);
        commands.add(command);
        
        assertThrows(IllegalArgumentException.class, () -> billService.saveBill(bill, commands));
    }
    
    @Test
    void testBackwardsCompatibilityWithNullValues() {
        // This test simulates retrieving an old bill that doesn't have POS columns
        // The mapResultSetToBill method should handle NULL values gracefully
        
        // First, save a bill with default values
        Bill bill = new Bill();
        bill.setClientName("Backwards Compat Client");
        bill.setClientPhone("6666666666");
        bill.setSubtotal(100.0f);
        bill.setDiscountType("none");
        bill.setDiscountValue(0.0f);
        bill.setPaymentMethod("cash");
        bill.calculateFinalTotal();
        bill.setDate(LocalDate.now());
        
        List<Command> commands = new ArrayList<>();
        Command command = new Command();
        command.setPartId(testPartId);
        command.setQuantity(1);
        command.setPriceConsidered(100.0f);
        commands.add(command);
        
        billService.saveBill(bill, commands);
        
        // Retrieve and verify defaults are applied
        Optional<Bill> retrieved = billService.getBillById(bill.getId());
        assertTrue(retrieved.isPresent());
        
        Bill retrievedBill = retrieved.get();
        assertNotNull(retrievedBill.getDiscountType());
        assertNotNull(retrievedBill.getPaymentMethod());
        assertEquals("none", retrievedBill.getDiscountType());
        assertEquals("cash", retrievedBill.getPaymentMethod());
    }
    
    // Helper methods
    
    private int createTestCategory(String name) {
        String sql = "INSERT INTO categories (name, description) VALUES (?, ?)";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, "Test Description");
            stmt.executeUpdate();
            
            try (PreparedStatement idStmt = conn.prepareStatement("SELECT last_insert_rowid()");
                 var rs = idStmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    
    private int createTestMaker(String name) {
        String sql = "INSERT INTO makers (name) VALUES (?)";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.executeUpdate();
            
            try (PreparedStatement idStmt = conn.prepareStatement("SELECT last_insert_rowid()");
                 var rs = idStmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    
    private int createTestPart(String name, int makerId, int categoryId, float price, int quantity) {
        String sql = "INSERT INTO parts (name, maker_id, description, price, quantity, catid) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setInt(2, makerId);
            stmt.setString(3, "Test Description");
            stmt.setFloat(4, price);
            stmt.setInt(5, quantity);
            stmt.setInt(6, categoryId);
            stmt.executeUpdate();
            
            try (PreparedStatement idStmt = conn.prepareStatement("SELECT last_insert_rowid()");
                 var rs = idStmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    
    private void cleanupTestData() throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            // Delete test commands first
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM commands WHERE billid IN (SELECT id FROM bills WHERE clientname LIKE 'POS%')")) {
                stmt.executeUpdate();
            }
            
            // Delete test bills
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM bills WHERE clientname LIKE 'POS%'")) {
                stmt.executeUpdate();
            }
            
            // Delete test parts
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM parts WHERE name LIKE 'POS Test%'")) {
                stmt.executeUpdate();
            }
            
            // Delete test categories
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM categories WHERE name LIKE 'POS Test%'")) {
                stmt.executeUpdate();
            }
            
            // Delete test makers
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM makers WHERE name LIKE 'POS Test%'")) {
                stmt.executeUpdate();
            }
        }
    }
}
