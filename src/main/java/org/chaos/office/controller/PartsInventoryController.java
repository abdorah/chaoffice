package org.chaos.office.controller;

import javafx.collections.FXCollections;
import javafx.scene.Parent;
import org.chaos.office.service.PartService;
import org.chaos.office.util.AlertHelper;
import org.chaos.office.util.LocaleManager;
import org.chaos.office.view.PartsInventoryView;

/**
 * PartsInventoryController - Manages parts inventory
 * Requirements: 4.2, 4.3, 4.5, 4.6, 4.7, 13.2, 13.4, 15.1
 */
public class PartsInventoryController {
    
    private final PartsInventoryView view;
    private final PartService partService;
    
    public PartsInventoryController() {
        this.view = new PartsInventoryView();
        this.partService = new PartService();
        
        setupEventHandlers();
        loadParts();
    }
    
    private void setupEventHandlers() {
        view.getSearchField().textProperty().addListener((obs, old, newVal) -> handleSearch(newVal));
        view.getAddButton().setOnAction(e -> handleAdd());
        view.getEditButton().setOnAction(e -> handleEdit());
        view.getDeleteButton().setOnAction(e -> handleDelete());
    }
    
    private void loadParts() {
        var parts = partService.getAllParts();
        view.getPartsTable().setItems(FXCollections.observableArrayList(parts));
    }
    
    private void handleSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            loadParts();
        } else {
            var results = partService.searchParts(query);
            view.getPartsTable().setItems(FXCollections.observableArrayList(results));
        }
    }
    
    private void handleAdd() {
        AlertHelper.showInfo("Add Part", "Add part dialog - Coming soon");
    }
    
    private void handleEdit() {
        var selected = view.getPartsTable().getSelectionModel().getSelectedItem();
        if (selected != null) {
            AlertHelper.showInfo("Edit Part", "Edit part dialog - Coming soon");
        }
    }
    
    private void handleDelete() {
        var selected = view.getPartsTable().getSelectionModel().getSelectedItem();
        if (selected != null) {
            boolean confirmed = AlertHelper.showConfirmation(
                LocaleManager.getString("parts.delete.confirm.title"),
                LocaleManager.getString("parts.delete.confirm.message")
            );
            if (confirmed) {
                partService.deletePart(selected.getId());
                loadParts();
            }
        }
    }
    
    public Parent getView() {
        return view;
    }
}
