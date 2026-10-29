package com.nexus.ims.controller;

import com.nexus.ims.model.User;
import com.nexus.ims.service.AuthService;
import com.nexus.ims.view.LoginFrame;
import com.nexus.ims.view.MainFrame;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Controller handling user authentication flow and screen navigation.
 */
public class AuthController {
    private final LoginFrame loginFrame;
    private final AuthService authService;

    public AuthController(LoginFrame loginFrame, AuthService authService) {
        this.loginFrame = loginFrame;
        this.authService = authService;
        initController();
    }

    private void initController() {
        loginFrame.getLoginButton().addActionListener(this::handleLogin);
        loginFrame.getExitButton().addActionListener(e -> System.exit(0));
    }

    private void handleLogin(ActionEvent e) {
        String username = loginFrame.getUsername();
        String password = new String(loginFrame.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            loginFrame.showErrorMessage("Username and password must not be empty.");
            return;
        }

        try {
            Optional<User> userOpt = authService.authenticate(username, password);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                loginFrame.dispose();

                // Open Main Dashboard
                SwingUtilities.invokeLater(() -> {
                    MainFrame mainFrame = new MainFrame(user);
                    new ProductController(mainFrame);
                    mainFrame.setVisible(true);
                });
            } else {
                loginFrame.showErrorMessage("Invalid username or password.");
            }
        } catch (SQLException ex) {
            loginFrame.showErrorMessage("Database connection error: " + ex.getMessage());
        }
    }
}

