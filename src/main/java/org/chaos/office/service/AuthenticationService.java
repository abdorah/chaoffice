package org.chaos.office.service;

import org.chaos.office.model.User;
import org.chaos.office.util.DatabaseConnection;
import org.chaos.office.util.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * AuthenticationService handles user authentication and session management.
 * 
 * <p>This service is responsible for:
 * <ul>
 *   <li>Authenticating users with username and password</li>
 *   <li>Validating user credentials against the database</li>
 *   <li>Managing user sessions via SessionManager</li>
 *   <li>Logging out users and clearing sessions</li>
 * </ul>
 * 
 * <p>Requirements: 3.2, 3.3, 3.6
 */
public class AuthenticationService {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    
    /**
     * Authenticates a user with the provided username and password.
     * If authentication is successful, creates a session for the user.
     * 
     * @param username the username to authenticate
     * @param password the password to authenticate
     * @return Optional containing the authenticated User if successful, empty otherwise
     */
    public Optional<User> authenticate(String username, String password) {
        logger.info("Attempting to authenticate user: {}", username);
        
        if (username == null || username.trim().isEmpty() || 
            password == null || password.trim().isEmpty()) {
            logger.warn("Authentication failed: username or password is empty");
            return Optional.empty();
        }
        
        if (!validateCredentials(username, password)) {
            logger.warn("Authentication failed for user: {}", username);
            return Optional.empty();
        }
        
        // Retrieve user details from database
        Optional<User> userOptional = getUserByUsername(username);
        
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            // Set the current user in session
            SessionManager.getInstance().setCurrentUser(user);
            logger.info("Authentication successful for user: {}", username);
            return Optional.of(user);
        }
        
        logger.warn("User not found after credential validation: {}", username);
        return Optional.empty();
    }
    
    /**
     * Validates user credentials against the database.
     * 
     * @param username the username to validate
     * @param password the password to validate
     * @return true if credentials are valid, false otherwise
     */
    public boolean validateCredentials(String username, String password) {
        String sql = "SELECT password FROM users WHERE username = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("password");
                    return verifyPassword(password, storedPassword);
                }
            }
        } catch (SQLException e) {
            logger.error("Error validating credentials for user: {}", username, e);
        }
        
        return false;
    }
    
    /**
     * Retrieves a user from the database by username.
     * 
     * @param username the username to search for
     * @return Optional containing the User if found, empty otherwise
     */
    private Optional<User> getUserByUsername(String username) {
        String sql = "SELECT id, username, password, role, firstname, lastname FROM users WHERE username = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setPassword(rs.getString("password"));
                    user.setRole(rs.getString("role"));
                    user.setFirstName(rs.getString("firstname"));
                    user.setLastName(rs.getString("lastname"));
                    
                    return Optional.of(user);
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving user by username: {}", username, e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Logs out the current user by clearing the session.
     */
    public void logout() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            logger.info("Logging out user: {}", currentUser.getUsername());
        }
        SessionManager.getInstance().clearSession();
        logger.info("Session cleared successfully");
    }
    
    /**
     * Changes the username for the current user.
     * Requires the current password for verification.
     * 
     * @param currentPassword the current password for verification
     * @param newUsername the new username to set
     * @return true if username was changed successfully, false otherwise
     */
    public boolean changeUsername(String currentPassword, String newUsername) {
        logger.info("Attempting to change username");
        
        // Validate new username is non-empty
        if (newUsername == null || newUsername.trim().isEmpty()) {
            logger.warn("Username change failed: new username is empty");
            return false;
        }
        
        // Get current user from session
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            logger.warn("Username change failed: no user in session");
            return false;
        }
        
        // Verify current password
        if (!verifyPassword(currentPassword, currentUser.getPassword())) {
            logger.warn("Username change failed: incorrect current password");
            return false;
        }
        
        // Update username in database
        String sql = "UPDATE users SET username = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, newUsername.trim());
            stmt.setInt(2, currentUser.getId());
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                // Update the session with new username
                currentUser.setUsername(newUsername.trim());
                SessionManager.getInstance().setCurrentUser(currentUser);
                logger.info("Username changed successfully to: {}", newUsername);
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error changing username", e);
        }
        
        return false;
    }
    
    /**
     * Changes the password for the current user.
     * Requires the current password for verification and confirmation of new password.
     * 
     * @param currentPassword the current password for verification
     * @param newPassword the new password to set
     * @param confirmPassword confirmation of the new password
     * @return true if password was changed successfully, false otherwise
     */
    public boolean changePassword(String currentPassword, String newPassword, String confirmPassword) {
        logger.info("Attempting to change password");
        
        // Validate new password length (minimum 6 characters)
        if (newPassword == null || newPassword.length() < 6) {
            logger.warn("Password change failed: new password is too short (minimum 6 characters)");
            return false;
        }
        
        // Verify password confirmation matches
        if (!newPassword.equals(confirmPassword)) {
            logger.warn("Password change failed: password confirmation does not match");
            return false;
        }
        
        // Get current user from session
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            logger.warn("Password change failed: no user in session");
            return false;
        }
        
        // Verify current password
        if (!verifyPassword(currentPassword, currentUser.getPassword())) {
            logger.warn("Password change failed: incorrect current password");
            return false;
        }
        
        // Hash new password
        String hashedPassword = hashPassword(newPassword);
        if (hashedPassword == null) {
            logger.error("Password change failed: error hashing password");
            return false;
        }
        
        // Update password in database
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, hashedPassword);
            stmt.setInt(2, currentUser.getId());
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                // Update the session with new password hash
                currentUser.setPassword(hashedPassword);
                SessionManager.getInstance().setCurrentUser(currentUser);
                logger.info("Password changed successfully");
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error changing password", e);
        }
        
        return false;
    }
    
    /**
     * Hashes a password using SHA-256.
     * 
     * @param password the password to hash
     * @return the hashed password as a hexadecimal string, or null if hashing fails
     */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            
            // Convert byte array to hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error("Error hashing password: SHA-256 algorithm not available", e);
            return null;
        }
    }
    
    /**
     * Verifies a password against a stored hash.
     * For backward compatibility, also supports plain text password comparison.
     * 
     * @param password the password to verify
     * @param storedPassword the stored password (either plain text or hash)
     * @return true if the password matches, false otherwise
     */
    private boolean verifyPassword(String password, String storedPassword) {
        if (password == null || storedPassword == null) {
            return false;
        }
        
        // First try hashed comparison
        String hashedPassword = hashPassword(password);
        if (hashedPassword != null && hashedPassword.equals(storedPassword)) {
            return true;
        }
        
        // Fall back to plain text comparison for backward compatibility
        return password.equals(storedPassword);
    }
}
