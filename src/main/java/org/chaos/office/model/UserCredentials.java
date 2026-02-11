package org.chaos.office.model;

/**
 * UserCredentials model representing authentication credentials.
 * This is a simple immutable data class for username and password hash.
 */
public class UserCredentials {
    private final String username;
    private final String passwordHash;

    /**
     * Constructor with username and password hash.
     *
     * @param username     The username for authentication
     * @param passwordHash The hashed password
     */
    public UserCredentials(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    /**
     * Gets the username.
     *
     * @return The username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Gets the password hash.
     *
     * @return The hashed password
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    @Override
    public String toString() {
        return "UserCredentials{" +
                "username='" + username + '\'' +
                ", passwordHash='[PROTECTED]'" +
                '}';
    }
}
