// =============== ProfileController.java ===============
package com.focusbuddy.controllers;

import com.focusbuddy.models.Mahasiswa;
import com.focusbuddy.services.ProfileService;
import com.focusbuddy.utils.NotificationManager;
import com.focusbuddy.utils.UserSession;
import com.focusbuddy.utils.ValidationUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.format.DateTimeFormatter;

public class ProfileController {

    @FXML private ImageView profileImageView;
    @FXML private Button changePhotoButton;
    @FXML private TextField usernameField;
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private TextField studentIdField;
    @FXML private TextField majorField;
    @FXML private TextArea bioArea;
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button saveProfileButton;
    @FXML private Button changePasswordButton;
    @FXML private Label memberSinceLabel;
    @FXML private Label totalTasksLabel;
    @FXML private Label completedTasksLabel;
    @FXML private Label totalFocusTimeLabel;
    @FXML private Button deleteAccountButton;

    // Statistics components
    @FXML private Label profileStatsTasksCompleted;
    @FXML private Label profileStatsFocusTime;
    @FXML private Label profileStatsNotesCount;
    @FXML private Label profileStatsGoalsAchieved;

    private ProfileService profileService;
    private Mahasiswa currentUser;

    @FXML
    private void initialize() {
        profileService = new ProfileService();
        currentUser = (Mahasiswa) UserSession.getInstance().getCurrentUser();

        setupButtons();
        loadProfileData();
        loadProfileStatistics();
    }

    private void setupButtons() {
        changePhotoButton.setOnAction(e -> changeProfilePhoto());
        saveProfileButton.setOnAction(e -> saveProfile());
        changePasswordButton.setOnAction(e -> changePassword());
        deleteAccountButton.setOnAction(e -> deleteAccount());
    }

    private void loadProfileData() {
        if (currentUser != null) {
            // Load user data
            usernameField.setText(currentUser.getUsername());
            fullNameField.setText(currentUser.getFullName());
            emailField.setText(currentUser.getEmail());

            // Load additional profile data from database
            var profileData = profileService.getProfileData(currentUser.getId());
            if (profileData != null) {
                studentIdField.setText(profileData.getStudentId());
                majorField.setText(profileData.getMajor());
                bioArea.setText(profileData.getBio());

                // Load profile image if exists
                if (profileData.getProfileImagePath() != null) {
                    loadProfileImage(profileData.getProfileImagePath());
                }
            }

            // Set member since date
            if (currentUser.getCreatedAt() != null) {
                memberSinceLabel.setText("Member since: " +
                        currentUser.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM yyyy")));
            }
        }
    }

    private void loadProfileStatistics() {
        if (currentUser != null) {
            var stats = profileService.getProfileStatistics(currentUser.getId());

            profileStatsTasksCompleted.setText(String.valueOf(stats.getCompletedTasks()));
            profileStatsFocusTime.setText(stats.getTotalFocusTime() + " hours");
            profileStatsNotesCount.setText(String.valueOf(stats.getNotesCount()));
            profileStatsGoalsAchieved.setText(String.valueOf(stats.getAchievedGoals()));
        }
    }

    private void changeProfilePhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Photo");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        Stage stage = (Stage) changePhotoButton.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            try {
                // Save image path to database
                String imagePath = profileService.saveProfileImage(currentUser.getId(), selectedFile);
                if (imagePath != null) {
                    loadProfileImage(imagePath);
                    NotificationManager.getInstance().showNotification(
                            "Profile Photo Updated",
                            "Your profile photo has been updated successfully!",
                            NotificationManager.NotificationType.SUCCESS
                    );
                }
            } catch (Exception e) {
                NotificationManager.getInstance().showNotification(
                        "Error",
                        "Failed to update profile photo: " + e.getMessage(),
                        NotificationManager.NotificationType.ERROR
                );
            }
        }
    }

    private void loadProfileImage(String imagePath) {
        try {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                Image image = new Image(imageFile.toURI().toString());
                profileImageView.setImage(image);
            }
        } catch (Exception e) {
            System.err.println("Failed to load profile image: " + e.getMessage());
        }
    }

    private void saveProfile() {
        // Validate inputs
        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String studentId = studentIdField.getText().trim();
        String major = majorField.getText().trim();
        String bio = bioArea.getText().trim();

        if (!ValidationUtils.isNotEmpty(fullName)) {
            showValidationError("Full name cannot be empty");
            return;
        }

        if (!email.isEmpty() && !ValidationUtils.isValidEmail(email)) {
            showValidationError("Please enter a valid email address");
            return;
        }

        // Update user object
        currentUser.setFullName(fullName);
        currentUser.setEmail(email);
        currentUser.setStudentId(studentId);
        currentUser.setMajor(major);

        // Save to database
        boolean success = profileService.updateProfile(currentUser, bio);

        if (success) {
            // Update session
            UserSession.getInstance().setCurrentUser(currentUser);

            NotificationManager.getInstance().showNotification(
                    "Profile Updated",
                    "Your profile has been updated successfully!",
                    NotificationManager.NotificationType.SUCCESS
            );
        } else {
            NotificationManager.getInstance().showNotification(
                    "Error",
                    "Failed to update profile. Please try again.",
                    NotificationManager.NotificationType.ERROR
            );
        }
    }

    private void changePassword() {
        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Validate inputs
        if (!ValidationUtils.isNotEmpty(currentPassword)) {
            showValidationError("Please enter your current password");
            return;
        }

        if (!ValidationUtils.isValidPassword(newPassword)) {
            showValidationError("New password must be at least 6 characters long");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showValidationError("New password and confirmation do not match");
            return;
        }

        // Verify current password and update
        boolean success = profileService.changePassword(currentUser.getId(), currentPassword, newPassword);

        if (success) {
            // Clear password fields
            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();

            NotificationManager.getInstance().showNotification(
                    "Password Changed",
                    "Your password has been changed successfully!",
                    NotificationManager.NotificationType.SUCCESS
            );
        } else {
            NotificationManager.getInstance().showNotification(
                    "Error",
                    "Failed to change password. Please check your current password.",
                    NotificationManager.NotificationType.ERROR
            );
        }
    }

    private void deleteAccount() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Account");
        alert.setHeaderText("Are you sure you want to delete your account?");
        alert.setContentText("This action cannot be undone. All your data will be permanently deleted.");

        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                // Show password confirmation dialog
                TextInputDialog passwordDialog = new TextInputDialog();
                passwordDialog.setTitle("Confirm Password");
                passwordDialog.setHeaderText("Please enter your password to confirm account deletion:");
                passwordDialog.setContentText("Password:");

                passwordDialog.showAndWait().ifPresent(password -> {
                    boolean success = profileService.deleteAccount(currentUser.getId(), password);

                    if (success) {
                        NotificationManager.getInstance().showNotification(
                                "Account Deleted",
                                "Your account has been deleted successfully.",
                                NotificationManager.NotificationType.SUCCESS
                        );

                        // Logout and redirect to login
                        UserSession.getInstance().logout();
                        // Note: You would need to implement navigation back to login here

                    } else {
                        NotificationManager.getInstance().showNotification(
                                "Error",
                                "Failed to delete account. Please check your password.",
                                NotificationManager.NotificationType.ERROR
                        );
                    }
                });
            }
        });
    }

    private void showValidationError(String message) {
        NotificationManager.getInstance().showNotification(
                "Validation Error",
                message,
                NotificationManager.NotificationType.WARNING
        );
    }
}