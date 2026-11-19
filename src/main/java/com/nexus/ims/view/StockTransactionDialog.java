package com.nexus.ims.view;

import com.nexus.ims.model.Product;
import com.nexus.ims.model.StockTransaction;
import com.nexus.ims.util.IconFactory;
import com.nexus.ims.util.LanguageManager;
import com.nexus.ims.util.ThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * Modal dialog for logging Stock IN receipts and Stock OUT dispatches/adjustments.
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
              inboundMode
                      ? LanguageManager.get("transactions.dialog.inTitle")
                      : LanguageManager.get("transactions.dialog.outTitle"),
              true);
        this.inboundMode = inboundMode;

        initComponents(products, preselectedProductId);
        LanguageManager.applyOrientation(this);
        pack();
        setMinimumSize(new Dimension(500, 380));
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    private void initComponents(List<Product> products, Integer preselectedProductId) {
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
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Product p) {
                    label.setText(LanguageManager.format("transactions.form.stockFormat",
                            p.getSku(), p.getName(), p.getQuantityInStock()));
                }
                label.setHorizontalAlignment(SwingConstants.LEADING);
                return label;
            }
        });
        addFormRow(formPanel, gbc, 0, LanguageManager.get("transactions.form.product") + " *:", productCombo);

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
        typeCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof StockTransaction.TransactionType type) {
                    label.setText(LanguageManager.get("transactions.type." + type.name()));
                }
                label.setHorizontalAlignment(SwingConstants.LEADING);
                return label;
            }
        });
        addFormRow(formPanel, gbc, 1, LanguageManager.get("transactions.form.type") + " *:", typeCombo);

        quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1_000_000, 1));
        addFormRow(formPanel, gbc, 2, LanguageManager.get("transactions.form.quantity") + " *:", quantitySpinner);

        notesArea = new JTextArea(3, 22);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        JScrollPane notesScroll = new JScrollPane(notesArea);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        JLabel notesLabel = new JLabel(LanguageManager.get("transactions.form.notes") + ":");
        notesLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        notesLabel.setForeground(ThemeManager.textSecondary());
        notesLabel.setHorizontalAlignment(SwingConstants.LEADING);
        formPanel.add(notesLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        formPanel.add(notesScroll, gbc);

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

        JButton submitButton = new JButton(inboundMode
                ? LanguageManager.get("transactions.form.submitIn")
                : LanguageManager.get("transactions.form.submitOut"));
        submitButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        submitButton.setBackground(inboundMode ? ThemeManager.accentSuccess() : ThemeManager.accentDanger());
        submitButton.setForeground(Color.WHITE);
        submitButton.addActionListener(e -> handleSubmit());

        buttonPanel.add(cancelButton);
        buttonPanel.add(submitButton);
        root.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(root);
        getRootPane().setDefaultButton(submitButton);
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

    private void handleSubmit() {
        Product selected = getSelectedProduct();
        if (selected == null) {
            showValidationError(LanguageManager.get("transactions.validation.product"));
            return;
        }

        int qty = getQuantity();
        if (qty <= 0) {
            showValidationError(LanguageManager.get("transactions.validation.quantity"));
            return;
        }

        if (!inboundMode && qty > selected.getQuantityInStock()) {
            showValidationError(LanguageManager.format("transactions.validation.insufficient",
                    selected.getQuantityInStock(), qty));
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
