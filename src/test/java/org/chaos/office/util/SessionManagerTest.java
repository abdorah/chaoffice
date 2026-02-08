package org.chaos.office.util;

import static org.junit.jupiter.api.Assertions.*;

import org.chaos.office.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the SessionManager utility class.
 * Tests the singleton pattern, session management, and authentication state.
 */
class SessionManagerTest {

    @BeforeEach
    void setUp() {
        // Clear session before each test to ensure clean state
        SessionManager.getInstance().clearSession();
    }

    @Test
    void testGetInstance_ReturnsSameInstance() {
        SessionManager instance1 = SessionManager.getInstance();
        SessionManager instance2 = SessionManager.getInstance();
        
        assertNotNull(instance1);
        assertNotNull(instance2);
        assertSame(instance1, instance2, "getInstance should return the same singleton instance");
    }

    @Test
    void testInitialState_NotAuthenticated() {
        SessionManager sessionManager = SessionManager.getInstance();
        
        assertFalse(sessionManager.isAuthenticated(), "Initial state should not be authenticated");
        assertNull(sessionManager.getCurrentUser(), "Initial state should have no current user");
    }

    @Test
    void testSetCurrentUser_SetsUser() {
        SessionManager sessionManager = SessionManager.getInstance();
        User user = new User(1, "testuser", "password", "user", "Test", "User");
        
        sessionManager.setCurrentUser(user);
        
        assertNotNull(sessionManager.getCurrentUser());
        assertEquals(user, sessionManager.getCurrentUser());
        assertEquals("testuser", sessionManager.getCurrentUser().getUsername());
    }

    @Test
    void testSetCurrentUser_MakesAuthenticated() {
        SessionManager sessionManager = SessionManager.getInstance();
        User user = new User(1, "testuser", "password", "user", "Test", "User");
        
        sessionManager.setCurrentUser(user);
        
        assertTrue(sessionManager.isAuthenticated(), "Should be authenticated after setting user");
    }

    @Test
    void testClearSession_RemovesUser() {
        SessionManager sessionManager = SessionManager.getInstance();
        User user = new User(1, "testuser", "password", "user", "Test", "User");
        
        sessionManager.setCurrentUser(user);
        assertTrue(sessionManager.isAuthenticated());
        
        sessionManager.clearSession();
        
        assertNull(sessionManager.getCurrentUser(), "User should be null after clearing session");
        assertFalse(sessionManager.isAuthenticated(), "Should not be authenticated after clearing session");
    }

    @Test
    void testClearSession_WhenNoUser() {
        SessionManager sessionManager = SessionManager.getInstance();
        
        // Should not throw exception when clearing empty session
        assertDoesNotThrow(() -> sessionManager.clearSession());
        assertFalse(sessionManager.isAuthenticated());
        assertNull(sessionManager.getCurrentUser());
    }

    @Test
    void testSetCurrentUser_WithNull() {
        SessionManager sessionManager = SessionManager.getInstance();
        User user = new User(1, "testuser", "password", "user", "Test", "User");
        
        sessionManager.setCurrentUser(user);
        assertTrue(sessionManager.isAuthenticated());
        
        // Setting null should clear the session
        sessionManager.setCurrentUser(null);
        
        assertNull(sessionManager.getCurrentUser());
        assertFalse(sessionManager.isAuthenticated());
    }

    @Test
    void testSetCurrentUser_ReplacesExistingUser() {
        SessionManager sessionManager = SessionManager.getInstance();
        User user1 = new User(1, "user1", "password1", "user", "First", "User");
        User user2 = new User(2, "user2", "password2", "admin", "Second", "User");
        
        sessionManager.setCurrentUser(user1);
        assertEquals("user1", sessionManager.getCurrentUser().getUsername());
        
        sessionManager.setCurrentUser(user2);
        assertEquals("user2", sessionManager.getCurrentUser().getUsername());
        assertEquals("admin", sessionManager.getCurrentUser().getRole());
    }

    @Test
    void testSessionPersistsAcrossMultipleCalls() {
        SessionManager sessionManager = SessionManager.getInstance();
        User user = new User(1, "testuser", "password", "user", "Test", "User");
        
        sessionManager.setCurrentUser(user);
        
        // Multiple calls should return the same user
        User retrieved1 = sessionManager.getCurrentUser();
        User retrieved2 = sessionManager.getCurrentUser();
        
        assertSame(retrieved1, retrieved2);
        assertEquals("testuser", retrieved1.getUsername());
        assertEquals("testuser", retrieved2.getUsername());
    }

    @Test
    void testIsAuthenticated_WithDifferentUserRoles() {
        SessionManager sessionManager = SessionManager.getInstance();
        
        User admin = new User(1, "admin", "pass", "admin", "Admin", "User");
        sessionManager.setCurrentUser(admin);
        assertTrue(sessionManager.isAuthenticated());
        
        sessionManager.clearSession();
        
        User regularUser = new User(2, "user", "pass", "user", "Regular", "User");
        sessionManager.setCurrentUser(regularUser);
        assertTrue(sessionManager.isAuthenticated());
        
        sessionManager.clearSession();
        
        User guest = new User(3, "guest", "pass", "guest", "Guest", "User");
        sessionManager.setCurrentUser(guest);
        assertTrue(sessionManager.isAuthenticated());
    }

    @Test
    void testGetCurrentUser_ReturnsCorrectUserDetails() {
        SessionManager sessionManager = SessionManager.getInstance();
        User user = new User(42, "johndoe", "secret", "admin", "John", "Doe");
        
        sessionManager.setCurrentUser(user);
        User retrieved = sessionManager.getCurrentUser();
        
        assertEquals(42, retrieved.getId());
        assertEquals("johndoe", retrieved.getUsername());
        assertEquals("secret", retrieved.getPassword());
        assertEquals("admin", retrieved.getRole());
        assertEquals("John", retrieved.getFirstName());
        assertEquals("Doe", retrieved.getLastName());
    }

    @Test
    void testMultipleClearSession_Calls() {
        SessionManager sessionManager = SessionManager.getInstance();
        User user = new User(1, "testuser", "password", "user", "Test", "User");
        
        sessionManager.setCurrentUser(user);
        sessionManager.clearSession();
        sessionManager.clearSession(); // Should not throw exception
        
        assertFalse(sessionManager.isAuthenticated());
        assertNull(sessionManager.getCurrentUser());
    }

    @Test
    void testSessionManager_ThreadSafety_BasicCheck() {
        // Basic check that getInstance returns same instance across calls
        // Note: Full thread-safety testing would require more complex setup
        SessionManager instance1 = SessionManager.getInstance();
        SessionManager instance2 = SessionManager.getInstance();
        SessionManager instance3 = SessionManager.getInstance();
        
        assertSame(instance1, instance2);
        assertSame(instance2, instance3);
    }
}
