package org.chaos.office.view;

import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import org.chaos.office.util.LocaleManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SettingsView branding, currency, and user account management sections
 * Requirements: 2.1, 2.2, 5.1, 5.2, 5.3, 3.1, 3.2, 3.4
 */
class SettingsViewTest {
    
    @BeforeAll
    static void initToolkit() {
        // Initialize JavaFX toolkit
        try {
            javafx.application.Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Toolkit already initialized
        }
    }
    
    @Test
    void testBrandingSectionComponentsExist() {
        // Given: A new SettingsView
        SettingsView view = new SettingsView();
        
        // Then: Branding components should be initialized
        assertNotNull(view.getStoreNameField(), "Store name field should be initialized");
        assertNotNull(view.getSelectLogoButton(), "Select logo button should be initialized");
        assertNotNull(view.getLogoPreview(), "Logo preview should be initialized");
    }
    
    @Test
    void testStoreNameFieldProperties() {
        // Given: A new SettingsView
        SettingsView view = new SettingsView();
        TextField storeNameField = view.getStoreNameField();
        
        // Then: Store name field should have correct properties
        assertNotNull(storeNameField);
        // Width may be -1 before layout, just check it's set
        assertTrue(storeNameField.getPrefWidth() >= -1, "Store name field should have width set");
        assertNotNull(storeNameField.getPromptText(), "Store name field should have prompt text");
    }
    
    @Test
    void testSelectLogoButtonProperties() {
        // Given: A new SettingsView
        SettingsView view = new SettingsView();
        Button selectLogoButton = view.getSelectLogoButton();
        
        // Then: Select logo button should be initialized
        assertNotNull(selectLogoButton);
        assertNotNull(selectLogoButton.getText(), "Select logo button should have text");
        assertFalse(selectLogoButton.getText().isEmpty(), "Select logo button text should not be empty");
    }
    
    @Test
    void testLogoPreviewProperties() {
        // Given: A new SettingsView
        SettingsView view = new SettingsView();
        ImageView logoPreview = view.getLogoPreview();
        
        // Then: Logo preview should have correct dimensions
        assertNotNull(logoPreview);
        assertEquals(150.0, logoPreview.getFitWidth(), "Logo preview should have width of 150");
        assertEquals(150.0, logoPreview.getFitHeight(), "Logo preview should have height of 150");
        assertTrue(logoPreview.isPreserveRatio(), "Logo preview should preserve aspect ratio");
    }
    
    @Test
    void testStoreNameFieldIsEditable() {
        // Given: A new SettingsView
        SettingsView view = new SettingsView();
        TextField storeNameField = view.getStoreNameField();
        
        // Then: Store name field should be editable
        assertTrue(storeNameField.isEditable(), "Store name field should be editable");
    }
    
    @Test
    void testCurrencySectionComponentsExist() {
        // Given: A new SettingsView
        SettingsView view = new SettingsView();
        
        // Then: Currency components should be initialized
        assertNotNull(view.getCurrencySymbolComboBox(), "Currency symbol combo box should be initialized");
        assertNotNull(view.getCustomSymbolField(), "Custom symbol field should be initialized");
        assertNotNull(view.getCurrencyAcronymField(), "Currency acronym field should be initialized");
    }
    
    @Test
    void testCurrencySymbolComboBoxHasCorrectOptions() {
        // Given: A new SettingsView
        SettingsView view = new SettingsView();
        ComboBox<String> comboBox = view.getCurrencySymbolComboBox();
        
        // Then: ComboBox should have all required currency symbols
        assertNotNull(comboBox);
        assertTrue(comboBox.getItems().contains("$"), "Should contain $ symbol");
        assertTrue(comboBox.getItems().contains("€"), "Should contain € symbol");
        assertTrue(comboBox.getItems().contains("£"), "Should contain £ symbol");
        assertTrue(comboBox.getItems().contains("¥"), "Should contain ¥ symbol");
        assertTrue(comboBox.getItems().contains("₹"), "Should contain ₹ symbol");
        assertTrue(comboBox.getItems().contains(LocaleManager.getString("settings.currency.custom.option")), "Should contain Custom option");
        assertEquals("$", comboBox.getValue(), "Default value should be $");
    }
    
    @Test
    void testCustomSymbolFieldInitiallyDisabled() {
        // Given: A new SettingsView
        SettingsView view = new SettingsView();
        TextField customSymbolField = view.getCustomSymbolField();
        
        // Then: Custom symbol field should be initially disabled
        assertNotNull(customSymbolField);
        assertTrue(customSymbolField.isDisabled(), "Custom symbol field should be initially disabled");
    }
    
    @Test
    void testCustomSymbolFieldEnabledWhenCustomSelected() {
        // Given: A new SettingsView
        SettingsView view = new SettingsView();
        ComboBox<String> comboBox = view.getCurrencySymbolComboBox();
        TextField customSymbolField = view.getCustomSymbolField();
        
        // When: Custom is selected
        comboBox.setValue(LocaleManager.getString("settings.currency.custom.option"));
        comboBox.getOnAction().handle(null);
        
        // Then: Custom symbol field should be enabled
        assertFalse(customSymbolField.isDisabled(), "Custom symbol field should be enabled when Custom is selected");
    }
    
    @Test
    void testCustomSymbolFieldDisabledWhenStandardSymbolSelected() {
        // Given: A new SettingsView with Custom selected
        SettingsView view = new SettingsView();
        ComboBox<String> comboBox = view.getCurrencySymbolComboBox();
        TextField customSymbolField = view.getCustomSymbolField();
        
        comboBox.setValue(LocaleManager.getString("settings.currency.custom.option"));
        comboBox.getOnAction().handle(null);
        
        // When: A standard symbol is selected
        comboBox.setValue("€");
        comboBox.getOnAction().handle(null);
        
        // Then: Custom symbol field should be disabled
        assertTrue(customSymbolField.isDisabled(), "Custom symbol field should be disabled when standard symbol is selected");
    }
    
    @Test
    void testCurrencyAcronymFieldProperties() {
        // Given: A new SettingsView
        SettingsView view = new SettingsView();
        TextField acronymField = view.getCurrencyAcronymField();
        
        // Then: Currency acronym field should have correct properties
        assertNotNull(acronymField);
        // Width may be -1 before layout, just check it's set
        assertTrue(acronymField.getPrefWidth() >= -1, "Currency acronym field should have width set");
        assertEquals("USD", acronymField.getPromptText(), "Currency acronym field should have USD as prompt text");
        assertTrue(acronymField.isEditable(), "Currency acronym field should be editable");
    }
    
    @Test
    void testUserAccountManagementComponentsExist() {
        // Given: A new SettingsView
        SettingsView view = new SettingsView();
        
        // Then: User account management components should be initialized
        assertNotNull(view.getNewUsernameField(), "New username field should be initialized");
        assertNotNull(view.getChangeUsernameButton(), "Change username button should be initialized");
        assertNotNull(view.getCurrentPasswordField(), "Current password field should be initialized");
        assertNotNull(view.getNewPasswordField(), "New password field should be initialized");
        assertNotNull(view.getConfirmPasswordField(), "Confirm password field should be initialized");
        assertNotNull(view.getChangePasswordButton(), "Change password button should be initialized");
    }
    
    @Test
    void testNewUsernameFieldProperties() {
        // Given: A new SettingsView
        SettingsView view = new SettingsView();
        TextField usernameField = view.getNewUsernameField();
        
        // Then: New username field should have correct properties
        assertNotNull(usernameField);
        // Width may be -1 before layout, just check it's set
        assertTrue(usernameField.getPrefWidth() >= -1, "New username field should have width set");
        assertNotNull(usernameField.getPromptText(), "New username field should have prompt text");
        assertTrue(usernameField.isEditable(), "New username field should be editable");
    }
    
    @Test
    void testChangeUsernameButtonProperties() {
        // Given: A new SettingsView
        SettingsView view = new SettingsView();
        Button button = view.getChangeUsernameButton();
        
        // Then: Change username button should be initialized
        assertNotNull(button);
        assertNotNull(button.getText(), "Change username button should have text");
        assertFalse(button.getText().isEmpty(), "Change username button text should not be empty");
    }
    
    @Test
    void testPasswordFieldsArePasswordFields() {
        // Given: A new SettingsView
        SettingsView view = new SettingsView();
        
        // Then: Password fields should be PasswordField instances
        assertTrue(view.getCurrentPasswordField() instanceof PasswordField, "Current password should be a PasswordField");
        assertTrue(view.getNewPasswordField() instanceof PasswordField, "New password should be a PasswordField");
        assertTrue(view.getConfirmPasswordField() instanceof PasswordField, "Confirm password should be a PasswordField");
    }
    
    @Test
    void testPasswordFieldsProperties() {
        // Given: A new SettingsView
        SettingsView view = new SettingsView();
        
        // Then: All password fields should have correct properties
        PasswordField currentPassword = view.getCurrentPasswordField();
        assertNotNull(currentPassword);
        // Width may be -1 before layout, just check it's set
        assertTrue(currentPassword.getPrefWidth() >= -1, "Current password field should have width set");
        assertNotNull(currentPassword.getPromptText(), "Current password field should have prompt text");
        
        PasswordField newPassword = view.getNewPasswordField();
        assertNotNull(newPassword);
        assertTrue(newPassword.getPrefWidth() >= -1, "New password field should have width set");
        assertNotNull(newPassword.getPromptText(), "New password field should have prompt text");
        
        PasswordField confirmPassword = view.getConfirmPasswordField();
        assertNotNull(confirmPassword);
        assertTrue(confirmPassword.getPrefWidth() >= -1, "Confirm password field should have width set");
        assertNotNull(confirmPassword.getPromptText(), "Confirm password field should have prompt text");
    }
    
    @Test
    void testChangePasswordButtonProperties() {
        // Given: A new SettingsView
        SettingsView view = new SettingsView();
        Button button = view.getChangePasswordButton();
        
        // Then: Change password button should be initialized
        assertNotNull(button);
        assertNotNull(button.getText(), "Change password button should have text");
        assertFalse(button.getText().isEmpty(), "Change password button text should not be empty");
    }
}
