package com.nexus.ims.view;

import com.nexus.ims.dao.CategoryDAO;
import com.nexus.ims.model.Category;
import com.nexus.ims.util.IconFactory;
import com.nexus.ims.util.LanguageManager;
import com.nexus.ims.util.ThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * Modal dialog for managing product categories with foreign-key deletion checks.
 */
public class CategoryManagementDialog extends JDialog {
    private final CategoryDAO categoryDAO;
    private final Runnable onCategoriesModified;

    private JTable categoryTable;
    private DefaultTableModel categoryTableModel;
    private JTextField nameField;
    private JTextField descriptionField;
    private JLabel errorLabel;
    private Integer selectedCategoryId = null;

    public CategoryManagementDialog(Frame owner, CategoryDAO categoryDAO, Runnable onCategoriesModified) {
        super(owner, LanguageManager.get("categories.dialog.title"), true);
        this.categoryDAO = categoryDAO;
        this.onCategoriesModified = onCategoriesModified;

        initComponents();
        LanguageManager.applyOrientation(this);
        loadCategories();
        pack();
        setMinimumSize(new Dimension(680, 500));
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(16, 18, 16, 18));
        root.setBackground(ThemeManager.bgSurface());

        String[] columns = {
                LanguageManager.get("common.id"),
                LanguageManager.get("categories.col.name"),
                LanguageManager.get("categories.col.description")
        };
        categoryTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        categoryTable = new JTable(categoryTableModel);
        categoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        categoryTable.getTableHeader().setReorderingAllowed(false);
        categoryTable.setBackground(ThemeManager.bgSurface());
        categoryTable.setForeground(ThemeManager.textPrimary());
        categoryTable.setGridColor(ThemeManager.borderSubtle());
        categoryTable.setDefaultRenderer(Object.class, new CategoryCellRenderer());

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(categoryTableModel);
        sorter.setComparator(0, DashboardFrame.NUMERIC_COMPARATOR);
        categoryTable.setRowSorter(sorter);

        TableColumnModel cm = categoryTable.getColumnModel();
        cm.getColumn(0).setPreferredWidth(50);
        cm.getColumn(0).setMaxWidth(65);
        cm.getColumn(1).setPreferredWidth(190);
        cm.getColumn(2).setPreferredWidth(340);

        categoryTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                populateFormFromSelection();
            }
        });

        JScrollPane scrollPane = new JScrollPane(categoryTable);
        scrollPane.setPreferredSize(new Dimension(620, 240));
        scrollPane.setBorder(new MatteBorder(1, 1, 1, 1, ThemeManager.borderSubtle()));
        scrollPane.getViewport().setBackground(ThemeManager.bgSurface());
        root.add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setBackground(ThemeManager.bgSurfaceAlt());
        bottomPanel.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 1, 1, 1, ThemeManager.borderSubtle()),
                new EmptyBorder(12, 14, 12, 14)
        ));

        errorLabel = new JLabel(" ");
        errorLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        errorLabel.setForeground(ThemeManager.accentDanger());
        errorLabel.setIconTextGap(6);
        errorLabel.setHorizontalAlignment(SwingConstants.LEADING);
        bottomPanel.add(errorLabel, BorderLayout.NORTH);

        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.LINE_START;

        nameField = new JTextField(22);
        nameField.putClientProperty("JTextField.placeholderText", LanguageManager.get("categories.form.namePlaceholder"));
        descriptionField = new JTextField(26);
        descriptionField.putClientProperty("JTextField.placeholderText", LanguageManager.get("categories.form.descPlaceholder"));

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        JLabel nameLbl = new JLabel(LanguageManager.get("categories.form.name") + " *:");
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        nameLbl.setForeground(ThemeManager.textSecondary());
        nameLbl.setHorizontalAlignment(SwingConstants.LEADING);
        fieldsPanel.add(nameLbl, gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        fieldsPanel.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        JLabel descLbl = new JLabel(LanguageManager.get("categories.form.description") + ":");
        descLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        descLbl.setForeground(ThemeManager.textSecondary());
        descLbl.setHorizontalAlignment(SwingConstants.LEADING);
        fieldsPanel.add(descLbl, gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        fieldsPanel.add(descriptionField, gbc);

        bottomPanel.add(fieldsPanel, BorderLayout.CENTER);

        JPanel buttonsRow = new JPanel(new BorderLayout(8, 0));
        buttonsRow.setOpaque(false);

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
        leftActions.setOpaque(false);

        JButton addBtn = new JButton(LanguageManager.get("categories.form.add"), IconFactory.add(14, Color.WHITE));
        addBtn.setIconTextGap(6);
        addBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        addBtn.setBackground(ThemeManager.accentPrimary());
        addBtn.setForeground(Color.WHITE);
        addBtn.addActionListener(e -> handleAdd());

        JButton updateBtn = new JButton(LanguageManager.get("categories.form.update"),
                IconFactory.edit(14, ThemeManager.btnSecondaryFg()));
        updateBtn.setIconTextGap(6);
        updateBtn.setBackground(ThemeManager.btnSecondaryBg());
        updateBtn.setForeground(ThemeManager.btnSecondaryFg());
        updateBtn.addActionListener(e -> handleUpdate());

        JButton deleteBtn = new JButton(LanguageManager.get("categories.form.delete"),
                IconFactory.delete(14, ThemeManager.accentDanger()));
        deleteBtn.setIconTextGap(6);
        deleteBtn.setBackground(ThemeManager.btnSecondaryBg());
        deleteBtn.setForeground(ThemeManager.accentDanger());
        deleteBtn.addActionListener(e -> handleDelete());

        JButton clearBtn = new JButton(LanguageManager.get("categories.form.clear"));
        clearBtn.setBackground(ThemeManager.btnSecondaryBg());
        clearBtn.setForeground(ThemeManager.btnSecondaryFg());
        clearBtn.addActionListener(e -> clearForm());

        leftActions.add(addBtn);
        leftActions.add(updateBtn);
        leftActions.add(deleteBtn);
        leftActions.add(clearBtn);

        JButton closeBtn = new JButton(LanguageManager.get("common.close"));
        closeBtn.setBackground(ThemeManager.btnSecondaryBg());
        closeBtn.setForeground(ThemeManager.btnSecondaryFg());
        closeBtn.addActionListener(e -> dispose());

        buttonsRow.add(leftActions, BorderLayout.LINE_START);
        buttonsRow.add(closeBtn, BorderLayout.LINE_END);
        bottomPanel.add(buttonsRow, BorderLayout.SOUTH);

        root.add(bottomPanel, BorderLayout.SOUTH);
        setContentPane(root);

        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    private void loadCategories() {
        SwingWorker<List<Category>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Category> doInBackground() {
                return categoryDAO.findAll();
            }

            @Override
            protected void done() {
                try {
                    List<Category> list = get();
                    categoryTableModel.setRowCount(0);
                    for (Category c : list) {
                        categoryTableModel.addRow(new Object[]{
                                c.getId(),
                                c.getName(),
                                c.getDescription() != null ? c.getDescription() : ""
                        });
                    }
                } catch (Exception ex) {
                    showError(LanguageManager.format("categories.msg.loadFailed", ex.getMessage()));
                }
            }
        };
        worker.execute();
    }

    private void populateFormFromSelection() {
        int viewRow = categoryTable.getSelectedRow();
        if (viewRow == -1) {
            selectedCategoryId = null;
            return;
        }
        int modelRow = categoryTable.convertRowIndexToModel(viewRow);
        selectedCategoryId = (Integer) categoryTableModel.getValueAt(modelRow, 0);
        nameField.setText(String.valueOf(categoryTableModel.getValueAt(modelRow, 1)));
        descriptionField.setText(String.valueOf(categoryTableModel.getValueAt(modelRow, 2)));
        clearError();
    }

    private void handleAdd() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showError(LanguageManager.get("categories.validation.nameRequired"));
            nameField.requestFocusInWindow();
            return;
        }
        String desc = descriptionField.getText().trim();
        Category category = new Category(name, desc.isEmpty() ? null : desc);

        SwingWorker<String, Void> saver = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                if (categoryDAO.findByName(name) != null) {
                    return LanguageManager.format("categories.msg.duplicate", name);
                }
                return categoryDAO.save(category) ? null : LanguageManager.get("categories.msg.saveFailed");
            }

            @Override
            protected void done() {
                try {
                    String err = get();
                    if (err == null) {
                        clearForm();
                        loadCategories();
                        notifyParent();
                    } else {
                        showError(err);
                    }
                } catch (Exception ex) {
                    showError(ex.getMessage());
                }
            }
        };
        saver.execute();
    }

    private void handleUpdate() {
        if (selectedCategoryId == null) {
            showError(LanguageManager.get("categories.selectUpdate"));
            return;
        }
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showError(LanguageManager.get("categories.validation.nameRequired"));
            nameField.requestFocusInWindow();
            return;
        }
        String desc = descriptionField.getText().trim();
        Category category = new Category(selectedCategoryId, name, desc.isEmpty() ? null : desc, null);

        SwingWorker<String, Void> updater = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                Category conflict = categoryDAO.findByName(name);
                if (conflict != null && conflict.getId() != selectedCategoryId) {
                    return LanguageManager.format("categories.msg.conflict", name);
                }
                return categoryDAO.update(category) ? null : LanguageManager.get("categories.msg.updateFailed");
            }

            @Override
            protected void done() {
                try {
                    String err = get();
                    if (err == null) {
                        clearForm();
                        loadCategories();
                        notifyParent();
                    } else {
                        showError(err);
                    }
                } catch (Exception ex) {
                    showError(ex.getMessage());
                }
            }
        };
        updater.execute();
    }

    private void handleDelete() {
        if (selectedCategoryId == null) {
            showError(LanguageManager.get("categories.selectDelete"));
            return;
        }
        final int catId = selectedCategoryId;
        final String catName = nameField.getText().trim();

        int confirm = JOptionPane.showConfirmDialog(this,
                LanguageManager.format("categories.deleteConfirm", catName),
                LanguageManager.get("categories.deleteTitle"),
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        SwingWorker<String, Void> deleter = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                int linkedCount = categoryDAO.countProductsByCategoryId(catId);
                if (linkedCount > 0) {
                    return LanguageManager.format("categories.msg.inUse", catName, linkedCount);
                }
                return categoryDAO.delete(catId) ? null : LanguageManager.get("categories.msg.deleteFailed");
            }

            @Override
            protected void done() {
                try {
                    String err = get();
                    if (err == null) {
                        clearForm();
                        loadCategories();
                        notifyParent();
                    } else {
                        showError(err);
                    }
                } catch (Exception ex) {
                    showError(ex.getMessage());
                }
            }
        };
        deleter.execute();
    }

    private void notifyParent() {
        if (onCategoriesModified != null) {
            onCategoriesModified.run();
        }
    }

    private void clearForm() {
        selectedCategoryId = null;
        categoryTable.clearSelection();
        nameField.setText("");
        descriptionField.setText("");
        clearError();
    }

    private void showError(String msg) {
        errorLabel.setIcon(IconFactory.alert(15, ThemeManager.accentDanger()));
        errorLabel.setForeground(ThemeManager.accentDanger());
        errorLabel.setText(msg);
    }

    private void clearError() {
        errorLabel.setIcon(null);
        errorLabel.setText(" ");
    }

    private static class CategoryCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            JLabel cell = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            cell.setBorder(new EmptyBorder(0, 10, 0, 10));
            cell.setHorizontalAlignment(column == 0 ? SwingConstants.TRAILING : SwingConstants.LEADING);
            if (column == 0 && value != null) {
                cell.setText(LanguageManager.isolateLTR(value.toString()));
            }
            if (column == 1) {
                cell.setFont(cell.getFont().deriveFont(Font.BOLD));
            }
            if (!isSelected) {
                cell.setBackground(row % 2 == 0 ? ThemeManager.bgSurface() : ThemeManager.bgSurfaceAlt());
                cell.setForeground(ThemeManager.textPrimary());
            }
            return cell;
        }
    }
}
