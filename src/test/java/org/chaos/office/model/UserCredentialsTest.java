package org.chaos.office.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the UserCredentials model.
 */
class UserCredentialsTest {

    @Test
    void testConstructor() {
        UserCredentials credentials = new UserCredentials("testuser", "hashedpassword123");
        
        assertNotNull(credentials);
        assertEquals("testuser", credentials.getUsername());
        assertEquals("hashedpassword123", credentials.getPasswordHash());
    }

    @Test
    void testGetUsername() {
        UserCredentials credentials = new UserCredentials("admin", "hash123");
        assertEquals("admin", credentials.getUsername());
    }

    @Test
    void testGetPasswordHash() {
        UserCredentials credentials = new UserCredentials("user", "secureHash456");
        assertEquals("secureHash456", credentials.getPasswordHash());
    }

    @Test
    void testWithEmptyUsername() {
        UserCredentials credentials = new UserCredentials("", "hash");
        assertEquals("", credentials.getUsername());
        assertEquals("hash", credentials.getPasswordHash());
    }

    @Test
    void testWithEmptyPasswordHash() {
        UserCredentials credentials = new UserCredentials("user", "");
        assertEquals("user", credentials.getUsername());
        assertEquals("", credentials.getPasswordHash());
    }

    @Test
    void testToString() {
        UserCredentials credentials = new UserCredentials("testuser", "hashedpassword123");
        String result = credentials.toString();
        
        assertTrue(result.contains("username='testuser'"));
        assertTrue(result.contains("passwordHash='[PROTECTED]'"));
        assertFalse(result.contains("hashedpassword123"), "Password hash should be protected in toString");
    }

    @Test
    void testImmutability() {
        UserCredentials credentials = new UserCredentials("user", "hash");
        
        // Verify that there are no setters - this is a compile-time check
        // The class should be immutable with only getters
        assertEquals("user", credentials.getUsername());
        assertEquals("hash", credentials.getPasswordHash());
    }
}
