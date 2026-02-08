package org.chaos.office.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the Maker model.
 */
class MakerTest {

    @Test
    void testDefaultConstructor() {
        Maker maker = new Maker();
        assertNotNull(maker);
        assertEquals(0, maker.getId());
        assertNull(maker.getName());
    }

    @Test
    void testParameterizedConstructor() {
        Maker maker = new Maker(1, "Bosch");
        
        assertEquals(1, maker.getId());
        assertEquals("Bosch", maker.getName());
    }

    @Test
    void testSettersAndGetters() {
        Maker maker = new Maker();
        
        maker.setId(42);
        maker.setName("NGK");
        
        assertEquals(42, maker.getId());
        assertEquals("NGK", maker.getName());
    }

    @Test
    void testPropertyAccessors() {
        Maker maker = new Maker(1, "Bosch");
        
        assertNotNull(maker.idProperty());
        assertNotNull(maker.nameProperty());
        
        assertEquals(1, maker.idProperty().get());
        assertEquals("Bosch", maker.nameProperty().get());
    }

    @Test
    void testPropertyBinding() {
        Maker maker = new Maker();
        
        // Test that properties can be bound
        maker.idProperty().set(100);
        assertEquals(100, maker.getId());
        
        maker.nameProperty().set("Brembo");
        assertEquals("Brembo", maker.getName());
    }

    @Test
    void testToString() {
        Maker maker = new Maker(1, "Bosch");
        String result = maker.toString();
        
        assertTrue(result.contains("id=1"));
        assertTrue(result.contains("name='Bosch'"));
    }

    @Test
    void testEmptyString() {
        Maker maker = new Maker(1, "");
        
        assertEquals("", maker.getName());
    }

    @Test
    void testNameUpdate() {
        Maker maker = new Maker(1, "Bosch");
        assertEquals("Bosch", maker.getName());
        
        maker.setName("NGK");
        assertEquals("NGK", maker.getName());
    }

    @Test
    void testMultipleMakers() {
        Maker maker1 = new Maker(1, "Bosch");
        Maker maker2 = new Maker(2, "NGK");
        Maker maker3 = new Maker(3, "Brembo");
        
        assertEquals("Bosch", maker1.getName());
        assertEquals("NGK", maker2.getName());
        assertEquals("Brembo", maker3.getName());
        
        assertNotEquals(maker1.getId(), maker2.getId());
        assertNotEquals(maker2.getId(), maker3.getId());
    }

    @Test
    void testLongMakerName() {
        String longName = "Very Long Manufacturer Name Corporation International Limited";
        Maker maker = new Maker(1, longName);
        
        assertEquals(longName, maker.getName());
    }

    @Test
    void testSpecialCharactersInName() {
        Maker maker1 = new Maker(1, "O'Reilly");
        Maker maker2 = new Maker(2, "K&N");
        Maker maker3 = new Maker(3, "3M");
        
        assertEquals("O'Reilly", maker1.getName());
        assertEquals("K&N", maker2.getName());
        assertEquals("3M", maker3.getName());
    }
}
