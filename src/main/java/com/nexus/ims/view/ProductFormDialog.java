package com.nexus.ims.view;

import com.nexus.ims.model.Category;
import com.nexus.ims.model.Product;
import com.nexus.ims.model.Supplier;
import com.nexus.ims.util.IconFactory;
import com.nexus.ims.util.LanguageManager;
import com.nexus.ims.util.ThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.util.List;

/**
 * Modal form dialog for creating or editing a catalog product.
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
              existingProduct == null
                      ? LanguageManager.get("products.dialog.addTitle")
                      : LanguageManager.format("products.dialog.editTitleFormat", existingProduct.getSku()),
              true);
        this.existingProduct = existingProduct;

        initComponents(categories, suppliers);
        populateExistingData();
        LanguageManager.applyOrientation(this);
        pack();
        setMinimumSize(new Dimension(520, 560));
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    private void initComponents(List<Category> categories, List<Supplier> suppliers) {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(18, 20, 16, 20));
        root.setBackground(ThemeManager.bgSurface());

        errorLabel = new JLabel(" ");
        errorLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        errorLabel.setForeground(ThemeManager.accentDanger());
        errorLabel.setHorizontalAlignment(SwingConstants.LEADING);
        root.add(errorLabel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.LINE_START;

        int row = 0;

        skuField = new JTextField(22);
        skuField.putClientProperty("JTextField.placeholderText", LanguageManager.get("products.form.skuPlaceholder"));
        addFormRow(formPanel, gbc, row++, LanguageManager.get("products.form.sku") + " *:", skuField);

        nameField = new JTextField(22);
        nameField.putClientProperty("JTextField.placeholderText", LanguageManager.get("products.form.namePlaceholder"));
        addFormRow(formPanel, gbc, row++, LanguageManager.get("products.form.name") + " *:", nameField);

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
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Category c) {
                    label.setText(c.getName());
                } else {
                    label.setText("-- " + LanguageManager.get("common.unassigned") + " --");
                }
                label.setHorizontalAlignment(SwingConstants.LEADING);
                return label;
            }
        });
        addFormRow(formPanel, gbc, row++, LanguageManager.get("products.form.category") + ":", categoryCombo);

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
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Supplier s) {
                    label.setText(s.getName());
                } else {
                    label.setText("-- " + LanguageManager.get("common.unassigned") + " --");
                }
                label.setHorizontalAlignment(SwingConstants.LEADING);
                return label;
            }
        });
        addFormRow(formPanel, gbc, row++, LanguageManager.get("products.form.supplier") + ":", supplierCombo);

        purchasePriceField = new JTextField("0.00", 12);
        addFormRow(formPanel, gbc, row++, LanguageManager.get("products.form.costPrice") + " *:", purchasePriceField);

        unitPriceField = new JTextField("0.00", 12);
        addFormRow(formPanel, gbc, row++, LanguageManager.get("products.form.sellingPrice") + " *:", unitPriceField);

        quantitySpinner = new JSpinner(new SpinnerNumberModel(0, 0, 1_000_000, 1));
        String qtyLabel = existingProduct == null
                ? LanguageManager.get("products.form.initialStock") + " *:"
                : LanguageManager.get("products.form.currentStock") + " *:";
        addFormRow(formPanel, gbc, row++, qtyLabel, quantitySpinner);

        reorderLevelSpinner = new JSpinner(new SpinnerNumberModel(10, 0, 100_000, 1));
        addFormRow(formPanel, gbc, row++, LanguageManager.get("products.form.minThreshold") + " *:", reorderLevelSpinner);

        descriptionArea = new JTextArea(3, 22);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(descriptionArea);
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        JLabel descLabel = new JLabel(LanguageManager.get("products.form.description") + ":");
        descLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        descLabel.setForeground(ThemeManager.textSecondary());
        descLabel.setHorizontalAlignment(SwingConstants.LEADING);
        formPanel.add(descLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        formPanel.add(descScroll, gbc);

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

        JButton saveButton = new JButton(existingProduct == null
                ? LanguageManager.get("products.form.save")
                : LanguageManager.get("products.form.update"));
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
            showValidationError(LanguageManager.get("products.form.validation.skuRequired"));
            skuField.requestFocusInWindow();
            return;
        }
        if (name.isEmpty()) {
            showValidationError(LanguageManager.get("products.form.validation.nameRequired"));
            nameField.requestFocusInWindow();
            return;
        }

        BigDecimal purchasePrice;
        try {
            purchasePrice = new BigDecimal(purchasePriceField.getText().trim());
            if (purchasePrice.compareTo(BigDecimal.ZERO) < 0) {
                showValidationError(LanguageManager.get("products.form.validation.purchaseNonNeg"));
                return;
            }
        } catch (NumberFormatException ex) {
            showValidationError(LanguageManager.get("products.form.validation.purchaseInvalid"));
            purchasePriceField.requestFocusInWindow();
            return;
        }

        BigDecimal unitPrice;
        try {
            unitPrice = new BigDecimal(unitPriceField.getText().trim());
            if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                showValidationError(LanguageManager.get("products.form.validation.unitNonNeg"));
                return;
            }
        } catch (NumberFormatException ex) {
            showValidationError(LanguageManager.get("products.form.validation.unitInvalid"));
            unitPriceField.requestFocusInWindow();
            return;
        }

        confirmed = true;
        dispose();
    }

    private void showValidationError(String msg) {
        errorLabel.setIcon(IconFactory.alert(15, ThemeManager.accentDanger()));
        errorLabel.setIconTextGap(6);
        errorLabel.setForeground(ThemeManager.accentDanger());
        errorLabel.setText(msg);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

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
