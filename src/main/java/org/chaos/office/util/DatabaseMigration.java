package org.chaos.office.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DatabaseMigration utility class for managing database schema migrations.
 * Handles incremental schema updates while maintaining backwards compatibility.
 * 
 * <p>This class provides:
 * <ul>
 *   <li>Migration script execution</li>
 *   <li>Schema verification</li>
 *   <li>Rollback support</li>
 * </ul>
 * 
 * <p>Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 8.1, 8.2
 */
public class DatabaseMigration {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseMigration.class);
    
    private static final String MIGRATION_SCRIPT_PATH = "/sql/migration_pos_v1.sql";
    
    /**
     * Executes the POS system migration script.
     * This method adds new columns to the bills table and creates search indexes.
     * 
     * @param connection the database connection
     * @throws SQLException if a database access error occurs
     * @throws IOException if the migration script cannot be read
     */
    public static void executePOSMigration(Connection connection) throws SQLException, IOException {
        logger.info("Starting POS system database migration...");
        
        // Check if migration is needed
        if (isMigrationAlreadyApplied(connection)) {
            logger.info("POS migration already applied, skipping...");
            return;
        }
        
        // Read migration script
        String migrationScript = readMigrationScript();
        
        // Execute migration in a transaction
        boolean autoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(false);
            
            executeMigrationScript(connection, migrationScript);
            
            connection.commit();
            logger.info("POS migration completed successfully");
            
        } catch (SQLException e) {
            logger.error("Migration failed, rolling back changes", e);
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
        
        // Verify migration
        verifyMigration(connection);
    }
    
    /**
     * Checks if the POS migration has already been applied.
     * 
     * @param connection the database connection
     * @return true if migration is already applied, false otherwise
     * @throws SQLException if a database access error occurs
     */
    private static boolean isMigrationAlreadyApplied(Connection connection) throws SQLException {
        // Check if the subtotal column exists in bills table
        String query = "PRAGMA table_info(bills)";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                String columnName = rs.getString("name");
                if ("subtotal".equals(columnName)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Reads the migration script from resources.
     * 
     * @return the migration script content
     * @throws IOException if the script cannot be read
     */
    private static String readMigrationScript() throws IOException {
        try (InputStream inputStream = DatabaseMigration.class.getResourceAsStream(MIGRATION_SCRIPT_PATH)) {
            if (inputStream == null) {
                throw new IOException("Migration script not found at: " + MIGRATION_SCRIPT_PATH);
            }
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
    }
    
    /**
     * Executes a migration script containing multiple SQL statements.
     * 
     * @param connection the database connection
     * @param migrationScript the migration script to execute
     * @throws SQLException if a database access error occurs
     */
    private static void executeMigrationScript(Connection connection, String migrationScript) throws SQLException {
        StringBuilder currentStatement = new StringBuilder();
        
        try (Statement stmt = connection.createStatement()) {
            String[] lines = migrationScript.split("\n");
            
            for (String line : lines) {
                line = line.trim();
                
                // Skip empty lines and comment-only lines
                if (line.isEmpty() || line.startsWith("--")) {
                    continue;
                }
                
                // Remove inline comments
                int commentIndex = line.indexOf("--");
                if (commentIndex > 0) {
                    line = line.substring(0, commentIndex).trim();
                }
                
                // Add line to current statement
                currentStatement.append(line).append(" ");
                
                // If line ends with semicolon, execute the statement
                if (line.endsWith(";")) {
                    String sql = currentStatement.toString().trim();
                    sql = sql.substring(0, sql.length() - 1).trim();
                    
                    if (!sql.isEmpty()) {
                        logger.debug("Executing migration SQL: {}", sql.substring(0, Math.min(50, sql.length())) + "...");
                        stmt.execute(sql);
                    }
                    
                    currentStatement = new StringBuilder();
                }
            }
        }
    }
    
    /**
     * Verifies that the migration was applied successfully.
     * Checks for the presence of new columns and indexes.
     * 
     * @param connection the database connection
     * @throws SQLException if verification fails
     */
    public static void verifyMigration(Connection connection) throws SQLException {
        logger.info("Verifying POS migration...");
        
        List<String> errors = new ArrayList<>();
        
        // Verify bills table columns
        if (!verifyColumn(connection, "bills", "subtotal")) {
            errors.add("Column 'subtotal' not found in bills table");
        }
        if (!verifyColumn(connection, "bills", "discount_type")) {
            errors.add("Column 'discount_type' not found in bills table");
        }
        if (!verifyColumn(connection, "bills", "discount_value")) {
            errors.add("Column 'discount_value' not found in bills table");
        }
        if (!verifyColumn(connection, "bills", "payment_method")) {
            errors.add("Column 'payment_method' not found in bills table");
        }
        
        // Verify indexes
        if (!verifyIndex(connection, "idx_parts_name")) {
            errors.add("Index 'idx_parts_name' not found");
        }
        if (!verifyIndex(connection, "idx_parts_catid")) {
            errors.add("Index 'idx_parts_catid' not found");
        }
        
        // Verify constraints
        if (!verifyConstraints(connection)) {
            errors.add("Constraint verification failed");
        }
        
        if (!errors.isEmpty()) {
            String errorMessage = "Migration verification failed:\n" + String.join("\n", errors);
            logger.error(errorMessage);
            throw new SQLException(errorMessage);
        }
        
        logger.info("Migration verification completed successfully");
    }
    
    /**
     * Verifies that a column exists in a table.
     * 
     * @param connection the database connection
     * @param tableName the table name
     * @param columnName the column name
     * @return true if the column exists, false otherwise
     * @throws SQLException if a database access error occurs
     */
    private static boolean verifyColumn(Connection connection, String tableName, String columnName) throws SQLException {
        String query = "PRAGMA table_info(" + tableName + ")";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                String name = rs.getString("name");
                if (columnName.equals(name)) {
                    logger.debug("Column '{}' found in table '{}'", columnName, tableName);
                    return true;
                }
            }
        }
        
        logger.warn("Column '{}' not found in table '{}'", columnName, tableName);
        return false;
    }
    
    /**
     * Verifies that an index exists.
     * 
     * @param connection the database connection
     * @param indexName the index name
     * @return true if the index exists, false otherwise
     * @throws SQLException if a database access error occurs
     */
    private static boolean verifyIndex(Connection connection, String indexName) throws SQLException {
        String query = "SELECT name FROM sqlite_master WHERE type='index' AND name=?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, indexName);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    logger.debug("Index '{}' found", indexName);
                    return true;
                }
            }
        }
        
        logger.warn("Index '{}' not found", indexName);
        return false;
    }
    
    /**
     * Verifies that constraints are properly enforced.
     * Tests the discount_type and payment_method enum constraints.
     * 
     * @param connection the database connection
     * @return true if constraints are working, false otherwise
     */
    private static boolean verifyConstraints(Connection connection) {
        logger.debug("Verifying constraints...");
        
        // Test discount_type constraint
        try {
            String testQuery = "INSERT INTO bills (totalprice, clientname, clientphone, date, discount_type) " +
                             "VALUES (100.0, 'Test', '1234567890', '2024-01-01', 'invalid')";
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(testQuery);
                // If we get here, constraint didn't work
                logger.error("discount_type constraint not enforced");
                return false;
            }
        } catch (SQLException e) {
            // Expected - constraint should reject invalid value
            if (e.getMessage().contains("constraint failed") || e.getMessage().contains("CHECK constraint")) {
                logger.debug("discount_type constraint working correctly");
            } else {
                logger.error("Unexpected error testing discount_type constraint", e);
                return false;
            }
        }
        
        // Test payment_method constraint
        try {
            String testQuery = "INSERT INTO bills (totalprice, clientname, clientphone, date, payment_method) " +
                             "VALUES (100.0, 'Test', '1234567890', '2024-01-01', 'invalid')";
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(testQuery);
                // If we get here, constraint didn't work
                logger.error("payment_method constraint not enforced");
                return false;
            }
        } catch (SQLException e) {
            // Expected - constraint should reject invalid value
            if (e.getMessage().contains("constraint failed") || e.getMessage().contains("CHECK constraint")) {
                logger.debug("payment_method constraint working correctly");
            } else {
                logger.error("Unexpected error testing payment_method constraint", e);
                return false;
            }
        }
        
        logger.debug("Constraints verified successfully");
        return true;
    }
    
    /**
     * Gets detailed information about the bills table schema.
     * Useful for debugging and verification.
     * 
     * @param connection the database connection
     * @return a list of column information strings
     * @throws SQLException if a database access error occurs
     */
    public static List<String> getBillsTableSchema(Connection connection) throws SQLException {
        List<String> schema = new ArrayList<>();
        String query = "PRAGMA table_info(bills)";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                String columnInfo = String.format("Column: %s, Type: %s, NotNull: %d, Default: %s",
                    rs.getString("name"),
                    rs.getString("type"),
                    rs.getInt("notnull"),
                    rs.getString("dflt_value"));
                schema.add(columnInfo);
            }
        }
        
        return schema;
    }
    
    /**
     * Gets a list of all indexes in the database.
     * Useful for debugging and verification.
     * 
     * @param connection the database connection
     * @return a list of index names
     * @throws SQLException if a database access error occurs
     */
    public static List<String> getAllIndexes(Connection connection) throws SQLException {
        List<String> indexes = new ArrayList<>();
        String query = "SELECT name FROM sqlite_master WHERE type='index' AND name NOT LIKE 'sqlite_%'";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                indexes.add(rs.getString("name"));
            }
        }
        
        return indexes;
    }
}
