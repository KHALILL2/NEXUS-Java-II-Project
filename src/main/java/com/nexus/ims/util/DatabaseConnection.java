package com.nexus.ims.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton factory for MySQL JDBC connections configured via db.properties.
 */
public class DatabaseConnection {
    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());
    private static final String CONFIG_FILE = "db.properties";

    private static volatile DatabaseConnection instance;
    private final Properties properties = new Properties();

    private DatabaseConnection() {
        loadProperties();
        registerDriver();
    }

    /**
     * Thread-safe singleton accessor via double-checked locking.
     */
    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    private void loadProperties() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (inputStream == null) {
                String errorMsg = "Database configuration file '" + CONFIG_FILE + "' not found in classpath.";
                LOGGER.severe(errorMsg);
                throw new IllegalStateException(errorMsg);
            }
            properties.load(inputStream);
            LOGGER.info("Database properties loaded successfully from " + CONFIG_FILE);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to read database properties: " + e.getMessage(), e);
            throw new RuntimeException("Error initializing database configuration", e);
        }
    }

    private void registerDriver() {
        String driver = properties.getProperty("db.driver", "com.mysql.cj.jdbc.Driver");
        try {
            Class.forName(driver);
            LOGGER.info("JDBC Driver registered: " + driver);
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "MySQL JDBC Driver not found: " + driver, e);
            throw new RuntimeException("Missing MySQL JDBC Driver in classpath", e);
        }
    }

    public Connection createConnection() throws SQLException {
        String url = properties.getProperty("db.url");
        String username = properties.getProperty("db.username");
        String password = properties.getProperty("db.password");

        if (url == null || username == null) {
            throw new SQLException("Database connection URL or username is not configured properly in " + CONFIG_FILE);
        }

        try {
            Connection connection = DriverManager.getConnection(url, username, password);
            LOGGER.fine("New database connection created successfully.");
            return connection;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to connect to MySQL database at: " + url + " | Error: " + e.getMessage(), e);
            throw e;
        }
    }

    public static Connection getConnection() throws SQLException {
        return getInstance().createConnection();
    }

    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    LOGGER.fine("Database connection closed.");
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing database connection: " + e.getMessage(), e);
            }
        }
    }
}
