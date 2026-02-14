package org.chaos.office.view;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.chaos.office.util.LocaleManager;

/**
 * SettingsView - Application settings UI
 * Requirements: 1.2, 1.3, 12.1, 12.2, 12.3, 2.1, 2.2
 */
public class SettingsView extends BorderPane {
    
    private final ComboBox<String> languageComboBox;
    private final ComboBox<String> themeComboBox;
    private final TextField databasePathField;
    private final Button saveButton;
    
    // Branding section components
    private final TextField storeNameField;
    private final Button selectLogoButton;
    private final ImageView logoPreview;
    
    // Currency section components
    private final ComboBox<String> currencySymbolComboBox;
    private final TextField customSymbolField;
    private final TextField currencyAcronymField;
    
    // User account management section components
    private final TextField newUsernameField;
    private final Button changeUsernameButton;
    private final PasswordField currentPasswordField;
    private final PasswordField newPasswordField;
    private final PasswordField confirmPasswordField;
    private final Button changePasswordButton;
    
    public SettingsView() {
        // Create main HBox for two-column layout
        HBox mainLayout = new HBox(20);
        mainLayout.setPadding(new Insets(16));
        
        // Left column
        VBox leftColumn = new VBox(16);
        leftColumn.setPrefWidth(400);
        
        // Right column
        VBox rightColumn = new VBox(16);
        rightColumn.setPrefWidth(400);
        
        // Title (spans both columns)
        Label titleLabel = new Label(LocaleManager.getString("settings.title"));
        titleLabel.getStyleClass().add("label-headline");
        
        // LEFT COLUMN CONTENT
        
        // Language selection
        Label languageLabel = new Label(LocaleManager.getString("settings.language"));
        languageComboBox = new ComboBox<>();
        languageComboBox.getItems().addAll("English", "Français", "العربية");
        languageComboBox.setValue("English");
        languageComboBox.setMaxWidth(Double.MAX_VALUE);
        
        // Theme selection
        Label themeLabel = new Label(LocaleManager.getString("settings.theme"));
        themeComboBox = new ComboBox<>();
        themeComboBox.getItems().addAll(
            LocaleManager.getString("settings.theme.main"),
            LocaleManager.getString("settings.theme.dark"),
            LocaleManager.getString("settings.theme.ubuntu"),
            LocaleManager.getString("settings.theme.none")
        );
        themeComboBox.setValue(LocaleManager.getString("settings.theme.main"));
        themeComboBox.setMaxWidth(Double.MAX_VALUE);
        
        // Database path (read-only)
        Label dbLabel = new Label(LocaleManager.getString("settings.database"));
        databasePathField = new TextField();
        databasePathField.setEditable(false);
        databasePathField.setMaxWidth(Double.MAX_VALUE);
        
        // Store Branding Section
        Label brandingLabel = new Label(LocaleManager.getString("settings.branding"));
        brandingLabel.getStyleClass().add("label-headline");
        
        Label storeNameLabel = new Label(LocaleManager.getString("settings.branding.storeName"));
        storeNameField = new TextField();
        storeNameField.setPromptText(LocaleManager.getString("settings.branding.storeName"));
        storeNameField.setMaxWidth(Double.MAX_VALUE);
        
        Label logoLabel = new Label(LocaleManager.getString("settings.branding.logo"));
        selectLogoButton = new Button(LocaleManager.getString("settings.branding.selectLogo"));
        selectLogoButton.setMaxWidth(Double.MAX_VALUE);
        
        logoPreview = new ImageView();
        logoPreview.setFitWidth(150);
        logoPreview.setFitHeight(150);
        logoPreview.setPreserveRatio(true);
        logoPreview.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1;");
        
        // Add to left column
        leftColumn.getChildren().addAll(
            languageLabel, languageComboBox,
            themeLabel, themeComboBox,
            dbLabel, databasePathField,
            new Separator(),
            brandingLabel,
            storeNameLabel, storeNameField,
            logoLabel, selectLogoButton, logoPreview
        );
        
        // RIGHT COLUMN CONTENT
        
        // Currency Settings Section
        Label currencyLabel = new Label(LocaleManager.getString("settings.currency"));
        currencyLabel.getStyleClass().add("label-headline");
        
        Label currencySymbolLabel = new Label(LocaleManager.getString("settings.currency.symbol"));
        currencySymbolComboBox = new ComboBox<>();
        currencySymbolComboBox.getItems().addAll("$", "€", "£", "¥", "₹", LocaleManager.getString("settings.currency.custom.option"));
        currencySymbolComboBox.setValue("$");
        currencySymbolComboBox.setMaxWidth(Double.MAX_VALUE);
        
        Label customSymbolLabel = new Label(LocaleManager.getString("settings.currency.custom"));
        customSymbolField = new TextField();
        customSymbolField.setPromptText(LocaleManager.getString("settings.currency.custom"));
        customSymbolField.setMaxWidth(Double.MAX_VALUE);
        customSymbolField.setDisable(true); // Initially disabled
        
        // Enable/disable custom symbol field based on ComboBox selection
        currencySymbolComboBox.setOnAction(e -> {
            boolean isCustom = LocaleManager.getString("settings.currency.custom.option").equals(currencySymbolComboBox.getValue());
            customSymbolField.setDisable(!isCustom);
        });
        
        Label currencyAcronymLabel = new Label(LocaleManager.getString("settings.currency.acronym"));
        currencyAcronymField = new TextField();
        currencyAcronymField.setPromptText("USD");
        currencyAcronymField.setMaxWidth(Double.MAX_VALUE);
        
        // User Account Management Section
        Label accountLabel = new Label(LocaleManager.getString("settings.account"));
        accountLabel.getStyleClass().add("label-headline");
        
        // Username change
        Label usernameLabel = new Label(LocaleManager.getString("settings.account.username"));
        newUsernameField = new TextField();
        newUsernameField.setPromptText(LocaleManager.getString("settings.account.newUsername"));
        newUsernameField.setMaxWidth(Double.MAX_VALUE);
        
        changeUsernameButton = new Button(LocaleManager.getString("settings.account.changeUsername"));
        changeUsernameButton.setMaxWidth(Double.MAX_VALUE);
        
        // Password change
        Label passwordLabel = new Label(LocaleManager.getString("settings.account.password"));
        
        Label currentPasswordLabel = new Label(LocaleManager.getString("settings.account.currentPassword"));
        currentPasswordField = new PasswordField();
        currentPasswordField.setPromptText(LocaleManager.getString("settings.account.currentPassword"));
        currentPasswordField.setMaxWidth(Double.MAX_VALUE);
        
        Label newPasswordLabel = new Label(LocaleManager.getString("settings.account.newPassword"));
        newPasswordField = new PasswordField();
        newPasswordField.setPromptText(LocaleManager.getString("settings.account.newPassword"));
        newPasswordField.setMaxWidth(Double.MAX_VALUE);
        
        Label confirmPasswordLabel = new Label(LocaleManager.getString("settings.account.confirmPassword"));
        confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText(LocaleManager.getString("settings.account.confirmPassword"));
        confirmPasswordField.setMaxWidth(Double.MAX_VALUE);
        
        changePasswordButton = new Button(LocaleManager.getString("settings.account.changePassword"));
        changePasswordButton.setMaxWidth(Double.MAX_VALUE);
        
        // Add to right column
        rightColumn.getChildren().addAll(
            currencyLabel,
            currencySymbolLabel, currencySymbolComboBox,
            customSymbolLabel, customSymbolField,
            currencyAcronymLabel, currencyAcronymField,
            new Separator(),
            accountLabel,
            usernameLabel, newUsernameField, changeUsernameButton,
            passwordLabel,
            currentPasswordLabel, currentPasswordField,
            newPasswordLabel, newPasswordField,
            confirmPasswordLabel, confirmPasswordField,
            changePasswordButton
        );
        
        // Save button (bottom of right column)
        saveButton = new Button(LocaleManager.getString("settings.save"));
        saveButton.setMaxWidth(Double.MAX_VALUE);
        rightColumn.getChildren().add(saveButton);
        
        // Add columns to main layout
        mainLayout.getChildren().addAll(leftColumn, rightColumn);
        
        // Create wrapper VBox with title
        VBox wrapper = new VBox(16, titleLabel, mainLayout);
        wrapper.setPadding(new Insets(16));
        
        // Wrap content in ScrollPane
        ScrollPane scrollPane = new ScrollPane(wrapper);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        
        // Set ScrollPane to fill the BorderPane center
        setCenter(scrollPane);
    }
    
    public ComboBox<String> getLanguageComboBox() { return languageComboBox; }
    public ComboBox<String> getThemeComboBox() { return themeComboBox; }
    public TextField getDatabasePathField() { return databasePathField; }
    public Button getSaveButton() { return saveButton; }
    
    // Branding getters
    public TextField getStoreNameField() { return storeNameField; }
    public Button getSelectLogoButton() { return selectLogoButton; }
    public ImageView getLogoPreview() { return logoPreview; }
    
    // Currency getters
    public ComboBox<String> getCurrencySymbolComboBox() { return currencySymbolComboBox; }
    public TextField getCustomSymbolField() { return customSymbolField; }
    public TextField getCurrencyAcronymField() { return currencyAcronymField; }
    
    // User account management getters
    public TextField getNewUsernameField() { return newUsernameField; }
    public Button getChangeUsernameButton() { return changeUsernameButton; }
    public PasswordField getCurrentPasswordField() { return currentPasswordField; }
    public PasswordField getNewPasswordField() { return newPasswordField; }
    public PasswordField getConfirmPasswordField() { return confirmPasswordField; }
    public Button getChangePasswordButton() { return changePasswordButton; }
}
