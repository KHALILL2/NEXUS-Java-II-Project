package com.nexus.ims.view;

import com.nexus.ims.util.IconFactory;
import com.nexus.ims.util.LanguageManager;
import com.nexus.ims.util.LanguageManager.Language;
import com.nexus.ims.util.ThemeManager;
import com.nexus.ims.util.ThemeManager.ThemeMode;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Authentication window with theme/language selectors and EN/AR RTL support.
 */
public class LoginFrame extends JFrame {
    private JPanel rootPanel;
    private JPanel headerPanel;
    private JPanel formContainer;
    private JPanel footerPanel;

    private JLabel logoBadge;
    private JLabel subtitleLabel;
    private JLabel errorLabel;
    private JLabel usernamePrompt;
    private JLabel passwordPrompt;
    private JLabel footerLabel;

    private JComboBox<ThemeMode> themeCombo;
    private JComboBox<Language> languageCombo;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JCheckBox showPasswordCheckbox;
    private JButton loginButton;
    private JButton exitButton;
    private JProgressBar progressBar;

    private String lastErrorMessage = null;
    private boolean updatingControls = false;

    private final Runnable themeListener = this::applyThemeAndLanguage;
    private final Runnable languageListener = this::applyThemeAndLanguage;

    public LoginFrame() {
        initComponents();
        ThemeManager.addThemeListener(themeListener);
        LanguageManager.addLanguageListener(languageListener);
        applyThemeAndLanguage();
    }

    private void initComponents() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(460, 585);
        setLocationRelativeTo(null);
        setResizable(false);

        rootPanel = new JPanel(new BorderLayout());

        // Branded Header Panel with Quick Theme + Language Bar
        headerPanel = new JPanel(new BorderLayout(0, 14));
        headerPanel.setBorder(new EmptyBorder(14, 18, 24, 18));

        JPanel topControlsBar = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
        topControlsBar.setOpaque(false);

        themeCombo = new JComboBox<>(ThemeMode.values());
        themeCombo.setPreferredSize(new Dimension(115, 28));
        themeCombo.setFocusable(false);
        themeCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ThemeMode mode) {
                    label.setText(getThemeModeLabel(mode));
                    Color iconColor = isSelected ? Color.WHITE : ThemeManager.textPrimary();
                    label.setIcon(ThemeManager.modeIcon(mode, 14, iconColor));
                    label.setIconTextGap(6);
                }
                label.setHorizontalAlignment(SwingConstants.LEADING);
                return label;
            }
        });
        themeCombo.addActionListener(e -> {
            if (!updatingControls && themeCombo.getSelectedItem() instanceof ThemeMode mode) {
                ThemeManager.setMode(mode);
            }
        });

        languageCombo = new JComboBox<>(Language.values());
        languageCombo.setPreferredSize(new Dimension(108, 28));
        languageCombo.setFocusable(false);
        languageCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Language lang) {
                    label.setText(lang.getDisplayName());
                    Color iconColor = isSelected ? Color.WHITE : ThemeManager.accentPrimary();
                    label.setIcon(IconFactory.globe(14, iconColor));
                    label.setIconTextGap(6);
                }
                label.setHorizontalAlignment(SwingConstants.LEADING);
                return label;
            }
        });
        languageCombo.addActionListener(e -> {
            if (!updatingControls && languageCombo.getSelectedItem() instanceof Language lang) {
                LanguageManager.setLanguage(lang);
            }
        });

        topControlsBar.add(themeCombo);
        topControlsBar.add(languageCombo);
        headerPanel.add(topControlsBar, BorderLayout.NORTH);

        JPanel brandCenter = new JPanel();
        brandCenter.setOpaque(false);
        brandCenter.setLayout(new BoxLayout(brandCenter, BoxLayout.Y_AXIS));

        logoBadge = new JLabel("", SwingConstants.CENTER);
        logoBadge.setIconTextGap(12);
        logoBadge.setFont(new Font("Segoe UI", Font.BOLD, 22));
        logoBadge.setForeground(Color.WHITE);
        logoBadge.setAlignmentX(Component.CENTER_ALIGNMENT);

        subtitleLabel = new JLabel("", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(ThemeManager.sidebarTextMuted());
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        brandCenter.add(logoBadge);
        brandCenter.add(Box.createVerticalStrut(6));
        brandCenter.add(subtitleLabel);
        headerPanel.add(brandCenter, BorderLayout.CENTER);

        rootPanel.add(headerPanel, BorderLayout.NORTH);

        // Form Body Panel using GridBagLayout for automatic LTR/RTL alignment
        formContainer = new JPanel(new GridBagLayout());
        formContainer.setBorder(new EmptyBorder(20, 36, 18, 36));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.LINE_START;

        errorLabel = new JLabel(" ");
        errorLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        errorLabel.setIconTextGap(8);
        errorLabel.setPreferredSize(new Dimension(100, 32));
        errorLabel.setHorizontalAlignment(SwingConstants.LEADING);
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 10, 0);
        formContainer.add(errorLabel, gbc);

        usernamePrompt = new JLabel();
        usernamePrompt.setFont(new Font("Segoe UI", Font.BOLD, 12));
        usernamePrompt.setHorizontalAlignment(SwingConstants.LEADING);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 6, 0);
        formContainer.add(usernamePrompt, gbc);

        usernameField = new JTextField();
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        usernameField.setPreferredSize(new Dimension(100, 40));
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 16, 0);
        formContainer.add(usernameField, gbc);

        passwordPrompt = new JLabel();
        passwordPrompt.setFont(new Font("Segoe UI", Font.BOLD, 12));
        passwordPrompt.setHorizontalAlignment(SwingConstants.LEADING);
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 6, 0);
        formContainer.add(passwordPrompt, gbc);

        passwordField = new JPasswordField();
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordField.setPreferredSize(new Dimension(100, 40));
        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 10, 0);
        formContainer.add(passwordField, gbc);

        showPasswordCheckbox = new JCheckBox();
        showPasswordCheckbox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        showPasswordCheckbox.setOpaque(false);
        showPasswordCheckbox.setHorizontalAlignment(SwingConstants.LEADING);
        showPasswordCheckbox.addActionListener(e -> {
            if (showPasswordCheckbox.isSelected()) {
                passwordField.setEchoChar((char) 0);
            } else {
                passwordField.setEchoChar('*');
            }
        });
        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 20, 0);
        formContainer.add(showPasswordCheckbox, gbc);

        loginButton = new JButton();
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginButton.setPreferredSize(new Dimension(100, 42));
        gbc.gridy = 6;
        gbc.insets = new Insets(0, 0, 10, 0);
        formContainer.add(loginButton, gbc);

        exitButton = new JButton();
        exitButton.setIconTextGap(8);
        exitButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        exitButton.setFocusPainted(false);
        exitButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        exitButton.setPreferredSize(new Dimension(100, 38));
        gbc.gridy = 7;
        gbc.insets = new Insets(0, 0, 14, 0);
        formContainer.add(exitButton, gbc);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(100, 4));
        gbc.gridy = 8;
        gbc.insets = new Insets(0, 0, 0, 0);
        formContainer.add(progressBar, gbc);

        rootPanel.add(formContainer, BorderLayout.CENTER);

        footerPanel = new JPanel(new BorderLayout());
        footerLabel = new JLabel("", SwingConstants.CENTER);
        footerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        footerLabel.setBorder(new EmptyBorder(10, 10, 14, 10));
        footerPanel.add(footerLabel, BorderLayout.CENTER);
        rootPanel.add(footerPanel, BorderLayout.SOUTH);

        setContentPane(rootPanel);
        getRootPane().setDefaultButton(loginButton);
    }

    private void applyThemeAndLanguage() {
        updatingControls = true;
        try {
            setTitle(LanguageManager.get("login.title"));
            logoBadge.setText(LanguageManager.get("login.heading"));
            logoBadge.setIcon(IconFactory.brandLogo(28, ThemeManager.accentPrimary()));
            subtitleLabel.setText(LanguageManager.get("login.subheading"));
            subtitleLabel.setForeground(ThemeManager.sidebarTextMuted());

            usernamePrompt.setText(LanguageManager.get("login.username"));
            usernamePrompt.setForeground(ThemeManager.textSecondary());
            usernameField.putClientProperty("JTextField.placeholderText", LanguageManager.get("login.username"));

            passwordPrompt.setText(LanguageManager.get("login.password"));
            passwordPrompt.setForeground(ThemeManager.textSecondary());
            passwordField.putClientProperty("JTextField.placeholderText", LanguageManager.get("login.password"));

            showPasswordCheckbox.setText(LanguageManager.get("login.showPassword"));
            showPasswordCheckbox.setForeground(ThemeManager.textSecondary());

            loginButton.setText(LanguageManager.get("login.signIn"));
            loginButton.setBackground(ThemeManager.accentPrimary());
            loginButton.setForeground(Color.WHITE);

            exitButton.setText(LanguageManager.get("login.exit"));
            exitButton.setBackground(ThemeManager.btnSecondaryBg());
            exitButton.setForeground(ThemeManager.btnSecondaryFg());
            exitButton.setIcon(IconFactory.signOut(15, ThemeManager.btnSecondaryFg()));

            footerLabel.setText(LanguageManager.get("login.footer"));
            footerLabel.setForeground(ThemeManager.textSecondary());

            rootPanel.setBackground(ThemeManager.bgCanvas());
            headerPanel.setBackground(ThemeManager.bgSidebar());
            formContainer.setBackground(ThemeManager.bgSurface());
            footerPanel.setBackground(ThemeManager.bgCanvas());
            footerPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeManager.borderSubtle()));

            errorLabel.setForeground(ThemeManager.accentDanger());
            if (lastErrorMessage != null && !lastErrorMessage.trim().isEmpty()) {
                errorLabel.setIcon(IconFactory.alert(16, ThemeManager.accentDanger()));
            }

            themeCombo.setSelectedItem(ThemeManager.getMode());
            languageCombo.setSelectedItem(LanguageManager.getLanguage());
        } finally {
            updatingControls = false;
        }

        LanguageManager.applyOrientation(this);
        themeCombo.repaint();
        languageCombo.repaint();
    }

    private String getThemeModeLabel(ThemeMode mode) {
        return switch (mode) {
            case LIGHT -> LanguageManager.get("theme.light");
            case DARK -> LanguageManager.get("theme.dark");
            case SYSTEM -> LanguageManager.get("theme.system");
        };
    }

    public String getUsername() {
        return usernameField.getText().trim();
    }

    public char[] getPassword() {
        return passwordField.getPassword();
    }

    public void setErrorMessage(String message) {
        this.lastErrorMessage = message;
        if (message == null || message.trim().isEmpty()) {
            errorLabel.setIcon(null);
            errorLabel.setText(" ");
            errorLabel.setToolTipText(null);
        } else {
            errorLabel.setIcon(IconFactory.alert(16, ThemeManager.accentDanger()));
            errorLabel.setForeground(ThemeManager.accentDanger());
            errorLabel.setText("<html>" + message + "</html>");
            errorLabel.setToolTipText(message);
        }
    }

    public void setLoading(boolean isLoading) {
        progressBar.setVisible(isLoading);
        loginButton.setEnabled(!isLoading);
        loginButton.setText(isLoading ? LanguageManager.get("login.signingIn") : LanguageManager.get("login.signIn"));
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
        setErrorMessage(null);
        usernameField.requestFocusInWindow();
    }

    @Override
    public void dispose() {
        ThemeManager.removeThemeListener(themeListener);
        LanguageManager.removeLanguageListener(languageListener);
        super.dispose();
    }
}
