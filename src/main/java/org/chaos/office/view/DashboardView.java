package org.chaos.office.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import org.chaos.office.util.LocaleManager;
import org.chaos.office.util.SessionManager;

/**
 * DashboardView - Main application dashboard
 * Requirements: 1.2, 1.3, 14.1, 14.2, 14.6, 14.7
 */
public class DashboardView extends BorderPane {
    
    private final Label userLabel;
    private final Button logoutButton;
    private final Button partsButton;
    private final Button categoriesButton;
    private final Button billingButton;
    private final Button billsButton;
    private final Button analyticsButton;
    private final Button settingsButton;
    private final StackPane contentPane;
    
    public DashboardView() {
        getStyleClass().add("root");
        
        // Top header
        HBox header = createHeader();
        setTop(header);
        
        // Left navigation
        VBox navigation = createNavigation();
        setLeft(navigation);
        
        // Center content area
        contentPane = new StackPane();
        contentPane.getStyleClass().add("padding-md");
        setCenter(contentPane);
        
        // Initialize components
        userLabel = (Label) header.getChildren().get(1);
        logoutButton = (Button) header.getChildren().get(3);
        partsButton = (Button) navigation.getChildren().get(0);
        categoriesButton = (Button) navigation.getChildren().get(1);
        billingButton = (Button) navigation.getChildren().get(2);
        billsButton = (Button) navigation.getChildren().get(3);
        analyticsButton = (Button) navigation.getChildren().get(4);
        settingsButton = (Button) navigation.getChildren().get(5);
    }
    
    private HBox createHeader() {
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16));
        header.getStyleClass().add("card");
        
        Label titleLabel = new Label(LocaleManager.getString("dashboard.title"));
        titleLabel.getStyleClass().add("label-headline");
        
        String userName = SessionManager.getInstance().getCurrentUser() != null ?
            SessionManager.getInstance().getCurrentUser().getFirstName() : "User";
        Label userLabel = new Label(LocaleManager.getString("dashboard.welcome").replace("{0}", userName));
        
        Button logoutButton = new Button(LocaleManager.getString("dashboard.logout"));
        logoutButton.getStyleClass().add("button-outlined");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        header.getChildren().addAll(titleLabel, userLabel, spacer, logoutButton);
        return header;
    }
    
    private VBox createNavigation() {
        VBox nav = new VBox(8);
        nav.setPadding(new Insets(16));
        nav.setPrefWidth(200);
        nav.getStyleClass().add("card");
        
        Button partsBtn = createNavButton(LocaleManager.getString("dashboard.menu.parts"));
        Button categoriesBtn = createNavButton(LocaleManager.getString("dashboard.menu.categories"));
        Button billingBtn = createNavButton(LocaleManager.getString("dashboard.menu.billing"));
        Button billsBtn = createNavButton(LocaleManager.getString("dashboard.menu.bills"));
        Button analyticsBtn = createNavButton(LocaleManager.getString("dashboard.menu.analytics"));
        Button settingsBtn = createNavButton(LocaleManager.getString("dashboard.menu.settings"));
        
        nav.getChildren().addAll(partsBtn, categoriesBtn, billingBtn, billsBtn, analyticsBtn, settingsBtn);
        return nav;
    }
    
    private Button createNavButton(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.getStyleClass().add("button-text");
        btn.setAlignment(Pos.CENTER_LEFT);
        return btn;
    }
    
    public StackPane getContentPane() {
        return contentPane;
    }
    
    public Button getLogoutButton() {
        return logoutButton;
    }
    
    public Button getPartsButton() {
        return partsButton;
    }
    
    public Button getCategoriesButton() {
        return categoriesButton;
    }
    
    public Button getBillingButton() {
        return billingButton;
    }
    
    public Button getBillsButton() {
        return billsButton;
    }
    
    public Button getAnalyticsButton() {
        return analyticsButton;
    }
    
    public Button getSettingsButton() {
        return settingsButton;
    }
    
    public void refreshLanguage() {
        // Update header
        Label titleLabel = (Label) ((HBox) getTop()).getChildren().get(0);
        titleLabel.setText(LocaleManager.getString("dashboard.title"));
        
        String userName = SessionManager.getInstance().getCurrentUser() != null ?
            SessionManager.getInstance().getCurrentUser().getFirstName() : "User";
        userLabel.setText(LocaleManager.getString("dashboard.welcome").replace("{0}", userName));
        logoutButton.setText(LocaleManager.getString("dashboard.logout"));
        
        // Update navigation buttons
        partsButton.setText(LocaleManager.getString("dashboard.menu.parts"));
        categoriesButton.setText(LocaleManager.getString("dashboard.menu.categories"));
        billingButton.setText(LocaleManager.getString("dashboard.menu.billing"));
        billsButton.setText(LocaleManager.getString("dashboard.menu.bills"));
        analyticsButton.setText(LocaleManager.getString("dashboard.menu.analytics"));
        settingsButton.setText(LocaleManager.getString("dashboard.menu.settings"));
    }
}
