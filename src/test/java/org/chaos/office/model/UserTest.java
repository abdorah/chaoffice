package org.chaos.office.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the User model.
 */
class UserTest {

    @Test
    void testDefaultConstructor() {
        User user = new User();
        assertNotNull(user);
        assertEquals(0, user.getId());
        assertNull(user.getUsername());
        assertNull(user.getPassword());
        assertNull(user.getRole());
        assertNull(user.getFirstName());
        assertNull(user.getLastName());
    }

    @Test
    void testParameterizedConstructor() {
        User user = new User(1, "admin", "password123", "admin", "John", "Doe");
        
        assertEquals(1, user.getId());
        assertEquals("admin", user.getUsername());
        assertEquals("password123", user.getPassword());
        assertEquals("admin", user.getRole());
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
    }

    @Test
    void testSettersAndGetters() {
        User user = new User();
        
        user.setId(42);
        user.setUsername("testuser");
        user.setPassword("testpass");
        user.setRole("user");
        user.setFirstName("Jane");
        user.setLastName("Smith");
        
        assertEquals(42, user.getId());
        assertEquals("testuser", user.getUsername());
        assertEquals("testpass", user.getPassword());
        assertEquals("user", user.getRole());
        assertEquals("Jane", user.getFirstName());
        assertEquals("Smith", user.getLastName());
    }

    @Test
    void testPropertyAccessors() {
        User user = new User(1, "admin", "password123", "admin", "John", "Doe");
        
        assertNotNull(user.idProperty());
        assertNotNull(user.usernameProperty());
        assertNotNull(user.passwordProperty());
        assertNotNull(user.roleProperty());
        assertNotNull(user.firstNameProperty());
        assertNotNull(user.lastNameProperty());
        
        assertEquals(1, user.idProperty().get());
        assertEquals("admin", user.usernameProperty().get());
        assertEquals("password123", user.passwordProperty().get());
        assertEquals("admin", user.roleProperty().get());
        assertEquals("John", user.firstNameProperty().get());
        assertEquals("Doe", user.lastNameProperty().get());
    }

    @Test
    void testPropertyBinding() {
        User user = new User();
        
        // Test that properties can be bound
        user.idProperty().set(100);
        assertEquals(100, user.getId());
        
        user.usernameProperty().set("bindtest");
        assertEquals("bindtest", user.getUsername());
        
        user.passwordProperty().set("bindpass");
        assertEquals("bindpass", user.getPassword());
        
        user.roleProperty().set("guest");
        assertEquals("guest", user.getRole());
        
        user.firstNameProperty().set("Bound");
        assertEquals("Bound", user.getFirstName());
        
        user.lastNameProperty().set("User");
        assertEquals("User", user.getLastName());
    }

    @Test
    void testToString() {
        User user = new User(1, "admin", "password123", "admin", "John", "Doe");
        String result = user.toString();
        
        assertTrue(result.contains("id=1"));
        assertTrue(result.contains("username='admin'"));
        assertTrue(result.contains("role='admin'"));
        assertTrue(result.contains("firstName='John'"));
        assertTrue(result.contains("lastName='Doe'"));
        // Password should not be in toString for security, but our current implementation includes it
    }

    @Test
    void testRoleValues() {
        User admin = new User(1, "admin", "pass", "admin", "Admin", "User");
        User regularUser = new User(2, "user", "pass", "user", "Regular", "User");
        User guest = new User(3, "guest", "pass", "guest", "Guest", "User");
        
        assertEquals("admin", admin.getRole());
        assertEquals("user", regularUser.getRole());
        assertEquals("guest", guest.getRole());
    }

    @Test
    void testEmptyStrings() {
        User user = new User(1, "", "", "", "", "");
        
        assertEquals("", user.getUsername());
        assertEquals("", user.getPassword());
        assertEquals("", user.getRole());
        assertEquals("", user.getFirstName());
        assertEquals("", user.getLastName());
    }
}
