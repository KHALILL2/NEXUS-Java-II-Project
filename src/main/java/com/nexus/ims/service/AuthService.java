package com.nexus.ims.service;

import com.nexus.ims.dao.UserDao;
import com.nexus.ims.model.User;
import com.nexus.ims.util.PasswordHasher;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Service managing user authentication, credential verification, and user creation.
 */
public class AuthService {
    private final UserDao userDao;

    public AuthService() {
        this.userDao = new UserDao();
    }

    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * Authenticates a user given username and plaintext password.
     */
    public Optional<User> authenticate(String username, String plainPassword) throws SQLException {
        if (username == null || plainPassword == null) {
            return Optional.empty();
        }

        Optional<User> userOpt = userDao.findByUsername(username.trim());
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
        if (userDao.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already in use.");
        }

        User user = new User();
        user.setUsername(username.trim());
        user.setPasswordHash(PasswordHasher.hashPassword(plainPassword));
        user.setFullName(fullName);
        user.setEmail(email);
        user.setRole(role);
        user.setActive(true);

        return userDao.save(user);
    }
}

