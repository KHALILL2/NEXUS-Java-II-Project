package com.nexus.ims.view;

import com.nexus.ims.model.User;
import com.nexus.ims.util.IconFactory;
import com.nexus.ims.util.LanguageManager;
import com.nexus.ims.util.ThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Modal form dialog for creating or updating operator accounts and RBAC roles.
 */
public class UserFormDialog extends JDialog {
    private final User existingUser;
    private final boolean editingSelf;
    private boolean confirmed = false;

    private JTextField usernameField;
    private JTextField fullNameField;
    private JTextField emailField;
    private JComboBox<User.Role> roleCombo;
    private JPasswordField passwordField;
    private JCheckBox activeCheckBox;
    private JLabel errorLabel;

    public UserFormDialog(Frame owner, User existingUser, int currentAdminUserId) {
        super(owner,
              existingUser == null
                      ? LanguageManager.get("users.dialog.addTitle")
                      : LanguageManager.format("users.dialog.editTitleFormat", existingUser.getUsername()),
              true);
        this.existingUser = existingUser;
        this.editingSelf = existingUser != null && existingUser.getId() == currentAdminUserId;

        initComponents();
        populateExistingData();
        LanguageManager.applyOrientation(this);
        pack();
        setMinimumSize(new Dimension(470, existingUser == null ? 420 : 370));
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(18, 20, 16, 20));
        root.setBackground(ThemeManager.bgSurface());

        errorLabel = new JLabel(" ");
        errorLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        errorLabel.setForeground(ThemeManager.accentDanger());
        errorLabel.setIconTextGap(6);
        errorLabel.setHorizontalAlignment(SwingConstants.LEADING);
        root.add(errorLabel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.LINE_START;

        int row = 0;

        usernameField = new JTextField(20);
        usernameField.putClientProperty("JTextField.placeholderText", LanguageManager.get("users.form.usernamePlaceholder"));
        addFormRow(formPanel, gbc, row++, LanguageManager.get("users.form.username") + " *:", usernameField);

        fullNameField = new JTextField(20);
        fullNameField.putClientProperty("JTextField.placeholderText", LanguageManager.get("users.form.fullNamePlaceholder"));
        addFormRow(formPanel, gbc, row++, LanguageManager.get("users.form.fullName") + " *:", fullNameField);

        emailField = new JTextField(20);
        emailField.putClientProperty("JTextField.placeholderText", LanguageManager.get("users.form.emailPlaceholder"));
        addFormRow(formPanel, gbc, row++, LanguageManager.get("users.form.email") + ":", emailField);

        roleCombo = new JComboBox<>(User.Role.values());
        roleCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof User.Role r) {
                    label.setText(LanguageManager.get("role." + r.name()));
                }
                label.setHorizontalAlignment(SwingConstants.LEADING);
                return label;
            }
        });
        addFormRow(formPanel, gbc, row++, LanguageManager.get("users.form.role") + " *:", roleCombo);

        if (existingUser == null) {
            passwordField = new JPasswordField(20);
            passwordField.putClientProperty("JTextField.placeholderText", LanguageManager.get("users.form.passwordPlaceholder"));
            addFormRow(formPanel, gbc, row++, LanguageManager.get("users.form.password") + " *:", passwordField);
        }

        activeCheckBox = new JCheckBox(LanguageManager.get("users.form.activeFull"), true);
        activeCheckBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        activeCheckBox.setForeground(ThemeManager.textPrimary());
        activeCheckBox.setOpaque(false);
        activeCheckBox.setHorizontalAlignment(SwingConstants.LEADING);
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 1.0;
        formPanel.add(activeCheckBox, gbc);

        if (editingSelf) {
            roleCombo.setEnabled(false);
            roleCombo.setToolTipText(LanguageManager.get("users.form.selfDemoteTooltip"));
            activeCheckBox.setEnabled(false);
            activeCheckBox.setToolTipText(LanguageManager.get("users.cannotDeactivateSelf"));
        }

        root.add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 10, 0));
        buttonPanel.setOpaque(false);

        JButton cancelButton = new JButton(LanguageManager.get("common.cancel"));
        cancelButton.setBackground(ThemeManager.btnSecondaryBg());
        cancelButton.setForeground(ThemeManager.btnSecondaryFg());
        cancelButton.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        JButton saveButton = new JButton(existingUser == null
                ? LanguageManager.get("users.form.save")
                : LanguageManager.get("users.form.update"));
        saveButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        saveButton.setBackground(ThemeManager.accentPrimary());
        saveButton.setForeground(Color.WHITE);
        saveButton.addActionListener(e -> handleSave());

        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        root.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(root);
        getRootPane().setDefaultButton(saveButton);
        getRootPane().registerKeyboardAction(
                e -> {
                    confirmed = false;
                    dispose();
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String labelText, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.LINE_START;

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(ThemeManager.textSecondary());
        label.setHorizontalAlignment(SwingConstants.LEADING);
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(field, gbc);
    }

    private void populateExistingData() {
        if (existingUser == null) {
            return;
        }
        usernameField.setText(existingUser.getUsername());
        usernameField.setEnabled(false);
        fullNameField.setText(existingUser.getFullName());
        emailField.setText(existingUser.getEmail() != null ? existingUser.getEmail() : "");
        roleCombo.setSelectedItem(existingUser.getRole());
        activeCheckBox.setSelected(existingUser.isActive());
    }

    private void handleSave() {
        String username = usernameField.getText().trim().toLowerCase();
        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim().toLowerCase();

        if (username.isEmpty() || username.contains(" ")) {
            showValidationError(LanguageManager.get("users.validation.usernameNoSpaces"));
            usernameField.requestFocusInWindow();
            return;
        }
        if (fullName.isEmpty()) {
            showValidationError(LanguageManager.get("users.validation.fullNameRequired"));
            fullNameField.requestFocusInWindow();
            return;
        }
        if (!email.isEmpty() && (!email.contains("@") || !email.contains("."))) {
            showValidationError(LanguageManager.get("suppliers.validation.emailInvalid"));
            emailField.requestFocusInWindow();
            return;
        }

        if (existingUser == null) {
            String pwd = new String(passwordField.getPassword());
            if (pwd.length() < 6) {
                showValidationError(LanguageManager.get("users.validation.passwordMin"));
                passwordField.requestFocusInWindow();
                return;
            }
        }

        confirmed = true;
        dispose();
    }

    private void showValidationError(String msg) {
        errorLabel.setIcon(IconFactory.alert(15, ThemeManager.accentDanger()));
        errorLabel.setForeground(ThemeManager.accentDanger());
        errorLabel.setText(msg);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public User getUser() {
        User user = existingUser != null ? existingUser : new User();
        String normalizedUsername = usernameField.getText().trim().toLowerCase();
        user.setUsername(normalizedUsername);
        user.setFullName(fullNameField.getText().trim());

        String rawEmail = emailField.getText().trim().toLowerCase();
        user.setEmail(rawEmail.isEmpty() ? normalizedUsername + "@nexus.local" : rawEmail);

        if (!editingSelf) {
            user.setRole((User.Role) roleCombo.getSelectedItem());
            user.setActive(activeCheckBox.isSelected());
        }
        if (existingUser == null) {
            user.setPasswordHash(new String(passwordField.getPassword()));
        }
        return user;
    }
}
