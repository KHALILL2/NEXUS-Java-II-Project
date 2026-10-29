package com.nexus.ims.util;

/**
 * Global application constants.
 */
public final class Constants {
    private Constants() {}

    public static final String APP_NAME = "NEXUS Inventory Management System";
    public static final String APP_VERSION = "1.0.0";
    
    // Config Keys
    public static final String DB_URL_KEY = "db.url";
    public static final String DB_USER_KEY = "db.username";
    public static final String DB_PASS_KEY = "db.password";

    // Defaults
    public static final String DEFAULT_CONFIG_FILE = "application.properties";
    public static final int DEFAULT_LOW_STOCK_THRESHOLD = 10;
}

