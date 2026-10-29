package com.nexus.ims.view;

import com.nexus.ims.model.Product;
import com.nexus.ims.model.StockTransaction;
import com.nexus.ims.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Primary Desktop Dashboard Shell for the NEXUS Inventory Management System.
 * Implements a modern multi-pane layout featuring a collapsible/styled sidebar,
 * top header with role credentials, dynamic CardLayout center, and live status bar.
 */
public class DashboardFrame extends JFrame {
    // Card Identifiers
    public static final String CARD_DASHBOARD = "CARD_DASHBOARD";
    public static final String CARD_PRODUCTS = "CARD_PRODUCTS";
    public static final String CARD_TRANSACTIONS = "CARD_TRANSACTIONS";
    public static final String CARD_SUPPLIERS = "CARD_SUPPLIERS";
    public static final String CARD_REPORTS = "CARD_REPORTS";

    private final User currentUser;

    // Navigation Controls
    private JButton navDashboardBtn;
    private JButton navProductsBtn;
    private JButton navTransactionsBtn;
    private JButton navSuppliersBtn;
    private JButton navReportsBtn;
    private JButton logoutBtn;

    // Card Layout Container
    private JPanel cardsPanel;
    private CardLayout cardLayout;
    private JLabel viewTitleLabel;

    // KPI Metric Displays
    private JLabel kpiTotalProductsVal;
    private JLabel kpiLowStockVal;
    private JLabel kpiValuationVal;

    // Products View Components
    private JTable productTable;
    private DefaultTableModel productTableModel;
    private JTextField productSearchField;
    private JButton addProductBtn;
    private JButton editProductBtn;
    private JButton deleteProductBtn;
    private JButton refreshProductsBtn;

    // Transactions View Components
    private JTable transactionTable;
    private DefaultTableModel transactionTableModel;
    private JButton stockInBtn;
    private JButton stockOutBtn;
    private JButton refreshTransactionsBtn;

    // Status Bar Components
    private JLabel dbStatusLabel;
    private JLabel statusMessageLabel;
    private JLabel clockLabel;

    public DashboardFrame(User currentUser) {
        this.currentUser = currentUser != null ? currentUser : new User("guest", "", "Guest User", "guest@nexus.local", User.Role.STAFF);
        initComponents();
        startClockTimer();
    }

    private void initComponents() {
        setTitle("NEXUS IMS - Enterprise Inventory Management");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1240, 780);
        setMinimumSize(new Dimension(1000, 650));
        setLocationRelativeTo(null);

        JPanel mainLayout = new JPanel(new BorderLayout());

        // 1. WEST: Navigation Sidebar
        mainLayout.add(createSidebarPanel(), BorderLayout.WEST);

        // 2. NORTH: Top Header Bar
        mainLayout.add(createHeaderPanel(), BorderLayout.NORTH);

        // 3. CENTER: Dynamic CardLayout Content Panel
        mainLayout.add(createCenterCardsPanel(), BorderLayout.CENTER);

        // 4. SOUTH: Live Status Bar
        mainLayout.add(createStatusBarPanel(), BorderLayout.SOUTH);

        setContentPane(mainLayout);
    }

    // =========================================================================
    // UI Component Builders
    // =========================================================================

    private JPanel createSidebarPanel() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setBackground(new Color(24, 28, 36)); // Deep Obsidian
        sidebar.setBorder(new EmptyBorder(20, 15, 20, 15));

        // Brand Logo
        JLabel brandLogo = new JLabel("◈ NEXUS IMS", SwingConstants.LEFT);
        brandLogo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        brandLogo.setForeground(Color.WHITE);
        brandLogo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel brandSub = new JLabel("Inventory Management", SwingConstants.LEFT);
        brandSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        brandSub.setForeground(new Color(140, 150, 165));
        brandSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        sidebar.add(brandLogo);
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(brandSub);
        sidebar.add(Box.createVerticalStrut(25));

        // Navigation Action Buttons
        navDashboardBtn = createNavButton("📊  Dashboard", true);
        navProductsBtn = createNavButton("📦  Products", false);
        navTransactionsBtn = createNavButton("🔄  Transactions", false);
        navSuppliersBtn = createNavButton("🏢  Suppliers", false);
        navReportsBtn = createNavButton("📈  Reports", false);

        sidebar.add(navDashboardBtn);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(navProductsBtn);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(navTransactionsBtn);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(navSuppliersBtn);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(navReportsBtn);

        // Flexible Space
        sidebar.add(Box.createVerticalGlue());

        // Logout Action
        logoutBtn = new JButton("🚪  Sign Out");
        logoutBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        logoutBtn.setForeground(new Color(230, 235, 240));
        logoutBtn.setBackground(new Color(45, 52, 65));
        logoutBtn.setFocusPainted(false);
        logoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        logoutBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(logoutBtn);

        return sidebar;
    }

    private JButton createNavButton(String label, boolean active) {
        JButton btn = new JButton(label);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (active) {
            btn.setBackground(new Color(13, 110, 253));
            btn.setForeground(Color.WHITE);
        } else {
            btn.setBackground(new Color(32, 38, 48));
            btn.setForeground(new Color(200, 210, 220));
        }

        return btn;
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, 56));
        header.setBorder(new MatteBorder(0, 0, 1, 0, new Color(222, 226, 230)));

        // Left Header: Dynamic Title
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 14));
        titlePanel.setOpaque(false);
        viewTitleLabel = new JLabel("System Overview & KPI Metrics");
        viewTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        viewTitleLabel.setForeground(new Color(33, 37, 41));
        titlePanel.add(viewTitleLabel);
        header.add(titlePanel, BorderLayout.WEST);

        // Right Header: User Identity & Role Badge
        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 12));
        userPanel.setOpaque(false);

        JLabel userNameLabel = new JLabel(currentUser.getFullName());
        userNameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        userNameLabel.setForeground(new Color(33, 37, 41));

        // Role Badge Pill
        JLabel rolePill = new JLabel(" " + currentUser.getRole().name() + " ");
        rolePill.setFont(new Font("Segoe UI", Font.BOLD, 11));
        rolePill.setOpaque(true);
        rolePill.setBorder(new EmptyBorder(2, 6, 2, 6));

        if (currentUser.getRole() == User.Role.ADMIN) {
            rolePill.setBackground(new Color(254, 243, 199));
            rolePill.setForeground(new Color(180, 83, 9));
        } else if (currentUser.getRole() == User.Role.MANAGER) {
            rolePill.setBackground(new Color(224, 231, 255));
            rolePill.setForeground(new Color(67, 56, 202));
        } else {
            rolePill.setBackground(new Color(220, 252, 231));
            rolePill.setForeground(new Color(21, 128, 61));
        }

        userPanel.add(userNameLabel);
        userPanel.add(rolePill);
        header.add(userPanel, BorderLayout.EAST);

        return header;
    }

    private JPanel createCenterCardsPanel() {
        cardLayout = new CardLayout();
        cardsPanel = new JPanel(cardLayout);
        cardsPanel.setBackground(new Color(245, 247, 250));

        // Assemble Individual View Panels
        cardsPanel.add(buildDashboardOverviewPanel(), CARD_DASHBOARD);
        cardsPanel.add(buildProductsPanel(), CARD_PRODUCTS);
        cardsPanel.add(buildTransactionsPanel(), CARD_TRANSACTIONS);
        cardsPanel.add(buildPlaceholderPanel("Suppliers & Vendor Management"), CARD_SUPPLIERS);
        cardsPanel.add(buildPlaceholderPanel("Financial Reports & Inventory Auditing"), CARD_REPORTS);

        return cardsPanel;
    }

    private JPanel buildDashboardOverviewPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Top KPI Metrics Row (3 Cards)
        JPanel metricsGrid = new JPanel(new GridLayout(1, 3, 20, 0));
        metricsGrid.setOpaque(false);
        metricsGrid.setPreferredSize(new Dimension(0, 110));

        kpiTotalProductsVal = new JLabel("0", SwingConstants.CENTER);
        kpiLowStockVal = new JLabel("0", SwingConstants.CENTER);
        kpiValuationVal = new JLabel("$0.00", SwingConstants.CENTER);

        metricsGrid.add(createMetricCard("TOTAL CATALOG ITEMS", kpiTotalProductsVal, new Color(13, 110, 253)));
        metricsGrid.add(createMetricCard("LOW STOCK ALERTS", kpiLowStockVal, new Color(220, 53, 69)));
        metricsGrid.add(createMetricCard("TOTAL ASSET VALUATION", kpiValuationVal, new Color(25, 135, 84)));
        panel.add(metricsGrid, BorderLayout.NORTH);

        // Center Welcome Information
        JPanel welcomeCard = new JPanel(new BorderLayout(10, 10));
        welcomeCard.setBackground(Color.WHITE);
        welcomeCard.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 1, 1, 1, new Color(222, 226, 230)),
                new EmptyBorder(25, 25, 25, 25)
        ));

        JLabel welcomeTitle = new JLabel("Welcome to the NEXUS Inventory Operations Center");
        welcomeTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        welcomeTitle.setForeground(new Color(33, 37, 41));

        JTextArea welcomeDesc = new JTextArea(
                "You are authenticated as " + currentUser.getFullName() + " with " + currentUser.getRole() + " privileges.\n\n" +
                "• Products View: Inspect inventory SKU levels, update reorder boundaries, and manage catalog items.\n" +
                "• Transactions View: Perform ACID-compliant inbound shipments and sales dispatch with automatic stock calculations.\n" +
                "• Audit Trail: Every movement is permanently logged with timestamps and user identification."
        );
        welcomeDesc.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        welcomeDesc.setEditable(false);
        welcomeDesc.setLineWrap(true);
        welcomeDesc.setWrapStyleWord(true);
        welcomeDesc.setOpaque(false);
        welcomeDesc.setForeground(new Color(73, 80, 87));

        welcomeCard.add(welcomeTitle, BorderLayout.NORTH);
        welcomeCard.add(welcomeDesc, BorderLayout.CENTER);
        panel.add(welcomeCard, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createMetricCard(String title, JLabel valueLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 4, 0, 0, accentColor),
                BorderFactory.createCompoundBorder(
                        new MatteBorder(1, 0, 1, 1, new Color(222, 226, 230)),
                        new EmptyBorder(15, 20, 15, 20)
                )
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        titleLbl.setForeground(new Color(108, 117, 125));

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        valueLabel.setForeground(new Color(33, 37, 41));
        valueLabel.setHorizontalAlignment(SwingConstants.LEFT);

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildProductsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Toolbar Panel
        JPanel toolbar = new JPanel(new BorderLayout(10, 10));
        toolbar.setOpaque(false);

        // Search Bar
        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        searchBox.setOpaque(false);
        productSearchField = new JTextField(20);
        productSearchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        productSearchField.putClientProperty("JTextField.placeholderText", "Search SKU or Name...");
        searchBox.add(productSearchField);
        toolbar.add(searchBox, BorderLayout.WEST);

        // Action Buttons
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        refreshProductsBtn = new JButton("🔄 Refresh");
        addProductBtn = new JButton("➕ Add Product");
        addProductBtn.setBackground(new Color(13, 110, 253));
        addProductBtn.setForeground(Color.WHITE);

        editProductBtn = new JButton("✏ Edit");
        deleteProductBtn = new JButton("🗑 Delete");
        deleteProductBtn.setForeground(new Color(220, 53, 69));

        actions.add(refreshProductsBtn);
        actions.add(addProductBtn);
        actions.add(editProductBtn);
        actions.add(deleteProductBtn);
        toolbar.add(actions, BorderLayout.EAST);

        panel.add(toolbar, BorderLayout.NORTH);

        // Products Data Table
        String[] columns = {"ID", "SKU", "Product Name", "Cost ($)", "Selling ($)", "Stock Qty", "Min Threshold", "Status"};
        productTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        productTable = new JTable(productTableModel);
        productTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        productTable.getTableHeader().setReorderingAllowed(false);
        productTable.setDefaultRenderer(Object.class, new StatusCellRenderer());

        JScrollPane scrollPane = new JScrollPane(productTable);
        scrollPane.setBorder(new MatteBorder(1, 1, 1, 1, new Color(222, 226, 230)));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildTransactionsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Top Action Bar
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);

        stockInBtn = new JButton("📥 Inbound Receipt (Stock IN)");
        stockInBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        stockInBtn.setBackground(new Color(25, 135, 84));
        stockInBtn.setForeground(Color.WHITE);

        stockOutBtn = new JButton("📤 Outbound Dispatch (Stock OUT)");
        stockOutBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        stockOutBtn.setBackground(new Color(220, 53, 69));
        stockOutBtn.setForeground(Color.WHITE);

        refreshTransactionsBtn = new JButton("🔄 Refresh Log");

        actions.add(stockInBtn);
        actions.add(stockOutBtn);
        actions.add(refreshTransactionsBtn);
        panel.add(actions, BorderLayout.NORTH);

        // Transaction Audit Table
        String[] columns = {"ID", "Product ID", "Movement Type", "Quantity Delta", "Notes", "User ID", "Timestamp"};
        transactionTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        transactionTable = new JTable(transactionTableModel);
        transactionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(transactionTable);
        scrollPane.setBorder(new MatteBorder(1, 1, 1, 1, new Color(222, 226, 230)));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildPlaceholderPanel(String moduleName) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(new EmptyBorder(30, 40, 30, 40));

        JLabel title = new JLabel(moduleName, SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        JLabel desc = new JLabel("Module ready for configuration and reporting integration.", SwingConstants.CENTER);
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        desc.setForeground(Color.GRAY);

        card.add(title, BorderLayout.NORTH);
        card.add(desc, BorderLayout.CENTER);
        panel.add(card);
        return panel;
    }

    private JPanel createStatusBarPanel() {
        JPanel status = new JPanel(new BorderLayout(15, 0));
        status.setBackground(new Color(248, 249, 250));
        status.setPreferredSize(new Dimension(0, 28));
        status.setBorder(new MatteBorder(1, 0, 0, 0, new Color(222, 226, 230)));

        dbStatusLabel = new JLabel(" ● Database: Online [MySQL Server]", SwingConstants.LEFT);
        dbStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        dbStatusLabel.setForeground(new Color(25, 135, 84));

        statusMessageLabel = new JLabel("System Ready", SwingConstants.CENTER);
        statusMessageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusMessageLabel.setForeground(new Color(108, 117, 125));

        clockLabel = new JLabel(" ", SwingConstants.RIGHT);
        clockLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        clockLabel.setForeground(new Color(73, 80, 87));
        clockLabel.setBorder(new EmptyBorder(0, 0, 0, 12));

        status.add(dbStatusLabel, BorderLayout.WEST);
        status.add(statusMessageLabel, BorderLayout.CENTER);
        status.add(clockLabel, BorderLayout.EAST);

        return status;
    }

    private void startClockTimer() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd  HH:mm:ss");
        Timer timer = new Timer(1000, e -> clockLabel.setText(LocalDateTime.now().format(dtf)));
        timer.setInitialDelay(0);
        timer.start();
    }

    // =========================================================================
    // Public View API & Controller Delegation Methods
    // =========================================================================

    public void showCard(String cardKey) {
        cardLayout.show(cardsPanel, cardKey);
        updateActiveNavStyle(cardKey);

        switch (cardKey) {
            case CARD_DASHBOARD -> viewTitleLabel.setText("System Overview & KPI Metrics");
            case CARD_PRODUCTS -> viewTitleLabel.setText("Product Inventory Catalog");
            case CARD_TRANSACTIONS -> viewTitleLabel.setText("Stock Inbound / Outbound Audit Ledger");
            case CARD_SUPPLIERS -> viewTitleLabel.setText("Authorized Suppliers & Vendors");
            case CARD_REPORTS -> viewTitleLabel.setText("Financial Valuations & Asset Reports");
        }
    }

    private void updateActiveNavStyle(String activeKey) {
        resetNavBtn(navDashboardBtn, activeKey.equals(CARD_DASHBOARD));
        resetNavBtn(navProductsBtn, activeKey.equals(CARD_PRODUCTS));
        resetNavBtn(navTransactionsBtn, activeKey.equals(CARD_TRANSACTIONS));
        resetNavBtn(navSuppliersBtn, activeKey.equals(CARD_SUPPLIERS));
        resetNavBtn(navReportsBtn, activeKey.equals(CARD_REPORTS));
    }

    private void resetNavBtn(JButton btn, boolean isActive) {
        if (isActive) {
            btn.setBackground(new Color(13, 110, 253));
            btn.setForeground(Color.WHITE);
        } else {
            btn.setBackground(new Color(32, 38, 48));
            btn.setForeground(new Color(200, 210, 220));
        }
    }

    public void setMetrics(int totalProducts, int lowStockCount, BigDecimal valuation) {
        kpiTotalProductsVal.setText(String.valueOf(totalProducts));
        kpiLowStockVal.setText(String.valueOf(lowStockCount));
        NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.US);
        kpiValuationVal.setText(nf.format(valuation != null ? valuation : BigDecimal.ZERO));
    }

    public void populateProductTable(List<Product> products) {
        productTableModel.setRowCount(0);
        for (Product p : products) {
            String status = p.isLowStock() ? "LOW STOCK" : "IN STOCK";
            productTableModel.addRow(new Object[]{
                    p.getId(),
                    p.getSku(),
                    p.getName(),
                    p.getPurchasePrice(),
                    p.getUnitPrice(),
                    p.getQuantityInStock(),
                    p.getReorderLevel(),
                    status
            });
        }
    }

    public void populateTransactionTable(List<StockTransaction> transactions) {
        transactionTableModel.setRowCount(0);
        for (StockTransaction tx : transactions) {
            transactionTableModel.addRow(new Object[]{
                    tx.getId(),
                    tx.getProductId(),
                    tx.getTransactionType(),
                    (tx.getQuantityChange() > 0 ? "+" : "") + tx.getQuantityChange(),
                    tx.getNotes(),
                    tx.getPerformedByUserId() != null ? tx.getPerformedByUserId() : "System",
                    tx.getCreatedAt() != null ? tx.getCreatedAt().toString() : "N/A"
            });
        }
    }

    public void setStatusMessage(String message) {
        statusMessageLabel.setText(message);
    }

    // Accessors for Event Listeners
    public JButton getNavDashboardBtn() { return navDashboardBtn; }
    public JButton getNavProductsBtn() { return navProductsBtn; }
    public JButton getNavTransactionsBtn() { return navTransactionsBtn; }
    public JButton getNavSuppliersBtn() { return navSuppliersBtn; }
    public JButton getNavReportsBtn() { return navReportsBtn; }
    public JButton getLogoutBtn() { return logoutBtn; }

    public JButton getRefreshProductsBtn() { return refreshProductsBtn; }
    public JButton getAddProductBtn() { return addProductBtn; }
    public JButton getEditProductBtn() { return editProductBtn; }
    public JButton getDeleteProductBtn() { return deleteProductBtn; }
    public JTable getProductTable() { return productTable; }
    public DefaultTableModel getProductTableModel() { return productTableModel; }
    public JTextField getProductSearchField() { return productSearchField; }

    public JButton getStockInBtn() { return stockInBtn; }
    public JButton getStockOutBtn() { return stockOutBtn; }
    public JButton getRefreshTransactionsBtn() { return refreshTransactionsBtn; }

    public User getCurrentUser() { return currentUser; }

    /**
     * Cell renderer that badges low-stock rows in soft red.
     */
    private static class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object val, boolean isSel, boolean hasFocus, int r, int c) {
            Component cell = super.getTableCellRendererComponent(table, val, isSel, hasFocus, r, c);
            String status = (String) table.getModel().getValueAt(r, 7);

            if (!isSel) {
                if ("LOW STOCK".equals(status)) {
                    cell.setBackground(new Color(254, 242, 242));
                    cell.setForeground(new Color(185, 28, 28));
                } else {
                    cell.setBackground(r % 2 == 0 ? Color.WHITE : new Color(249, 250, 251));
                    cell.setForeground(new Color(33, 37, 41));
                }
            }
            return cell;
        }
    }
}

