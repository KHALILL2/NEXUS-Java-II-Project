package com.nexus.ims.util;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * Password hashing and constant-time verification via BCrypt (cost factor 12).
 */
public final class PasswordHasher {
    private static final int COST_FACTOR = 12;

    private PasswordHasher() {
    }

    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        return BCrypt.withDefaults().hashToString(COST_FACTOR, plainPassword.toCharArray());
    }

    public static boolean verifyPassword(String candidatePassword, String hashedPassword) {
        if (candidatePassword == null || hashedPassword == null) {
            return false;
        }
        BCrypt.Result result = BCrypt.verifyer().verify(candidatePassword.toCharArray(), hashedPassword);
        return result.verified;
    }
}
