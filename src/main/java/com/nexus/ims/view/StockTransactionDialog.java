package com.nexus.ims.view;

import com.nexus.ims.model.Product;
import com.nexus.ims.model.StockTransaction;
import com.nexus.ims.util.IconFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * Dedicated modal dialog for Stock IN (receipts/restocks) and Stock OUT (sales/write-offs).
 * Allows direct product selection via dropdown without requiring a pre-highlighted row in another tab.
 */
public class StockTransactionDialog extends JDialog {
    private final boolean inboundMode;
    private boolean confirmed = false;

    private JComboBox<Product> productCombo;
    private JComboBox<StockTransaction.TransactionType> typeCombo;
    private JSpinner quantitySpinner;
    private JTextArea notesArea;
    private JLabel errorLabel;

    public StockTransactionDialog(Frame owner, boolean inboundMode,
                                  List<Product> products, Integer preselectedProductId) {
        super(owner,
              inboundMode ? "Inbound Receipt (Stock IN)" : "Outbound Dispatch (Stock OUT)",
              true);
        this.inboundMode = inboundMode;

        initComponents(products, preselectedProductId);
        pack();
        setMinimumSize(new Dimension(500, 380));
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    private void initComponents(List<Product> products, Integer preselectedProductId) {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(18, 20, 16, 20));
        root.setBackground(Color.WHITE);

        errorLabel = new JLabel(" ");
        errorLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        errorLabel.setForeground(new Color(220, 53, 69));
        root.add(errorLabel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Product Selector
        productCombo = new JComboBox<>();
        int preselectIndex = -1;
        if (products != null) {
            for (int i = 0; i < products.size(); i++) {
                Product p = products.get(i);
                productCombo.addItem(p);
                if (preselectedProductId != null && p.getId() == preselectedProductId) {
                    preselectIndex = i;
                }
            }
        }
        if (preselectIndex >= 0) {
            productCombo.setSelectedIndex(preselectIndex);
        }
        productCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Product p) {
                    setText(String.format("[%s] %s (Stock: %d)", p.getSku(), p.getName(), p.getQuantityInStock()));
                }
                return this;
            }
        });
        addFormRow(formPanel, gbc, 0, "Product *:", productCombo);

        // Transaction Type Selector
        if (inboundMode) {
            typeCombo = new JComboBox<>(new StockTransaction.TransactionType[]{
                    StockTransaction.TransactionType.INBOUND_PURCHASE,
                    StockTransaction.TransactionType.RETURN_RESTOCK,
                    StockTransaction.TransactionType.ADJUSTMENT_COUNT
            });
        } else {
            typeCombo = new JComboBox<>(new StockTransaction.TransactionType[]{
                    StockTransaction.TransactionType.OUTBOUND_SALE,
                    StockTransaction.TransactionType.ADJUSTMENT_DAMAGE,
                    StockTransaction.TransactionType.ADJUSTMENT_COUNT
            });
        }
        addFormRow(formPanel, gbc, 1, "Transaction Type *:", typeCombo);

        // Quantity Input
        quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1_000_000, 1));
        addFormRow(formPanel, gbc, 2, "Quantity *:", quantitySpinner);

        // Notes Area
        notesArea = new JTextArea(3, 22);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        JScrollPane notesScroll = new JScrollPane(notesArea);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        JLabel notesLabel = new JLabel("Reference / Notes:");
        notesLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        notesLabel.setForeground(new Color(73, 80, 87));
        formPanel.add(notesLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        formPanel.add(notesScroll, gbc);

        root.add(formPanel, BorderLayout.CENTER);

        // Action Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        JButton submitButton = new JButton(inboundMode ? "Confirm Stock IN" : "Confirm Stock OUT");
        submitButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        submitButton.setBackground(inboundMode ? new Color(25, 135, 84) : new Color(220, 53, 69));
        submitButton.setForeground(Color.WHITE);
        submitButton.addActionListener(e -> handleSubmit());

        buttonPanel.add(cancelButton);
        buttonPanel.add(submitButton);
        root.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(root);
        getRootPane().setDefaultButton(submitButton);
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

    private void handleSubmit() {
        Product selected = getSelectedProduct();
        if (selected == null) {
            showValidationError("Please select a product.");
            return;
        }

        int qty = getQuantity();
        if (qty <= 0) {
            showValidationError("Quantity must be greater than zero.");
            return;
        }

        if (!inboundMode && qty > selected.getQuantityInStock()) {
            showValidationError(String.format("Insufficient stock for %s (Available: %d, Requested: %d).",
                    selected.getSku(), selected.getQuantityInStock(), qty));
            return;
        }

        confirmed = true;
        dispose();
    }

    private void showValidationError(String msg) {
        errorLabel.setIcon(IconFactory.alert(15, new Color(220, 53, 69)));
        errorLabel.setIconTextGap(6);
        errorLabel.setText(msg);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public Product getSelectedProduct() {
        return (Product) productCombo.getSelectedItem();
    }

    public StockTransaction.TransactionType getSelectedType() {
        return (StockTransaction.TransactionType) typeCombo.getSelectedItem();
    }

    public int getQuantity() {
        return (Integer) quantitySpinner.getValue();
    }

    public String getNotes() {
        String notes = notesArea.getText().trim();
        return notes.isEmpty() ? null : notes;
    }
}
