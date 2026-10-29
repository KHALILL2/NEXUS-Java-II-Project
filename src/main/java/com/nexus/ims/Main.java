package com.nexus.ims;

import com.formdev.flatlaf.FlatLightLaf;
import com.nexus.ims.controller.AuthController;
import com.nexus.ims.service.AuthService;
import com.nexus.ims.view.LoginFrame;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main desktop application bootstrap and UI lifecycle entry point.
 * Initializes FlatLaf modern styling and spawns the GUI onto the Event Dispatch Thread (EDT).
 */
public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        // 1. Configure System Properties for High-DPI screens
        System.setProperty("sun.java2d.uiScale.enabled", "true");
        System.setProperty("flatlaf.useWindowDecorations", "true");

        // 2. Initialize FlatLaf Modern Look and Feel
        setupLookAndFeel();

        // 3. Launch UI on the Swing Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            try {
                LOGGER.info("Starting NEXUS Inventory Management System Desktop Client...");

                // Initialize Core Services
                AuthService authService = new AuthService();

                // Initialize Views
                LoginFrame loginFrame = new LoginFrame();

                // Bind Controller
                new AuthController(loginFrame, authService);

                // Display Login Window
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
     * Attempts to install the modern FlatLaf theme with cross-platform fallbacks.
     */
    private static void setupLookAndFeel() {
        try {
            // Apply FlatLaf Light Theme
            FlatLightLaf.setup();

            // Set global font & component styling overrides
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("ProgressBar.arc", 8);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("ScrollBar.thumbArc", 8);
            UIManager.put("ScrollBar.width", 10);
            UIManager.put("Table.intercellSpacing", new Dimension(0, 1));
            UIManager.put("Table.rowHeight", 28);
            UIManager.put("TableHeader.height", 32);

            LOGGER.info("FlatLaf Modern Look & Feel applied successfully.");

        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "FlatLaf theme unavailable, applying System native Look & Feel.", ex);
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception fallbackEx) {
                LOGGER.log(Level.SEVERE, "Failed to apply system look and feel", fallbackEx);
            }
        }
    }
}
