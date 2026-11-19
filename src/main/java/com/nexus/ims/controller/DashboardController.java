package com.nexus.ims.controller;

import com.nexus.ims.dao.CategoryDAO;
import com.nexus.ims.dao.ProductDAO;
import com.nexus.ims.dao.StockTransactionDAO;
import com.nexus.ims.dao.SupplierDAO;
import com.nexus.ims.model.Category;
import com.nexus.ims.model.Product;
import com.nexus.ims.model.StockTransaction;
import com.nexus.ims.model.Supplier;
import com.nexus.ims.model.User;
import com.nexus.ims.service.AuthService;
import com.nexus.ims.service.InventoryService;
import com.nexus.ims.service.ReportExportService;
import com.nexus.ims.util.IconFactory;
import com.nexus.ims.view.DashboardFrame;
import com.nexus.ims.view.LoginFrame;
import com.nexus.ims.view.ProductFormDialog;
import com.nexus.ims.view.ReportsPanel;
import com.nexus.ims.view.StockTransactionDialog;
import com.nexus.ims.view.SupplierFormDialog;
import com.nexus.ims.view.SuppliersPanel;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller mediating interaction between DashboardFrame views and backend services/DAOs.
 * Executes database operations asynchronously via SwingWorker, filters lists in memory,
 * handles CSV reporting, and enforces Role-Based Access Control (RBAC).
 */
public class DashboardController {
    private final DashboardFrame view;
    private final ProductDAO productDAO;
    private final CategoryDAO categoryDAO;
    private final SupplierDAO supplierDAO;
    private final StockTransactionDAO transactionDAO;
    private final InventoryService inventoryService;
    private final ReportExportService reportExportService;

    private List<Product> cachedProducts = new ArrayList<>();
    private List<Supplier> cachedSuppliers = new ArrayList<>();

    public DashboardController(DashboardFrame view) {
        this.view = view;
        this.productDAO = new ProductDAO();
        this.categoryDAO = new CategoryDAO();
        this.supplierDAO = new SupplierDAO();
        this.transactionDAO = new StockTransactionDAO();
        this.inventoryService = new InventoryService(productDAO, transactionDAO);
        this.reportExportService = new ReportExportService();

        initController();
        applyRolePermissions();
        loadAllData();
    }

    private void initController() {
        // Sidebar Navigation
        view.getNavDashboardBtn().addActionListener(e -> {
            view.showCard(DashboardFrame.CARD_DASHBOARD);
            loadAllData();
        });

        view.getNavProductsBtn().addActionListener(e -> {
            view.showCard(DashboardFrame.CARD_PRODUCTS);
            loadProductCatalog();
        });

        view.getNavTransactionsBtn().addActionListener(e -> {
            view.showCard(DashboardFrame.CARD_TRANSACTIONS);
            loadTransactionHistory();
        });

        view.getNavSuppliersBtn().addActionListener(e -> {
            view.showCard(DashboardFrame.CARD_SUPPLIERS);
            loadSuppliers();
        });

        view.getNavReportsBtn().addActionListener(e -> {
            view.showCard(DashboardFrame.CARD_REPORTS);
            loadAllData();
        });

        view.getLogoutBtn().addActionListener(this::handleLogout);

        // Product Actions
        view.getRefreshProductsBtn().addActionListener(e -> loadProductCatalog());
        view.getExportProductsBtn().addActionListener(e -> showExportPopupMenu(view.getExportProductsBtn()));
        view.getAddProductBtn().addActionListener(this::handleAddProduct);
        view.getEditProductBtn().addActionListener(this::handleEditProduct);
        view.getDeleteProductBtn().addActionListener(this::handleDeleteProduct);

        view.getProductSearchField().getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterProducts(); }
            @Override public void removeUpdate(DocumentEvent e) { filterProducts(); }
            @Override public void changedUpdate(DocumentEvent e) { filterProducts(); }
        });

        // Transaction Actions
        view.getRefreshTransactionsBtn().addActionListener(e -> loadTransactionHistory());
        view.getExportTransactionsBtn().addActionListener(e -> showExportPopupMenu(view.getExportTransactionsBtn()));
        view.getStockInBtn().addActionListener(e -> openStockTransactionDialog(true));
        view.getStockOutBtn().addActionListener(e -> openStockTransactionDialog(false));

        // Suppliers Actions
        SuppliersPanel sp = view.getSuppliersPanel();
        sp.getRefreshBtn().addActionListener(e -> loadSuppliers());
        sp.getAddSupplierBtn().addActionListener(this::handleAddSupplier);
        sp.getEditSupplierBtn().addActionListener(this::handleEditSupplier);
        sp.getDeleteSupplierBtn().addActionListener(this::handleDeleteSupplier);
        sp.getSearchField().getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterSuppliers(); }
            @Override public void removeUpdate(DocumentEvent e) { filterSuppliers(); }
            @Override public void changedUpdate(DocumentEvent e) { filterSuppliers(); }
        });

        // Reports Actions
        ReportsPanel rp = view.getReportsPanel();
        rp.getRefreshReportsBtn().addActionListener(e -> loadAllData());
        rp.getExportReportBtn().addActionListener(e -> showExportPopupMenu(rp.getExportReportBtn()));
    }

    /**
     * Enforces UI permissions based on the authenticated user's role.
     */
    private void applyRolePermissions() {
        User user = view.getCurrentUser();
        User.Role role = user != null ? user.getRole() : User.Role.STAFF;
        SuppliersPanel sp = view.getSuppliersPanel();

        if (role == User.Role.MANAGER) {
            view.getDeleteProductBtn().setEnabled(false);
            view.getDeleteProductBtn().setToolTipText("Delete requires ADMIN role");
            sp.getDeleteSupplierBtn().setEnabled(false);
            sp.getDeleteSupplierBtn().setToolTipText("Delete requires ADMIN role");
        } else if (role == User.Role.STAFF) {
            view.getAddProductBtn().setVisible(false);
            view.getAddProductBtn().setEnabled(false);
            view.getEditProductBtn().setVisible(false);
            view.getEditProductBtn().setEnabled(false);
            view.getDeleteProductBtn().setVisible(false);
            view.getDeleteProductBtn().setEnabled(false);

            sp.getAddSupplierBtn().setVisible(false);
            sp.getAddSupplierBtn().setEnabled(false);
            sp.getEditSupplierBtn().setVisible(false);
            sp.getEditSupplierBtn().setEnabled(false);
            sp.getDeleteSupplierBtn().setVisible(false);
            sp.getDeleteSupplierBtn().setEnabled(false);
        }
    }

    public void loadAllData() {
        view.setStatusMessage("Synchronizing inventory data...");
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private List<Product> products = Collections.emptyList();
            private List<Supplier> suppliers = Collections.emptyList();
            private List<StockTransaction> transactions = Collections.emptyList();
            private List<Product> lowStock = Collections.emptyList();
            private BigDecimal totalValuation = BigDecimal.ZERO;

            @Override
            protected Void doInBackground() {
                products = productDAO.findAll();
                suppliers = supplierDAO.findAll();
                transactions = transactionDAO.findAll();
                lowStock = inventoryService.getLowStockAlerts();
                totalValuation = inventoryService.calculateTotalInventoryValuation();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    cachedProducts = products;
                    cachedSuppliers = suppliers;

                    Map<Integer, Product> productMap = buildProductMap(products);
                    view.setMetrics(products.size(), lowStock.size(), totalValuation);
                    view.populateDashboardWatchlists(lowStock, transactions, productMap);
                    filterProducts();
                    filterSuppliers();
                    view.populateTransactionTable(transactions, productMap);
                    view.getReportsPanel().updateReportData(products);
                    view.setStatusMessage("Synchronized " + products.size() + " products and " + suppliers.size() + " suppliers.");
                } catch (Exception ex) {
                    view.setStatusMessage("Failed to synchronize data: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void loadProductCatalog() {
        view.setStatusMessage("Loading product catalog...");
        SwingWorker<List<Product>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Product> doInBackground() {
                return productDAO.findAll();
            }

            @Override
            protected void done() {
                try {
                    cachedProducts = get();
                    filterProducts();
                    view.setStatusMessage("Loaded " + cachedProducts.size() + " catalog items.");
                } catch (Exception ex) {
                    view.setStatusMessage("Failed to load product catalog: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void filterProducts() {
        String query = view.getProductSearchField().getText().trim().toLowerCase();
        if (query.isEmpty()) {
            view.populateProductTable(cachedProducts);
            return;
        }

        List<Product> filtered = cachedProducts.stream()
                .filter(p -> (p.getSku() != null && p.getSku().toLowerCase().contains(query))
                          || (p.getName() != null && p.getName().toLowerCase().contains(query)))
                .collect(Collectors.toList());
        view.populateProductTable(filtered);
    }

    private void loadTransactionHistory() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private List<Product> products = Collections.emptyList();
            private List<StockTransaction> transactions = Collections.emptyList();

            @Override
            protected Void doInBackground() {
                products = productDAO.findAll();
                transactions = transactionDAO.findAll();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    cachedProducts = products;
                    view.populateTransactionTable(transactions, buildProductMap(products));
                    view.setStatusMessage("Loaded " + transactions.size() + " stock transactions.");
                } catch (Exception ex) {
                    view.setStatusMessage("Failed to load stock transactions: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void loadSuppliers() {
        view.setStatusMessage("Loading suppliers directory...");
        SwingWorker<List<Supplier>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Supplier> doInBackground() {
                return supplierDAO.findAll();
            }

            @Override
            protected void done() {
                try {
                    cachedSuppliers = get();
                    filterSuppliers();
                    view.setStatusMessage("Loaded " + cachedSuppliers.size() + " suppliers.");
                } catch (Exception ex) {
                    view.setStatusMessage("Failed to load suppliers: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void filterSuppliers() {
        SuppliersPanel sp = view.getSuppliersPanel();
        String query = sp.getSearchField().getText().trim().toLowerCase();
        if (query.isEmpty()) {
            sp.populateSuppliers(cachedSuppliers);
            return;
        }

        List<Supplier> filtered = cachedSuppliers.stream()
                .filter(s -> (s.getName() != null && s.getName().toLowerCase().contains(query))
                          || (s.getContactPerson() != null && s.getContactPerson().toLowerCase().contains(query))
                          || (s.getEmail() != null && s.getEmail().toLowerCase().contains(query)))
                .collect(Collectors.toList());
        sp.populateSuppliers(filtered);
    }

    private Map<Integer, Product> buildProductMap(List<Product> products) {
        Map<Integer, Product> map = new HashMap<>();
        if (products != null) {
            for (Product p : products) {
                map.put(p.getId(), p);
            }
        }
        return map;
    }

    // =========================================================================
    // Product CRUD Handlers
    // =========================================================================

    private void handleAddProduct(ActionEvent e) {
        if (view.getCurrentUser().getRole() == User.Role.STAFF) return;

        view.setStatusMessage("Loading form options...");
        SwingWorker<Void, Void> loader = new SwingWorker<>() {
            private List<Category> categories = Collections.emptyList();
            private List<Supplier> suppliers = Collections.emptyList();

            @Override
            protected Void doInBackground() {
                categories = categoryDAO.findAll();
                suppliers = supplierDAO.findAll();
                return null;
            }

            @Override
            protected void done() {
                view.setStatusMessage("System Ready");
                ProductFormDialog dialog = new ProductFormDialog(view, null, categories, suppliers);
                dialog.setVisible(true);
                if (dialog.isConfirmed()) {
                    persistNewProduct(dialog.getProduct());
                }
            }
        };
        loader.execute();
    }

    private void persistNewProduct(Product product) {
        SwingWorker<String, Void> saver = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                if (productDAO.findBySku(product.getSku()) != null) {
                    return "A product with SKU '" + product.getSku() + "' already exists.";
                }
                return productDAO.save(product) ? null : "Failed to save product to database.";
            }

            @Override
            protected void done() {
                try {
                    String error = get();
                    if (error == null) {
                        loadAllData();
                        JOptionPane.showMessageDialog(view, "Product '" + product.getName() + "' added successfully.",
                                "Product Saved", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(view, error, "Save Failed", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(view, "Error saving product: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        saver.execute();
    }

    private void handleEditProduct(ActionEvent e) {
        if (view.getCurrentUser().getRole() == User.Role.STAFF) return;

        int selectedRow = view.getProductTable().getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(view, "Please select a product from the table to edit.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int productId = (int) view.getProductTableModel().getValueAt(selectedRow, 0);
        view.setStatusMessage("Loading product details...");

        SwingWorker<Void, Void> loader = new SwingWorker<>() {
            private Product existing;
            private List<Category> categories = Collections.emptyList();
            private List<Supplier> suppliers = Collections.emptyList();

            @Override
            protected Void doInBackground() {
                existing = productDAO.findById(productId);
                categories = categoryDAO.findAll();
                suppliers = supplierDAO.findAll();
                return null;
            }

            @Override
            protected void done() {
                view.setStatusMessage("System Ready");
                if (existing == null) {
                    JOptionPane.showMessageDialog(view, "Selected product could not be found in database.",
                            "Not Found", JOptionPane.ERROR_MESSAGE);
                    loadProductCatalog();
                    return;
                }

                ProductFormDialog dialog = new ProductFormDialog(view, existing, categories, suppliers);
                dialog.setVisible(true);
                if (dialog.isConfirmed()) {
                    persistUpdatedProduct(dialog.getProduct());
                }
            }
        };
        loader.execute();
    }

    private void persistUpdatedProduct(Product product) {
        SwingWorker<String, Void> updater = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                Product conflict = productDAO.findBySku(product.getSku());
                if (conflict != null && conflict.getId() != product.getId()) {
                    return "Another product already uses SKU '" + product.getSku() + "'.";
                }
                return productDAO.update(product) ? null : "Failed to update product in database.";
            }

            @Override
            protected void done() {
                try {
                    String error = get();
                    if (error == null) {
                        loadAllData();
                        JOptionPane.showMessageDialog(view, "Product '" + product.getName() + "' updated successfully.",
                                "Product Updated", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(view, error, "Update Failed", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(view, "Error updating product: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        updater.execute();
    }

    private void handleDeleteProduct(ActionEvent e) {
        if (view.getCurrentUser().getRole() != User.Role.ADMIN) {
            JOptionPane.showMessageDialog(view, "Only Administrators are permitted to delete products.",
                    "Access Denied", JOptionPane.WARNING_MESSAGE);
            return;
        }

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
            SwingWorker<Boolean, Void> deleter = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() {
                    return productDAO.delete(productId);
                }

                @Override
                protected void done() {
                    try {
                        if (get()) {
                            loadAllData();
                            JOptionPane.showMessageDialog(view, "Product removed successfully.",
                                    "Success", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(view, "Failed to delete product.",
                                    "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(view, "Error deleting product: " + ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            deleter.execute();
        }
    }

    // =========================================================================
    // Supplier CRUD Handlers
    // =========================================================================

    private void handleAddSupplier(ActionEvent e) {
        if (view.getCurrentUser().getRole() == User.Role.STAFF) return;

        SupplierFormDialog dialog = new SupplierFormDialog(view, null);
        dialog.setVisible(true);
        if (!dialog.isConfirmed()) return;

        Supplier supplier = dialog.getSupplier();
        SwingWorker<Boolean, Void> saver = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return supplierDAO.save(supplier);
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        loadAllData();
                        JOptionPane.showMessageDialog(view, "Supplier '" + supplier.getName() + "' saved successfully.",
                                "Supplier Saved", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(view, "Failed to save supplier (name may already exist).",
                                "Save Failed", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(view, "Error saving supplier: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        saver.execute();
    }

    private void handleEditSupplier(ActionEvent e) {
        if (view.getCurrentUser().getRole() == User.Role.STAFF) return;

        SuppliersPanel sp = view.getSuppliersPanel();
        int selectedRow = sp.getSupplierTable().getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(view, "Please select a supplier from the table to edit.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int supplierId = (int) sp.getSupplierTableModel().getValueAt(selectedRow, 0);
        SwingWorker<Supplier, Void> loader = new SwingWorker<>() {
            @Override
            protected Supplier doInBackground() {
                return supplierDAO.findById(supplierId);
            }

            @Override
            protected void done() {
                try {
                    Supplier existing = get();
                    if (existing == null) {
                        JOptionPane.showMessageDialog(view, "Supplier record no longer exists.",
                                "Not Found", JOptionPane.ERROR_MESSAGE);
                        loadSuppliers();
                        return;
                    }
                    SupplierFormDialog dialog = new SupplierFormDialog(view, existing);
                    dialog.setVisible(true);
                    if (dialog.isConfirmed()) {
                        persistUpdatedSupplier(dialog.getSupplier());
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(view, "Error loading supplier: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        loader.execute();
    }

    private void persistUpdatedSupplier(Supplier supplier) {
        SwingWorker<Boolean, Void> updater = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return supplierDAO.update(supplier);
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        loadAllData();
                        JOptionPane.showMessageDialog(view, "Supplier '" + supplier.getName() + "' updated successfully.",
                                "Supplier Updated", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(view, "Failed to update supplier.",
                                "Update Failed", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(view, "Error updating supplier: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        updater.execute();
    }

    private void handleDeleteSupplier(ActionEvent e) {
        if (view.getCurrentUser().getRole() != User.Role.ADMIN) {
            JOptionPane.showMessageDialog(view, "Only Administrators are permitted to delete suppliers.",
                    "Access Denied", JOptionPane.WARNING_MESSAGE);
            return;
        }

        SuppliersPanel sp = view.getSuppliersPanel();
        int selectedRow = sp.getSupplierTable().getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(view, "Please select a supplier to delete.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int supplierId = (int) sp.getSupplierTableModel().getValueAt(selectedRow, 0);
        String supplierName = (String) sp.getSupplierTableModel().getValueAt(selectedRow, 1);

        int confirm = JOptionPane.showConfirmDialog(view,
                "Are you sure you want to delete supplier '" + supplierName + "' (ID: " + supplierId + ")?",
                "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            SwingWorker<Boolean, Void> deleter = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() {
                    return supplierDAO.delete(supplierId);
                }

                @Override
                protected void done() {
                    try {
                        if (get()) {
                            loadAllData();
                            JOptionPane.showMessageDialog(view, "Supplier deleted successfully.",
                                    "Success", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(view, "Failed to delete supplier.",
                                    "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(view, "Error deleting supplier: " + ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            deleter.execute();
        }
    }

    // =========================================================================
    // Stock Transaction Handlers
    // =========================================================================

    private void openStockTransactionDialog(boolean inboundMode) {
        Integer preselectedProductId = null;
        int selectedRow = view.getProductTable().getSelectedRow();
        if (selectedRow != -1) {
            preselectedProductId = (Integer) view.getProductTableModel().getValueAt(selectedRow, 0);
        }
        final Integer preselectedId = preselectedProductId;

        SwingWorker<List<Product>, Void> loader = new SwingWorker<>() {
            @Override
            protected List<Product> doInBackground() {
                return productDAO.findAll();
            }

            @Override
            protected void done() {
                try {
                    List<Product> products = get();
                    cachedProducts = products;
                    if (products.isEmpty()) {
                        JOptionPane.showMessageDialog(view, "No products found in catalog. Add a product first.",
                                "Empty Catalog", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    StockTransactionDialog dialog = new StockTransactionDialog(view, inboundMode, products, preselectedId);
                    dialog.setVisible(true);
                    if (dialog.isConfirmed()) {
                        executeStockTransaction(
                                inboundMode,
                                dialog.getSelectedProduct(),
                                dialog.getSelectedType(),
                                dialog.getQuantity(),
                                dialog.getNotes()
                        );
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(view, "Unable to load products: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        loader.execute();
    }

    private void executeStockTransaction(boolean inboundMode, Product product,
                                         StockTransaction.TransactionType type, int qty, String notes) {
        Integer userId = view.getCurrentUser() != null ? view.getCurrentUser().getId() : null;

        SwingWorker<Boolean, Void> txWorker = new SwingWorker<>() {
            private String failureMsg;

            @Override
            protected Boolean doInBackground() {
                try {
                    if (type == StockTransaction.TransactionType.INBOUND_PURCHASE) {
                        return inventoryService.recordInboundShipment(product.getId(), qty, notes, userId);
                    } else if (type == StockTransaction.TransactionType.OUTBOUND_SALE) {
                        return inventoryService.recordOutboundSale(product.getId(), qty, notes, userId);
                    } else {
                        int delta = inboundMode ? qty : -qty;
                        return inventoryService.recordAdjustment(product.getId(), delta, type, notes, userId);
                    }
                } catch (Exception ex) {
                    failureMsg = ex.getMessage();
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        loadAllData();
                        String sign = inboundMode ? "+" : "-";
                        JOptionPane.showMessageDialog(view,
                                String.format("%s processed for '%s': %s%d units.",
                                        type.name(), product.getName(), sign, qty),
                                "Transaction Committed", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(view,
                                failureMsg != null ? failureMsg : "Transaction could not be completed.",
                                "Transaction Rejected", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(view, "Transaction failed: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        txWorker.execute();
    }

    // =========================================================================
    // Multi-Format Export Dropdown & Background Execution
    // =========================================================================

    private void showExportPopupMenu(JComponent invoker) {
        JPopupMenu popup = new JPopupMenu();

        JMenuItem pdfItem = new JMenuItem("Export as PDF Document (.pdf)",
                IconFactory.pdfFile(16, new Color(220, 53, 69)));
        pdfItem.setIconTextGap(10);
        pdfItem.addActionListener(e -> promptAndExport(ReportExportService.ExportFormat.PDF));

        JMenuItem excelItem = new JMenuItem("Export as Excel Workbook (.xlsx)",
                IconFactory.excelFile(16, new Color(25, 135, 84)));
        excelItem.setIconTextGap(10);
        excelItem.addActionListener(e -> promptAndExport(ReportExportService.ExportFormat.EXCEL));

        JMenuItem csvItem = new JMenuItem("Export as CSV Spreadsheet (.csv)",
                IconFactory.csvFile(16, new Color(13, 110, 253)));
        csvItem.setIconTextGap(10);
        csvItem.addActionListener(e -> promptAndExport(ReportExportService.ExportFormat.CSV));

        JMenuItem jsonItem = new JMenuItem("Export as JSON Data (.json)",
                IconFactory.jsonFile(16, new Color(99, 102, 241)));
        jsonItem.setIconTextGap(10);
        jsonItem.addActionListener(e -> promptAndExport(ReportExportService.ExportFormat.JSON));

        popup.add(pdfItem);
        popup.add(excelItem);
        popup.addSeparator();
        popup.add(csvItem);
        popup.add(jsonItem);

        popup.show(invoker, 0, invoker.getHeight() + 2);
    }

    private void promptAndExport(ReportExportService.ExportFormat format) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Inventory Report (" + format.getExtension().toUpperCase() + ")");
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(new FileNameExtensionFilter(format.getDescription(), format.getExtension()));
        chooser.setSelectedFile(new File("nexus-inventory-report-" + LocalDate.now() + "." + format.getExtension()));

        if (chooser.showSaveDialog(view) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File targetFile = chooser.getSelectedFile();
        String requiredExt = "." + format.getExtension();
        if (!targetFile.getName().toLowerCase().endsWith(requiredExt)) {
            targetFile = new File(targetFile.getParentFile(), targetFile.getName() + requiredExt);
        }

        if (targetFile.exists()) {
            int overwrite = JOptionPane.showConfirmDialog(
                    view,
                    "File '" + targetFile.getName() + "' already exists. Do you want to overwrite it?",
                    "Confirm Overwrite",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (overwrite != JOptionPane.YES_OPTION) {
                return;
            }
        }

        final File exportFile = targetFile;
        view.setStatusMessage("Generating " + format.getExtension().toUpperCase() + " report...");

        SwingWorker<Integer, Void> exporter = new SwingWorker<>() {
            @Override
            protected Integer doInBackground() throws Exception {
                List<Product> products = productDAO.findAll();
                List<StockTransaction> transactions = transactionDAO.findAll();
                List<Supplier> suppliers = supplierDAO.findAll();

                reportExportService.exportReport(
                        format,
                        exportFile,
                        view.getCurrentUser(),
                        products,
                        transactions,
                        suppliers
                );
                return products.size();
            }

            @Override
            protected void done() {
                try {
                    int count = get();
                    view.setStatusMessage("Exported " + format.getExtension().toUpperCase()
                            + " report (" + count + " items) to " + exportFile.getName());

                    Object[] options = {"Open File", "OK"};
                    int choice = JOptionPane.showOptionDialog(
                            view,
                            "Inventory report exported (" + count + " products) to:\n" + exportFile.getAbsolutePath(),
                            "Export Complete",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.INFORMATION_MESSAGE,
                            null,
                            options,
                            options[1]
                    );

                    if (choice == 0 && Desktop.isDesktopSupported()) {
                        try {
                            Desktop.getDesktop().open(exportFile);
                        } catch (Exception openEx) {
                            JOptionPane.showMessageDialog(view,
                                    "Report saved, but could not be launched automatically:\n" + openEx.getMessage(),
                                    "Open File Notice", JOptionPane.WARNING_MESSAGE);
                        }
                    }
                } catch (Exception ex) {
                    view.setStatusMessage("Report export failed.");
                    JOptionPane.showMessageDialog(view,
                            "Failed to export " + format.getExtension().toUpperCase() + " report:\n" + ex.getMessage(),
                            "Export Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        exporter.execute();
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
