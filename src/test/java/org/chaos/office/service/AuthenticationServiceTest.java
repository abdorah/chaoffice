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
    
    @Test
    void testChangeUsernameWithValidCredentials() throws SQLException {
        // Authenticate first
        authService.authenticate("testuser", "testpass");
        
        // Change username
        boolean result = authService.changeUsername("testpass", "newusername");
        
        assertTrue(result);
        assertEquals("newusername", SessionManager.getInstance().getCurrentUser().getUsername());
        
        // Clean up new username
        deleteTestUser("newusername");
    }
    
    @Test
    void testChangeUsernameWithIncorrectPassword() {
        // Authenticate first
        authService.authenticate("testuser", "testpass");
        
        // Try to change username with wrong password
        boolean result = authService.changeUsername("wrongpass", "newusername");
        
        assertFalse(result);
        assertEquals("testuser", SessionManager.getInstance().getCurrentUser().getUsername());
    }
    
    @Test
    void testChangeUsernameWithEmptyUsername() {
        // Authenticate first
        authService.authenticate("testuser", "testpass");
        
        // Try to change to empty username
        boolean result = authService.changeUsername("testpass", "");
        
        assertFalse(result);
        assertEquals("testuser", SessionManager.getInstance().getCurrentUser().getUsername());
    }
    
    @Test
    void testChangeUsernameWithWhitespaceUsername() {
        // Authenticate first
        authService.authenticate("testuser", "testpass");
        
        // Try to change to whitespace-only username
        boolean result = authService.changeUsername("testpass", "   ");
        
        assertFalse(result);
        assertEquals("testuser", SessionManager.getInstance().getCurrentUser().getUsername());
    }
    
    @Test
    void testChangeUsernameWithoutAuthentication() {
        // Try to change username without being authenticated
        boolean result = authService.changeUsername("testpass", "newusername");
        
        assertFalse(result);
    }
    
    @Test
    void testChangePasswordWithValidCredentials() {
        // Authenticate first
        authService.authenticate("testuser", "testpass");
        
        // Change password
        boolean result = authService.changePassword("testpass", "newpass123", "newpass123");
        
        assertTrue(result);
        
        // Verify can authenticate with new password
        SessionManager.getInstance().clearSession();
        Optional<User> authResult = authService.authenticate("testuser", "newpass123");
        assertTrue(authResult.isPresent());
    }
    
    @Test
    void testChangePasswordWithIncorrectCurrentPassword() {
        // Authenticate first
        authService.authenticate("testuser", "testpass");
        
        // Try to change password with wrong current password
        boolean result = authService.changePassword("wrongpass", "newpass123", "newpass123");
        
        assertFalse(result);
        
        // Verify old password still works
        SessionManager.getInstance().clearSession();
        Optional<User> authResult = authService.authenticate("testuser", "testpass");
        assertTrue(authResult.isPresent());
    }
    
    @Test
    void testChangePasswordWithMismatchedConfirmation() {
        // Authenticate first
        authService.authenticate("testuser", "testpass");
        
        // Try to change password with mismatched confirmation
        boolean result = authService.changePassword("testpass", "newpass123", "differentpass");
        
        assertFalse(result);
        
        // Verify old password still works
        SessionManager.getInstance().clearSession();
        Optional<User> authResult = authService.authenticate("testuser", "testpass");
        assertTrue(authResult.isPresent());
    }
    
    @Test
    void testChangePasswordWithShortPassword() {
        // Authenticate first
        authService.authenticate("testuser", "testpass");
        
        // Try to change to password shorter than 6 characters
        boolean result = authService.changePassword("testpass", "short", "short");
        
        assertFalse(result);
        
        // Verify old password still works
        SessionManager.getInstance().clearSession();
        Optional<User> authResult = authService.authenticate("testuser", "testpass");
        assertTrue(authResult.isPresent());
    }
    
    @Test
    void testChangePasswordWithExactly6Characters() {
        // Authenticate first
        authService.authenticate("testuser", "testpass");
        
        // Change to password with exactly 6 characters (minimum)
        boolean result = authService.changePassword("testpass", "pass12", "pass12");
        
        assertTrue(result);
        
        // Verify can authenticate with new password
        SessionManager.getInstance().clearSession();
        Optional<User> authResult = authService.authenticate("testuser", "pass12");
        assertTrue(authResult.isPresent());
    }
    
    @Test
    void testChangePasswordWithoutAuthentication() {
        // Try to change password without being authenticated
        boolean result = authService.changePassword("testpass", "newpass123", "newpass123");
        
        assertFalse(result);
    }
    
    @Test
    void testChangePasswordWithNullPassword() {
        // Authenticate first
        authService.authenticate("testuser", "testpass");
        
        // Try to change to null password
        boolean result = authService.changePassword("testpass", null, null);
        
        assertFalse(result);
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
