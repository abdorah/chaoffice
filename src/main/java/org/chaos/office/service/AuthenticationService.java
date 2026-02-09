package org.chaos.office.service;

import org.chaos.office.model.User;
import org.chaos.office.util.DatabaseConnection;
import org.chaos.office.util.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        String sql = "SELECT COUNT(*) FROM users WHERE username = ? AND password = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            stmt.setString(2, password);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    return count > 0;
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
}
