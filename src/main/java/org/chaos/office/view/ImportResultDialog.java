package org.chaos.office.view;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import org.chaos.office.model.ImportError;
import org.chaos.office.model.ImportResult;
import org.chaos.office.util.LocaleManager;
import org.chaos.office.util.ThemeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ImportResultDialog - Custom dialog for displaying bulk import results
 * Shows success/failure counts and detailed error information grouped by type.
 * 
 * Requirements: 6.3, 6.4, 6.5
 */
public class ImportResultDialog extends Dialog<ButtonType> {
    private static final Logger logger = LoggerFactory.getLogger(ImportResultDialog.class);
    
    private final ImportResult importResult;
    
    /**
     * Creates a new import result dialog
     * 
     * @param importResult The import result to display
     */
    public ImportResultDialog(ImportResult importResult) {
        this.importResult = importResult;
        
        initializeDialog();
        createContent();
        applyTheme();
    }
    
    /**
     * Initializes the dialog properties
     */
    private void initializeDialog() {
        setTitle(LocaleManager.getString("import.result.title"));
        setHeaderText(null); // Material Design 3 style - no header
        
        // Add OK button
        getDialogPane().getButtonTypes().add(ButtonType.OK);
        
        logger.info("Created import result dialog - Success: {}, Failures: {}", 
                    importResult.getSuccessCount(), importResult.getFailureCount());
    }
    
    /**
     * Creates the dialog content with summary and error details
     */
    private void createContent() {
        VBox content = new VBox(16);
        content.setPadding(new Insets(16));
        content.setPrefWidth(600);
        
        // Add summary section
        Label summaryLabel = createSummaryLabel();
        content.getChildren().add(summaryLabel);
        
        // Add error details if there are any errors
        if (importResult.hasErrors()) {
            Label errorHeaderLabel = new Label(LocaleManager.getString("import.result.errors.header"));
            errorHeaderLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            content.getChildren().add(errorHeaderLabel);
            
            // Create scrollable error list
            ScrollPane errorScrollPane = createErrorScrollPane();
            VBox.setVgrow(errorScrollPane, Priority.ALWAYS);
            content.getChildren().add(errorScrollPane);
        }
        
        getDialogPane().setContent(content);
    }
    
    /**
     * Creates the summary label showing success and failure counts
     * 
     * @return Label with formatted summary text
     */
    private Label createSummaryLabel() {
        String summaryText = String.format(
            "%s: %d\n%s: %d",
            LocaleManager.getString("import.result.success"),
            importResult.getSuccessCount(),
            LocaleManager.getString("import.result.failure"),
            importResult.getFailureCount()
        );
        
        Label label = new Label(summaryText);
        label.setStyle("-fx-font-size: 14px;");
        return label;
    }
    
    /**
     * Creates a scrollable pane containing error details grouped by type
     * 
     * @return ScrollPane with error list
     */
    private ScrollPane createErrorScrollPane() {
        VBox errorContainer = new VBox(12);
        errorContainer.setPadding(new Insets(8));
        
        // Group errors by type
        Map<ImportError.ErrorType, List<ImportError>> errorsByType = 
            importResult.getErrors().stream()
                .collect(Collectors.groupingBy(ImportError::getErrorType));
        
        // Add errors for each type
        for (ImportError.ErrorType errorType : ImportError.ErrorType.values()) {
            List<ImportError> errors = errorsByType.get(errorType);
            if (errors != null && !errors.isEmpty()) {
                addErrorGroup(errorContainer, errorType, errors);
            }
        }
        
        ScrollPane scrollPane = new ScrollPane(errorContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(300);
        scrollPane.setMaxHeight(400);
        scrollPane.setStyle("-fx-background-color: transparent;");
        
        return scrollPane;
    }
    
    /**
     * Adds a group of errors for a specific error type to the container
     * 
     * @param container The container to add the error group to
     * @param errorType The type of errors in this group
     * @param errors The list of errors
     */
    private void addErrorGroup(VBox container, ImportError.ErrorType errorType, List<ImportError> errors) {
        // Add error type header
        Label typeHeader = new Label(getErrorTypeLabel(errorType) + " (" + errors.size() + ")");
        typeHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #d32f2f;");
        container.getChildren().add(typeHeader);
        
        // Add individual errors
        VBox errorList = new VBox(4);
        errorList.setPadding(new Insets(0, 0, 0, 16));
        
        for (ImportError error : errors) {
            Label errorLabel = new Label("• " + error.getFormattedMessage());
            errorLabel.setWrapText(true);
            errorLabel.setStyle("-fx-font-size: 12px;");
            errorList.getChildren().add(errorLabel);
        }
        
        container.getChildren().add(errorList);
    }
    
    /**
     * Gets the localized label for an error type
     * 
     * @param errorType The error type
     * @return Localized error type label
     */
    private String getErrorTypeLabel(ImportError.ErrorType errorType) {
        switch (errorType) {
            case VALIDATION:
                return LocaleManager.getString("import.error.type.validation");
            case PARSING:
                return LocaleManager.getString("import.error.type.parsing");
            case DATABASE:
                return LocaleManager.getString("import.error.type.database");
            default:
                return errorType.toString();
        }
    }
    
    /**
     * Applies the current theme to the dialog
     */
    private void applyTheme() {
        String currentTheme = ThemeManager.getCurrentTheme();
        if (!"none".equals(currentTheme)) {
            try {
                String stylesheet = getClass().getResource("/style/" + currentTheme + ".css").toExternalForm();
                getDialogPane().getStylesheets().add(stylesheet);
                logger.debug("Applied theme '{}' to import result dialog", currentTheme);
            } catch (Exception e) {
                // Stylesheet not found - try default theme
                try {
                    String defaultStylesheet = getClass().getResource("/style/main.css").toExternalForm();
                    getDialogPane().getStylesheets().add(defaultStylesheet);
                    logger.debug("Applied default theme to import result dialog");
                } catch (Exception ex) {
                    // No stylesheet available - continue without styling
                    logger.debug("Could not load stylesheet for dialog: {}", ex.getMessage());
                }
            }
        }
    }
}
