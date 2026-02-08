package org.chaos.office.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * DatabaseConnection utility class for managing SQLite database connections.
 * Implements singleton pattern to ensure a single connection instance throughout the application.
 * 
 * <p>This class handles:
 * <ul>
 *   <li>Database connection lifecycle (creation, retrieval, closing)</li>
 *   <li>Database initialization from DDL scripts</li>
 *   <li>Database file location management (user directory)</li>
 * </ul>
 * 
 * <p>Requirements: 10.3, 10.6
 */
public class DatabaseConnection {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);
    
    // Singleton instance
    private static DatabaseConnection instance;
    
    // Database connection
    private Connection connection;
    
    // Database configuration
    private static final String DB_NAME = "chaoffice.db";
    private static final String DDL_SCRIPT_PATH = "/sql/ddl.sql";
    
    /**
     * Private constructor to enforce singleton pattern.
     */
    private DatabaseConnection() {
        // Private constructor prevents direct instantiation
    }
    
    /**
     * Gets the singleton instance of DatabaseConnection.
     * 
     * @return the singleton instance
     */
    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }
    
    /**
     * Gets the database connection, creating it if necessary.
     * The database file is created in the user's home directory under .chaoffice/
     * 
     * @return the database connection
     * @throws SQLException if a database access error occurs
     */
    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                // Determine database file path in user directory
                Path dbPath = getDatabasePath();
                
                // Ensure parent directory exists
                Files.createDirectories(dbPath.getParent());
                
                // Create connection URL
                String url = "jdbc:sqlite:" + dbPath.toAbsolutePath();
                
                logger.info("Connecting to database at: {}", dbPath.toAbsolutePath());
                
                // Load SQLite JDBC driver (explicit loading for clarity)
                Class.forName("org.sqlite.JDBC");
                
                // Create connection
                connection = DriverManager.getConnection(url);
                
                // Enable foreign key constraints (disabled by default in SQLite)
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("PRAGMA foreign_keys = ON;");
                }
                
                logger.info("Database connection established successfully");
                
            } catch (ClassNotFoundException e) {
                logger.error("SQLite JDBC driver not found", e);
                throw new SQLException("SQLite JDBC driver not found", e);
            } catch (IOException e) {
                logger.error("Failed to create database directory", e);
                throw new SQLException("Failed to create database directory", e);
            }
        }
        
        return connection;
    }
    
    /**
     * Closes the database connection if it is open.
     * This method should be called when the application shuts down.
     */
    public synchronized void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    logger.info("Database connection closed successfully");
                }
            } catch (SQLException e) {
                logger.error("Error closing database connection", e);
            } finally {
                connection = null;
            }
        }
    }
    
    /**
     * Initializes the database by executing the DDL script.
     * This method creates all necessary tables and inserts default data.
     * It should be called once when the application starts.
     * 
     * @throws SQLException if a database access error occurs
     * @throws IOException if the DDL script cannot be read
     */
    public void initializeDatabase() throws SQLException, IOException {
        logger.info("Initializing database...");
        
        // Get connection (creates database file if it doesn't exist)
        Connection conn = getConnection();
        
        // Read DDL script from resources
        String ddlScript = readDDLScript();
        
        // Execute DDL script
        executeDDL(conn, ddlScript);
        
        logger.info("Database initialization completed successfully");
    }
    
    /**
     * Reads the DDL script from the resources directory.
     * 
     * @return the DDL script content
     * @throws IOException if the script cannot be read
     */
    private String readDDLScript() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(DDL_SCRIPT_PATH)) {
            if (inputStream == null) {
                throw new IOException("DDL script not found at: " + DDL_SCRIPT_PATH);
            }
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
    }
    
    /**
     * Executes a DDL script containing multiple SQL statements.
     * Statements are separated by semicolons.
     * 
     * @param conn the database connection
     * @param ddlScript the DDL script to execute
     * @throws SQLException if a database access error occurs
     */
    private void executeDDL(Connection conn, String ddlScript) throws SQLException {
        // Remove comments and split by semicolon
        StringBuilder currentStatement = new StringBuilder();
        
        try (Statement stmt = conn.createStatement()) {
            // Process script line by line
            String[] lines = ddlScript.split("\n");
            
            for (String line : lines) {
                // Trim the line
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
                    // Remove trailing semicolon
                    sql = sql.substring(0, sql.length() - 1).trim();
                    
                    if (!sql.isEmpty()) {
                        logger.debug("Executing SQL: {}", sql.substring(0, Math.min(50, sql.length())) + "...");
                        try {
                            stmt.execute(sql);
                        } catch (SQLException e) {
                            // Log but don't fail on INSERT OR IGNORE errors (duplicate key)
                            if (!e.getMessage().contains("UNIQUE constraint failed")) {
                                throw e;
                            }
                        }
                    }
                    
                    // Reset for next statement
                    currentStatement = new StringBuilder();
                }
            }
        }
    }
    
    /**
     * Gets the path to the database file in the user's home directory.
     * The database is stored in ~/.chaoffice/chaoffice.db
     * 
     * @return the path to the database file
     */
    private Path getDatabasePath() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, ".chaoffice", DB_NAME);
    }
    
    /**
     * Gets the absolute path to the database file as a string.
     * Useful for displaying the database location in the UI.
     * 
     * @return the absolute path to the database file
     */
    public String getDatabaseLocation() {
        return getDatabasePath().toAbsolutePath().toString();
    }
}
