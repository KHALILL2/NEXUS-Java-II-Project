package com.nexus.ims;

import com.formdev.flatlaf.FlatLightLaf;
import com.nexus.ims.controller.AuthController;
import com.nexus.ims.service.AuthService;
import com.nexus.ims.view.LoginFrame;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main application bootstrap class.
 */
public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        // Configure FlatLaf Modern Look & Feel
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Failed to initialize modern FlatLaf theme, falling back to system defaults", ex);
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
        }

        // Launch UI on Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            AuthService authService = new AuthService();
            new AuthController(loginFrame, authService);
            loginFrame.setVisible(true);
        });
    }
}

