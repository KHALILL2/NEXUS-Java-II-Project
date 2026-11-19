package com.nexus.ims.view;

import com.nexus.ims.model.Supplier;
import com.nexus.ims.util.IconFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Modal FlatLaf dialog supporting both creating a new Supplier and editing an existing Supplier.
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
              existingSupplier == null ? "Add New Supplier" : "Edit Supplier - " + existingSupplier.getName(),
              true);
        this.existingSupplier = existingSupplier;

        initComponents();
        populateExistingData();
        pack();
        setMinimumSize(new Dimension(490, 420));
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(18, 20, 16, 20));
        root.setBackground(Color.WHITE);

        errorLabel = new JLabel(" ");
        errorLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        errorLabel.setForeground(new Color(220, 53, 69));
        errorLabel.setIconTextGap(6);
        root.add(errorLabel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        nameField = new JTextField(22);
        nameField.putClientProperty("JTextField.placeholderText", "e.g. Dell Technologies Direct");
        addFormRow(formPanel, gbc, row++, "Supplier Name *:", nameField);

        contactPersonField = new JTextField(22);
        contactPersonField.putClientProperty("JTextField.placeholderText", "Primary account contact");
        addFormRow(formPanel, gbc, row++, "Contact Person:", contactPersonField);

        emailField = new JTextField(22);
        emailField.putClientProperty("JTextField.placeholderText", "orders@vendor.com");
        addFormRow(formPanel, gbc, row++, "Email:", emailField);

        phoneField = new JTextField(22);
        phoneField.putClientProperty("JTextField.placeholderText", "+1-800-555-0100");
        addFormRow(formPanel, gbc, row++, "Phone:", phoneField);

        addressArea = new JTextArea(3, 22);
        addressArea.setLineWrap(true);
        addressArea.setWrapStyleWord(true);
        JScrollPane addressScroll = new JScrollPane(addressArea);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        JLabel addrLabel = new JLabel("Address:");
        addrLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        addrLabel.setForeground(new Color(73, 80, 87));
        formPanel.add(addrLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        formPanel.add(addressScroll, gbc);

        root.add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        JButton saveButton = new JButton(existingSupplier == null ? "Save Supplier" : "Update Supplier");
        saveButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        saveButton.setBackground(new Color(13, 110, 253));
        saveButton.setForeground(Color.WHITE);
        saveButton.addActionListener(e -> handleSave());

        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        root.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(root);
        getRootPane().setDefaultButton(saveButton);
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String labelText, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(new Color(73, 80, 87));
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
            showValidationError("Supplier Name is required.");
            nameField.requestFocusInWindow();
            return;
        }

        String email = emailField.getText().trim();
        if (!email.isEmpty() && (!email.contains("@") || !email.contains("."))) {
            showValidationError("Please enter a valid email address.");
            emailField.requestFocusInWindow();
            return;
        }

        confirmed = true;
        dispose();
    }

    private void showValidationError(String message) {
        errorLabel.setIcon(IconFactory.alert(15, new Color(220, 53, 69)));
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
