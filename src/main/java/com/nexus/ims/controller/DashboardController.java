package com.nexus.ims.controller;

import com.nexus.ims.dao.ProductDAO;
import com.nexus.ims.dao.StockTransactionDAO;
import com.nexus.ims.model.Product;
import com.nexus.ims.model.StockTransaction;
import com.nexus.ims.model.User;
import com.nexus.ims.service.AuthService;
import com.nexus.ims.service.InventoryService;
import com.nexus.ims.view.DashboardFrame;
import com.nexus.ims.view.LoginFrame;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller mediating interaction between the DashboardFrame view and backend services.
 * Implements strict separation of concerns, decoupling Swing components from database operations.
 */
public class DashboardController {
    private final DashboardFrame view;
    private final ProductDAO productDAO;
    private final StockTransactionDAO transactionDAO;
    private final InventoryService inventoryService;

    public DashboardController(DashboardFrame view) {
        this.view = view;
        this.productDAO = new ProductDAO();
        this.transactionDAO = new StockTransactionDAO();
        this.inventoryService = new InventoryService(productDAO, transactionDAO);

        initController();
        loadAllData();
    }

    private void initController() {
        // Sidebar Navigation
        view.getNavDashboardBtn().addActionListener(e -> {
            view.showCard(DashboardFrame.CARD_DASHBOARD);
            loadDashboardMetrics();
        });

        view.getNavProductsBtn().addActionListener(e -> {
            view.showCard(DashboardFrame.CARD_PRODUCTS);
            loadProductCatalog();
        });

        view.getNavTransactionsBtn().addActionListener(e -> {
            view.showCard(DashboardFrame.CARD_TRANSACTIONS);
            loadTransactionHistory();
        });

        view.getNavSuppliersBtn().addActionListener(e -> view.showCard(DashboardFrame.CARD_SUPPLIERS));
        view.getNavReportsBtn().addActionListener(e -> {
            view.showCard(DashboardFrame.CARD_REPORTS);
            loadDashboardMetrics();
        });

        // Sign Out Handling
        view.getLogoutBtn().addActionListener(this::handleLogout);

        // Product Actions
        view.getRefreshProductsBtn().addActionListener(e -> loadProductCatalog());
        view.getDeleteProductBtn().addActionListener(this::handleDeleteProduct);

        // Realtime Product Search Filter
        view.getProductSearchField().getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterProducts(); }
            @Override public void removeUpdate(DocumentEvent e) { filterProducts(); }
            @Override public void changedUpdate(DocumentEvent e) { filterProducts(); }
        });

        // Transaction Actions
        view.getRefreshTransactionsBtn().addActionListener(e -> loadTransactionHistory());
        view.getStockInBtn().addActionListener(this::handleStockIn);
        view.getStockOutBtn().addActionListener(this::handleStockOut);
    }

    public void loadAllData() {
        loadDashboardMetrics();
        loadProductCatalog();
        loadTransactionHistory();
    }

    private void loadDashboardMetrics() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private int totalProducts;
            private int lowStockCount;
            private BigDecimal totalValuation;

            @Override
            protected Void doInBackground() {
                List<Product> all = productDAO.findAll();
                totalProducts = all.size();
                lowStockCount = inventoryService.getLowStockAlerts().size();
                totalValuation = inventoryService.calculateTotalInventoryValuation();
                return null;
            }

            @Override
            protected void done() {
                view.setMetrics(totalProducts, lowStockCount, totalValuation);
                view.setStatusMessage("Metrics updated successfully.");
            }
        };
        worker.execute();
    }

    private void loadProductCatalog() {
        List<Product> products = productDAO.findAll();
        view.populateProductTable(products);
        view.setStatusMessage("Loaded " + products.size() + " catalog items.");
    }

    private void filterProducts() {
        String query = view.getProductSearchField().getText().trim().toLowerCase();
        List<Product> all = productDAO.findAll();
        if (query.isEmpty()) {
            view.populateProductTable(all);
            return;
        }

        List<Product> filtered = all.stream()
                .filter(p -> p.getSku().toLowerCase().contains(query) || p.getName().toLowerCase().contains(query))
                .collect(Collectors.toList());
        view.populateProductTable(filtered);
    }

    private void loadTransactionHistory() {
        List<StockTransaction> transactions = transactionDAO.findAll();
        view.populateTransactionTable(transactions);
        view.setStatusMessage("Loaded " + transactions.size() + " stock transactions.");
    }

    private void handleDeleteProduct(ActionEvent e) {
        int selectedRow = view.getProductTable().getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(view, "Please select a product from the table first.", 
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int productId = (int) view.getProductTableModel().getValueAt(selectedRow, 0);
        String productName = (String) view.getProductTableModel().getValueAt(selectedRow, 2);

        int confirm = JOptionPane.showConfirmDialog(view, 
                "Are you sure you want to delete '" + productName + "' (ID: " + productId + ")?", 
                "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            boolean success = productDAO.delete(productId);
            if (success) {
                loadAllData();
                JOptionPane.showMessageDialog(view, "Product removed successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(view, "Failed to delete product.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleStockIn(ActionEvent e) {
        int selectedRow = view.getProductTable().getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(view, "Select a product in the Products tab first.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int productId = (int) view.getProductTableModel().getValueAt(selectedRow, 0);
        String productName = (String) view.getProductTableModel().getValueAt(selectedRow, 2);

        String input = JOptionPane.showInputDialog(view, 
                "Enter received quantity for: " + productName, "Inbound Receipt (Stock IN)", JOptionPane.QUESTION_MESSAGE);
        if (input == null || input.trim().isEmpty()) return;

        try {
            int qty = Integer.parseInt(input.trim());
            String notes = JOptionPane.showInputDialog(view, "Enter PO number or notes (optional):", "Receipt Notes", JOptionPane.PLAIN_MESSAGE);
            
            boolean ok = inventoryService.recordInboundShipment(productId, qty, notes, view.getCurrentUser().getId());
            if (ok) {
                loadAllData();
                JOptionPane.showMessageDialog(view, "Stock-IN processed successfully: +" + qty + " units.", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(view, "Invalid integer quantity.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(view, "Transaction failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleStockOut(ActionEvent e) {
        int selectedRow = view.getProductTable().getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(view, "Select a product in the Products tab first.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int productId = (int) view.getProductTableModel().getValueAt(selectedRow, 0);
        String productName = (String) view.getProductTableModel().getValueAt(selectedRow, 2);

        String input = JOptionPane.showInputDialog(view, 
                "Enter deduction quantity for: " + productName, "Outbound Dispatch (Stock OUT)", JOptionPane.QUESTION_MESSAGE);
        if (input == null || input.trim().isEmpty()) return;

        try {
            int qty = Integer.parseInt(input.trim());
            String notes = JOptionPane.showInputDialog(view, "Enter Sales Order / Dispatch reason:", "Dispatch Notes", JOptionPane.PLAIN_MESSAGE);

            boolean ok = inventoryService.recordOutboundSale(productId, qty, notes, view.getCurrentUser().getId());
            if (ok) {
                loadAllData();
                JOptionPane.showMessageDialog(view, "Stock-OUT completed: -" + qty + " units.", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(view, "Invalid integer quantity.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(view, "Stock-OUT Rejected: " + ex.getMessage(), "Validation Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void handleLogout(ActionEvent e) {
        int confirm = JOptionPane.showConfirmDialog(view, "Are you sure you want to sign out?", 
                "Confirm Sign Out", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            view.dispose();
            SwingUtilities.invokeLater(() -> {
                LoginFrame loginFrame = new LoginFrame();
                new AuthController(loginFrame, new AuthService());
                loginFrame.setVisible(true);
            });
        }
    }
}

