package org.chaos.office.util;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ToastNotification utility class for displaying temporary, non-blocking notifications.
 * Provides toast-style messages that appear briefly and fade out automatically.
 * 
 * <p>This class is responsible for:
 * <ul>
 *   <li>Displaying success notifications for completed operations</li>
 *   <li>Displaying info notifications for general feedback</li>
 *   <li>Auto-dismissing after a configurable duration</li>
 *   <li>Fade-in and fade-out animations</li>
 * </ul>
 * 
 * <p>Requirements: 1.4, 2.2, 2.4, 9.2, 12.5
 */
public class ToastNotification {
    private static final Logger logger = LoggerFactory.getLogger(ToastNotification.class);
    
    private static final int DEFAULT_DURATION_MS = 3000;
    private static final int FADE_DURATION_MS = 500;
    
    /**
     * Toast notification types for different styling.
     */
    public enum Type {
        SUCCESS,
        INFO,
        WARNING,
        ERROR
    }
    
    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with static methods only.
     */
    private ToastNotification() {
        // Utility class - no instantiation
    }
    
    /**
     * Shows a success toast notification with default duration.
     * 
     * @param message the message to display
     */
    public static void showSuccess(String message) {
        show(message, Type.SUCCESS, DEFAULT_DURATION_MS);
    }
    
    /**
     * Shows an info toast notification with default duration.
     * 
     * @param message the message to display
     */
    public static void showInfo(String message) {
        show(message, Type.INFO, DEFAULT_DURATION_MS);
    }
    
    /**
     * Shows a warning toast notification with default duration.
     * 
     * @param message the message to display
     */
    public static void showWarning(String message) {
        show(message, Type.WARNING, DEFAULT_DURATION_MS);
    }
    
    /**
     * Shows an error toast notification with default duration.
     * 
     * @param message the message to display
     */
    public static void showError(String message) {
        show(message, Type.ERROR, DEFAULT_DURATION_MS);
    }
    
    /**
     * Shows a toast notification with specified type and duration.
     * 
     * @param message the message to display
     * @param type the type of notification (affects styling)
     * @param durationMs the duration in milliseconds before auto-dismiss
     */
    public static void show(String message, Type type, int durationMs) {
        logger.info("Showing {} toast: {}", type, message);
        
        // Create label for message
        Label label = new Label(message);
        label.setWrapText(true);
        label.setMaxWidth(400);
        label.setPadding(new javafx.geometry.Insets(15, 20, 15, 20));
        
        // Apply styling based on type
        applyTypeStyle(label, type);
        
        // Create container
        StackPane container = new StackPane(label);
        container.setAlignment(Pos.BOTTOM_CENTER);
        container.setPadding(new javafx.geometry.Insets(0, 0, 50, 0));
        container.setStyle("-fx-background-color: transparent;");
        
        // Create stage
        Stage toastStage = new Stage();
        toastStage.initStyle(StageStyle.TRANSPARENT);
        toastStage.setAlwaysOnTop(true);
        
        Scene scene = new Scene(container);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        toastStage.setScene(scene);
        
        // Position at bottom center of screen
        javafx.stage.Screen screen = javafx.stage.Screen.getPrimary();
        javafx.geometry.Rectangle2D bounds = screen.getVisualBounds();
        toastStage.setX((bounds.getWidth() - 400) / 2);
        toastStage.setY(bounds.getHeight() - 150);
        
        // Fade in animation
        FadeTransition fadeIn = new FadeTransition(Duration.millis(FADE_DURATION_MS), container);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        
        // Fade out animation
        FadeTransition fadeOut = new FadeTransition(Duration.millis(FADE_DURATION_MS), container);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> toastStage.close());
        
        // Auto-dismiss after duration
        PauseTransition pause = new PauseTransition(Duration.millis(durationMs));
        pause.setOnFinished(e -> fadeOut.play());
        
        // Show and start animations
        toastStage.show();
        fadeIn.play();
        fadeIn.setOnFinished(e -> pause.play());
    }
    
    /**
     * Applies styling to the label based on notification type.
     * 
     * @param label the label to style
     * @param type the notification type
     */
    private static void applyTypeStyle(Label label, Type type) {
        // Base style
        String baseStyle = "-fx-background-color: %s; " +
                          "-fx-text-fill: white; " +
                          "-fx-background-radius: 5; " +
                          "-fx-font-size: 14px; " +
                          "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 2);";
        
        String color;
        switch (type) {
            case SUCCESS:
                color = "#4CAF50"; // Green
                break;
            case INFO:
                color = "#2196F3"; // Blue
                break;
            case WARNING:
                color = "#FF9800"; // Orange
                break;
            case ERROR:
                color = "#F44336"; // Red
                break;
            default:
                color = "#757575"; // Gray
        }
        
        label.setStyle(String.format(baseStyle, color));
    }
}
