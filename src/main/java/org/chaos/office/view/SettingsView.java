package org.chaos.office.view;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.chaos.office.util.LocaleManager;

/**
 * SettingsView - Application settings UI
 * Requirements: 1.2, 1.3, 12.1, 12.2, 12.3
 */
public class SettingsView extends VBox {
    
    private final ComboBox<String> languageComboBox;
    private final TextField databasePathField;
    private final Button saveButton;
    
    public SettingsView() {
        setSpacing(16);
        setPadding(new Insets(16));
        
        // Title
        Label titleLabel = new Label(LocaleManager.getString("settings.title"));
        titleLabel.getStyleClass().add("label-headline");
        
        // Language selection
        Label languageLabel = new Label(LocaleManager.getString("settings.language"));
        languageComboBox = new ComboBox<>();
        languageComboBox.getItems().addAll("English", "Français", "العربية");
        languageComboBox.setValue("English");
        languageComboBox.setPrefWidth(300);
        
        // Database path (read-only)
        Label dbLabel = new Label(LocaleManager.getString("settings.database"));
        databasePathField = new TextField();
        databasePathField.setEditable(false);
        databasePathField.setPrefWidth(300);
        
        // Save button
        saveButton = new Button(LocaleManager.getString("settings.save"));
        
        getChildren().addAll(
            titleLabel,
            languageLabel, languageComboBox,
            dbLabel, databasePathField,
            saveButton
        );
    }
    
    public ComboBox<String> getLanguageComboBox() { return languageComboBox; }
    public TextField getDatabasePathField() { return databasePathField; }
    public Button getSaveButton() { return saveButton; }
}
