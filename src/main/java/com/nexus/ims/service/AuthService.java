package com.nexus.ims.service;

import com.nexus.ims.dao.UserDAO;
import com.nexus.ims.model.User;
import com.nexus.ims.util.DatabaseConnection;
import com.nexus.ims.util.DatabaseMigrator;
import com.nexus.ims.util.PasswordHasher;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Handles user login verification and account registration.
 */
public class AuthService {
    private final UserDAO userDAO;

    public AuthService() {
        this.userDAO = new UserDAO();
        DatabaseMigrator.ensureDefaultUsersSeededQuietly();
    }

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Authenticates a user given username and plaintext password.
     */
    public Optional<User> authenticate(String username, String plainPassword) throws SQLException {
        if (username == null || plainPassword == null) {
            return Optional.empty();
        }

        String normalizedUsername = username.trim().toLowerCase();
        if (normalizedUsername.isEmpty() || plainPassword.isEmpty()) {
            return Optional.empty();
        }

        // Verify database connectivity so connection errors propagate distinctly from invalid credentials
        try (Connection ignored = DatabaseConnection.getConnection()) {
            // Connection verified
        }

        Optional<User> userOpt = userDAO.findByUsername(normalizedUsername);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.isActive() && PasswordHasher.verifyPassword(plainPassword, user.getPasswordHash())) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    /**
     * Registers a new user with BCrypt hashed password.
     */
    public boolean registerUser(String username, String plainPassword, String fullName,
                                String email, User.Role role) throws SQLException {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty.");
        }

        String normalizedUsername = username.trim().toLowerCase();
        if (userDAO.findByUsername(normalizedUsername).isPresent()) {
            throw new IllegalArgumentException("Username already in use.");
        }

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setPasswordHash(PasswordHasher.hashPassword(plainPassword));
        user.setFullName(fullName);
        user.setEmail(email);
        user.setRole(role);
        user.setActive(true);

        return userDAO.save(user);
    }
}
