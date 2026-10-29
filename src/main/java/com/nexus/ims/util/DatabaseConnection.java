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
 * Singleton database connection manager utilizing JDBC.
 */
public class DatabaseConnection {
    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());
    private static final Properties properties = new Properties();

    static {
        try (InputStream input = DatabaseConnection.class.getClassLoader().getResourceAsStream(Constants.DEFAULT_CONFIG_FILE)) {
            if (input != null) {
                properties.load(input);
            } else {
                LOGGER.warning("application.properties not found in classpath. Falling back to defaults / environment variables.");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load database configuration", e);
        }
    }

    private DatabaseConnection() {}

    /**
     * Obtains a new active connection to the MySQL database.
     *
     * @return active java.sql.Connection
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        String url = properties.getProperty(Constants.DB_URL_KEY, 
                "jdbc:mysql://localhost:3306/nexus_ims_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        String user = properties.getProperty(Constants.DB_USER_KEY, "root");
        String password = properties.getProperty(Constants.DB_PASS_KEY, "");

        return DriverManager.getConnection(url, user, password);
    }
}

