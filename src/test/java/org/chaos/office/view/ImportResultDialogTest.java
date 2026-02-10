package org.chaos.office.view;

import org.chaos.office.model.ImportError;
import org.chaos.office.model.ImportResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ImportResultDialog.
 * 
 * <p>These tests verify:
 * <ul>
 *   <li>Dialog class structure and methods</li>
 *   <li>Constructor accepts ImportResult parameter</li>
 *   <li>Dialog extends JavaFX Dialog class</li>
 *   <li>Required helper methods exist</li>
 * </ul>
 * 
 * <p>Note: These tests focus on component structure and configuration.
 * Full integration tests with JavaFX runtime are performed separately.
 * 
 * <p>Requirements: 6.3, 6.4, 6.5
 */
class ImportResultDialogTest {
    
    @Test
    void testDialogClassExists() {
        // Verify the dialog class exists
        assertNotNull(ImportResultDialog.class);
    }
    
    @Test
    void testDialogExtendsDialog() {
        // Verify the dialog extends JavaFX Dialog class
        assertTrue(javafx.scene.control.Dialog.class.isAssignableFrom(ImportResultDialog.class));
    }
    
    @Test
    void testDialogHasConstructorWithImportResult() throws Exception {
        // Verify the dialog has a constructor that accepts ImportResult
        var constructor = ImportResultDialog.class.getConstructor(ImportResult.class);
        assertNotNull(constructor);
    }
    
    @Test
    void testDialogHasInitializeDialogMethod() throws Exception {
        // Verify the initializeDialog method exists
        var method = ImportResultDialog.class.getDeclaredMethod("initializeDialog");
        assertNotNull(method);
    }
    
    @Test
    void testDialogHasCreateContentMethod() throws Exception {
        // Verify the createContent method exists
        var method = ImportResultDialog.class.getDeclaredMethod("createContent");
        assertNotNull(method);
    }
    
    @Test
    void testDialogHasCreateSummaryLabelMethod() throws Exception {
        // Verify the createSummaryLabel method exists
        var method = ImportResultDialog.class.getDeclaredMethod("createSummaryLabel");
        assertNotNull(method);
        assertEquals(javafx.scene.control.Label.class, method.getReturnType());
    }
    
    @Test
    void testDialogHasCreateErrorScrollPaneMethod() throws Exception {
        // Verify the createErrorScrollPane method exists
        var method = ImportResultDialog.class.getDeclaredMethod("createErrorScrollPane");
        assertNotNull(method);
        assertEquals(javafx.scene.control.ScrollPane.class, method.getReturnType());
    }
    
    @Test
    void testDialogHasAddErrorGroupMethod() throws Exception {
        // Verify the addErrorGroup method exists
        var method = ImportResultDialog.class.getDeclaredMethod(
            "addErrorGroup",
            javafx.scene.layout.VBox.class,
            ImportError.ErrorType.class,
            List.class
        );
        assertNotNull(method);
    }
    
    @Test
    void testDialogHasGetErrorTypeLabelMethod() throws Exception {
        // Verify the getErrorTypeLabel method exists
        var method = ImportResultDialog.class.getDeclaredMethod(
            "getErrorTypeLabel",
            ImportError.ErrorType.class
        );
        assertNotNull(method);
        assertEquals(String.class, method.getReturnType());
    }
    
    @Test
    void testDialogHasApplyThemeMethod() throws Exception {
        // Verify the applyTheme method exists
        var method = ImportResultDialog.class.getDeclaredMethod("applyTheme");
        assertNotNull(method);
    }
    
    @Test
    void testImportResultModelHasRequiredMethods() throws Exception {
        // Verify ImportResult has the methods needed by the dialog
        assertNotNull(ImportResult.class.getMethod("getSuccessCount"));
        assertNotNull(ImportResult.class.getMethod("getFailureCount"));
        assertNotNull(ImportResult.class.getMethod("hasErrors"));
        assertNotNull(ImportResult.class.getMethod("getErrors"));
    }
    
    @Test
    void testImportErrorModelHasRequiredMethods() throws Exception {
        // Verify ImportError has the methods needed by the dialog
        assertNotNull(ImportError.class.getMethod("getErrorType"));
        assertNotNull(ImportError.class.getMethod("getFormattedMessage"));
    }
    
    @Test
    void testErrorTypeEnumExists() {
        // Verify ErrorType enum exists and has required values
        assertNotNull(ImportError.ErrorType.VALIDATION);
        assertNotNull(ImportError.ErrorType.PARSING);
        assertNotNull(ImportError.ErrorType.DATABASE);
    }
}
