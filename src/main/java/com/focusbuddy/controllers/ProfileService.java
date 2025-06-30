// =============== ProfileService.java ===============
package com.focusbuddy.services;

import com.focusbuddy.database.DatabaseManager;
import com.focusbuddy.models.Mahasiswa;
import com.focusbuddy.utils.PasswordUtils;

import java.sql.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;

public class ProfileService {

    private static final String PROFILE_IMAGES_DIR = "profile_images";

    public ProfileService() {
        // Create profile images directory if it doesn't exist
        createProfileImagesDirectory();
    }

    private void createProfileImagesDirectory() {
        File dir = new File(PROFILE_IMAGES_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public ProfileData getProfileData(int userId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = """
                SELECT u.*, p.student_id, p.major, p.bio, p.profile_image_path 
                FROM users u 
                LEFT JOIN user_profiles p ON u.id = p.user_id 
                WHERE u.id = ?
                """;

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                ProfileData data = new ProfileData();
                data.setStudentId(rs.getString("student_id"));
                data.setMajor(rs.getString("major"));
                data.setBio(rs.getString("bio"));
                data.setProfileImagePath(rs.getString("profile_image_path"));
                return data;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new ProfileData(); // Return empty data if not found
    }

    public boolean updateProfile(Mahasiswa user, String bio) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            // Update users table
            String updateUserQuery = "UPDATE users SET full_name = ?, email = ? WHERE id = ?";
            PreparedStatement userStmt = conn.prepareStatement(updateUserQuery);
            userStmt.setString(1, user.getFullName());
            userStmt.setString(2, user.getEmail());
            userStmt.setInt(3, user.getId());
            userStmt.executeUpdate();

            // Update or insert profile data
            String upsertProfileQuery = """
                INSERT INTO user_profiles (user_id, student_id, major, bio, updated_at) 
                VALUES (?, ?, ?, ?, ?) 
                ON DUPLICATE KEY UPDATE 
                student_id = VALUES(student_id), 
                major = VALUES(major), 
                bio = VALUES(bio), 
                updated_at = VALUES(updated_at)
                """;

            PreparedStatement profileStmt = conn.prepareStatement(upsertProfileQuery);
            profileStmt.setInt(1, user.getId());
            profileStmt.setString(2, user.getStudentId());
            profileStmt.setString(3, user.getMajor());
            profileStmt.setString(4, bio);
            profileStmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            profileStmt.executeUpdate();

            conn.commit();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String saveProfileImage(int userId, File imageFile) {
        try {
            // Create unique filename
            String extension = getFileExtension(imageFile.getName());
            String filename = "profile_" + userId + "_" + System.currentTimeMillis() + "." + extension;
            Path targetPath = Paths.get(PROFILE_IMAGES_DIR, filename);

            // Copy file
            Files.copy(imageFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // Update database
            String imagePath = targetPath.toString();
            updateProfileImagePath(userId, imagePath);

            return imagePath;

        } catch (IOException | SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void updateProfileImagePath(int userId, String imagePath) throws SQLException {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = """
                INSERT INTO user_profiles (user_id, profile_image_path, updated_at) 
                VALUES (?, ?, ?) 
                ON DUPLICATE KEY UPDATE 
                profile_image_path = VALUES(profile_image_path), 
                updated_at = VALUES(updated_at)
                """;

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setString(2, imagePath);
            stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();
        }
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex == -1 ? "" : filename.substring(lastDotIndex + 1);
    }

    public ProfileStatistics getProfileStatistics(int userId) {
        ProfileStatistics stats = new ProfileStatistics();

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // Get completed tasks count
            String tasksQuery = "SELECT COUNT(*) FROM tasks WHERE user_id = ? AND status = 'COMPLETED'";
            PreparedStatement tasksStmt = conn.prepareStatement(tasksQuery);
            tasksStmt.setInt(1, userId);
            ResultSet tasksRs = tasksStmt.executeQuery();
            if (tasksRs.next()) {
                stats.setCompletedTasks(tasksRs.getInt(1));
            }

            // Get total focus time (from focus_sessions table)
            String focusQuery = "SELECT SUM(duration_minutes) FROM focus_sessions WHERE user_id = ?";
            PreparedStatement focusStmt = conn.prepareStatement(focusQuery);
            focusStmt.setInt(1, userId);
            ResultSet focusRs = focusStmt.executeQuery();
            if (focusRs.next()) {
                int totalMinutes = focusRs.getInt(1);
                stats.setTotalFocusTime(totalMinutes / 60); // Convert to hours
            }

            // Get notes count
            String notesQuery = "SELECT COUNT(*) FROM notes WHERE user_id = ?";
            PreparedStatement notesStmt = conn.prepareStatement(notesQuery);
            notesStmt.setInt(1, userId);
            ResultSet notesRs = notesStmt.executeQuery();
            if (notesRs.next()) {
                stats.setNotesCount(notesRs.getInt(1));
            }

            // Get achieved goals count
            String goalsQuery = "SELECT COUNT(*) FROM goals WHERE user_id = ? AND status = 'COMPLETED'";
            PreparedStatement goalsStmt = conn.prepareStatement(goalsQuery);
            goalsStmt.setInt(1, userId);
            ResultSet goalsRs = goalsStmt.executeQuery();
            if (goalsRs.next()) {
                stats.setAchievedGoals(goalsRs.getInt(1));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return stats;
    }

    public boolean changePassword(int userId, String currentPassword, String newPassword) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // Verify current password
            String getPasswordQuery = "SELECT password, salt FROM users WHERE id = ?";
            PreparedStatement getStmt = conn.prepareStatement(getPasswordQuery);
            getStmt.setInt(1, userId);

            ResultSet rs = getStmt.executeQuery();
            if (rs.next()) {
                String storedPassword = rs.getString("password");
                String salt = rs.getString("salt");

                // Verify current password
                boolean passwordValid = false;
                if (salt != null) {
                    passwordValid = PasswordUtils.verifyPassword(currentPassword, storedPassword, salt);
                } else {
                    // Legacy password check
                    passwordValid = currentPassword.equals(storedPassword);
                }

                if (passwordValid) {
                    // Update with new password
                    String newSalt = PasswordUtils.generateSalt();
                    String hashedNewPassword = PasswordUtils.hashPassword(newPassword, newSalt);

                    String updateQuery = "UPDATE users SET password = ?, salt = ? WHERE id = ?";
                    PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
                    updateStmt.setString(1, hashedNewPassword);
                    updateStmt.setString(2, newSalt);
                    updateStmt.setInt(3, userId);

                    return updateStmt.executeUpdate() > 0;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean deleteAccount(int userId, String password) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // First verify password
            String getPasswordQuery = "SELECT password, salt FROM users WHERE id = ?";
            PreparedStatement getStmt = conn.prepareStatement(getPasswordQuery);
            getStmt.setInt(1, userId);

            ResultSet rs = getStmt.executeQuery();
            if (rs.next()) {
                String storedPassword = rs.getString("password");
                String salt = rs.getString("salt");

                boolean passwordValid = false;
                if (salt != null) {
                    passwordValid = PasswordUtils.verifyPassword(password, storedPassword, salt);
                } else {
                    passwordValid = password.equals(storedPassword);
                }

                if (passwordValid) {
                    // Delete user (cascade will handle related data)
                    String deleteQuery = "DELETE FROM users WHERE id = ?";
                    PreparedStatement deleteStmt = conn.prepareStatement(deleteQuery);
                    deleteStmt.setInt(1, userId);

                    return deleteStmt.executeUpdate() > 0;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // Helper classes
    public static class ProfileData {
        private String studentId;
        private String major;
        private String bio;
        private String profileImagePath;

        // Getters and Setters
        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }

        public String getMajor() { return major; }
        public void setMajor(String major) { this.major = major; }

        public String getBio() { return bio; }
        public void setBio(String bio) { this.bio = bio; }

        public String getProfileImagePath() { return profileImagePath; }
        public void setProfileImagePath(String profileImagePath) { this.profileImagePath = profileImagePath; }
    }

    public static class ProfileStatistics {
        private int completedTasks;
        private int totalFocusTime;
        private int notesCount;
        private int achievedGoals;

        // Getters and Setters
        public int getCompletedTasks() { return completedTasks; }
        public void setCompletedTasks(int completedTasks) { this.completedTasks = completedTasks; }

        public int getTotalFocusTime() { return totalFocusTime; }
        public void setTotalFocusTime(int totalFocusTime) { this.totalFocusTime = totalFocusTime; }

        public int getNotesCount() { return notesCount; }
        public void setNotesCount(int notesCount) { this.notesCount = notesCount; }

        public int getAchievedGoals() { return achievedGoals; }
        public void setAchievedGoals(int achievedGoals) { this.achievedGoals = achievedGoals; }
    }
}