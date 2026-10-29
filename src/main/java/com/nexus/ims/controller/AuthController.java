package com.nexus.ims.controller;

import com.nexus.ims.model.User;
import com.nexus.ims.service.AuthService;
import com.nexus.ims.view.DashboardFrame;
import com.nexus.ims.view.LoginFrame;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Controller handling user authentication flow and screen navigation.
 * Validates credentials in the background to keep the UI responsive.
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
        loginFrame.addLoginListener(this::handleLogin);
        loginFrame.addExitListener(e -> System.exit(0));
    }

    private void handleLogin(ActionEvent e) {
        String username = loginFrame.getUsername();
        String password = new String(loginFrame.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            loginFrame.setErrorMessage("Username and password must not be empty.");
            return;
        }

        loginFrame.setLoading(true);
        loginFrame.setErrorMessage(null);

        // Perform authentication asynchronously so the UI thread doesn't freeze
        SwingWorker<Optional<User>, Void> authWorker = new SwingWorker<>() {
            private String failureReason = null;

            @Override
            protected Optional<User> doInBackground() {
                try {
                    return authService.authenticate(username, password);
                } catch (SQLException ex) {
                    failureReason = "Database Error: " + ex.getMessage();
                    return Optional.empty();
                } catch (Exception ex) {
                    failureReason = "Authentication failed: " + ex.getMessage();
                    return Optional.empty();
                }
            }

            @Override
            protected void done() {
                loginFrame.setLoading(false);
                try {
                    Optional<User> userOpt = get();
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        loginFrame.dispose();

                        // Launch Dashboard Shell with Authenticated User
                        SwingUtilities.invokeLater(() -> {
                            DashboardFrame dashboardFrame = new DashboardFrame(user);
                            new DashboardController(dashboardFrame);
                            dashboardFrame.setVisible(true);
                        });
                    } else {
                        if (failureReason != null) {
                            loginFrame.setErrorMessage(failureReason);
                        } else {
                            loginFrame.setErrorMessage("Invalid credentials or inactive account.");
                        }
                    }
                } catch (Exception ex) {
                    loginFrame.setErrorMessage("System error during login: " + ex.getMessage());
                }
            }
        };

        authWorker.execute();
    }
}
