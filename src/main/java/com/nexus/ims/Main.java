package com.nexus.ims;

import com.nexus.ims.controller.AuthController;
import com.nexus.ims.service.AuthService;
import com.nexus.ims.util.LanguageManager;
import com.nexus.ims.util.ThemeManager;
import com.nexus.ims.view.LoginFrame;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Application entry point. Initializes localization/theme settings and launches LoginFrame on the EDT.
 */
public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        System.setProperty("sun.java2d.uiScale.enabled", "true");
        System.setProperty("flatlaf.useWindowDecorations", "true");

        setupEnvironment();

        SwingUtilities.invokeLater(() -> {
            try {
                LOGGER.info("Starting NEXUS Inventory Management System Desktop Client...");
                AuthService authService = new AuthService();
                LoginFrame loginFrame = new LoginFrame();
                new AuthController(loginFrame, authService);
                loginFrame.setVisible(true);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Fatal error initializing application UI", e);
                JOptionPane.showMessageDialog(null,
                        "Fatal initialization error: " + e.getMessage(),
                        "Startup Failure",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    /**
     * Initializes persisted language/locale settings and the FlatLaf 3-mode theme engine.
     */
    private static void setupEnvironment() {
        try {
            LanguageManager.init();
            ThemeManager.init();
            LOGGER.info("Theme (" + ThemeManager.getMode() + ") & Language (" + LanguageManager.getLanguage() + ") initialized.");
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Failed to initialize custom theme engine, falling back to system Look & Feel.", ex);
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception fallbackEx) {
                LOGGER.log(Level.SEVERE, "Failed to apply system look and feel", fallbackEx);
            }
        }
    }
}
