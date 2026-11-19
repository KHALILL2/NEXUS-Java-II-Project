package com.nexus.ims.controller;

import com.nexus.ims.dao.CategoryDAO;
import com.nexus.ims.dao.ProductDAO;
import com.nexus.ims.dao.StockTransactionDAO;
import com.nexus.ims.dao.SupplierDAO;
import com.nexus.ims.dao.UserDAO;
import com.nexus.ims.model.Category;
import com.nexus.ims.model.Product;
import com.nexus.ims.model.StockTransaction;
import com.nexus.ims.model.Supplier;
import com.nexus.ims.model.User;
import com.nexus.ims.service.AuthService;
import com.nexus.ims.service.InventoryService;
import com.nexus.ims.service.ReportExportService;
import com.nexus.ims.util.IconFactory;
import com.nexus.ims.util.LanguageManager;
import com.nexus.ims.util.ThemeManager;
import com.nexus.ims.view.CategoryManagementDialog;
import com.nexus.ims.view.DashboardFrame;
import com.nexus.ims.view.LoginFrame;
import com.nexus.ims.view.ProductFormDialog;
import com.nexus.ims.view.ReportsPanel;
import com.nexus.ims.view.StockTransactionDialog;
import com.nexus.ims.view.SupplierFormDialog;
import com.nexus.ims.view.SuppliersPanel;
import com.nexus.ims.view.UserFormDialog;
import com.nexus.ims.view.UsersPanel;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Coordinates DashboardFrame views with backend DAOs and services, handling
 * background SwingWorker loading, RBAC rules, search/filters, and report exports.
 */
public class DashboardController {
    private final DashboardFrame view;
    private final ProductDAO productDAO;
    private final CategoryDAO categoryDAO;
    private final SupplierDAO supplierDAO;
    private final StockTransactionDAO transactionDAO;
    private final UserDAO userDAO;
    private final InventoryService inventoryService;
    private final ReportExportService reportExportService;

    private List<Product> cachedProducts = new ArrayList<>();
    private List<Category> cachedCategories = new ArrayList<>();
    private List<Supplier> cachedSuppliers = new ArrayList<>();
    private List<StockTransaction> cachedTransactions = new ArrayList<>();
    private List<User> cachedUsers = new ArrayList<>();

    private final Runnable languageRefreshListener = this::applyRoleTooltips;

    public DashboardController(DashboardFrame view) {
        this.view = view;
        this.productDAO = new ProductDAO();
        this.categoryDAO = new CategoryDAO();
        this.supplierDAO = new SupplierDAO();
        this.transactionDAO = new StockTransactionDAO();
        this.userDAO = new UserDAO();
        this.inventoryService = new InventoryService(productDAO, transactionDAO);
        this.reportExportService = new ReportExportService();

        initController();
        applyRolePermissions();
        LanguageManager.addLanguageListener(languageRefreshListener);
        loadAllData();
    }

    private void initController() {
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

        view.getNavUsersBtn().addActionListener(e -> {
            if (view.getCurrentUser().getRole() == User.Role.ADMIN) {
                view.showCard(DashboardFrame.CARD_USERS);
                loadUsers();
            }
        });

        view.getLogoutBtn().addActionListener(this::handleLogout);
        view.registerRefreshShortcut(this::loadAllData);

        // Overview Quick Restock Actions
        view.getQuickRestockBtn().addActionListener(e -> handleQuickRestock());
        view.getWatchlistTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)
                        && view.getWatchlistTable().getSelectedRow() != -1) {
                    handleQuickRestock();
                }
            }
        });

        // Product Actions & Filters
        view.getRefreshProductsBtn().addActionListener(e -> loadProductCatalog());
        view.getManageCategoriesBtn().addActionListener(e -> handleManageCategories());
        view.getExportProductsBtn().addActionListener(e -> showExportPopupMenu(view.getExportProductsBtn()));
        view.getAddProductBtn().addActionListener(this::handleAddProduct);
        view.getEditProductBtn().addActionListener(this::handleEditProduct);
        view.getDeleteProductBtn().addActionListener(this::handleDeleteProduct);
        view.getProductStatusFilterCombo().addActionListener(e -> filterProducts());

        view.getProductSearchField().getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterProducts(); }
            @Override public void removeUpdate(DocumentEvent e) { filterProducts(); }
            @Override public void changedUpdate(DocumentEvent e) { filterProducts(); }
        });

        view.getProductTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)
                        && view.getProductTable().getSelectedRow() != -1
                        && view.getCurrentUser().getRole() != User.Role.STAFF) {
                    handleEditProduct(null);
                }
            }
        });

        // Product Context Menu Actions
        view.getCtxEditProductItem().addActionListener(this::handleEditProduct);
        view.getCtxStockInItem().addActionListener(e -> openStockTransactionDialog(true, null));
        view.getCtxStockOutItem().addActionListener(e -> openStockTransactionDialog(false, null));
        view.getCtxCopySkuItem().addActionListener(e -> handleCopySelectedProductSku());
        view.getCtxDeleteProductItem().addActionListener(this::handleDeleteProduct);

        // Transaction Actions & Filters
        view.getRefreshTransactionsBtn().addActionListener(e -> loadTransactionHistory());
        view.getExportTransactionsBtn().addActionListener(e -> showExportPopupMenu(view.getExportTransactionsBtn()));
        view.getStockInBtn().addActionListener(e -> openStockTransactionDialog(true, null));
        view.getStockOutBtn().addActionListener(e -> openStockTransactionDialog(false, null));
        view.getTransactionTypeFilterCombo().addActionListener(e -> filterTransactions());
        view.getTransactionTimeframeFilterCombo().addActionListener(e -> filterTransactions());

        view.getTransactionSearchField().getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterTransactions(); }
            @Override public void removeUpdate(DocumentEvent e) { filterTransactions(); }
            @Override public void changedUpdate(DocumentEvent e) { filterTransactions(); }
        });

        // Suppliers Actions
        SuppliersPanel sp = view.getSuppliersPanel();
        sp.getRefreshBtn().addActionListener(e -> loadSuppliers());
        sp.getExportSuppliersBtn().addActionListener(e -> showExportPopupMenu(sp.getExportSuppliersBtn()));
        sp.getAddSupplierBtn().addActionListener(this::handleAddSupplier);
        sp.getEditSupplierBtn().addActionListener(this::handleEditSupplier);
        sp.getDeleteSupplierBtn().addActionListener(this::handleDeleteSupplier);
        sp.getSearchField().getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterSuppliers(); }
            @Override public void removeUpdate(DocumentEvent e) { filterSuppliers(); }
            @Override public void changedUpdate(DocumentEvent e) { filterSuppliers(); }
        });

        sp.getSupplierTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)
                        && sp.getSupplierTable().getSelectedRow() != -1
                        && view.getCurrentUser().getRole() != User.Role.STAFF) {
                    handleEditSupplier(null);
                }
            }
        });

        // Reports Actions
        ReportsPanel rp = view.getReportsPanel();
        rp.getRefreshReportsBtn().addActionListener(e -> loadAllData());
        rp.getExportReportBtn().addActionListener(e -> showExportPopupMenu(rp.getExportReportBtn()));

        // Users Actions (Admin-Only)
        UsersPanel up = view.getUsersPanel();
        up.getRefreshBtn().addActionListener(e -> loadUsers());
        up.getAddUserBtn().addActionListener(this::handleAddUser);
        up.getEditUserBtn().addActionListener(this::handleEditUser);
        up.getResetPasswordBtn().addActionListener(this::handleResetPassword);
        up.getToggleActiveBtn().addActionListener(this::handleToggleUserActive);
        up.getRoleFilterCombo().addActionListener(e -> filterUsers());
        up.getSearchField().getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterUsers(); }
            @Override public void removeUpdate(DocumentEvent e) { filterUsers(); }
            @Override public void changedUpdate(DocumentEvent e) { filterUsers(); }
        });
        up.getUserTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)
                        && up.getUserTable().getSelectedRow() != -1
                        && view.getCurrentUser().getRole() == User.Role.ADMIN) {
                    handleEditUser(null);
                }
            }
        });
    }

    private void applyRolePermissions() {
        User user = view.getCurrentUser();
        User.Role role = user != null ? user.getRole() : User.Role.STAFF;
        SuppliersPanel sp = view.getSuppliersPanel();

        if (role == User.Role.MANAGER) {
            view.getDeleteProductBtn().setEnabled(false);
            sp.getDeleteSupplierBtn().setEnabled(false);
        } else if (role == User.Role.STAFF) {
            view.getManageCategoriesBtn().setVisible(false);
            view.getManageCategoriesBtn().setEnabled(false);
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

        if (role != User.Role.ADMIN) {
            view.getNavUsersBtn().setVisible(false);
            view.getNavUsersBtn().setEnabled(false);
        }
        view.setProductRolePermissions(role != User.Role.STAFF, role == User.Role.ADMIN);
        sp.setRolePermissions(role != User.Role.STAFF, role == User.Role.ADMIN);
        applyRoleTooltips();
    }

    private void applyRoleTooltips() {
        User user = view.getCurrentUser();
        if (user != null && user.getRole() == User.Role.MANAGER) {
            String tip = LanguageManager.get("rbac.deleteAdminOnly");
            view.getDeleteProductBtn().setToolTipText(tip);
            view.getSuppliersPanel().getDeleteSupplierBtn().setToolTipText(tip);
        }
    }

    public void loadAllData() {
        view.setBusy(true, LanguageManager.get("status.syncing"));
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private List<Product> products = Collections.emptyList();
            private List<Category> categories = Collections.emptyList();
            private List<Supplier> suppliers = Collections.emptyList();
            private List<StockTransaction> transactions = Collections.emptyList();
            private List<User> users = Collections.emptyList();
            private List<Product> lowStock = Collections.emptyList();
            private BigDecimal totalValuation = BigDecimal.ZERO;
            private BigDecimal totalRetailValuation = BigDecimal.ZERO;
            private int totalUnits = 0;

            @Override
            protected Void doInBackground() {
                products = productDAO.findAll();
                categories = categoryDAO.findAll();
                suppliers = supplierDAO.findAll();
                transactions = transactionDAO.findAll();
                users = userDAO.findAll();
                lowStock = inventoryService.getLowStockAlerts();
                totalValuation = inventoryService.calculateTotalInventoryValuation();
                for (Product p : products) {
                    int qty = Math.max(0, p.getQuantityInStock());
                    totalUnits += qty;
                    BigDecimal retail = p.getUnitPrice() != null ? p.getUnitPrice() : BigDecimal.ZERO;
                    totalRetailValuation = totalRetailValuation.add(retail.multiply(BigDecimal.valueOf(qty)));
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    cachedProducts = products;
                    cachedCategories = categories;
                    cachedSuppliers = suppliers;
                    cachedTransactions = transactions;
                    cachedUsers = users;

                    Map<Integer, Product> productMap = buildProductMap(products);
                    Map<Integer, String> categoryNames = buildCategoryNameMap(categories);
                    view.setMetrics(products.size(), categories.size(), totalUnits,
                            lowStock.size(), totalValuation, totalRetailValuation);
                    view.populateDashboardWatchlists(lowStock, transactions, productMap);
                    filterProducts();
                    filterSuppliers();
                    filterTransactions();
                    filterUsers();
                    view.getReportsPanel().updateReportData(products, categoryNames);
                    view.setBusy(false, LanguageManager.format("status.syncedSummary", products.size(), suppliers.size()));
                } catch (Exception ex) {
                    view.setBusy(false, LanguageManager.format("status.syncFailed", ex.getMessage()));
                }
            }
        };
        worker.execute();
    }

    private void loadProductCatalog() {
        view.setBusy(true, LanguageManager.get("status.loading"));
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private List<Product> products = Collections.emptyList();
            private List<Category> categories = Collections.emptyList();
            private List<Supplier> suppliers = Collections.emptyList();

            @Override
            protected Void doInBackground() {
                products = productDAO.findAll();
                categories = categoryDAO.findAll();
                suppliers = supplierDAO.findAll();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    cachedProducts = products;
                    cachedCategories = categories;
                    cachedSuppliers = suppliers;
                    filterProducts();
                    view.setBusy(false, LanguageManager.format("status.loadedProducts", cachedProducts.size()));
                } catch (Exception ex) {
                    view.setBusy(false, LanguageManager.format("status.syncFailed", ex.getMessage()));
                }
            }
        };
        worker.execute();
    }

    private void filterProducts() {
        String query = view.getProductSearchField().getText().trim().toLowerCase();
        String statusFilter = (String) view.getProductStatusFilterCombo().getSelectedItem();

        Map<Integer, String> categoryNames = buildCategoryNameMap(cachedCategories);
        Map<Integer, String> supplierNames = buildSupplierNameMap(cachedSuppliers);
        String unassigned = LanguageManager.get("common.unassigned").toLowerCase();

        List<Product> filtered = cachedProducts.stream()
                .filter(p -> {
                    if ("IN_STOCK".equals(statusFilter) && p.isLowStock()) {
                        return false;
                    }
                    if ("LOW_STOCK".equals(statusFilter) && !p.isLowStock()) {
                        return false;
                    }
                    if (query.isEmpty()) {
                        return true;
                    }
                    String catName = p.getCategoryId() != null
                            ? categoryNames.getOrDefault(p.getCategoryId(), unassigned)
                            : unassigned;
                    String supName = p.getSupplierId() != null
                            ? supplierNames.getOrDefault(p.getSupplierId(), unassigned)
                            : unassigned;

                    return (p.getSku() != null && p.getSku().toLowerCase().contains(query))
                            || (p.getName() != null && p.getName().toLowerCase().contains(query))
                            || catName.toLowerCase().contains(query)
                            || supName.toLowerCase().contains(query);
                })
                .collect(Collectors.toList());

        view.populateProductTable(filtered, categoryNames, supplierNames, cachedProducts.size());
    }

    private void loadTransactionHistory() {
        view.setBusy(true, LanguageManager.get("status.loading"));
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private List<Product> products = Collections.emptyList();
            private List<StockTransaction> transactions = Collections.emptyList();
            private List<User> users = Collections.emptyList();

            @Override
            protected Void doInBackground() {
                products = productDAO.findAll();
                transactions = transactionDAO.findAll();
                users = userDAO.findAll();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    cachedProducts = products;
                    cachedTransactions = transactions;
                    cachedUsers = users;
                    filterTransactions();
                    view.setBusy(false, LanguageManager.format("status.loadedTransactions", transactions.size()));
                } catch (Exception ex) {
                    view.setBusy(false, LanguageManager.format("status.syncFailed", ex.getMessage()));
                }
            }
        };
        worker.execute();
    }

    private void filterTransactions() {
        String query = view.getTransactionSearchField().getText().trim().toLowerCase();
        String typeFilter = (String) view.getTransactionTypeFilterCombo().getSelectedItem();
        String timeframeFilter = (String) view.getTransactionTimeframeFilterCombo().getSelectedItem();
        LocalDateTime cutoff = resolveTimeframeCutoff(timeframeFilter);

        Map<Integer, Product> productMap = buildProductMap(cachedProducts);
        Map<Integer, User> userMap = buildUserMap(cachedUsers);
        String systemLabel = LanguageManager.get("common.system").toLowerCase();

        List<StockTransaction> filtered = cachedTransactions.stream()
                .filter(tx -> {
                    if (typeFilter != null && !"ALL".equals(typeFilter)
                            && !tx.getTransactionType().name().equals(typeFilter)) {
                        return false;
                    }
                    if (cutoff != null && (tx.getCreatedAt() == null || tx.getCreatedAt().isBefore(cutoff))) {
                        return false;
                    }
                    if (query.isEmpty()) {
                        return true;
                    }
                    Product prod = productMap.get(tx.getProductId());
                    String sku = prod != null && prod.getSku() != null ? prod.getSku().toLowerCase() : "";
                    String prodName = prod != null && prod.getName() != null ? prod.getName().toLowerCase() : "";
                    String notes = tx.getNotes() != null ? tx.getNotes().toLowerCase() : "";
                    User op = tx.getPerformedByUserId() != null ? userMap.get(tx.getPerformedByUserId()) : null;
                    String opName = op != null
                            ? (op.getUsername() + " " + op.getFullName()).toLowerCase()
                            : systemLabel;

                    return sku.contains(query) || prodName.contains(query)
                            || notes.contains(query) || opName.contains(query);
                })
                .collect(Collectors.toList());

        view.populateTransactionTable(filtered, productMap, userMap);
    }

    private LocalDateTime resolveTimeframeCutoff(String timeframeFilter) {
        if ("TODAY".equals(timeframeFilter)) {
            return LocalDate.now().atStartOfDay();
        }
        if ("DAYS_7".equals(timeframeFilter)) {
            return LocalDateTime.now().minusDays(7);
        }
        if ("DAYS_30".equals(timeframeFilter)) {
            return LocalDateTime.now().minusDays(30);
        }
        return null;
    }

    private void loadSuppliers() {
        view.setBusy(true, LanguageManager.get("status.loading"));
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private List<Supplier> suppliers = Collections.emptyList();
            private List<Product> products = Collections.emptyList();

            @Override
            protected Void doInBackground() {
                suppliers = supplierDAO.findAll();
                products = productDAO.findAll();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    cachedSuppliers = suppliers;
                    cachedProducts = products;
                    filterSuppliers();
                    view.setBusy(false, LanguageManager.format("status.loadedSuppliers", cachedSuppliers.size()));
                } catch (Exception ex) {
                    view.setBusy(false, LanguageManager.format("status.syncFailed", ex.getMessage()));
                }
            }
        };
        worker.execute();
    }

    private void filterSuppliers() {
        SuppliersPanel sp = view.getSuppliersPanel();
        String query = sp.getSearchField().getText().trim().toLowerCase();
        Map<Integer, Integer> skuCounts = buildSupplierSkuCounts(cachedProducts);

        if (query.isEmpty()) {
            sp.populateSuppliers(cachedSuppliers, skuCounts);
            return;
        }

        List<Supplier> filtered = cachedSuppliers.stream()
                .filter(s -> (s.getName() != null && s.getName().toLowerCase().contains(query))
                        || (s.getContactPerson() != null && s.getContactPerson().toLowerCase().contains(query))
                        || (s.getEmail() != null && s.getEmail().toLowerCase().contains(query)))
                .collect(Collectors.toList());
        sp.populateSuppliers(filtered, skuCounts);
    }

    private Map<Integer, Integer> buildSupplierSkuCounts(List<Product> products) {
        Map<Integer, Integer> counts = new HashMap<>();
        if (products != null) {
            for (Product p : products) {
                if (p.getSupplierId() != null) {
                    counts.merge(p.getSupplierId(), 1, Integer::sum);
                }
            }
        }
        return counts;
    }

    private void handleCopySelectedProductSku() {
        int viewRow = view.getProductTable().getSelectedRow();
        if (viewRow == -1) {
            return;
        }
        int modelRow = view.getProductTable().convertRowIndexToModel(viewRow);
        String sku = String.valueOf(view.getProductTableModel().getValueAt(modelRow, 1));
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sku), null);
        view.setStatusMessage(LanguageManager.format("products.ctx.skuCopied", sku));
    }

    private void loadUsers() {
        if (view.getCurrentUser().getRole() != User.Role.ADMIN) return;
        view.setBusy(true, LanguageManager.get("status.loading"));
        SwingWorker<List<User>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<User> doInBackground() {
                return userDAO.findAll();
            }

            @Override
            protected void done() {
                try {
                    cachedUsers = get();
                    filterUsers();
                    view.setBusy(false, LanguageManager.format("status.loadedUsers", cachedUsers.size()));
                } catch (Exception ex) {
                    view.setBusy(false, LanguageManager.format("status.syncFailed", ex.getMessage()));
                }
            }
        };
        worker.execute();
    }

    private void filterUsers() {
        UsersPanel up = view.getUsersPanel();
        String query = up.getSearchField().getText().trim().toLowerCase();
        String roleFilter = (String) up.getRoleFilterCombo().getSelectedItem();

        List<User> filtered = cachedUsers.stream()
                .filter(u -> {
                    if (roleFilter != null && !"ALL".equals(roleFilter)
                            && !u.getRole().name().equals(roleFilter)) {
                        return false;
                    }
                    if (query.isEmpty()) {
                        return true;
                    }
                    return (u.getUsername() != null && u.getUsername().toLowerCase().contains(query))
                            || (u.getFullName() != null && u.getFullName().toLowerCase().contains(query))
                            || (u.getEmail() != null && u.getEmail().toLowerCase().contains(query));
                })
                .collect(Collectors.toList());

        up.populateUsers(filtered);
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

    private Map<Integer, String> buildCategoryNameMap(List<Category> categories) {
        Map<Integer, String> map = new HashMap<>();
        if (categories != null) {
            for (Category c : categories) {
                map.put(c.getId(), c.getName());
            }
        }
        return map;
    }

    private Map<Integer, String> buildSupplierNameMap(List<Supplier> suppliers) {
        Map<Integer, String> map = new HashMap<>();
        if (suppliers != null) {
            for (Supplier s : suppliers) {
                map.put(s.getId(), s.getName());
            }
        }
        return map;
    }

    private Map<Integer, User> buildUserMap(List<User> users) {
        Map<Integer, User> map = new HashMap<>();
        if (users != null) {
            for (User u : users) {
                map.put(u.getId(), u);
            }
        }
        return map;
    }

    // =========================================================================
    // Product CRUD Handlers
    // =========================================================================

    private void handleAddProduct(ActionEvent e) {
        if (view.getCurrentUser().getRole() == User.Role.STAFF) return;

        view.setBusy(true, LanguageManager.get("status.loading"));
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
                view.setBusy(false, LanguageManager.get("status.ready"));
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
        view.setBusy(true, LanguageManager.get("status.syncing"));
        SwingWorker<String, Void> saver = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                if (productDAO.findBySku(product.getSku()) != null) {
                    return LanguageManager.format("products.msg.duplicateSku", product.getSku());
                }
                return productDAO.save(product) ? null : LanguageManager.get("products.msg.saveFailed");
            }

            @Override
            protected void done() {
                view.setBusy(false, null);
                try {
                    String error = get();
                    if (error == null) {
                        loadAllData();
                        JOptionPane.showMessageDialog(view,
                                LanguageManager.format("products.msg.added", product.getName()),
                                LanguageManager.get("common.success"), JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(view, error,
                                LanguageManager.get("common.saveFailed"), JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(view, ex.getMessage(),
                            LanguageManager.get("common.error"), JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        saver.execute();
    }

    private void handleEditProduct(ActionEvent e) {
        if (view.getCurrentUser().getRole() == User.Role.STAFF) return;

        int viewRow = view.getProductTable().getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(view, LanguageManager.get("products.selectEdit"),
                    LanguageManager.get("common.noSelection"), JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = view.getProductTable().convertRowIndexToModel(viewRow);
        int productId = (int) view.getProductTableModel().getValueAt(modelRow, 0);
        view.setBusy(true, LanguageManager.get("products.msg.loadingDetails"));

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
                view.setBusy(false, LanguageManager.get("status.ready"));
                if (existing == null) {
                    JOptionPane.showMessageDialog(view, LanguageManager.get("products.msg.notFound"),
                            LanguageManager.get("common.notFound"), JOptionPane.ERROR_MESSAGE);
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
        view.setBusy(true, LanguageManager.get("status.syncing"));
        SwingWorker<String, Void> updater = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                Product conflict = productDAO.findBySku(product.getSku());
                if (conflict != null && conflict.getId() != product.getId()) {
                    return LanguageManager.format("products.msg.skuInUse", product.getSku());
                }
                return productDAO.update(product) ? null : LanguageManager.get("products.msg.updateFailed");
            }

            @Override
            protected void done() {
                view.setBusy(false, null);
                try {
                    String error = get();
                    if (error == null) {
                        loadAllData();
                        JOptionPane.showMessageDialog(view,
                                LanguageManager.format("products.msg.updated", product.getName()),
                                LanguageManager.get("common.success"), JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(view, error,
                                LanguageManager.get("common.updateFailed"), JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(view, ex.getMessage(),
                            LanguageManager.get("common.error"), JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        updater.execute();
    }

    private void handleDeleteProduct(ActionEvent e) {
        if (view.getCurrentUser().getRole() != User.Role.ADMIN) {
            JOptionPane.showMessageDialog(view, LanguageManager.get("rbac.productsDeleteAdmin"),
                    LanguageManager.get("header.accessDenied"), JOptionPane.WARNING_MESSAGE);
            return;
        }

        int viewRow = view.getProductTable().getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(view, LanguageManager.get("products.selectDelete"),
                    LanguageManager.get("common.noSelection"), JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = view.getProductTable().convertRowIndexToModel(viewRow);
        int productId = (int) view.getProductTableModel().getValueAt(modelRow, 0);
        String sku = String.valueOf(view.getProductTableModel().getValueAt(modelRow, 1));
        String productName = String.valueOf(view.getProductTableModel().getValueAt(modelRow, 2));

        int confirm = JOptionPane.showConfirmDialog(view,
                LanguageManager.format("products.deleteConfirm", productName, sku),
                LanguageManager.get("products.deleteTitle"),
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            view.setBusy(true, LanguageManager.get("status.syncing"));
            SwingWorker<Boolean, Void> deleter = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() {
                    return productDAO.delete(productId);
                }

                @Override
                protected void done() {
                    view.setBusy(false, null);
                    try {
                        if (get()) {
                            loadAllData();
                            JOptionPane.showMessageDialog(view, LanguageManager.get("products.msg.deleted"),
                                    LanguageManager.get("common.success"), JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(view, LanguageManager.get("products.msg.deleteFailed"),
                                    LanguageManager.get("common.error"), JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(view, ex.getMessage(),
                                LanguageManager.get("common.error"), JOptionPane.ERROR_MESSAGE);
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
        view.setBusy(true, LanguageManager.get("status.syncing"));
        SwingWorker<Boolean, Void> saver = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return supplierDAO.save(supplier);
            }

            @Override
            protected void done() {
                view.setBusy(false, null);
                try {
                    if (get()) {
                        loadAllData();
                        JOptionPane.showMessageDialog(view,
                                LanguageManager.format("suppliers.msg.added", supplier.getName()),
                                LanguageManager.get("common.success"), JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(view, LanguageManager.get("suppliers.msg.saveFailed"),
                                LanguageManager.get("common.saveFailed"), JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(view, ex.getMessage(),
                            LanguageManager.get("common.error"), JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        saver.execute();
    }

    private void handleEditSupplier(ActionEvent e) {
        if (view.getCurrentUser().getRole() == User.Role.STAFF) return;

        SuppliersPanel sp = view.getSuppliersPanel();
        int viewRow = sp.getSupplierTable().getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(view, LanguageManager.get("suppliers.selectEdit"),
                    LanguageManager.get("common.noSelection"), JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = sp.getSupplierTable().convertRowIndexToModel(viewRow);
        int supplierId = (int) sp.getSupplierTableModel().getValueAt(modelRow, 0);
        view.setBusy(true, LanguageManager.get("status.loading"));

        SwingWorker<Supplier, Void> loader = new SwingWorker<>() {
            @Override
            protected Supplier doInBackground() {
                return supplierDAO.findById(supplierId);
            }

            @Override
            protected void done() {
                view.setBusy(false, LanguageManager.get("status.ready"));
                try {
                    Supplier existing = get();
                    if (existing == null) {
                        JOptionPane.showMessageDialog(view, LanguageManager.get("suppliers.msg.notFound"),
                                LanguageManager.get("common.notFound"), JOptionPane.ERROR_MESSAGE);
                        loadSuppliers();
                        return;
                    }
                    SupplierFormDialog dialog = new SupplierFormDialog(view, existing);
                    dialog.setVisible(true);
                    if (dialog.isConfirmed()) {
                        persistUpdatedSupplier(dialog.getSupplier());
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(view, ex.getMessage(),
                            LanguageManager.get("common.error"), JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        loader.execute();
    }

    private void persistUpdatedSupplier(Supplier supplier) {
        view.setBusy(true, LanguageManager.get("status.syncing"));
        SwingWorker<Boolean, Void> updater = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return supplierDAO.update(supplier);
            }

            @Override
            protected void done() {
                view.setBusy(false, null);
                try {
                    if (get()) {
                        loadAllData();
                        JOptionPane.showMessageDialog(view,
                                LanguageManager.format("suppliers.msg.updated", supplier.getName()),
                                LanguageManager.get("common.success"), JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(view, LanguageManager.get("suppliers.msg.updateFailed"),
                                LanguageManager.get("common.updateFailed"), JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(view, ex.getMessage(),
                            LanguageManager.get("common.error"), JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        updater.execute();
    }

    private void handleDeleteSupplier(ActionEvent e) {
        if (view.getCurrentUser().getRole() != User.Role.ADMIN) {
            JOptionPane.showMessageDialog(view, LanguageManager.get("rbac.suppliersDeleteAdmin"),
                    LanguageManager.get("header.accessDenied"), JOptionPane.WARNING_MESSAGE);
            return;
        }

        SuppliersPanel sp = view.getSuppliersPanel();
        int viewRow = sp.getSupplierTable().getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(view, LanguageManager.get("suppliers.selectDelete"),
                    LanguageManager.get("common.noSelection"), JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = sp.getSupplierTable().convertRowIndexToModel(viewRow);
        int supplierId = (int) sp.getSupplierTableModel().getValueAt(modelRow, 0);
        String supplierName = String.valueOf(sp.getSupplierTableModel().getValueAt(modelRow, 1));

        int confirm = JOptionPane.showConfirmDialog(view,
                LanguageManager.format("suppliers.deleteConfirm", supplierName),
                LanguageManager.get("suppliers.deleteTitle"),
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            view.setBusy(true, LanguageManager.get("status.syncing"));
            SwingWorker<Boolean, Void> deleter = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() {
                    return supplierDAO.delete(supplierId);
                }

                @Override
                protected void done() {
                    view.setBusy(false, null);
                    try {
                        if (get()) {
                            loadAllData();
                            JOptionPane.showMessageDialog(view, LanguageManager.get("suppliers.msg.deleted"),
                                    LanguageManager.get("common.success"), JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(view, LanguageManager.get("suppliers.msg.deleteFailed"),
                                    LanguageManager.get("common.error"), JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(view, ex.getMessage(),
                                LanguageManager.get("common.error"), JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            deleter.execute();
        }
    }

    // =========================================================================
    // Category Management & Admin User Lifecycle Handlers
    // =========================================================================

    private void handleManageCategories() {
        if (view.getCurrentUser().getRole() == User.Role.STAFF) {
            JOptionPane.showMessageDialog(view, LanguageManager.get("rbac.categoriesManagerAdmin"),
                    LanguageManager.get("header.accessDenied"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        CategoryManagementDialog dialog = new CategoryManagementDialog(view, categoryDAO, this::loadAllData);
        dialog.setVisible(true);
    }

    private void handleAddUser(ActionEvent e) {
        if (view.getCurrentUser().getRole() != User.Role.ADMIN) return;

        UserFormDialog dialog = new UserFormDialog(view, null, view.getCurrentUser().getId());
        dialog.setVisible(true);
        if (!dialog.isConfirmed()) return;

        User newUser = dialog.getUser();
        view.setBusy(true, LanguageManager.get("status.syncing"));
        SwingWorker<String, Void> saver = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                if (userDAO.findByUsername(newUser.getUsername()).isPresent()) {
                    return LanguageManager.format("users.msg.duplicateUsername", newUser.getUsername());
                }
                return userDAO.save(newUser) ? null : LanguageManager.get("users.msg.saveFailed");
            }

            @Override
            protected void done() {
                view.setBusy(false, null);
                try {
                    String err = get();
                    if (err == null) {
                        loadAllData();
                        JOptionPane.showMessageDialog(view,
                                LanguageManager.format("users.msg.created", newUser.getUsername(),
                                        LanguageManager.get("role." + newUser.getRole().name())),
                                LanguageManager.get("common.success"), JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(view, err,
                                LanguageManager.get("common.saveFailed"), JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(view, ex.getMessage(),
                            LanguageManager.get("common.error"), JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        saver.execute();
    }

    private void handleEditUser(ActionEvent e) {
        if (view.getCurrentUser().getRole() != User.Role.ADMIN) return;

        UsersPanel up = view.getUsersPanel();
        int viewRow = up.getUserTable().getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(view, LanguageManager.get("users.selectEdit"),
                    LanguageManager.get("common.noSelection"), JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = up.getUserTable().convertRowIndexToModel(viewRow);
        int userId = (int) up.getUserTableModel().getValueAt(modelRow, 0);
        view.setBusy(true, LanguageManager.get("status.loading"));

        SwingWorker<User, Void> loader = new SwingWorker<>() {
            @Override
            protected User doInBackground() {
                return userDAO.findById(userId);
            }

            @Override
            protected void done() {
                view.setBusy(false, LanguageManager.get("status.ready"));
                try {
                    User existing = get();
                    if (existing == null) {
                        JOptionPane.showMessageDialog(view, LanguageManager.get("users.msg.notFound"),
                                LanguageManager.get("common.notFound"), JOptionPane.ERROR_MESSAGE);
                        loadUsers();
                        return;
                    }
                    UserFormDialog dialog = new UserFormDialog(view, existing, view.getCurrentUser().getId());
                    dialog.setVisible(true);
                    if (dialog.isConfirmed()) {
                        User updated = dialog.getUser();
                        if (updated.getId() == view.getCurrentUser().getId()) {
                            updated.setRole(view.getCurrentUser().getRole());
                            updated.setActive(true);
                        }
                        persistUpdatedUser(updated);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(view, ex.getMessage(),
                            LanguageManager.get("common.error"), JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        loader.execute();
    }

    private void persistUpdatedUser(User updated) {
        view.setBusy(true, LanguageManager.get("status.syncing"));
        SwingWorker<Boolean, Void> updater = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return userDAO.update(updated);
            }

            @Override
            protected void done() {
                view.setBusy(false, null);
                try {
                    if (get()) {
                        loadAllData();
                        JOptionPane.showMessageDialog(view,
                                LanguageManager.format("users.msg.updated", updated.getUsername()),
                                LanguageManager.get("common.success"), JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(view, LanguageManager.get("users.msg.updateFailed"),
                                LanguageManager.get("common.updateFailed"), JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(view, ex.getMessage(),
                            LanguageManager.get("common.error"), JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        updater.execute();
    }

    private void handleResetPassword(ActionEvent e) {
        if (view.getCurrentUser().getRole() != User.Role.ADMIN) return;

        UsersPanel up = view.getUsersPanel();
        int viewRow = up.getUserTable().getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(view, LanguageManager.get("users.selectReset"),
                    LanguageManager.get("common.noSelection"), JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = up.getUserTable().convertRowIndexToModel(viewRow);
        int userId = (int) up.getUserTableModel().getValueAt(modelRow, 0);
        String username = String.valueOf(up.getUserTableModel().getValueAt(modelRow, 1));

        JPasswordField newPassField = new JPasswordField(18);
        JPasswordField confirmPassField = new JPasswordField(18);
        JPanel promptPanel = new JPanel(new java.awt.GridLayout(4, 1, 4, 6));
        promptPanel.add(new JLabel(LanguageManager.format("users.msg.resetNewLabel", username)));
        promptPanel.add(newPassField);
        promptPanel.add(new JLabel(LanguageManager.get("users.msg.resetConfirmLabel")));
        promptPanel.add(confirmPassField);
        LanguageManager.applyOrientation(promptPanel);

        int result = JOptionPane.showConfirmDialog(view, promptPanel,
                LanguageManager.get("users.resetTitle"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String newPass = new String(newPassField.getPassword());
        String confirmPass = new String(confirmPassField.getPassword());
        if (newPass.length() < 6) {
            JOptionPane.showMessageDialog(view, LanguageManager.get("users.resetMinLength"),
                    LanguageManager.get("common.validationError"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!newPass.equals(confirmPass)) {
            JOptionPane.showMessageDialog(view, LanguageManager.get("users.msg.resetMismatch"),
                    LanguageManager.get("common.validationError"), JOptionPane.WARNING_MESSAGE);
            return;
        }

        view.setBusy(true, LanguageManager.get("status.syncing"));
        SwingWorker<Boolean, Void> resetter = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return userDAO.updatePassword(userId, newPass);
            }

            @Override
            protected void done() {
                view.setBusy(false, LanguageManager.get("status.ready"));
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(view,
                                LanguageManager.format("users.resetSuccess", username),
                                LanguageManager.get("users.resetTitle"), JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(view, LanguageManager.get("users.msg.resetFailed"),
                                LanguageManager.get("common.error"), JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(view, ex.getMessage(),
                            LanguageManager.get("common.error"), JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        resetter.execute();
    }

    private void handleToggleUserActive(ActionEvent e) {
        if (view.getCurrentUser().getRole() != User.Role.ADMIN) return;

        UsersPanel up = view.getUsersPanel();
        int viewRow = up.getUserTable().getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(view, LanguageManager.get("users.selectToggle"),
                    LanguageManager.get("common.noSelection"), JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = up.getUserTable().convertRowIndexToModel(viewRow);
        int userId = (int) up.getUserTableModel().getValueAt(modelRow, 0);
        String username = String.valueOf(up.getUserTableModel().getValueAt(modelRow, 1));

        if (userId == view.getCurrentUser().getId()) {
            JOptionPane.showMessageDialog(view, LanguageManager.get("users.cannotDeactivateSelf"),
                    LanguageManager.get("header.accessDenied"), JOptionPane.WARNING_MESSAGE);
            return;
        }

        view.setBusy(true, LanguageManager.get("status.syncing"));
        SwingWorker<Boolean, Void> toggler = new SwingWorker<>() {
            private boolean newStatus;

            @Override
            protected Boolean doInBackground() {
                User target = userDAO.findById(userId);
                if (target == null) return false;
                newStatus = !target.isActive();
                target.setActive(newStatus);
                return userDAO.update(target);
            }

            @Override
            protected void done() {
                view.setBusy(false, null);
                try {
                    if (get()) {
                        loadAllData();
                        String statusText = newStatus
                                ? LanguageManager.get("status.active")
                                : LanguageManager.get("status.disabled");
                        JOptionPane.showMessageDialog(view,
                                LanguageManager.format("users.msg.statusUpdated", username, statusText),
                                LanguageManager.get("users.toggleTitle"), JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(view, LanguageManager.get("users.msg.updateFailed"),
                                LanguageManager.get("common.updateFailed"), JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(view, ex.getMessage(),
                            LanguageManager.get("common.error"), JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        toggler.execute();
    }

    // =========================================================================
    // Quick Restock & Stock Transaction Handlers
    // =========================================================================

    private void handleQuickRestock() {
        Integer targetProductId = null;
        int viewRow = view.getWatchlistTable().getSelectedRow();
        if (viewRow != -1) {
            int modelRow = view.getWatchlistTable().convertRowIndexToModel(viewRow);
            targetProductId = (Integer) view.getWatchlistTableModel().getValueAt(modelRow, 0);
        } else if (view.getWatchlistTableModel().getRowCount() > 0) {
            int modelRow = view.getWatchlistTable().convertRowIndexToModel(0);
            targetProductId = (Integer) view.getWatchlistTableModel().getValueAt(modelRow, 0);
        }
        openStockTransactionDialog(true, targetProductId);
    }

    private void openStockTransactionDialog(boolean inboundMode, Integer explicitPreselectId) {
        Integer preselectedProductId = explicitPreselectId;
        if (preselectedProductId == null) {
            int viewRow = view.getProductTable().getSelectedRow();
            if (viewRow != -1) {
                int modelRow = view.getProductTable().convertRowIndexToModel(viewRow);
                preselectedProductId = (Integer) view.getProductTableModel().getValueAt(modelRow, 0);
            }
        }
        final Integer preselectedId = preselectedProductId;
        view.setBusy(true, LanguageManager.get("status.loading"));

        SwingWorker<List<Product>, Void> loader = new SwingWorker<>() {
            @Override
            protected List<Product> doInBackground() {
                return productDAO.findAll();
            }

            @Override
            protected void done() {
                view.setBusy(false, LanguageManager.get("status.ready"));
                try {
                    List<Product> products = get();
                    cachedProducts = products;
                    if (products.isEmpty()) {
                        JOptionPane.showMessageDialog(view, LanguageManager.get("transactions.msg.emptyCatalog"),
                                LanguageManager.get("common.warning"), JOptionPane.WARNING_MESSAGE);
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
                    JOptionPane.showMessageDialog(view, ex.getMessage(),
                            LanguageManager.get("common.error"), JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        loader.execute();
    }

    private void executeStockTransaction(boolean inboundMode, Product product,
                                         StockTransaction.TransactionType type, int qty, String notes) {
        Integer userId = view.getCurrentUser() != null ? view.getCurrentUser().getId() : null;
        view.setBusy(true, LanguageManager.get("status.syncing"));

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
                view.setBusy(false, null);
                try {
                    if (get()) {
                        loadAllData();
                        String sign = inboundMode ? "+" : "-";
                        JOptionPane.showMessageDialog(view,
                                LanguageManager.format("transactions.msg.committed",
                                        LanguageManager.get("transactions.type." + type.name()),
                                        product.getName(), sign, qty),
                                LanguageManager.get("transactions.msg.committedTitle"),
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(view,
                                failureMsg != null ? failureMsg : LanguageManager.get("status.error"),
                                LanguageManager.get("transactions.msg.rejected"), JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(view, ex.getMessage(),
                            LanguageManager.get("common.error"), JOptionPane.ERROR_MESSAGE);
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

        JMenuItem pdfItem = new JMenuItem(LanguageManager.get("export.pdf"),
                IconFactory.pdfFile(16, ThemeManager.accentDanger()));
        pdfItem.setIconTextGap(10);
        pdfItem.addActionListener(e -> promptAndExport(ReportExportService.ExportFormat.PDF));

        JMenuItem excelItem = new JMenuItem(LanguageManager.get("export.excel"),
                IconFactory.excelFile(16, ThemeManager.accentSuccess()));
        excelItem.setIconTextGap(10);
        excelItem.addActionListener(e -> promptAndExport(ReportExportService.ExportFormat.EXCEL));

        JMenuItem csvItem = new JMenuItem(LanguageManager.get("export.csv"),
                IconFactory.csvFile(16, ThemeManager.accentPrimary()));
        csvItem.setIconTextGap(10);
        csvItem.addActionListener(e -> promptAndExport(ReportExportService.ExportFormat.CSV));

        JMenuItem jsonItem = new JMenuItem(LanguageManager.get("export.json"),
                IconFactory.jsonFile(16, ThemeManager.accentSecondary()));
        jsonItem.setIconTextGap(10);
        jsonItem.addActionListener(e -> promptAndExport(ReportExportService.ExportFormat.JSON));

        popup.add(pdfItem);
        popup.add(excelItem);
        popup.addSeparator();
        popup.add(csvItem);
        popup.add(jsonItem);

        LanguageManager.applyOrientation(popup);
        int x = LanguageManager.isRTL()
                ? invoker.getWidth() - popup.getPreferredSize().width
                : 0;
        popup.show(invoker, x, invoker.getHeight() + 2);
    }

    private void promptAndExport(ReportExportService.ExportFormat format) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(LanguageManager.format("export.dialogTitle", format.getExtension().toUpperCase()));
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(new FileNameExtensionFilter(format.getDescription(), format.getExtension()));
        chooser.setSelectedFile(new File("nexus-inventory-report-" + LocalDate.now() + "." + format.getExtension()));
        LanguageManager.applyOrientation(chooser);

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
                    LanguageManager.format("common.overwritePrompt", targetFile.getName()),
                    LanguageManager.get("common.overwriteTitle"),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (overwrite != JOptionPane.YES_OPTION) {
                return;
            }
        }

        final File exportFile = targetFile;
        String formatUpper = format.getExtension().toUpperCase();
        view.setBusy(true, LanguageManager.format("status.exporting", formatUpper));

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
                    view.setBusy(false, LanguageManager.format("status.exported", exportFile.getName()));

                    Object[] options = {
                            LanguageManager.get("common.openFile"),
                            LanguageManager.get("common.ok")
                    };
                    int choice = JOptionPane.showOptionDialog(
                            view,
                            LanguageManager.format("export.savedPrompt", count, exportFile.getAbsolutePath()),
                            LanguageManager.get("export.successTitle"),
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
                                    LanguageManager.format("export.openNoticeMsg", openEx.getMessage()),
                                    LanguageManager.get("export.openNoticeTitle"), JOptionPane.WARNING_MESSAGE);
                        }
                    }
                } catch (Exception ex) {
                    view.setBusy(false, LanguageManager.get("status.error"));
                    JOptionPane.showMessageDialog(view,
                            LanguageManager.format("export.failed", ex.getMessage()),
                            LanguageManager.get("common.error"), JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        exporter.execute();
    }

    private void handleLogout(ActionEvent e) {
        int confirm = JOptionPane.showConfirmDialog(view,
                LanguageManager.get("nav.signOutConfirm"),
                LanguageManager.get("nav.signOutTitle"),
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            LanguageManager.removeLanguageListener(languageRefreshListener);
            view.dispose();
            SwingUtilities.invokeLater(() -> {
                LoginFrame loginFrame = new LoginFrame();
                new AuthController(loginFrame, new AuthService());
                loginFrame.setVisible(true);
            });
        }
    }
}
