package org.chaos.office.service;

import org.chaos.office.model.User;
import org.chaos.office.util.DatabaseConnection;
import org.chaos.office.util.SessionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AuthenticationService.
 */
class AuthenticationServiceTest {
    
    private AuthenticationService authService;
    
    @BeforeEach
    void setUp() throws Exception {
        authService = new AuthenticationService();
        
        // Initialize database and create test user
        DatabaseConnection.getInstance().initializeDatabase();
        createTestUser("testuser", "testpass", "admin", "Test", "User");
    }
    
    @AfterEach
    void tearDown() {
        // Clear session after each test
        SessionManager.getInstance().clearSession();
        
        // Clean up test data
        try {
            deleteTestUser("testuser");
        } catch (SQLException e) {
            // Ignore cleanup errors
        }
    }
    
    @Test
    void testAuthenticateWithValidCredentials() {
        Optional<User> result = authService.authenticate("testuser", "testpass");
        
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
        assertEquals("admin", result.get().getRole());
        assertEquals("Test", result.get().getFirstName());
        assertEquals("User", result.get().getLastName());
        
        // Verify session was created
        assertTrue(SessionManager.getInstance().isAuthenticated());
        assertEquals("testuser", SessionManager.getInstance().getCurrentUser().getUsername());
    }
    
    @Test
    void testAuthenticateWithInvalidPassword() {
        Optional<User> result = authService.authenticate("testuser", "wrongpass");
        
        assertFalse(result.isPresent());
        assertFalse(SessionManager.getInstance().isAuthenticated());
    }
    
    @Test
    void testAuthenticateWithInvalidUsername() {
        Optional<User> result = authService.authenticate("nonexistent", "testpass");
        
        assertFalse(result.isPresent());
        assertFalse(SessionManager.getInstance().isAuthenticated());
    }
    
    @Test
    void testAuthenticateWithEmptyUsername() {
        Optional<User> result = authService.authenticate("", "testpass");
        
        assertFalse(result.isPresent());
        assertFalse(SessionManager.getInstance().isAuthenticated());
    }
    
    @Test
    void testAuthenticateWithEmptyPassword() {
        Optional<User> result = authService.authenticate("testuser", "");
        
        assertFalse(result.isPresent());
        assertFalse(SessionManager.getInstance().isAuthenticated());
    }
    
    @Test
    void testAuthenticateWithNullCredentials() {
        Optional<User> result = authService.authenticate(null, null);
        
        assertFalse(result.isPresent());
        assertFalse(SessionManager.getInstance().isAuthenticated());
    }
    
    @Test
    void testValidateCredentialsWithValidData() {
        boolean result = authService.validateCredentials("testuser", "testpass");
        assertTrue(result);
    }
    
    @Test
    void testValidateCredentialsWithInvalidData() {
        boolean result = authService.validateCredentials("testuser", "wrongpass");
        assertFalse(result);
    }
    
    @Test
    void testLogout() {
        // First authenticate
        authService.authenticate("testuser", "testpass");
        assertTrue(SessionManager.getInstance().isAuthenticated());
        
        // Then logout
        authService.logout();
        assertFalse(SessionManager.getInstance().isAuthenticated());
        assertNull(SessionManager.getInstance().getCurrentUser());
    }
    
    @Test
    void testLogoutWithoutAuthentication() {
        // Should not throw exception
        assertDoesNotThrow(() -> authService.logout());
        assertFalse(SessionManager.getInstance().isAuthenticated());
    }
    
    // Helper methods
    
    private void createTestUser(String username, String password, String role, 
                                 String firstName, String lastName) throws SQLException {
        String sql = "INSERT INTO users (username, password, role, firstname, lastname) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, role);
            stmt.setString(4, firstName);
            stmt.setString(5, lastName);
            stmt.executeUpdate();
        }
    }
    
    private void deleteTestUser(String username) throws SQLException {
        String sql = "DELETE FROM users WHERE username = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            stmt.executeUpdate();
        }
    }
}
