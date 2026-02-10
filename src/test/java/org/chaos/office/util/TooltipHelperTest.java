package org.chaos.office.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TooltipHelper utility.
 * 
 * Note: Full JavaFX tests require a GUI environment.
 * These tests verify the basic logic and null safety.
 */
class TooltipHelperTest {
    
    @Test
    void testShowErrorWithNullControl() {
        // Should not throw exception
        assertDoesNotThrow(() -> TooltipHelper.showError(null, "Test error"));
    }
    
    @Test
    void testShowErrorWithNullMessage() {
        // Should not throw exception
        assertDoesNotThrow(() -> TooltipHelper.showError(null, null));
    }
    
    @Test
    void testShowErrorWithEmptyMessage() {
        // Should not throw exception
        assertDoesNotThrow(() -> TooltipHelper.showError(null, ""));
    }
    
    @Test
    void testClearErrorWithNullControl() {
        // Should not throw exception
        assertDoesNotThrow(() -> TooltipHelper.clearError(null));
    }
    
    @Test
    void testHasErrorWithNullControl() {
        // Should return false for null control
        assertFalse(TooltipHelper.hasError(null));
    }
}
