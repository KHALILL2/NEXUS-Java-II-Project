package com.nexus.ims.view;

import com.nexus.ims.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Main application dashboard window displaying inventory table, controls, and status bar.
 */
public class MainFrame extends JFrame {
    private final User currentUser;
    private JTable productTable;
    private DefaultTableModel tableModel;
    private JButton refreshButton;
    private JButton addProductButton;
    private JButton deleteProductButton;
    private JLabel statusLabel;

    public MainFrame(User currentUser) {
        this.currentUser = currentUser;
        initComponents();
    }

    private void initComponents() {
        setTitle("NEXUS IMS - Dashboard [" + (currentUser != null ? currentUser.getFullName() : "Guest") + "]");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(960, 600);
        setLocationRelativeTo(null);

        JPanel contentPane = new JPanel(new BorderLayout(10, 10));
        contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top Toolbar
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        refreshButton = new JButton("Refresh");
        addProductButton = new JButton("Add Product");
        deleteProductButton = new JButton("Delete Product");

        toolBar.add(refreshButton);
        toolBar.addSeparator();
        toolBar.add(addProductButton);
        toolBar.add(deleteProductButton);
        contentPane.add(toolBar, BorderLayout.NORTH);

        // Center Table
        String[] columnNames = {"ID", "SKU", "Product Name", "Price ($)", "Qty", "Reorder Level", "Status"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        productTable = new JTable(tableModel);
        productTable.setRowHeight(24);
        productTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(productTable);
        contentPane.add(scrollPane, BorderLayout.CENTER);

        // Bottom Status Bar
        statusLabel = new JLabel("Logged in as: " + (currentUser != null ? currentUser.getUsername() : "N/A"));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        contentPane.add(statusLabel, BorderLayout.SOUTH);

        add(contentPane);
    }

    public DefaultTableModel getTableModel() {
        return tableModel;
    }

    public JTable getProductTable() {
        return productTable;
    }

    public JButton getRefreshButton() {
        return refreshButton;
    }

    public JButton getAddProductButton() {
        return addProductButton;
    }

    public JButton getDeleteProductButton() {
        return deleteProductButton;
    }

    public void setStatus(String message) {
        statusLabel.setText(message);
    }
}

