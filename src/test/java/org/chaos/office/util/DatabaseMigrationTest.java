package org.chaos.office.util;

import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for DatabaseMigration.
 * Tests the POS system database migration and verification.
 * 
 * Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 8.1, 8.2
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatabaseMigrationTest {
    
    private static Connection connection;
    private static Path testDbPath;
    
    @BeforeAll
    static void setUp() throws SQLException, IOException {
        // Create a test database in a temporary location
        testDbPath = Paths.get(System.getProperty("java.io.tmpdir"), "test_chaoffice_migration.db");
        
        // Delete existing test database if it exists
        Files.deleteIfExists(testDbPath);
        
        // Initialize database connection
        DatabaseConnection dbConn = DatabaseConnection.getInstance();
        connection = dbConn.getConnection();
        
        // Initialize the base schema
        dbConn.initializeDatabase();
    }
    
    @AfterAll
    static void tearDown() throws SQLException, IOException {
        // Close connection
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
        
        // Clean up test database
        DatabaseConnection.getInstance().closeConnection();
        
        // Note: We don't delete the test database file here so it can be inspected if needed
        // Files.deleteIfExists(testDbPath);
    }
    
    @Test
    @Order(1)
    @DisplayName("Test migration execution")
    void testMigrationExecution() throws SQLException, IOException {
        // Execute migration
        assertDoesNotThrow(() -> DatabaseMigration.executePOSMigration(connection));
    }
    
    @Test
    @Order(2)
    @DisplayName("Test migration verification - columns exist")
    void testMigrationVerificationColumns() throws SQLException {
        // Verify all new columns exist
        assertTrue(verifyColumnExists("bills", "subtotal"), 
            "Column 'subtotal' should exist in bills table");
        assertTrue(verifyColumnExists("bills", "discount_type"), 
            "Column 'discount_type' should exist in bills table");
        assertTrue(verifyColumnExists("bills", "discount_value"), 
            "Column 'discount_value' should exist in bills table");
        assertTrue(verifyColumnExists("bills", "payment_method"), 
            "Column 'payment_method' should exist in bills table");
    }
    
    @Test
    @Order(3)
    @DisplayName("Test migration verification - indexes exist")
    void testMigrationVerificationIndexes() throws SQLException {
        // Get all indexes
        List<String> indexes = DatabaseMigration.getAllIndexes(connection);
        
        // Verify search optimization indexes exist
        assertTrue(indexes.contains("idx_parts_name"), 
            "Index 'idx_parts_name' should exist");
        assertTrue(indexes.contains("idx_parts_catid"), 
            "Index 'idx_parts_catid' should exist");
    }
    
    @Test
    @Order(4)
    @DisplayName("Test discount_type constraint enforcement")
    void testDiscountTypeConstraint() {
        // Test valid values
        assertDoesNotThrow(() -> {
            connection.createStatement().execute(
                "INSERT INTO bills (totalprice, clientname, clientphone, date, discount_type) " +
                "VALUES (100.0, 'Test Client', '1234567890', '2024-01-01', 'none')"
            );
        }, "Should accept 'none' as discount_type");
        
        assertDoesNotThrow(() -> {
            connection.createStatement().execute(
                "INSERT INTO bills (totalprice, clientname, clientphone, date, discount_type) " +
                "VALUES (100.0, 'Test Client', '1234567890', '2024-01-01', 'percentage')"
            );
        }, "Should accept 'percentage' as discount_type");
        
        assertDoesNotThrow(() -> {
            connection.createStatement().execute(
                "INSERT INTO bills (totalprice, clientname, clientphone, date, discount_type) " +
                "VALUES (100.0, 'Test Client', '1234567890', '2024-01-01', 'fixed')"
            );
        }, "Should accept 'fixed' as discount_type");
        
        // Test invalid value
        SQLException exception = assertThrows(SQLException.class, () -> {
            connection.createStatement().execute(
                "INSERT INTO bills (totalprice, clientname, clientphone, date, discount_type) " +
                "VALUES (100.0, 'Test Client', '1234567890', '2024-01-01', 'invalid')"
            );
        }, "Should reject invalid discount_type");
        
        assertTrue(exception.getMessage().contains("constraint") || 
                   exception.getMessage().contains("CHECK"),
            "Error message should indicate constraint violation");
    }
    
    @Test
    @Order(5)
    @DisplayName("Test payment_method constraint enforcement")
    void testPaymentMethodConstraint() {
        // Test valid values
        assertDoesNotThrow(() -> {
            connection.createStatement().execute(
                "INSERT INTO bills (totalprice, clientname, clientphone, date, payment_method) " +
                "VALUES (100.0, 'Test Client', '1234567890', '2024-01-01', 'cash')"
            );
        }, "Should accept 'cash' as payment_method");
        
        assertDoesNotThrow(() -> {
            connection.createStatement().execute(
                "INSERT INTO bills (totalprice, clientname, clientphone, date, payment_method) " +
                "VALUES (100.0, 'Test Client', '1234567890', '2024-01-01', 'card')"
            );
        }, "Should accept 'card' as payment_method");
        
        assertDoesNotThrow(() -> {
            connection.createStatement().execute(
                "INSERT INTO bills (totalprice, clientname, clientphone, date, payment_method) " +
                "VALUES (100.0, 'Test Client', '1234567890', '2024-01-01', 'check')"
            );
        }, "Should accept 'check' as payment_method");
        
        // Test invalid value
        SQLException exception = assertThrows(SQLException.class, () -> {
            connection.createStatement().execute(
                "INSERT INTO bills (totalprice, clientname, clientphone, date, payment_method) " +
                "VALUES (100.0, 'Test Client', '1234567890', '2024-01-01', 'bitcoin')"
            );
        }, "Should reject invalid payment_method");
        
        assertTrue(exception.getMessage().contains("constraint") || 
                   exception.getMessage().contains("CHECK"),
            "Error message should indicate constraint violation");
    }
    
    @Test
    @Order(6)
    @DisplayName("Test default values for new columns")
    void testDefaultValues() throws SQLException {
        // Insert a bill without specifying new columns
        connection.createStatement().execute(
            "INSERT INTO bills (totalprice, clientname, clientphone, date) " +
            "VALUES (150.0, 'Default Test', '9876543210', '2024-01-02')"
        );
        
        // Query the bill and verify default values
        var rs = connection.createStatement().executeQuery(
            "SELECT subtotal, discount_type, discount_value, payment_method " +
            "FROM bills WHERE clientname = 'Default Test'"
        );
        
        assertTrue(rs.next(), "Should find the inserted bill");
        
        assertEquals(0.0, rs.getDouble("subtotal"), 0.001, 
            "Default subtotal should be 0.0");
        assertEquals("none", rs.getString("discount_type"), 
            "Default discount_type should be 'none'");
        assertEquals(0.0, rs.getDouble("discount_value"), 0.001, 
            "Default discount_value should be 0.0");
        assertEquals("cash", rs.getString("payment_method"), 
            "Default payment_method should be 'cash'");
    }
    
    @Test
    @Order(7)
    @DisplayName("Test idempotent migration execution")
    void testIdempotentMigration() {
        // Running migration again should not cause errors
        assertDoesNotThrow(() -> DatabaseMigration.executePOSMigration(connection),
            "Migration should be idempotent and not fail when run multiple times");
    }
    
    @Test
    @Order(8)
    @DisplayName("Test schema information retrieval")
    void testSchemaInformation() throws SQLException {
        // Get bills table schema
        List<String> schema = DatabaseMigration.getBillsTableSchema(connection);
        
        assertFalse(schema.isEmpty(), "Schema information should not be empty");
        
        // Verify new columns are in the schema
        boolean hasSubtotal = schema.stream().anyMatch(s -> s.contains("subtotal"));
        boolean hasDiscountType = schema.stream().anyMatch(s -> s.contains("discount_type"));
        boolean hasDiscountValue = schema.stream().anyMatch(s -> s.contains("discount_value"));
        boolean hasPaymentMethod = schema.stream().anyMatch(s -> s.contains("payment_method"));
        
        assertTrue(hasSubtotal, "Schema should include subtotal column");
        assertTrue(hasDiscountType, "Schema should include discount_type column");
        assertTrue(hasDiscountValue, "Schema should include discount_value column");
        assertTrue(hasPaymentMethod, "Schema should include payment_method column");
    }
    
    @Test
    @Order(9)
    @DisplayName("Test numeric precision for monetary values")
    void testNumericPrecision() throws SQLException {
        // Insert a bill with precise decimal values
        connection.createStatement().execute(
            "INSERT INTO bills (totalprice, clientname, clientphone, date, subtotal, discount_value) " +
            "VALUES (123.45, 'Precision Test', '5555555555', '2024-01-03', 150.99, 27.54)"
        );
        
        // Query and verify precision
        var rs = connection.createStatement().executeQuery(
            "SELECT totalprice, subtotal, discount_value " +
            "FROM bills WHERE clientname = 'Precision Test'"
        );
        
        assertTrue(rs.next(), "Should find the inserted bill");
        
        assertEquals(123.45, rs.getDouble("totalprice"), 0.001, 
            "Total price should preserve 2 decimal places");
        assertEquals(150.99, rs.getDouble("subtotal"), 0.001, 
            "Subtotal should preserve 2 decimal places");
        assertEquals(27.54, rs.getDouble("discount_value"), 0.001, 
            "Discount value should preserve 2 decimal places");
    }
    
    /**
     * Helper method to verify if a column exists in a table.
     */
    private boolean verifyColumnExists(String tableName, String columnName) throws SQLException {
        var rs = connection.createStatement().executeQuery("PRAGMA table_info(" + tableName + ")");
        
        while (rs.next()) {
            if (columnName.equals(rs.getString("name"))) {
                return true;
            }
        }
        
        return false;
    }
}
