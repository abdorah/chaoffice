package org.chaos.office.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the ValidationHelper utility class.
 */
class ValidationHelperTest {

    @Test
    void testValidationHelperCannotBeInstantiated() {
        try {
            var constructor = ValidationHelper.class.getDeclaredConstructor();
            assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()),
                    "ValidationHelper constructor should be private");
        } catch (NoSuchMethodException e) {
            fail("ValidationHelper should have a private no-arg constructor");
        }
    }

    // Tests for isNotEmpty
    @Test
    void testIsNotEmptyWithValidString() {
        assertTrue(ValidationHelper.isNotEmpty("hello"));
        assertTrue(ValidationHelper.isNotEmpty("  hello  "));
        assertTrue(ValidationHelper.isNotEmpty("123"));
    }

    @Test
    void testIsNotEmptyWithEmptyString() {
        assertFalse(ValidationHelper.isNotEmpty(""));
        assertFalse(ValidationHelper.isNotEmpty("   "));
        assertFalse(ValidationHelper.isNotEmpty("\t\n"));
    }

    @Test
    void testIsNotEmptyWithNull() {
        assertFalse(ValidationHelper.isNotEmpty(null));
    }

    // Tests for isPositiveNumber
    @Test
    void testIsPositiveNumberWithValidPositiveNumbers() {
        assertTrue(ValidationHelper.isPositiveNumber("1"));
        assertTrue(ValidationHelper.isPositiveNumber("123"));
        assertTrue(ValidationHelper.isPositiveNumber("0.5"));
        assertTrue(ValidationHelper.isPositiveNumber("99.99"));
        assertTrue(ValidationHelper.isPositiveNumber("  42  "));
    }

    @Test
    void testIsPositiveNumberWithZero() {
        assertFalse(ValidationHelper.isPositiveNumber("0"));
        assertFalse(ValidationHelper.isPositiveNumber("0.0"));
    }

    @Test
    void testIsPositiveNumberWithNegativeNumbers() {
        assertFalse(ValidationHelper.isPositiveNumber("-1"));
        assertFalse(ValidationHelper.isPositiveNumber("-0.5"));
        assertFalse(ValidationHelper.isPositiveNumber("-99.99"));
    }

    @Test
    void testIsPositiveNumberWithInvalidInput() {
        assertFalse(ValidationHelper.isPositiveNumber("abc"));
        assertFalse(ValidationHelper.isPositiveNumber("12.34.56"));
        assertFalse(ValidationHelper.isPositiveNumber(""));
        assertFalse(ValidationHelper.isPositiveNumber(null));
    }

    // Tests for isValidPhone
    @Test
    void testIsValidPhoneWithValidFormats() {
        assertTrue(ValidationHelper.isValidPhone("1234567890"));
        assertTrue(ValidationHelper.isValidPhone("123 456 7890"));
        assertTrue(ValidationHelper.isValidPhone("123-456-7890"));
        assertTrue(ValidationHelper.isValidPhone("(123) 456-7890"));
        assertTrue(ValidationHelper.isValidPhone("+1 123 456 7890"));
        assertTrue(ValidationHelper.isValidPhone("+33123456789"));
        assertTrue(ValidationHelper.isValidPhone("1234567")); // Minimum 7 digits
    }

    @Test
    void testIsValidPhoneWithInvalidFormats() {
        assertFalse(ValidationHelper.isValidPhone("123456")); // Too short
        assertFalse(ValidationHelper.isValidPhone("12345678901234567")); // Too long (>15 digits)
        assertFalse(ValidationHelper.isValidPhone("abc"));
        assertFalse(ValidationHelper.isValidPhone(""));
        assertFalse(ValidationHelper.isValidPhone(null));
        assertFalse(ValidationHelper.isValidPhone("   "));
    }

    @Test
    void testIsValidPhoneWithEdgeCases() {
        assertTrue(ValidationHelper.isValidPhone("123456789012345")); // Exactly 15 digits
        assertFalse(ValidationHelper.isValidPhone("1234567890123456")); // 16 digits - too long
    }
}
