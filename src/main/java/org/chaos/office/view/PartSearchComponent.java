package org.chaos.office.view;

import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.chaos.office.model.Category;
import org.chaos.office.model.Part;
import org.chaos.office.service.CategoryService;
import org.chaos.office.service.SearchService;
import org.chaos.office.util.LocaleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

/**
 * PartSearchComponent is a custom JavaFX control for searching and selecting parts.
 * 
 * <p>This component provides:
 * <ul>
 *   <li>Real-time text search with debouncing (300ms delay)</li>
 *   <li>Category filtering with "All Categories" option</li>
 *   <li>Custom cell rendering for parts with visual stock indicators</li>
 *   <li>Click-to-select functionality with callback support</li>
 * </ul>
 * 
 * <p>The component extends VBox and integrates with SearchService for
 * optimized database queries.
 * 
 * <p>Requirements: 4.1, 4.5, 5.1, 5.2, 5.3, 5.4, 6.1, 6.4, 6.5, 12.3
 */
public class PartSearchComponent extends VBox {
    private static final Logger logger = LoggerFactory.getLogger(PartSearchComponent.class);
    private static final int DEBOUNCE_DELAY_MS = 300;
    
    // UI Components
    private final TextField searchField;
    private final ComboBox<Category> categoryFilter;
    private final ListView<Part> resultsView;
    
    // Services
    private final SearchService searchService;
    private final CategoryService categoryService;
    
    // Data
    private final ObservableList<Part> filteredParts;
    
    // Debouncing
    private final PauseTransition searchDebounce;
    
    // Callback for part selection
    private Consumer<Part> onPartSelected;
    
    /**
     * Creates a new PartSearchComponent with default services.
     */
    public PartSearchComponent() {
        this(new SearchService(), new CategoryService());
    }
    
    /**
     * Creates a new PartSearchComponent with specified services.
     * This constructor allows dependency injection for testing.
     * 
     * @param searchService the search service to use
     * @param categoryService the category service to use
     */
    public PartSearchComponent(SearchService searchService, CategoryService categoryService) {
        this.searchService = searchService;
        this.categoryService = categoryService;
        this.filteredParts = FXCollections.observableArrayList();
        
        // Initialize search field
        this.searchField = new TextField();
        this.searchField.setPromptText("Search parts by name, maker, category...");
        
        // Initialize category filter
        this.categoryFilter = new ComboBox<>();
        this.categoryFilter.setPromptText(LocaleManager.getString("category.all"));
        
        // Initialize results view
        this.resultsView = new ListView<>();
        this.resultsView.setItems(filteredParts);
        this.resultsView.setCellFactory(param -> new PartCell());
        
        // Initialize debounce timer
        this.searchDebounce = new PauseTransition(Duration.millis(DEBOUNCE_DELAY_MS));
        this.searchDebounce.setOnFinished(event -> performSearch());
        
        // Build UI
        initializeUI();
        
        // Load initial data
        loadCategories();
        performSearch(); // Load all parts initially
        
        // Setup event handlers
        setupEventHandlers();
        
        logger.info("PartSearchComponent initialized");
    }
    
    /**
     * Initializes the UI layout and styling.
     */
    private void initializeUI() {
        // Search controls container
        HBox searchControls = new HBox(10);
        searchControls.setAlignment(Pos.CENTER_LEFT);
        searchControls.setPadding(new Insets(10));
        
        // Search field label
        Label searchLabel = new Label("Search:");
        searchLabel.setMinWidth(60);
        
        // Category filter label
        Label categoryLabel = new Label("Category:");
        categoryLabel.setMinWidth(70);
        
        // Configure search field to grow horizontally
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.setMaxWidth(Double.MAX_VALUE);
        
        // Configure category filter
        categoryFilter.setMinWidth(200);
        categoryFilter.setPrefWidth(250);
        
        // Add controls to container
        searchControls.getChildren().addAll(
            searchLabel, searchField,
            categoryLabel, categoryFilter
        );
        
        // Configure results view to fill available space
        resultsView.setMinHeight(300);
        resultsView.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(resultsView, Priority.ALWAYS);
        
        // Add all components to this VBox
        this.getChildren().addAll(searchControls, resultsView);
        this.setSpacing(5);
        this.setPadding(new Insets(5));
        
        // Make this component fill available horizontal space
        this.setMaxWidth(Double.MAX_VALUE);
        
        // Apply styling
        this.getStyleClass().add("part-search-component");
        searchControls.getStyleClass().add("search-controls");
        resultsView.getStyleClass().add("part-results-list");
    }
    
    /**
     * Loads all categories from the database and populates the filter dropdown.
     * Adds an "All Categories" option at the beginning.
     */
    private void loadCategories() {
        try {
            List<Category> categories = categoryService.getAllCategories();
            
            // Create "All Categories" option
            Category allCategories = new Category();
            allCategories.setId(-1);
            allCategories.setName(LocaleManager.getString("category.all"));
            
            // Add to combo box
            categoryFilter.getItems().clear();
            categoryFilter.getItems().add(allCategories);
            categoryFilter.getItems().addAll(categories);
            
            // Set custom cell factory for display
            categoryFilter.setCellFactory(param -> new javafx.scene.control.ListCell<Category>() {
                @Override
                protected void updateItem(Category item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getName());
                    }
                }
            });
            
            // Set button cell for selected item display
            categoryFilter.setButtonCell(new javafx.scene.control.ListCell<Category>() {
                @Override
                protected void updateItem(Category item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(LocaleManager.getString("category.all"));
                    } else {
                        setText(item.getName());
                    }
                }
            });
            
            // Select "All Categories" by default
            categoryFilter.getSelectionModel().selectFirst();
            
            logger.info("Loaded {} categories into filter", categories.size());
        } catch (Exception e) {
            logger.error("Error loading categories", e);
        }
    }
    
    /**
     * Sets up event handlers for user interactions.
     */
    private void setupEventHandlers() {
        // Search field - trigger debounced search on text change
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            searchDebounce.playFromStart();
        });
        
        // Category filter - trigger immediate search on selection change
        categoryFilter.setOnAction(event -> performSearch());
        
        // Results view - handle part selection
        resultsView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) { // Single click
                Part selectedPart = resultsView.getSelectionModel().getSelectedItem();
                if (selectedPart != null && onPartSelected != null) {
                    handlePartSelection(selectedPart);
                }
            }
        });
    }
    
    /**
     * Performs the search based on current search text and category filter.
     * This method is called after the debounce delay or immediately on filter change.
     */
    private void performSearch() {
        String query = searchField.getText();
        Category selectedCategory = categoryFilter.getSelectionModel().getSelectedItem();
        
        // Determine category ID (null for "All Categories")
        Integer categoryId = null;
        if (selectedCategory != null && selectedCategory.getId() != -1) {
            categoryId = selectedCategory.getId();
        }
        
        // Perform search
        try {
            List<Part> results = searchService.searchParts(query, categoryId);
            filteredParts.setAll(results);
            
            logger.debug("Search performed - query: '{}', category: {}, results: {}", 
                        query, categoryId, results.size());
        } catch (Exception e) {
            logger.error("Error performing search", e);
            filteredParts.clear();
        }
    }
    
    /**
     * Handles part selection from the results list.
     * Validates stock availability and invokes the callback if set.
     * 
     * @param part the selected part
     */
    private void handlePartSelection(Part part) {
        if (part == null) {
            return;
        }
        
        // Check stock availability (Requirement 12.5)
        if (part.getQuantity() <= 0) {
            logger.warn("Attempted to select out-of-stock part: {}", part.getName());
            
            // Show error alert for zero-stock parts
            javafx.application.Platform.runLater(() -> {
                org.chaos.office.util.AlertHelper.showError(
                    org.chaos.office.util.LocaleManager.getString("parts.out.of.stock.title"),
                    String.format(org.chaos.office.util.LocaleManager.getString("parts.out.of.stock.message"), 
                        part.getName())
                );
            });
            return;
        }
        
        // Invoke callback
        if (onPartSelected != null) {
            logger.info("Part selected: {} (ID: {})", part.getName(), part.getId());
            onPartSelected.accept(part);
        }
    }
    
    /**
     * Sets the callback to be invoked when a part is selected.
     * 
     * @param onPartSelected the callback consumer
     */
    public void setOnPartSelected(Consumer<Part> onPartSelected) {
        this.onPartSelected = onPartSelected;
    }
    
    /**
     * Gets the search field for external access (e.g., for testing or additional configuration).
     * 
     * @return the search text field
     */
    public TextField getSearchField() {
        return searchField;
    }
    
    /**
     * Gets the category filter for external access.
     * 
     * @return the category filter combo box
     */
    public ComboBox<Category> getCategoryFilter() {
        return categoryFilter;
    }
    
    /**
     * Gets the results view for external access.
     * 
     * @return the results list view
     */
    public ListView<Part> getResultsView() {
        return resultsView;
    }
    
    /**
     * Gets the filtered parts list for external access.
     * 
     * @return the observable list of filtered parts
     */
    public ObservableList<Part> getFilteredParts() {
        return filteredParts;
    }
    
    /**
     * Refreshes the search results based on current filters.
     * Useful for updating the view after external data changes.
     */
    public void refresh() {
        performSearch();
    }
    
    /**
     * Clears the search field and resets filters.
     */
    public void clear() {
        searchField.clear();
        categoryFilter.getSelectionModel().selectFirst(); // Select "All Categories"
    }
}
