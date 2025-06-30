package com.focusbuddy;

import com.focusbuddy.controllers.LoginController;
import com.focusbuddy.database.DatabaseManager;
import com.focusbuddy.utils.ThemeManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

public class FocusBuddyApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Initialize database
            DatabaseManager.getInstance().initializeDatabase();

            // Load login scene
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));

            // Get screen dimensions for responsive design
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

            // Create scene without fixed dimensions - let it size naturally based on content
            Scene scene = new Scene(loader.load());

            // Apply default theme
            ThemeManager.getInstance().applyTheme(scene, ThemeManager.Theme.LIGHT);

            // Configure the primary stage
            primaryStage.setTitle("FocusBuddy - Productivity Assistant");
            primaryStage.setScene(scene);

            // Set responsive window properties
            primaryStage.setResizable(true);

            // Set minimum size to ensure usability
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);

            // Set initial size based on screen size (80% of screen, but not larger than optimal size)
            double initialWidth = Math.min(1200, screenBounds.getWidth() * 0.8);
            double initialHeight = Math.min(800, screenBounds.getHeight() * 0.8);

            primaryStage.setWidth(initialWidth);
            primaryStage.setHeight(initialHeight);

            // Center the window on screen
            primaryStage.setX((screenBounds.getWidth() - initialWidth) / 2);
            primaryStage.setY((screenBounds.getHeight() - initialHeight) / 2);

            // Set application icon (with proper error handling)
            try {
                Image icon = new Image(getClass().getResourceAsStream("/images/icon.png"));
                if (!icon.isError()) {
                    primaryStage.getIcons().add(icon);
                }
            } catch (Exception iconError) {
                System.out.println("Warning: Could not load application icon - " + iconError.getMessage());
                // Continue without icon
            }

            // Show the stage
            primaryStage.show();

            // Optional: Handle window close request for cleanup
            primaryStage.setOnCloseRequest(event -> {
                try {
                    // Close database connections
                    DatabaseManager.getInstance().closeConnections();
                } catch (Exception e) {
                    System.err.println("Error during application shutdown: " + e.getMessage());
                } finally {
                    System.exit(0);
                }
            });

            // Optional: Add window state listener for better responsive behavior
            primaryStage.maximizedProperty().addListener((obs, wasMaximized, isMaximized) -> {
                if (isMaximized) {
                    // When maximized, ensure content scales properly
                    System.out.println("Window maximized - content should scale to full screen");
                }
            });

        } catch (Exception e) {
            System.err.println("Failed to start FocusBuddy application: " + e.getMessage());
            e.printStackTrace();

            // Show error dialog or fallback UI if needed
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        // Set system properties for better UI scaling and performance
        System.setProperty("prism.allowhidpi", "true");
        System.setProperty("glass.gtk.uiScale", "1.0");

        // Enable hardware acceleration if available
        System.setProperty("prism.order", "sw,d3d,es2");

        // Launch the JavaFX application
        launch(args);
    }
}