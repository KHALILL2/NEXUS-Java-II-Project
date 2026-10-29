package com.nexus.ims.util;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * Utility for hashing and verifying credentials using modern BCrypt algorithm.
 */
public final class PasswordHasher {
    private static final int COST_FACTOR = 12;

    private PasswordHasher() {}

    /**
     * Hashes a raw plaintext password using BCrypt with salt.
     *
     * @param plainPassword plaintext char array or string
     * @return salted hash string
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        return BCrypt.withDefaults().hashToString(COST_FACTOR, plainPassword.toCharArray());
    }

    /**
     * Verifies a candidate password against an existing BCrypt hash.
     *
     * @param candidatePassword candidate password
     * @param hashedPassword stored hash string
     * @return true if credentials match, false otherwise
     */
    public static boolean verifyPassword(String candidatePassword, String hashedPassword) {
        if (candidatePassword == null || hashedPassword == null) {
            return false;
        }
        BCrypt.Result result = BCrypt.verifyer().verify(candidatePassword.toCharArray(), hashedPassword);
        return result.verified;
    }
}

