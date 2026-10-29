package com.nexus.ims.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Modern authentication dialog for the NEXUS Inventory Management System.
 * Fully decoupled view exposing event-registration and input accessors to the controller.
 */
public class LoginFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JCheckBox showPasswordCheckbox;
    private JButton loginButton;
    private JButton exitButton;
    private JLabel errorLabel;
    private JProgressBar progressBar;

    public LoginFrame() {
        initComponents();
    }

    private void initComponents() {
        setTitle("NEXUS IMS - Secure Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(440, 520);
        setLocationRelativeTo(null);
        setResizable(false);

        // Root Background Container
        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBackground(new Color(248, 249, 250));

        // Header Panel (Branding & Identity)
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(new Color(33, 37, 41)); // Deep Charcoal Accent
        headerPanel.setBorder(new EmptyBorder(30, 25, 25, 25));

        JLabel logoBadge = new JLabel("◈ NEXUS IMS", SwingConstants.CENTER);
        logoBadge.setFont(new Font("Segoe UI", Font.BOLD, 22));
        logoBadge.setForeground(Color.WHITE);
        logoBadge.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Enterprise Inventory Management System", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(173, 181, 189));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(logoBadge);
        headerPanel.add(Box.createVerticalStrut(6));
        headerPanel.add(subtitleLabel);
        rootPanel.add(headerPanel, BorderLayout.NORTH);

        // Body Form Panel
        JPanel formContainer = new JPanel();
        formContainer.setLayout(new BoxLayout(formContainer, BoxLayout.Y_AXIS));
        formContainer.setBackground(Color.WHITE);
        formContainer.setBorder(new EmptyBorder(25, 35, 20, 35));

        // Error message banner (invisible by default)
        errorLabel = new JLabel(" ");
        errorLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        errorLabel.setForeground(new Color(220, 53, 69));
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(errorLabel);
        formContainer.add(Box.createVerticalStrut(10));

        // Username Field
        JLabel usernamePrompt = new JLabel("Username");
        usernamePrompt.setFont(new Font("Segoe UI", Font.BOLD, 12));
        usernamePrompt.setForeground(new Color(73, 80, 87));
        usernamePrompt.setAlignmentX(Component.LEFT_ALIGNMENT);

        usernameField = new JTextField();
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);

        formContainer.add(usernamePrompt);
        formContainer.add(Box.createVerticalStrut(6));
        formContainer.add(usernameField);
        formContainer.add(Box.createVerticalStrut(16));

        // Password Field
        JLabel passwordPrompt = new JLabel("Password");
        passwordPrompt.setFont(new Font("Segoe UI", Font.BOLD, 12));
        passwordPrompt.setForeground(new Color(73, 80, 87));
        passwordPrompt.setAlignmentX(Component.LEFT_ALIGNMENT);

        passwordField = new JPasswordField();
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);

        formContainer.add(passwordPrompt);
        formContainer.add(Box.createVerticalStrut(6));
        formContainer.add(passwordField);
        formContainer.add(Box.createVerticalStrut(8));

        // Show Password Checkbox
        showPasswordCheckbox = new JCheckBox("Show Password");
        showPasswordCheckbox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        showPasswordCheckbox.setOpaque(false);
        showPasswordCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        showPasswordCheckbox.addActionListener(e -> {
            if (showPasswordCheckbox.isSelected()) {
                passwordField.setEchoChar((char) 0);
            } else {
                passwordField.setEchoChar('•');
            }
        });
        formContainer.add(showPasswordCheckbox);
        formContainer.add(Box.createVerticalStrut(20));

        // Sign In Action Button
        loginButton = new JButton("Sign In");
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginButton.setBackground(new Color(13, 110, 253));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        loginButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Exit Button
        exitButton = new JButton("Exit");
        exitButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        exitButton.setFocusPainted(false);
        exitButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        exitButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        exitButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        formContainer.add(loginButton);
        formContainer.add(Box.createVerticalStrut(10));
        formContainer.add(exitButton);
        formContainer.add(Box.createVerticalStrut(15));

        // Loading Progress Bar
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 4));
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(progressBar);

        rootPanel.add(formContainer, BorderLayout.CENTER);

        // Footer info
        JLabel footerLabel = new JLabel("NEXUS IMS v1.0.0 • Java II Architecture", SwingConstants.CENTER);
        footerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        footerLabel.setForeground(new Color(108, 117, 125));
        footerLabel.setBorder(new EmptyBorder(10, 10, 15, 10));
        rootPanel.add(footerLabel, BorderLayout.SOUTH);

        setContentPane(rootPanel);

        // Set default enter key action
        getRootPane().setDefaultButton(loginButton);
    }

    // Accessors for Controller Delegation
    public String getUsername() {
        return usernameField.getText().trim();
    }

    public char[] getPassword() {
        return passwordField.getPassword();
    }

    public void setErrorMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            errorLabel.setText(" ");
        } else {
            errorLabel.setText("⚠ " + message);
        }
    }

    public void setLoading(boolean isLoading) {
        progressBar.setVisible(isLoading);
        loginButton.setEnabled(!isLoading);
        usernameField.setEnabled(!isLoading);
        passwordField.setEnabled(!isLoading);
    }

    public void addLoginListener(ActionListener listener) {
        loginButton.addActionListener(listener);
    }

    public void addExitListener(ActionListener listener) {
        exitButton.addActionListener(listener);
    }

    public void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
        errorLabel.setText(" ");
        usernameField.requestFocusInWindow();
    }
}
