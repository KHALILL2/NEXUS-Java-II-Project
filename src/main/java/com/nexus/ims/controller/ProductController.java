package com.nexus.ims.controller;

import com.nexus.ims.model.Product;
import com.nexus.ims.service.ProductService;
import com.nexus.ims.view.MainFrame;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.List;

/**
 * Controller mediating user interaction on the MainFrame and business operations in ProductService.
 */
public class ProductController {
    private final MainFrame mainFrame;
    private final ProductService productService;

    public ProductController(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.productService = new ProductService();
        initController();
        loadProductData();
    }

    public ProductController(MainFrame mainFrame, ProductService productService) {
        this.mainFrame = mainFrame;
        this.productService = productService;
        initController();
        loadProductData();
    }

    private void initController() {
        mainFrame.getRefreshButton().addActionListener(this::handleRefresh);
        mainFrame.getDeleteProductButton().addActionListener(this::handleDelete);
    }

    private void handleRefresh(ActionEvent e) {
        loadProductData();
    }

    private void handleDelete(ActionEvent e) {
        int selectedRow = mainFrame.getProductTable().getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(mainFrame, "Please select a product to delete.", 
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int productId = (int) mainFrame.getTableModel().getValueAt(selectedRow, 0);
        int confirm = JOptionPane.showConfirmDialog(mainFrame, 
                "Are you sure you want to delete Product ID #" + productId + "?", 
                "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                boolean success = productService.deleteProduct(productId);
                if (success) {
                    loadProductData();
                    mainFrame.setStatus("Product #" + productId + " successfully removed.");
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(mainFrame, "Error deleting product: " + ex.getMessage(), 
                        "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void loadProductData() {
        DefaultTableModel model = mainFrame.getTableModel();
        model.setRowCount(0);

        try {
            List<Product> products = productService.getAllProducts();
            for (Product p : products) {
                String status = p.isLowStock() ? "LOW STOCK" : "IN STOCK";
                model.addRow(new Object[]{
                        p.getId(),
                        p.getSku(),
                        p.getName(),
                        p.getUnitPrice(),
                        p.getQuantityInStock(),
                        p.getReorderLevel(),
                        status
                });
            }
            mainFrame.setStatus("Loaded " + products.size() + " products successfully.");
        } catch (SQLException e) {
            mainFrame.setStatus("Failed to load inventory: " + e.getMessage());
        }
    }
}

