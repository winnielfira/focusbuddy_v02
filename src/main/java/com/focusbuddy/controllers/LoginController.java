package com.focusbuddy.controllers;

import com.focusbuddy.database.DatabaseManager;
import com.focusbuddy.models.Mahasiswa;
import com.focusbuddy.utils.ThemeManager;
import com.focusbuddy.utils.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.focusbuddy.utils.PasswordUtils;
import com.focusbuddy.utils.ValidationUtils;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private Label statusLabel;
    @FXML private VBox loginContainer;
    @FXML private ToggleButton themeToggle;

    @FXML
    private void initialize() {
        // Set up theme toggle
        themeToggle.setOnAction(e -> toggleTheme());

        // Set up login button action
        loginButton.setOnAction(e -> handleLogin());
        registerButton.setOnAction(e -> showRegisterDialog());

        // Add Enter key listener for login
        passwordField.setOnAction(e -> handleLogin());
        usernameField.setOnAction(e -> passwordField.requestFocus());
    }

    private void toggleTheme() {
        Scene scene = loginContainer.getScene();
        ThemeManager.getInstance().toggleTheme(scene);

        // Update toggle button text
        if (ThemeManager.getInstance().getCurrentTheme() == ThemeManager.Theme.DARK) {
            themeToggle.setText("🌙");
        } else {
            themeToggle.setText("☀️");
        }
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (!ValidationUtils.isNotEmpty(username) || !ValidationUtils.isNotEmpty(password)) {
            showStatus("Please fill in all fields", false);
            return;
        }

        if (!ValidationUtils.isValidUsername(username)) {
            showStatus("Invalid username format", false);
            return;
        }

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = "SELECT * FROM users WHERE username = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("password");
                String salt = rs.getString("salt");

                // For backward compatibility, check if salt exists
                boolean passwordValid;
                if (salt != null) {
                    passwordValid = PasswordUtils.verifyPassword(password, storedPassword, salt);
                } else {
                    // Legacy plain text password check
                    passwordValid = password.equals(storedPassword);

                    // Upgrade to hashed password
                    if (passwordValid) {
                        String newSalt = PasswordUtils.generateSalt();
                        String hashedPassword = PasswordUtils.hashPassword(password, newSalt);

                        String updateQuery = "UPDATE users SET password = ?, salt = ? WHERE id = ?";
                        PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
                        updateStmt.setString(1, hashedPassword);
                        updateStmt.setString(2, newSalt);
                        updateStmt.setInt(3, rs.getInt("id"));
                        updateStmt.executeUpdate();
                    }
                }

                if (passwordValid) {
                    // Create user object
                    Mahasiswa user = new Mahasiswa();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setEmail(rs.getString("email"));
                    user.setFullName(rs.getString("full_name"));

                    // Set current user session
                    UserSession.getInstance().setCurrentUser(user);

                    // Navigate to main dashboard
                    navigateToMainDashboard();
                } else {
                    showStatus("Invalid username or password", false);
                }
            } else {
                showStatus("Invalid username or password", false);
            }

        } catch (Exception e) {
            showStatus("Login failed: " + e.getMessage(), false);
            e.printStackTrace();
        }
    }

    private void showRegisterDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Register New Account");
        dialog.setHeaderText("Create your FocusBuddy account");

        // Create form fields
        TextField regUsername = new TextField();
        regUsername.setPromptText("Username");

        PasswordField regPassword = new PasswordField();
        regPassword.setPromptText("Password");

        TextField regEmail = new TextField();
        regEmail.setPromptText("Email");

        TextField regFullName = new TextField();
        regFullName.setPromptText("Full Name");

        VBox content = new VBox(10);
        content.getChildren().addAll(
                new Label("Username:"), regUsername,
                new Label("Password:"), regPassword,
                new Label("Email:"), regEmail,
                new Label("Full Name:"), regFullName
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                registerUser(regUsername.getText(), regPassword.getText(),
                        regEmail.getText(), regFullName.getText());
            }
        });
    }

    private void registerUser(String username, String password, String email, String fullName) {
        // Validate inputs
        if (!ValidationUtils.isValidUsername(username)) {
            showStatus("Username must be 3-20 characters, letters, numbers, and underscores only", false);
            return;
        }

        if (!ValidationUtils.isValidPassword(password)) {
            showStatus("Password must be at least 6 characters long", false);
            return;
        }

        if (!email.isEmpty() && !ValidationUtils.isValidEmail(email)) {
            showStatus("Please enter a valid email address", false);
            return;
        }

        // Sanitize inputs
        username = ValidationUtils.sanitizeInput(username);
        email = ValidationUtils.sanitizeInput(email);
        fullName = ValidationUtils.sanitizeInput(fullName);

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // Generate salt and hash password
            String salt = PasswordUtils.generateSalt();
            String hashedPassword = PasswordUtils.hashPassword(password, salt);

            String query = "INSERT INTO users (username, password, salt, email, full_name) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            stmt.setString(3, salt);
            stmt.setString(4, email);
            stmt.setString(5, fullName);

            int result = stmt.executeUpdate();
            if (result > 0) {
                showStatus("Registration successful! Please login.", true);
            }

        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry")) {
                showStatus("Username already exists. Please choose a different username.", false);
            } else {
                showStatus("Registration failed: " + e.getMessage(), false);
            }
            e.printStackTrace();
        }
    }

    private void navigateToMainDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));

            Stage stage = (Stage) loginButton.getScene().getWindow();

            // Get screen dimensions
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

            // Create scene without fixed dimensions
            Scene scene = new Scene(loader.load());

            // Apply current theme
            ThemeManager.getInstance().applyTheme(scene, ThemeManager.getInstance().getCurrentTheme());

            // Set scene
            stage.setScene(scene);
            stage.setTitle("FocusBuddy - Dashboard");

            // Make window maximized and resizable
            stage.setMaximized(true);
            stage.setResizable(true);

            // Set minimum size
            stage.setMinWidth(1200);
            stage.setMinHeight(800);

            // Center window if not maximized
            if (!stage.isMaximized()) {
                stage.setX((screenBounds.getWidth() - stage.getWidth()) / 2);
                stage.setY((screenBounds.getHeight() - stage.getHeight()) / 2);
            }

        } catch (Exception e) {
            showStatus("Failed to load dashboard: " + e.getMessage(), false);
            e.printStackTrace();
        }
    }

    private void showStatus(String message, boolean isSuccess) {
        statusLabel.setText(message);
        statusLabel.setStyle(isSuccess ? "-fx-text-fill: #4CAF50;" : "-fx-text-fill: #F44336;");
    }
}