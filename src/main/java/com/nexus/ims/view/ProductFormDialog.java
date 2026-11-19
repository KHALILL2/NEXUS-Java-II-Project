package com.nexus.ims.view;

import com.nexus.ims.model.Category;
import com.nexus.ims.model.Product;
import com.nexus.ims.model.Supplier;
import com.nexus.ims.util.IconFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Modal FlatLaf dialog supporting both creating a new Product and editing an existing Product.
 */
public class ProductFormDialog extends JDialog {
    private final Product existingProduct;
    private boolean confirmed = false;

    private JTextField skuField;
    private JTextField nameField;
    private JTextArea descriptionArea;
    private JComboBox<Category> categoryCombo;
    private JComboBox<Supplier> supplierCombo;
    private JTextField purchasePriceField;
    private JTextField unitPriceField;
    private JSpinner quantitySpinner;
    private JSpinner reorderLevelSpinner;
    private JLabel errorLabel;

    public ProductFormDialog(Frame owner, Product existingProduct,
                             List<Category> categories, List<Supplier> suppliers) {
        super(owner,
              existingProduct == null ? "Add New Product" : "Edit Product - " + existingProduct.getSku(),
              true);
        this.existingProduct = existingProduct;

        initComponents(categories, suppliers);
        populateExistingData();
        pack();
        setMinimumSize(new Dimension(520, 560));
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    private void initComponents(List<Category> categories, List<Supplier> suppliers) {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(18, 20, 16, 20));
        root.setBackground(Color.WHITE);

        // Inline Error Banner
        errorLabel = new JLabel(" ");
        errorLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        errorLabel.setForeground(new Color(220, 53, 69));
        root.add(errorLabel, BorderLayout.NORTH);

        // Form Fields Grid
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        skuField = new JTextField(22);
        skuField.putClientProperty("JTextField.placeholderText", "e.g. DELL-LAT-5540");
        addFormRow(formPanel, gbc, row++, "SKU *:", skuField);

        nameField = new JTextField(22);
        nameField.putClientProperty("JTextField.placeholderText", "Product display name");
        addFormRow(formPanel, gbc, row++, "Product Name *:", nameField);

        categoryCombo = new JComboBox<>();
        categoryCombo.addItem(null);
        if (categories != null) {
            for (Category c : categories) {
                categoryCombo.addItem(c);
            }
        }
        categoryCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Category c) {
                    setText(c.getName());
                } else {
                    setText("-- Unassigned --");
                }
                return this;
            }
        });
        addFormRow(formPanel, gbc, row++, "Category:", categoryCombo);

        supplierCombo = new JComboBox<>();
        supplierCombo.addItem(null);
        if (suppliers != null) {
            for (Supplier s : suppliers) {
                supplierCombo.addItem(s);
            }
        }
        supplierCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Supplier s) {
                    setText(s.getName());
                } else {
                    setText("-- Unassigned --");
                }
                return this;
            }
        });
        addFormRow(formPanel, gbc, row++, "Supplier:", supplierCombo);

        purchasePriceField = new JTextField("0.00", 12);
        addFormRow(formPanel, gbc, row++, "Purchase Cost ($) *:", purchasePriceField);

        unitPriceField = new JTextField("0.00", 12);
        addFormRow(formPanel, gbc, row++, "Unit Selling Price ($) *:", unitPriceField);

        quantitySpinner = new JSpinner(new SpinnerNumberModel(0, 0, 1_000_000, 1));
        addFormRow(formPanel, gbc, row++, existingProduct == null ? "Initial Stock Qty *:" : "Current Stock Qty *:", quantitySpinner);

        reorderLevelSpinner = new JSpinner(new SpinnerNumberModel(10, 0, 100_000, 1));
        addFormRow(formPanel, gbc, row++, "Reorder Threshold *:", reorderLevelSpinner);

        descriptionArea = new JTextArea(3, 22);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(descriptionArea);
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        JLabel descLabel = new JLabel("Description:");
        descLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        formPanel.add(descLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        formPanel.add(descScroll, gbc);

        root.add(formPanel, BorderLayout.CENTER);

        // Action Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        JButton saveButton = new JButton(existingProduct == null ? "Save Product" : "Update Product");
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
        if (existingProduct == null) {
            return;
        }
        skuField.setText(existingProduct.getSku());
        nameField.setText(existingProduct.getName());
        descriptionArea.setText(existingProduct.getDescription() != null ? existingProduct.getDescription() : "");
        purchasePriceField.setText(existingProduct.getPurchasePrice() != null
                ? existingProduct.getPurchasePrice().toPlainString() : "0.00");
        unitPriceField.setText(existingProduct.getUnitPrice() != null
                ? existingProduct.getUnitPrice().toPlainString() : "0.00");
        quantitySpinner.setValue(existingProduct.getQuantityInStock());
        reorderLevelSpinner.setValue(existingProduct.getReorderLevel());

        if (existingProduct.getCategoryId() != null) {
            for (int i = 0; i < categoryCombo.getItemCount(); i++) {
                Category c = categoryCombo.getItemAt(i);
                if (c != null && c.getId() == existingProduct.getCategoryId()) {
                    categoryCombo.setSelectedIndex(i);
                    break;
                }
            }
        }

        if (existingProduct.getSupplierId() != null) {
            for (int i = 0; i < supplierCombo.getItemCount(); i++) {
                Supplier s = supplierCombo.getItemAt(i);
                if (s != null && s.getId() == existingProduct.getSupplierId()) {
                    supplierCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void handleSave() {
        String sku = skuField.getText().trim();
        String name = nameField.getText().trim();

        if (sku.isEmpty()) {
            showValidationError("SKU is required.");
            skuField.requestFocusInWindow();
            return;
        }
        if (name.isEmpty()) {
            showValidationError("Product Name is required.");
            nameField.requestFocusInWindow();
            return;
        }

        BigDecimal purchasePrice;
        try {
            purchasePrice = new BigDecimal(purchasePriceField.getText().trim());
            if (purchasePrice.compareTo(BigDecimal.ZERO) < 0) {
                showValidationError("Purchase cost must be non-negative.");
                return;
            }
        } catch (NumberFormatException ex) {
            showValidationError("Purchase cost must be a valid decimal number.");
            purchasePriceField.requestFocusInWindow();
            return;
        }

        BigDecimal unitPrice;
        try {
            unitPrice = new BigDecimal(unitPriceField.getText().trim());
            if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                showValidationError("Unit selling price must be non-negative.");
                return;
            }
        } catch (NumberFormatException ex) {
            showValidationError("Unit selling price must be a valid decimal number.");
            unitPriceField.requestFocusInWindow();
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

    /**
     * Builds a Product object populated from the validated dialog form fields.
     */
    public Product getProduct() {
        Product product = existingProduct != null ? existingProduct : new Product();
        product.setSku(skuField.getText().trim().toUpperCase());
        product.setName(nameField.getText().trim());
        String desc = descriptionArea.getText().trim();
        product.setDescription(desc.isEmpty() ? null : desc);

        Category selectedCat = (Category) categoryCombo.getSelectedItem();
        product.setCategoryId(selectedCat != null ? selectedCat.getId() : null);

        Supplier selectedSup = (Supplier) supplierCombo.getSelectedItem();
        product.setSupplierId(selectedSup != null ? selectedSup.getId() : null);

        product.setPurchasePrice(new BigDecimal(purchasePriceField.getText().trim()));
        product.setUnitPrice(new BigDecimal(unitPriceField.getText().trim()));
        product.setQuantityInStock((Integer) quantitySpinner.getValue());
        product.setReorderLevel((Integer) reorderLevelSpinner.getValue());

        return product;
    }
}
