package com.nexus.ims.view;

import com.nexus.ims.model.Supplier;
import com.nexus.ims.util.IconFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.List;

/**
 * Suppliers Management panel featuring live search, polished vendor table, and CRUD toolbar.
 */
public class SuppliersPanel extends JPanel {
    private JTextField searchField;
    private JButton refreshBtn;
    private JButton addSupplierBtn;
    private JButton editSupplierBtn;
    private JButton deleteSupplierBtn;
    private JTable supplierTable;
    private DefaultTableModel supplierTableModel;

    public SuppliersPanel() {
        initComponents();
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
        searchField.putClientProperty("JTextField.leadingIcon", IconFactory.search(15, new Color(108, 117, 125)));
        searchField.putClientProperty("JTextField.placeholderText", "Search supplier, contact, or email...");
        searchField.putClientProperty("JTextField.showClearButton", true);
        toolbar.add(searchField, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        refreshBtn = new JButton("Refresh", IconFactory.refresh(15, new Color(73, 80, 87)));
        refreshBtn.setIconTextGap(6);

        addSupplierBtn = new JButton("Add Supplier", IconFactory.add(15, Color.WHITE));
        addSupplierBtn.setIconTextGap(6);
        addSupplierBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        addSupplierBtn.setBackground(new Color(13, 110, 253));
        addSupplierBtn.setForeground(Color.WHITE);

        editSupplierBtn = new JButton("Edit", IconFactory.edit(15, new Color(73, 80, 87)));
        editSupplierBtn.setIconTextGap(6);

        deleteSupplierBtn = new JButton("Delete", IconFactory.delete(15, new Color(220, 53, 69)));
        deleteSupplierBtn.setIconTextGap(6);
        deleteSupplierBtn.setForeground(new Color(220, 53, 69));

        actions.add(refreshBtn);
        actions.add(addSupplierBtn);
        actions.add(editSupplierBtn);
        actions.add(deleteSupplierBtn);
        toolbar.add(actions, BorderLayout.EAST);

        add(toolbar, BorderLayout.NORTH);

        // Supplier Data Table
        String[] columns = {"ID", "Supplier Name", "Contact Person", "Email", "Phone", "Address"};
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

        TableColumnModel cm = supplierTable.getColumnModel();
        cm.getColumn(0).setPreferredWidth(55);
        cm.getColumn(0).setMaxWidth(75);
        cm.getColumn(1).setPreferredWidth(210);
        cm.getColumn(2).setPreferredWidth(150);
        cm.getColumn(3).setPreferredWidth(180);
        cm.getColumn(4).setPreferredWidth(130);
        cm.getColumn(5).setPreferredWidth(260);

        JScrollPane scrollPane = new JScrollPane(supplierTable);
        scrollPane.setBorder(new MatteBorder(1, 1, 1, 1, new Color(222, 226, 230)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void populateSuppliers(List<Supplier> suppliers) {
        supplierTableModel.setRowCount(0);
        for (Supplier s : suppliers) {
            supplierTableModel.addRow(new Object[]{
                    s.getId(),
                    s.getName(),
                    s.getContactPerson() != null ? s.getContactPerson() : "-",
                    s.getEmail() != null ? s.getEmail() : "-",
                    s.getPhone() != null ? s.getPhone() : "-",
                    s.getAddress() != null ? s.getAddress() : "-"
            });
        }
    }

    public JTextField getSearchField() { return searchField; }
    public JButton getRefreshBtn() { return refreshBtn; }
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
            cell.setHorizontalAlignment(column == 0 ? SwingConstants.RIGHT : SwingConstants.LEFT);
            if (column == 1) {
                cell.setFont(cell.getFont().deriveFont(Font.BOLD));
            }
            if (!isSelected) {
                cell.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                cell.setForeground(new Color(33, 37, 41));
            }
            return cell;
        }
    }
}
