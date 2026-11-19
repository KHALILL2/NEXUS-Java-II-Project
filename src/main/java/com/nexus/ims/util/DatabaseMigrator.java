package com.nexus.ims.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Standalone Java Database Migrator.
 * Reads and applies schema.sql and data.sql via JDBC without requiring the 'mysql' CLI binary,
 * and ensures default user accounts have valid cryptographic BCrypt password hashes.
 */
public class DatabaseMigrator {
    private static final Logger LOGGER = Logger.getLogger(DatabaseMigrator.class.getName());
    private static volatile boolean defaultUsersVerified = false;

    private static final String[][] DEFAULT_USERS = {
        {"admin", "admin123", "System Administrator", "admin@nexus.local", "ADMIN"},
        {"manager", "manager123", "Inventory Manager", "manager@nexus.local", "MANAGER"},
        {"staff", "staff123", "Warehouse Staff", "staff@nexus.local", "STAFF"}
    };

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   NEXUS IMS - Automated Java Database Migrator   ");
        System.out.println("==================================================");

        Properties props = new Properties();
        try (InputStream in = DatabaseMigrator.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                System.err.println("[ERROR] 'db.properties' not found in classpath.");
                return;
            }
            props.load(in);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load db.properties", e);
            return;
        }

        String rawUrl = props.getProperty("db.url");
        String user = props.getProperty("db.username");
        String pass = props.getProperty("db.password");

        // Connect to server root (without database name) to ensure database creation
        String rootUrl = "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&allowMultiQueries=true";
        if (rawUrl != null && rawUrl.contains("3306")) {
            int qIndex = rawUrl.indexOf('?');
            String params = (qIndex != -1) ? rawUrl.substring(qIndex) : "";
            if (!params.contains("allowMultiQueries=true")) {
                params = params.isEmpty() ? "?allowMultiQueries=true" : params + "&allowMultiQueries=true";
            }
            rootUrl = "jdbc:mysql://localhost:3306/" + params;
        }

        System.out.println("[*] Connecting to MySQL Server...");
        try (Connection conn = DriverManager.getConnection(rootUrl, user, pass)) {
            System.out.println("[✓] Connected to MySQL successfully.");

            // 1. Execute schema.sql
            System.out.println("\n[*] Executing schema.sql (Creating database, tables, indexes)...");
            executeSqlScript(conn, "database/schema.sql");
            System.out.println("[✓] Schema applied successfully.");

            // 2. Execute data.sql
            System.out.println("\n[*] Executing data.sql (Seeding default users, categories, products)...");
            executeSqlScript(conn, "database/data.sql");
            System.out.println("[✓] Seed data inserted successfully.");

            // 3. Verify and repair BCrypt password hashes for default accounts
            System.out.println("\n[*] Verifying BCrypt credential hashes for default accounts...");
            ensureDefaultUsersSeeded(conn);
            System.out.println("[✓] Default user credentials verified/updated.");

            System.out.println("\n==================================================");
            System.out.println(" [SUCCESS] Database migration completed!");
            System.out.println(" You can now run the application with: .\\mvnw.cmd exec:java");
            System.out.println("==================================================");

        } catch (Exception e) {
            System.err.println("\n[ERROR] Migration failed: " + e.getMessage());
            System.err.println("Please check that MySQL Server is running and credentials in 'db.properties' are correct.");
            LOGGER.log(Level.SEVERE, "Migration exception", e);
        }
    }

    /**
     * Quietly verifies and repairs default user BCrypt hashes once per application session.
     * Safe to call during startup even if the database is temporarily unavailable.
     */
    public static void ensureDefaultUsersSeededQuietly() {
        if (defaultUsersVerified) {
            return;
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            ensureDefaultUsersSeeded(conn);
            defaultUsersVerified = true;
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Skipped startup user credential verification: " + e.getMessage(), e);
        }
    }

    /**
     * Ensures admin, manager, and staff exist with valid BCrypt hashes matching
     * their expected default passwords. Repairs placeholder or corrupted hashes at runtime.
     */
    public static void ensureDefaultUsersSeeded(Connection conn) throws SQLException {
        try (Statement useStmt = conn.createStatement()) {
            useStmt.execute("USE nexus_ims_db");
        }

        String selectSql = "SELECT id, password_hash FROM users WHERE username = ?";
        String updateSql = "UPDATE users SET password_hash = ?, is_active = TRUE WHERE username = ?";
        String insertSql = "INSERT INTO users (username, password_hash, full_name, email, role, is_active) " +
                           "VALUES (?, ?, ?, ?, ?, TRUE)";

        for (String[] userSpec : DEFAULT_USERS) {
            String username = userSpec[0];
            String rawPassword = userSpec[1];
            String fullName = userSpec[2];
            String email = userSpec[3];
            String role = userSpec[4];

            boolean exists = false;
            String existingHash = null;

            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setString(1, username);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        exists = true;
                        existingHash = rs.getString("password_hash");
                    }
                }
            }

            if (exists) {
                if (!PasswordHasher.verifyPassword(rawPassword, existingHash)) {
                    String validHash = PasswordHasher.hashPassword(rawPassword);
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setString(1, validHash);
                        updateStmt.setString(2, username);
                        updateStmt.executeUpdate();
                    }
                    LOGGER.info("Repaired invalid BCrypt password hash for default user: " + username);
                }
            } else {
                String validHash = PasswordHasher.hashPassword(rawPassword);
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setString(1, username);
                    insertStmt.setString(2, validHash);
                    insertStmt.setString(3, fullName);
                    insertStmt.setString(4, email);
                    insertStmt.setString(5, role);
                    insertStmt.executeUpdate();
                }
                LOGGER.info("Inserted missing default user: " + username);
            }
        }
        defaultUsersVerified = true;
    }

    private static void executeSqlScript(Connection conn, String resourcePath) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (InputStream stream = DatabaseMigrator.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalArgumentException("Resource file not found: " + resourcePath);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("--") || trimmed.startsWith("//")) {
                        continue;
                    }
                    String cleaned = stripInlineSqlComment(line);
                    if (!cleaned.trim().isEmpty()) {
                        sb.append(cleaned).append("\n");
                    }
                }
            }
        }

        String fullSql = sb.toString();
        String[] statements = fullSql.split(";");
        try (Statement stmt = conn.createStatement()) {
            for (String sql : statements) {
                String trimmedSql = sql.trim();
                if (!trimmedSql.isEmpty()) {
                    stmt.execute(trimmedSql);
                }
            }
        }
    }

    /**
     * Strips inline '--' SQL comments while preserving '--' inside single-quoted string literals.
     */
    private static String stripInlineSqlComment(String line) {
        boolean inSingleQuote = false;
        int len = line.length();
        for (int i = 0; i < len; i++) {
            char c = line.charAt(i);
            if (c == '\'' && (i == 0 || line.charAt(i - 1) != '\\')) {
                inSingleQuote = !inSingleQuote;
            } else if (!inSingleQuote && c == '-' && i + 1 < len && line.charAt(i + 1) == '-') {
                return line.substring(0, i);
            }
        }
        return line;
    }
}
