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
 * Unit tests for BillService.
 */
class BillServiceTest {
    
    private BillService billService;
    private int testCategoryId;
    private int testMakerId;
    private int testPartId;
    
    @BeforeEach
    void setUp() throws Exception {
        billService = new BillService();
        DatabaseConnection.getInstance().initializeDatabase();
        
        // Create test data
        testCategoryId = createTestCategory("Test Category");
        testMakerId = createTestMaker("Test Maker");
        testPartId = createTestPart("Test Part", testMakerId, testCategoryId, 100.0f, 50);
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
    void testSaveBillWithCommands() {
        Bill bill = new Bill();
        bill.setClientName("John Doe");
        bill.setClientPhone("1234567890");
        bill.setTotalPrice(500.0f);
        bill.setDate(LocalDate.now());
        
        List<Command> commands = new ArrayList<>();
        Command command = new Command();
        command.setPartId(testPartId);
        command.setQuantity(5);
        command.setPriceConsidered(100.0f);
        commands.add(command);
        
        billService.saveBill(bill, commands);
        
        // Verify bill was saved
        assertTrue(bill.getId() > 0);
        
        Optional<Bill> retrieved = billService.getBillById(bill.getId());
        assertTrue(retrieved.isPresent());
        assertEquals("John Doe", retrieved.get().getClientName());
        assertEquals("1234567890", retrieved.get().getClientPhone());
        assertEquals(500.0f, retrieved.get().getTotalPrice(), 0.01f);
    }
    
    @Test
    void testGetCommandsForBill() {
        Bill bill = new Bill();
        bill.setClientName("Jane Smith");
        bill.setClientPhone("9876543210");
        bill.setTotalPrice(300.0f);
        bill.setDate(LocalDate.now());
        
        List<Command> commands = new ArrayList<>();
        Command command = new Command();
        command.setPartId(testPartId);
        command.setQuantity(3);
        command.setPriceConsidered(100.0f);
        commands.add(command);
        
        billService.saveBill(bill, commands);
        
        List<Command> retrievedCommands = billService.getCommandsForBill(bill.getId());
        assertEquals(1, retrievedCommands.size());
        assertEquals(testPartId, retrievedCommands.get(0).getPartId());
        assertEquals(3, retrievedCommands.get(0).getQuantity());
        assertEquals(100.0f, retrievedCommands.get(0).getPriceConsidered(), 0.01f);
    }
    
    @Test
    void testInventoryReductionOnSale() throws SQLException {
        // Get initial inventory
        int initialQuantity = getPartQuantity(testPartId);
        
        Bill bill = new Bill();
        bill.setClientName("Test Client");
        bill.setClientPhone("5555555555");
        bill.setTotalPrice(200.0f);
        bill.setDate(LocalDate.now());
        
        List<Command> commands = new ArrayList<>();
        Command command = new Command();
        command.setPartId(testPartId);
        command.setQuantity(2);
        command.setPriceConsidered(100.0f);
        commands.add(command);
        
        billService.saveBill(bill, commands);
        
        // Verify inventory was reduced
        int newQuantity = getPartQuantity(testPartId);
        assertEquals(initialQuantity - 2, newQuantity);
    }
    
    @Test
    void testInsufficientInventoryThrowsException() {
        Bill bill = new Bill();
        bill.setClientName("Test Client");
        bill.setClientPhone("5555555555");
        bill.setTotalPrice(10000.0f);
        bill.setDate(LocalDate.now());
        
        List<Command> commands = new ArrayList<>();
        Command command = new Command();
        command.setPartId(testPartId);
        command.setQuantity(1000); // More than available
        command.setPriceConsidered(100.0f);
        commands.add(command);
        
        assertThrows(IllegalArgumentException.class, () -> billService.saveBill(bill, commands));
    }
    
    @Test
    void testFilterByDateRange() {
        // Create bills with different dates
        Bill bill1 = new Bill();
        bill1.setClientName("Client 1");
        bill1.setClientPhone("1111111111");
        bill1.setTotalPrice(100.0f);
        bill1.setDate(LocalDate.of(2024, 1, 15));
        
        List<Command> commands1 = new ArrayList<>();
        Command command1 = new Command();
        command1.setPartId(testPartId);
        command1.setQuantity(1);
        command1.setPriceConsidered(100.0f);
        commands1.add(command1);
        
        billService.saveBill(bill1, commands1);
        
        Bill bill2 = new Bill();
        bill2.setClientName("Client 2");
        bill2.setClientPhone("2222222222");
        bill2.setTotalPrice(200.0f);
        bill2.setDate(LocalDate.of(2024, 2, 20));
        
        List<Command> commands2 = new ArrayList<>();
        Command command2 = new Command();
        command2.setPartId(testPartId);
        command2.setQuantity(2);
        command2.setPriceConsidered(100.0f);
        commands2.add(command2);
        
        billService.saveBill(bill2, commands2);
        
        // Filter by date range
        List<Bill> filtered = billService.filterByDateRange(
            LocalDate.of(2024, 1, 1), 
            LocalDate.of(2024, 1, 31)
        );
        
        // Should only contain bill1
        assertTrue(filtered.stream().anyMatch(b -> "Client 1".equals(b.getClientName())));
        assertFalse(filtered.stream().anyMatch(b -> "Client 2".equals(b.getClientName())));
    }
    
    @Test
    void testGetAllBills() {
        int initialCount = billService.getAllBills().size();
        
        Bill bill = new Bill();
        bill.setClientName("Test Client");
        bill.setClientPhone("5555555555");
        bill.setTotalPrice(100.0f);
        bill.setDate(LocalDate.now());
        
        List<Command> commands = new ArrayList<>();
        Command command = new Command();
        command.setPartId(testPartId);
        command.setQuantity(1);
        command.setPriceConsidered(100.0f);
        commands.add(command);
        
        billService.saveBill(bill, commands);
        
        List<Bill> allBills = billService.getAllBills();
        assertTrue(allBills.size() >= initialCount + 1);
    }
    
    @Test
    void testTransactionRollbackOnError() throws SQLException {
        // Get initial inventory
        int initialQuantity = getPartQuantity(testPartId);
        
        Bill bill = new Bill();
        bill.setClientName("Test Client");
        bill.setClientPhone("5555555555");
        bill.setTotalPrice(10000.0f);
        bill.setDate(LocalDate.now());
        
        List<Command> commands = new ArrayList<>();
        Command command = new Command();
        command.setPartId(testPartId);
        command.setQuantity(1000); // More than available
        command.setPriceConsidered(100.0f);
        commands.add(command);
        
        try {
            billService.saveBill(bill, commands);
            fail("Should have thrown exception");
        } catch (IllegalArgumentException e) {
            // Expected
        }
        
        // Verify inventory was not changed
        int newQuantity = getPartQuantity(testPartId);
        assertEquals(initialQuantity, newQuantity);
    }
    
    @Test
    void testMultipleCommandsInBill() {
        int testPartId2 = createTestPart("Test Part 2", testMakerId, testCategoryId, 50.0f, 30);
        
        Bill bill = new Bill();
        bill.setClientName("Multi Command Client");
        bill.setClientPhone("9999999999");
        bill.setTotalPrice(350.0f);
        bill.setDate(LocalDate.now());
        
        List<Command> commands = new ArrayList<>();
        
        Command command1 = new Command();
        command1.setPartId(testPartId);
        command1.setQuantity(2);
        command1.setPriceConsidered(100.0f);
        commands.add(command1);
        
        Command command2 = new Command();
        command2.setPartId(testPartId2);
        command2.setQuantity(3);
        command2.setPriceConsidered(50.0f);
        commands.add(command2);
        
        billService.saveBill(bill, commands);
        
        List<Command> retrievedCommands = billService.getCommandsForBill(bill.getId());
        assertEquals(2, retrievedCommands.size());
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
        // First check if maker already exists
        String checkSql = "SELECT id FROM makers WHERE name = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, name);
            try (var rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        
        // If not exists, create new
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
    
    private int getPartQuantity(int partId) throws SQLException {
        String sql = "SELECT quantity FROM parts WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, partId);
            
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("quantity");
                }
            }
        }
        
        return 0;
    }
    
    private void cleanupTestData() throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            // Delete test commands first
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM commands WHERE billid IN (SELECT id FROM bills WHERE clientname LIKE 'Test%' OR clientname LIKE '%Client%')")) {
                stmt.executeUpdate();
            }
            
            // Delete test bills
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM bills WHERE clientname LIKE 'Test%' OR clientname LIKE '%Client%'")) {
                stmt.executeUpdate();
            }
            
            // Delete test parts
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM parts WHERE name LIKE 'Test%'")) {
                stmt.executeUpdate();
            }
            
            // Delete test categories
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM categories WHERE name LIKE 'Test%'")) {
                stmt.executeUpdate();
            }
            
            // Delete test makers
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM makers WHERE name LIKE 'Test%'")) {
                stmt.executeUpdate();
            }
        }
    }
}
