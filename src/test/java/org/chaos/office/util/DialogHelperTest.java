package org.chaos.office.util;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for DialogHelper utility.
 * Note: These are basic unit tests. Full UI testing requires JavaFX runtime.
 */
class DialogHelperTest {
    
    @Test
    void testApplyThemeToNullDialog() {
        // Should not throw exception with null dialog
        assertDoesNotThrow(() -> DialogHelper.applyTheme(null));
    }
    
    @Test
    void testDialogHelperClassExists() {
        // Verify the class can be loaded
        assertNotNull(DialogHelper.class);
    }
    
    @Test
    void testDialogHelperHasApplyThemeMethod() throws NoSuchMethodException {
        // Verify the applyTheme method exists
        assertNotNull(DialogHelper.class.getMethod("applyTheme", Dialog.class));
    }
}
