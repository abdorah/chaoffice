package org.chaos.office.service;

import org.chaos.office.util.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;

/**
 * DatabaseService handles database initialization and management.
 * 
 * <p>This service is responsible for:
 * <ul>
 *   <li>Initializing the database schema</li>
 *   <li>Executing DDL scripts</li>
 *   <li>Seeding default data</li>
 * </ul>
 * 
 * <p>Requirements: 10.2, 10.3, 10.4
 */
public class DatabaseService {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);
    
    /**
     * Initializes the database by creating tables and seeding default data.
     */
    public void initializeDatabase() {
        try {
            // Execute DDL script
            executeDDL("/sql/ddl.sql");
            
            // Seed default admin user if not exists
            seedDefaultAdmin();
            
            logger.info("Database initialized successfully");
        } catch (Exception e) {
            logger.error("Error initializing database", e);
            throw new RuntimeException("Failed to initialize database", e);
        }
    }
    
    /**
     * Executes a DDL SQL script from resources.
     * 
     * @param scriptPath the path to the SQL script in resources
     */
    public void executeDDL(String scriptPath) {
        try (InputStream is = getClass().getResourceAsStream(scriptPath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is));
             Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            
            StringBuilder sql = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // Skip comments and empty lines
                if (line.isEmpty() || line.startsWith("--")) {
                    continue;
                }
                
                sql.append(line).append(" ");
                
                // Execute statement when semicolon is found
                if (line.endsWith(";")) {
                    String statement = sql.toString().trim();
                    if (!statement.isEmpty()) {
                        stmt.execute(statement);
                    }
                    sql.setLength(0);
                }
            }
            
            logger.info("Executed DDL script: {}", scriptPath);
            
        } catch (Exception e) {
            logger.error("Error executing DDL script: {}", scriptPath, e);
            throw new RuntimeException("Failed to execute DDL script", e);
        }
    }
    
    /**
     * Seeds the database with a default admin user if one doesn't exist.
     */
    private void seedDefaultAdmin() {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Check if admin user exists
            var rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE username = 'admin'");
            rs.next();
            int count = rs.getInt(1);
            
            if (count == 0) {
                // Create default admin user
                stmt.execute("INSERT INTO users (username, password, role, firstname, lastname) " +
                           "VALUES ('admin', 'admin', 'admin', 'Admin', 'User')");
                logger.info("Created default admin user");
            }
            
        } catch (Exception e) {
            logger.error("Error seeding default admin", e);
        }
    }
    
    /**
     * Resets the database to an empty state, keeping only essential information:
     * - User accounts
     * - Store branding settings (logo, store name)
     * - Currency settings
     * 
     * This will delete:
     * - All parts
     * - All categories
     * - All bills and commands
     */
    public void resetDatabase() {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            
            logger.info("Starting database reset...");
            
            // Delete all commands (must be first due to foreign key constraints)
            stmt.execute("DELETE FROM commands");
            logger.info("Deleted all commands");
            
            // Delete all bills
            stmt.execute("DELETE FROM bills");
            logger.info("Deleted all bills");
            
            // Delete all parts
            stmt.execute("DELETE FROM parts");
            logger.info("Deleted all parts");
            
            // Delete all categories
            stmt.execute("DELETE FROM categories");
            logger.info("Deleted all categories");
            
            // Note: users, settings, and branding tables are NOT deleted
            
            logger.info("Database reset completed successfully");
            
        } catch (Exception e) {
            logger.error("Error resetting database", e);
            throw new RuntimeException("Failed to reset database", e);
        }
    }
}
