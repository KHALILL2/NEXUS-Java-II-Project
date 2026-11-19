package com.nexus.ims.view;

import com.nexus.ims.model.Supplier;
import com.nexus.ims.util.IconFactory;
import com.nexus.ims.util.LanguageManager;
import com.nexus.ims.util.ThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Modal form dialog for creating or editing a supplier record.
 */
public class SupplierFormDialog extends JDialog {
    private final Supplier existingSupplier;
    private boolean confirmed = false;

    private JTextField nameField;
    private JTextField contactPersonField;
    private JTextField emailField;
    private JTextField phoneField;
    private JTextArea addressArea;
    private JLabel errorLabel;

    public SupplierFormDialog(Frame owner, Supplier existingSupplier) {
        super(owner,
              existingSupplier == null
                      ? LanguageManager.get("suppliers.dialog.addTitle")
                      : LanguageManager.format("suppliers.dialog.editTitleFormat", existingSupplier.getName()),
              true);
        this.existingSupplier = existingSupplier;

        initComponents();
        populateExistingData();
        LanguageManager.applyOrientation(this);
        pack();
        setMinimumSize(new Dimension(490, 420));
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

        nameField = new JTextField(22);
        nameField.putClientProperty("JTextField.placeholderText", LanguageManager.get("suppliers.form.namePlaceholder"));
        addFormRow(formPanel, gbc, row++, LanguageManager.get("suppliers.form.name") + " *:", nameField);

        contactPersonField = new JTextField(22);
        contactPersonField.putClientProperty("JTextField.placeholderText", LanguageManager.get("suppliers.form.contactPlaceholder"));
        addFormRow(formPanel, gbc, row++, LanguageManager.get("suppliers.form.contact") + ":", contactPersonField);

        emailField = new JTextField(22);
        emailField.putClientProperty("JTextField.placeholderText", LanguageManager.get("suppliers.form.emailPlaceholder"));
        addFormRow(formPanel, gbc, row++, LanguageManager.get("suppliers.form.email") + ":", emailField);

        phoneField = new JTextField(22);
        phoneField.putClientProperty("JTextField.placeholderText", LanguageManager.get("suppliers.form.phonePlaceholder"));
        addFormRow(formPanel, gbc, row++, LanguageManager.get("suppliers.form.phone") + ":", phoneField);

        addressArea = new JTextArea(3, 22);
        addressArea.setLineWrap(true);
        addressArea.setWrapStyleWord(true);
        JScrollPane addressScroll = new JScrollPane(addressArea);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        JLabel addrLabel = new JLabel(LanguageManager.get("suppliers.form.address") + ":");
        addrLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        addrLabel.setForeground(ThemeManager.textSecondary());
        addrLabel.setHorizontalAlignment(SwingConstants.LEADING);
        formPanel.add(addrLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        formPanel.add(addressScroll, gbc);

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

        JButton saveButton = new JButton(existingSupplier == null
                ? LanguageManager.get("suppliers.form.save")
                : LanguageManager.get("suppliers.form.update"));
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
        gbc.weighty = 0.0;
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
        if (existingSupplier == null) {
            return;
        }
        nameField.setText(existingSupplier.getName() != null ? existingSupplier.getName() : "");
        contactPersonField.setText(existingSupplier.getContactPerson() != null ? existingSupplier.getContactPerson() : "");
        emailField.setText(existingSupplier.getEmail() != null ? existingSupplier.getEmail() : "");
        phoneField.setText(existingSupplier.getPhone() != null ? existingSupplier.getPhone() : "");
        addressArea.setText(existingSupplier.getAddress() != null ? existingSupplier.getAddress() : "");
    }

    private void handleSave() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showValidationError(LanguageManager.get("suppliers.validation.nameRequired"));
            nameField.requestFocusInWindow();
            return;
        }

        String email = emailField.getText().trim();
        if (!email.isEmpty() && (!email.contains("@") || !email.contains("."))) {
            showValidationError(LanguageManager.get("suppliers.validation.emailInvalid"));
            emailField.requestFocusInWindow();
            return;
        }

        confirmed = true;
        dispose();
    }

    private void showValidationError(String message) {
        errorLabel.setIcon(IconFactory.alert(15, ThemeManager.accentDanger()));
        errorLabel.setForeground(ThemeManager.accentDanger());
        errorLabel.setText(message);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public Supplier getSupplier() {
        Supplier supplier = existingSupplier != null ? existingSupplier : new Supplier();
        supplier.setName(nameField.getText().trim());
        supplier.setContactPerson(emptyToNull(contactPersonField.getText()));
        supplier.setEmail(emptyToNull(emailField.getText()));
        supplier.setPhone(emptyToNull(phoneField.getText()));
        supplier.setAddress(emptyToNull(addressArea.getText()));
        return supplier;
    }

    private String emptyToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
