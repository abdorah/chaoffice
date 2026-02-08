package org.chaos.office.util;

import org.chaos.office.model.User;

/**
 * SessionManager manages the current user session in the application.
 * Implements the singleton pattern to ensure a single session instance across the application.
 * 
 * This class is responsible for:
 * - Storing the currently authenticated user
 * - Providing access to user information throughout the application
 * - Managing session lifecycle (login/logout)
 */
public class SessionManager {
    private static SessionManager instance;
    private User currentUser;

    /**
     * Private constructor to enforce singleton pattern.
     */
    private SessionManager() {
        this.currentUser = null;
    }

    /**
     * Gets the singleton instance of SessionManager.
     * Creates the instance if it doesn't exist.
     *
     * @return The singleton SessionManager instance
     */
    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /**
     * Sets the current authenticated user.
     * This method should be called after successful authentication.
     *
     * @param user The authenticated user to set as current
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    /**
     * Gets the current authenticated user.
     *
     * @return The current user, or null if no user is authenticated
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Checks if a user is currently authenticated.
     *
     * @return true if a user is authenticated, false otherwise
     */
    public boolean isAuthenticated() {
        return currentUser != null;
    }

    /**
     * Clears the current session by removing the authenticated user.
     * This method should be called during logout.
     */
    public void clearSession() {
        this.currentUser = null;
    }
}
