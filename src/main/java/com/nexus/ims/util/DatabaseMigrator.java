package com.nexus.ims.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Standalone Java Database Migrator.
 * Reads and applies schema.sql and data.sql via JDBC without requiring the 'mysql' CLI binary.
 */
public class DatabaseMigrator {
    private static final Logger LOGGER = Logger.getLogger(DatabaseMigrator.class.getName());

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
        try (Connection conn = DriverManager.getConnection(rootUrl, user, pass);
             Statement stmt = conn.createStatement()) {

            System.out.println("[✓] Connected to MySQL successfully.");

            // 1. Execute schema.sql
            System.out.println("\n[*] Executing schema.sql (Creating database, tables, indexes)...");
            executeSqlScript(conn, "database/schema.sql");
            System.out.println("[✓] Schema applied successfully.");

            // 2. Execute data.sql
            System.out.println("\n[*] Executing data.sql (Seeding default users, categories, products)...");
            executeSqlScript(conn, "database/data.sql");
            System.out.println("[✓] Seed data inserted successfully.");

            System.out.println("\n==================================================");
            System.out.println(" [SUCCESS] Database migration completed!");
            System.out.println(" You can now run the application with: mvn exec:java");
            System.out.println("==================================================");

        } catch (Exception e) {
            System.err.println("\n[ERROR] Migration failed: " + e.getMessage());
            System.err.println("Please check that MySQL Server is running and credentials in 'db.properties' are correct.");
            LOGGER.log(Level.SEVERE, "Migration exception", e);
        }
    }

    private static void executeSqlScript(Connection conn, String resourcePath) throws Exception {
        InputStream stream = DatabaseMigrator.class.getClassLoader().getResourceAsStream(resourcePath);
        if (stream == null) {
            throw new IllegalArgumentException("Resource file not found: " + resourcePath);
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Strip SQL line comments
                String trimmed = line.trim();
                if (trimmed.startsWith("--") || trimmed.startsWith("//")) {
                    continue;
                }
                sb.append(line).append("\n");
            }
        }

        String fullSql = sb.toString();
        // Execute multi-query script or split on semicolons
        String[] statements = fullSql.split(";\\s*\\n");
        try (Statement stmt = conn.createStatement()) {
            for (String sql : statements) {
                String trimmedSql = sql.trim();
                if (!trimmedSql.isEmpty()) {
                    stmt.execute(trimmedSql);
                }
            }
        }
    }
}

