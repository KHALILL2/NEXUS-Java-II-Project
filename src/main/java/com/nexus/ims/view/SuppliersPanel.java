package com.nexus.ims.view;

import com.nexus.ims.model.Supplier;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Vendor directory panel with search, supplied SKU counts, selection-aware toolbar actions, and exports.
 */
public class SuppliersPanel extends JPanel {
    private JTextField searchField;
    private JButton refreshBtn;
    private JButton exportSuppliersBtn;
    private JButton addSupplierBtn;
    private JButton editSupplierBtn;
    private JButton deleteSupplierBtn;
    private JTable supplierTable;
    private DefaultTableModel supplierTableModel;
    private JScrollPane scrollPane;

    private boolean canEdit = true;
    private boolean canDelete = true;

    public SuppliersPanel() {
        initComponents();
        refreshThemeAndLanguage();
    }

    private void initComponents() {
        setLayout(new BorderLayout(12, 12));
        setOpaque(false);
        setBorder(new EmptyBorder(18, 20, 18, 20));

        // Top Toolbar
        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setOpaque(false);

        searchField = new JTextField(24);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setPreferredSize(new Dimension(280, 36));
        searchField.putClientProperty("JTextField.showClearButton", true);
        toolbar.add(searchField, BorderLayout.LINE_START);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
        actions.setOpaque(false);

        refreshBtn = new JButton();
        refreshBtn.setIconTextGap(6);

        exportSuppliersBtn = new JButton();
        exportSuppliersBtn.setIconTextGap(6);

        addSupplierBtn = new JButton();
        addSupplierBtn.setIconTextGap(6);
        addSupplierBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));

        editSupplierBtn = new JButton();
        editSupplierBtn.setIconTextGap(6);

        deleteSupplierBtn = new JButton();
        deleteSupplierBtn.setIconTextGap(6);

        actions.add(refreshBtn);
        actions.add(exportSuppliersBtn);
        actions.add(addSupplierBtn);
        actions.add(editSupplierBtn);
        actions.add(deleteSupplierBtn);
        toolbar.add(actions, BorderLayout.LINE_END);

        add(toolbar, BorderLayout.NORTH);

        // Supplier Data Table (7 columns including Supplied SKUs)
        String[] columns = new String[7];
        supplierTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        supplierTable = new JTable(supplierTableModel);
        supplierTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        supplierTable.getTableHeader().setReorderingAllowed(false);
        supplierTable.setDefaultRenderer(Object.class, new PaddedSupplierRenderer());

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(supplierTableModel);
        sorter.setComparator(0, DashboardFrame.NUMERIC_COMPARATOR);
        sorter.setComparator(5, DashboardFrame.NUMERIC_COMPARATOR);
        supplierTable.setRowSorter(sorter);

        TableColumnModel cm = supplierTable.getColumnModel();
        cm.getColumn(0).setPreferredWidth(55);
        cm.getColumn(0).setMaxWidth(75);
        cm.getColumn(1).setPreferredWidth(200);
        cm.getColumn(2).setPreferredWidth(145);
        cm.getColumn(3).setPreferredWidth(180);
        cm.getColumn(4).setPreferredWidth(125);
        cm.getColumn(5).setPreferredWidth(105);
        cm.getColumn(6).setPreferredWidth(235);

        supplierTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateSelectionState();
            }
        });

        scrollPane = new JScrollPane(supplierTable);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void setRolePermissions(boolean canEdit, boolean canDelete) {
        this.canEdit = canEdit;
        this.canDelete = canDelete;
        updateSelectionState();
    }

    private void updateSelectionState() {
        boolean hasSelection = supplierTable.getSelectedRow() != -1;
        boolean editEnabled = canEdit && hasSelection;
        boolean deleteEnabled = canDelete && hasSelection;

        editSupplierBtn.setEnabled(editEnabled);
        Color editFg = editEnabled ? ThemeManager.btnSecondaryFg() : ThemeManager.textMuted();
        editSupplierBtn.setForeground(editFg);
        editSupplierBtn.setIcon(IconFactory.edit(15, editFg));

        deleteSupplierBtn.setEnabled(deleteEnabled);
        Color deleteFg = deleteEnabled ? ThemeManager.accentDanger() : ThemeManager.textMuted();
        deleteSupplierBtn.setForeground(deleteFg);
        deleteSupplierBtn.setIcon(IconFactory.delete(15, deleteFg));
    }

    public void refreshThemeAndLanguage() {
        searchField.putClientProperty("JTextField.leadingIcon", IconFactory.search(15, ThemeManager.textSecondary()));
        searchField.putClientProperty("JTextField.placeholderText", LanguageManager.get("suppliers.searchPlaceholder"));
        searchField.setBackground(ThemeManager.bgSurface());
        searchField.setForeground(ThemeManager.textPrimary());

        refreshBtn.setText(LanguageManager.get("common.refresh"));
        refreshBtn.setBackground(ThemeManager.btnSecondaryBg());
        refreshBtn.setForeground(ThemeManager.btnSecondaryFg());
        refreshBtn.setIcon(IconFactory.refresh(15, ThemeManager.btnSecondaryFg()));

        exportSuppliersBtn.setText(LanguageManager.get("export.button"));
        exportSuppliersBtn.setBackground(ThemeManager.btnSecondaryBg());
        exportSuppliersBtn.setForeground(ThemeManager.btnSecondaryFg());
        exportSuppliersBtn.setIcon(IconFactory.exportDropdown(15, ThemeManager.btnSecondaryFg()));

        addSupplierBtn.setText(LanguageManager.get("suppliers.add"));
        addSupplierBtn.setBackground(ThemeManager.accentPrimary());
        addSupplierBtn.setForeground(Color.WHITE);
        addSupplierBtn.setIcon(IconFactory.add(15, Color.WHITE));

        editSupplierBtn.setText(LanguageManager.get("common.edit"));
        editSupplierBtn.setBackground(ThemeManager.btnSecondaryBg());

        deleteSupplierBtn.setText(LanguageManager.get("common.delete"));
        deleteSupplierBtn.setBackground(ThemeManager.btnSecondaryBg());

        updateSelectionState();

        String[] headers = {
                LanguageManager.get("common.id"),
                LanguageManager.get("suppliers.col.name"),
                LanguageManager.get("suppliers.col.contact"),
                LanguageManager.get("suppliers.col.email"),
                LanguageManager.get("suppliers.col.phone"),
                LanguageManager.get("suppliers.col.skus"),
                LanguageManager.get("suppliers.col.address")
        };
        TableColumnModel cm = supplierTable.getColumnModel();
        for (int i = 0; i < headers.length && i < cm.getColumnCount(); i++) {
            cm.getColumn(i).setHeaderValue(headers[i]);
        }
        if (supplierTable.getTableHeader() != null) {
            supplierTable.getTableHeader().repaint();
        }

        scrollPane.setBorder(new MatteBorder(1, 1, 1, 1, ThemeManager.borderSubtle()));
        scrollPane.getViewport().setBackground(ThemeManager.bgSurface());
        supplierTable.setBackground(ThemeManager.bgSurface());
        supplierTable.setForeground(ThemeManager.textPrimary());
        supplierTable.setGridColor(ThemeManager.borderSubtle());

        LanguageManager.applyOrientation(this);
        supplierTable.repaint();
    }

    public void populateSuppliers(List<Supplier> suppliers) {
        populateSuppliers(suppliers, Collections.emptyMap());
    }

    public void populateSuppliers(List<Supplier> suppliers, Map<Integer, Integer> skuCounts) {
        List<Supplier> safeSuppliers = suppliers != null ? suppliers : Collections.emptyList();
        Map<Integer, Integer> safeSkuCounts = skuCounts != null ? skuCounts : Collections.emptyMap();

        supplierTableModel.setRowCount(0);
        for (Supplier s : safeSuppliers) {
            int count = safeSkuCounts.getOrDefault(s.getId(), 0);
            supplierTableModel.addRow(new Object[]{
                    s.getId(),
                    s.getName(),
                    s.getContactPerson() != null ? s.getContactPerson() : "-",
                    s.getEmail() != null ? s.getEmail() : "-",
                    s.getPhone() != null ? s.getPhone() : "-",
                    count,
                    s.getAddress() != null ? s.getAddress() : "-"
            });
        }
        updateSelectionState();
    }

    public JTextField getSearchField() { return searchField; }
    public JButton getRefreshBtn() { return refreshBtn; }
    public JButton getExportSuppliersBtn() { return exportSuppliersBtn; }
    public JButton getAddSupplierBtn() { return addSupplierBtn; }
    public JButton getEditSupplierBtn() { return editSupplierBtn; }
    public JButton getDeleteSupplierBtn() { return deleteSupplierBtn; }
    public JTable getSupplierTable() { return supplierTable; }
    public DefaultTableModel getSupplierTableModel() { return supplierTableModel; }

    private static class PaddedSupplierRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            JLabel cell = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            cell.setBorder(new EmptyBorder(0, 12, 0, 12));
            boolean numericCol = column == 0 || column == 5;
            cell.setHorizontalAlignment(numericCol ? SwingConstants.TRAILING : SwingConstants.LEADING);

            if ((numericCol || column == 3 || column == 4) && value != null) {
                cell.setText(LanguageManager.isolateLTR(value.toString()));
            }
            if (column == 1 || column == 5) {
                cell.setFont(cell.getFont().deriveFont(Font.BOLD));
            }
            if (!isSelected) {
                cell.setBackground(row % 2 == 0 ? ThemeManager.bgSurface() : ThemeManager.bgSurfaceAlt());
                cell.setForeground(column == 5 ? ThemeManager.accentPrimary() : ThemeManager.textPrimary());
            }
            return cell;
        }
    }
}
