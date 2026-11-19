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
 * Configures FlatLaf styling and spawns the GUI onto the Event Dispatch Thread (EDT).
 */
public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        System.setProperty("sun.java2d.uiScale.enabled", "true");
        System.setProperty("flatlaf.useWindowDecorations", "true");

        setupLookAndFeel();

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
     * Configures global FlatLaf Look & Feel and visual defaults before any UI components are instantiated.
     */
    private static void setupLookAndFeel() {
        try {
            FlatLightLaf.setup();

            Font baseFont = new Font("Segoe UI", Font.PLAIN, 13);
            UIManager.put("defaultFont", baseFont);

            // Rounded geometry
            UIManager.put("Button.arc", 10);
            UIManager.put("Component.arc", 10);
            UIManager.put("ProgressBar.arc", 10);
            UIManager.put("TextComponent.arc", 10);

            // Clean modern scrollbars
            UIManager.put("ScrollBar.width", 10);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));

            // Modern data table styling
            UIManager.put("Table.rowHeight", 38);
            UIManager.put("Table.showHorizontalLines", true);
            UIManager.put("Table.showVerticalLines", false);
            UIManager.put("Table.intercellSpacing", new Dimension(0, 1));
            UIManager.put("Table.gridColor", new Color(233, 236, 239));
            UIManager.put("Table.selectionBackground", new Color(231, 241, 255));
            UIManager.put("Table.selectionForeground", new Color(13, 110, 253));
            UIManager.put("TableHeader.height", 40);
            UIManager.put("TableHeader.background", new Color(248, 249, 250));
            UIManager.put("TableHeader.foreground", new Color(73, 80, 87));
            UIManager.put("TableHeader.font", new Font("Segoe UI", Font.BOLD, 12));

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
