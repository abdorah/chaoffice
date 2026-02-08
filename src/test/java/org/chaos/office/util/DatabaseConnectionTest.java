package org.chaos.office.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DatabaseConnection utility class.
 * Tests connection management, database initialization, and singleton pattern.
 */
class DatabaseConnectionTest {
    
    private DatabaseConnection dbConnection;
    
    @BeforeEach
    void setUp() {
        dbConnection = DatabaseConnection.getInstance();
    }
    
    @AfterEach
    void tearDown() {
        // Close connection after each test
        dbConnection.closeConnection();
    }
    
    @Test
    void testSingletonPattern() {
        // Verify that getInstance returns the same instance
        DatabaseConnection instance1 = DatabaseConnection.getInstance();
        DatabaseConnection instance2 = DatabaseConnection.getInstance();
        
        assertSame(instance1, instance2, "getInstance should return the same instance");
    }
    
    @Test
    void testGetConnection() throws SQLException {
        // Test that getConnection returns a valid connection
        Connection conn = dbConnection.getConnection();
        
        assertNotNull(conn, "Connection should not be null");
        assertFalse(conn.isClosed(), "Connection should be open");
    }
    
    @Test
    void testGetConnectionMultipleCalls() throws SQLException {
        // Test that multiple calls to getConnection return the same connection
        Connection conn1 = dbConnection.getConnection();
        Connection conn2 = dbConnection.getConnection();
        
        assertSame(conn1, conn2, "Multiple calls should return the same connection");
    }
    
    @Test
    void testCloseConnection() throws SQLException {
        // Get a connection
        Connection conn = dbConnection.getConnection();
        assertFalse(conn.isClosed(), "Connection should be open initially");
        
        // Close the connection
        dbConnection.closeConnection();
        
        assertTrue(conn.isClosed(), "Connection should be closed after closeConnection()");
    }
    
    @Test
    void testInitializeDatabase() throws SQLException, IOException {
        // Initialize the database
        dbConnection.initializeDatabase();
        
        // Verify that tables were created by querying one of them
        Connection conn = dbConnection.getConnection();
        try (Statement stmt = conn.createStatement()) {
            // Query the users table to verify it exists
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
            assertTrue(rs.next(), "Should be able to query users table");
            
            // Verify default admin user was inserted
            int userCount = rs.getInt(1);
            assertTrue(userCount >= 1, "Should have at least one user (admin)");
        }
    }
    
    @Test
    void testDatabaseLocation() {
        // Test that getDatabaseLocation returns a valid path
        String location = dbConnection.getDatabaseLocation();
        
        assertNotNull(location, "Database location should not be null");
        assertTrue(location.contains(".chaoffice"), "Database should be in .chaoffice directory");
        assertTrue(location.endsWith("chaoffice.db"), "Database file should be named chaoffice.db");
    }
    
    @Test
    void testForeignKeyConstraintsEnabled() throws SQLException {
        // Verify that foreign key constraints are enabled
        Connection conn = dbConnection.getConnection();
        
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("PRAGMA foreign_keys");
            assertTrue(rs.next(), "Should get foreign_keys pragma result");
            int foreignKeysEnabled = rs.getInt(1);
            assertEquals(1, foreignKeysEnabled, "Foreign keys should be enabled");
        }
    }
    
    @Test
    void testDatabaseFileCreation() throws SQLException {
        // Get connection (should create database file)
        dbConnection.getConnection();
        
        // Verify database file exists
        String userHome = System.getProperty("user.home");
        Path dbPath = Paths.get(userHome, ".chaoffice", "chaoffice.db");
        
        assertTrue(Files.exists(dbPath), "Database file should exist after getConnection()");
    }
    
    @Test
    void testReconnectAfterClose() throws SQLException {
        // Get initial connection
        Connection conn1 = dbConnection.getConnection();
        assertFalse(conn1.isClosed(), "Initial connection should be open");
        
        // Close connection
        dbConnection.closeConnection();
        assertTrue(conn1.isClosed(), "Connection should be closed");
        
        // Get new connection
        Connection conn2 = dbConnection.getConnection();
        assertFalse(conn2.isClosed(), "New connection should be open");
        assertNotSame(conn1, conn2, "Should get a new connection instance after close");
    }
    
    @Test
    void testInitializeDatabaseIdempotent() throws SQLException, IOException {
        // Initialize database twice
        dbConnection.initializeDatabase();
        dbConnection.initializeDatabase();
        
        // Verify tables still exist and data is not duplicated
        Connection conn = dbConnection.getConnection();
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE username = 'admin'");
            assertTrue(rs.next(), "Should be able to query users table");
            
            // Should have exactly one admin user (INSERT OR IGNORE prevents duplicates)
            int adminCount = rs.getInt(1);
            assertEquals(1, adminCount, "Should have exactly one admin user");
        }
    }
}
