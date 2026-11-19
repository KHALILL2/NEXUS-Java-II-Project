package com.nexus.ims.view;

import com.nexus.ims.model.Product;
import com.nexus.ims.model.StockTransaction;
import com.nexus.ims.model.User;
import com.nexus.ims.util.IconFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Primary Desktop Dashboard Shell for the NEXUS Inventory Management System.
 * Uses pure Java2D vector icons, polished FlatLaf tables, split overview watchlists,
 * and integrated Suppliers & Reports modules.
 */
public class DashboardFrame extends JFrame {
    public static final String CARD_DASHBOARD = "CARD_DASHBOARD";
    public static final String CARD_PRODUCTS = "CARD_PRODUCTS";
    public static final String CARD_TRANSACTIONS = "CARD_TRANSACTIONS";
    public static final String CARD_SUPPLIERS = "CARD_SUPPLIERS";
    public static final String CARD_REPORTS = "CARD_REPORTS";

    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(Locale.US);
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final User currentUser;

    // Navigation Controls
    private SidebarButton navDashboardBtn;
    private SidebarButton navProductsBtn;
    private SidebarButton navTransactionsBtn;
    private SidebarButton navSuppliersBtn;
    private SidebarButton navReportsBtn;
    private JButton logoutBtn;

    // Card Layout Container
    private JPanel cardsPanel;
    private CardLayout cardLayout;
    private JLabel viewTitleLabel;

    // Dashboard Overview KPIs & Watchlists
    private JLabel kpiTotalProductsVal;
    private JLabel kpiLowStockVal;
    private JLabel kpiValuationVal;
    private DefaultTableModel watchlistTableModel;
    private DefaultTableModel recentMovementsTableModel;

    // Products View Components
    private JTable productTable;
    private DefaultTableModel productTableModel;
    private JTextField productSearchField;
    private JButton addProductBtn;
    private JButton editProductBtn;
    private JButton deleteProductBtn;
    private JButton refreshProductsBtn;
    private JButton exportProductsBtn;

    // Transactions View Components
    private JTable transactionTable;
    private DefaultTableModel transactionTableModel;
    private JButton stockInBtn;
    private JButton stockOutBtn;
    private JButton refreshTransactionsBtn;
    private JButton exportTransactionsBtn;

    // Dedicated Module Panels
    private SuppliersPanel suppliersPanel;
    private ReportsPanel reportsPanel;

    // Status Bar Components
    private JLabel dbStatusLabel;
    private JLabel statusMessageLabel;
    private JLabel clockLabel;

    public DashboardFrame(User currentUser) {
        this.currentUser = currentUser != null
                ? currentUser
                : new User("guest", "", "Guest User", "guest@nexus.local", User.Role.STAFF);
        initComponents();
        startClockTimer();
    }

    private void initComponents() {
        setTitle("NEXUS IMS - Enterprise Inventory Management");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 820);
        setMinimumSize(new Dimension(1060, 680));
        setLocationRelativeTo(null);

        JPanel mainLayout = new JPanel(new BorderLayout());
        mainLayout.add(createSidebarPanel(), BorderLayout.WEST);

        JPanel rightArea = new JPanel(new BorderLayout());
        rightArea.add(createHeaderPanel(), BorderLayout.NORTH);
        rightArea.add(createCenterCardsPanel(), BorderLayout.CENTER);
        rightArea.add(createStatusBarPanel(), BorderLayout.SOUTH);

        mainLayout.add(rightArea, BorderLayout.CENTER);
        setContentPane(mainLayout);
    }

    // =========================================================================
    // UI Component Builders
    // =========================================================================

    private JPanel createSidebarPanel() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(245, 0));
        sidebar.setBackground(new Color(24, 28, 36));
        sidebar.setBorder(new EmptyBorder(22, 14, 20, 14));

        // Brand Logo Header
        JPanel brandPanel = new JPanel(new BorderLayout(10, 0));
        brandPanel.setOpaque(false);
        brandPanel.setBorder(new EmptyBorder(0, 8, 0, 8));
        brandPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        brandPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel logoIcon = new JLabel(IconFactory.brandLogo(26, new Color(59, 130, 246)));
        JPanel brandText = new JPanel();
        brandText.setLayout(new BoxLayout(brandText, BoxLayout.Y_AXIS));
        brandText.setOpaque(false);

        JLabel brandLogo = new JLabel("NEXUS IMS");
        brandLogo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        brandLogo.setForeground(Color.WHITE);

        JLabel brandSub = new JLabel("Inventory Operations");
        brandSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        brandSub.setForeground(new Color(148, 163, 184));

        brandText.add(brandLogo);
        brandText.add(brandSub);
        brandPanel.add(logoIcon, BorderLayout.WEST);
        brandPanel.add(brandText, BorderLayout.CENTER);

        sidebar.add(brandPanel);
        sidebar.add(Box.createVerticalStrut(28));

        Color activeIconColor = Color.WHITE;
        Color inactiveIconColor = new Color(148, 163, 184);

        navDashboardBtn = new SidebarButton("Dashboard",
                IconFactory.dashboard(17, activeIconColor),
                IconFactory.dashboard(17, inactiveIconColor), true);
        navProductsBtn = new SidebarButton("Products",
                IconFactory.products(17, activeIconColor),
                IconFactory.products(17, inactiveIconColor), false);
        navTransactionsBtn = new SidebarButton("Transactions",
                IconFactory.transactions(17, activeIconColor),
                IconFactory.transactions(17, inactiveIconColor), false);
        navSuppliersBtn = new SidebarButton("Suppliers",
                IconFactory.suppliers(17, activeIconColor),
                IconFactory.suppliers(17, inactiveIconColor), false);
        navReportsBtn = new SidebarButton("Reports",
                IconFactory.reports(17, activeIconColor),
                IconFactory.reports(17, inactiveIconColor), false);

        sidebar.add(navDashboardBtn);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(navProductsBtn);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(navTransactionsBtn);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(navSuppliersBtn);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(navReportsBtn);

        sidebar.add(Box.createVerticalGlue());

        JSeparator divider = new JSeparator();
        divider.setForeground(new Color(45, 52, 65));
        divider.setBackground(new Color(45, 52, 65));
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sidebar.add(divider);
        sidebar.add(Box.createVerticalStrut(12));

        logoutBtn = new JButton("Sign Out", IconFactory.signOut(16, new Color(248, 113, 113)));
        logoutBtn.setIconTextGap(10);
        logoutBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        logoutBtn.setForeground(new Color(226, 232, 240));
        logoutBtn.setBackground(new Color(38, 44, 56));
        logoutBtn.setBorderPainted(false);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setHorizontalAlignment(SwingConstants.LEFT);
        logoutBtn.setBorder(new EmptyBorder(10, 16, 10, 16));
        logoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        logoutBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(logoutBtn);

        return sidebar;
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, 58));
        header.setBorder(new MatteBorder(0, 0, 1, 0, new Color(226, 232, 240)));

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 16));
        titlePanel.setOpaque(false);
        viewTitleLabel = new JLabel("System Overview & KPI Metrics");
        viewTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        viewTitleLabel.setForeground(new Color(30, 41, 59));
        titlePanel.add(viewTitleLabel);
        header.add(titlePanel, BorderLayout.WEST);

        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 14));
        userPanel.setOpaque(false);

        JLabel userNameLabel = new JLabel(currentUser.getFullName());
        userNameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        userNameLabel.setForeground(new Color(30, 41, 59));

        JLabel rolePill = new JLabel(currentUser.getRole().name());
        rolePill.setFont(new Font("Segoe UI", Font.BOLD, 11));
        rolePill.setOpaque(true);
        rolePill.setBorder(new EmptyBorder(4, 10, 4, 10));

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

        suppliersPanel = new SuppliersPanel();
        reportsPanel = new ReportsPanel();

        cardsPanel.add(buildDashboardOverviewPanel(), CARD_DASHBOARD);
        cardsPanel.add(buildProductsPanel(), CARD_PRODUCTS);
        cardsPanel.add(buildTransactionsPanel(), CARD_TRANSACTIONS);
        cardsPanel.add(suppliersPanel, CARD_SUPPLIERS);
        cardsPanel.add(reportsPanel, CARD_REPORTS);

        return cardsPanel;
    }

    private JPanel buildDashboardOverviewPanel() {
        JPanel panel = new JPanel(new BorderLayout(16, 16));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(18, 20, 18, 20));

        // Top 3 KPI Metric Cards
        JPanel metricsGrid = new JPanel(new GridLayout(1, 3, 18, 0));
        metricsGrid.setOpaque(false);
        metricsGrid.setPreferredSize(new Dimension(0, 112));

        kpiTotalProductsVal = new JLabel("0");
        kpiLowStockVal = new JLabel("0");
        kpiValuationVal = new JLabel("$0.00");

        metricsGrid.add(createMetricCard("TOTAL CATALOG ITEMS", kpiTotalProductsVal,
                new Color(13, 110, 253), new Color(231, 241, 255),
                IconFactory.products(20, new Color(13, 110, 253))));
        metricsGrid.add(createMetricCard("LOW STOCK ALERTS", kpiLowStockVal,
                new Color(220, 53, 69), new Color(254, 226, 226),
                IconFactory.alert(20, new Color(220, 53, 69))));
        metricsGrid.add(createMetricCard("TOTAL ASSET VALUATION", kpiValuationVal,
                new Color(25, 135, 84), new Color(220, 252, 231),
                IconFactory.reports(20, new Color(25, 135, 84))));
        panel.add(metricsGrid, BorderLayout.NORTH);

        // Center Split Two-Column Grid: Low Stock Watchlist + Recent Movements
        JPanel splitGrid = new JPanel(new GridLayout(1, 2, 18, 0));
        splitGrid.setOpaque(false);

        // Left Card: Low Stock Watchlist
        String[] watchCols = {"SKU", "Product Name", "Stock Qty", "Min Level", "Deficit"};
        watchlistTableModel = new DefaultTableModel(watchCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable watchlistTable = new JTable(watchlistTableModel);
        watchlistTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        watchlistTable.getTableHeader().setReorderingAllowed(false);
        watchlistTable.setDefaultRenderer(Object.class, new WatchlistCellRenderer());
        splitGrid.add(createOverviewTableCard("Low Stock Watchlist",
                IconFactory.alert(16, new Color(220, 53, 69)), watchlistTable));

        // Right Card: Recent Stock Movements
        String[] recentCols = {"Timestamp", "Product", "Type", "Delta"};
        recentMovementsTableModel = new DefaultTableModel(recentCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable recentTable = new JTable(recentMovementsTableModel);
        recentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        recentTable.getTableHeader().setReorderingAllowed(false);
        recentTable.setDefaultRenderer(Object.class, new RecentMovementRenderer());
        splitGrid.add(createOverviewTableCard("Recent Stock Movements",
                IconFactory.transactions(16, new Color(13, 110, 253)), recentTable));

        panel.add(splitGrid, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createOverviewTableCard(String title, Icon icon, JTable table) {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 1, 1, 1, new Color(226, 232, 240)),
                new EmptyBorder(16, 16, 16, 16)
        ));

        JLabel titleLbl = new JLabel(title, icon, SwingConstants.LEFT);
        titleLbl.setIconTextGap(8);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleLbl.setForeground(new Color(30, 41, 59));
        card.add(titleLbl, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new MatteBorder(1, 1, 1, 1, new Color(233, 236, 239)));
        scroll.getViewport().setBackground(Color.WHITE);
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    private JPanel createMetricCard(String title, JLabel valueLabel, Color accentColor, Color badgeBg, Icon badgeIcon) {
        JPanel card = new JPanel(new BorderLayout(12, 6));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 4, 0, 0, accentColor),
                BorderFactory.createCompoundBorder(
                        new MatteBorder(1, 0, 1, 1, new Color(226, 232, 240)),
                        new EmptyBorder(16, 18, 16, 18)
                )
        ));

        JPanel textCol = new JPanel();
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
        textCol.setOpaque(false);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        titleLbl.setForeground(new Color(100, 116, 139));

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 25));
        valueLabel.setForeground(new Color(30, 41, 59));

        textCol.add(titleLbl);
        textCol.add(Box.createVerticalStrut(6));
        textCol.add(valueLabel);

        JLabel iconBadge = new JLabel(badgeIcon, SwingConstants.CENTER);
        iconBadge.setOpaque(true);
        iconBadge.setBackground(badgeBg);
        iconBadge.setPreferredSize(new Dimension(44, 44));

        JPanel badgeWrapper = new JPanel(new GridBagLayout());
        badgeWrapper.setOpaque(false);
        badgeWrapper.add(iconBadge);

        card.add(textCol, BorderLayout.CENTER);
        card.add(badgeWrapper, BorderLayout.EAST);
        return card;
    }

    private JPanel buildProductsPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(18, 20, 18, 20));

        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setOpaque(false);

        productSearchField = new JTextField(22);
        productSearchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        productSearchField.setPreferredSize(new Dimension(280, 36));
        productSearchField.putClientProperty("JTextField.leadingIcon", IconFactory.search(15, new Color(108, 117, 125)));
        productSearchField.putClientProperty("JTextField.placeholderText", "Search SKU or product name...");
        productSearchField.putClientProperty("JTextField.showClearButton", true);
        toolbar.add(productSearchField, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        refreshProductsBtn = new JButton("Refresh", IconFactory.refresh(15, new Color(73, 80, 87)));
        refreshProductsBtn.setIconTextGap(6);

        addProductBtn = new JButton("Add Product", IconFactory.add(15, Color.WHITE));
        addProductBtn.setIconTextGap(6);
        addProductBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        addProductBtn.setBackground(new Color(13, 110, 253));
        addProductBtn.setForeground(Color.WHITE);

        editProductBtn = new JButton("Edit", IconFactory.edit(15, new Color(73, 80, 87)));
        editProductBtn.setIconTextGap(6);

        deleteProductBtn = new JButton("Delete", IconFactory.delete(15, new Color(220, 53, 69)));
        deleteProductBtn.setIconTextGap(6);
        deleteProductBtn.setForeground(new Color(220, 53, 69));

        exportProductsBtn = new JButton("Export", IconFactory.exportDropdown(15, new Color(73, 80, 87)));
        exportProductsBtn.setIconTextGap(6);

        actions.add(refreshProductsBtn);
        actions.add(exportProductsBtn);
        actions.add(addProductBtn);
        actions.add(editProductBtn);
        actions.add(deleteProductBtn);
        toolbar.add(actions, BorderLayout.EAST);

        panel.add(toolbar, BorderLayout.NORTH);

        String[] columns = {"ID", "SKU", "Product Name", "Cost ($)", "Selling ($)", "Stock Qty", "Min Threshold", "Status"};
        productTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        productTable = new JTable(productTableModel);
        productTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        productTable.getTableHeader().setReorderingAllowed(false);
        productTable.setDefaultRenderer(Object.class, new ProductTableCellRenderer());
        productTable.getColumnModel().getColumn(7).setCellRenderer(new StatusBadgeRenderer());

        TableColumnModel cm = productTable.getColumnModel();
        cm.getColumn(0).setPreferredWidth(55);
        cm.getColumn(0).setMaxWidth(75);
        cm.getColumn(1).setPreferredWidth(140);
        cm.getColumn(2).setPreferredWidth(260);
        cm.getColumn(7).setPreferredWidth(120);

        JScrollPane scrollPane = new JScrollPane(productTable);
        scrollPane.setBorder(new MatteBorder(1, 1, 1, 1, new Color(226, 232, 240)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildTransactionsPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(18, 20, 18, 20));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);

        stockInBtn = new JButton("Inbound Receipt (Stock IN)", IconFactory.stockIn(16, Color.WHITE));
        stockInBtn.setIconTextGap(8);
        stockInBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        stockInBtn.setBackground(new Color(25, 135, 84));
        stockInBtn.setForeground(Color.WHITE);

        stockOutBtn = new JButton("Outbound Dispatch (Stock OUT)", IconFactory.stockOut(16, Color.WHITE));
        stockOutBtn.setIconTextGap(8);
        stockOutBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        stockOutBtn.setBackground(new Color(220, 53, 69));
        stockOutBtn.setForeground(Color.WHITE);

        refreshTransactionsBtn = new JButton("Refresh Log", IconFactory.refresh(15, new Color(73, 80, 87)));
        refreshTransactionsBtn.setIconTextGap(6);

        exportTransactionsBtn = new JButton("Export", IconFactory.exportDropdown(15, new Color(73, 80, 87)));
        exportTransactionsBtn.setIconTextGap(6);

        actions.add(stockInBtn);
        actions.add(stockOutBtn);
        actions.add(refreshTransactionsBtn);
        actions.add(exportTransactionsBtn);
        panel.add(actions, BorderLayout.NORTH);

        String[] columns = {"ID", "Product", "Movement Type", "Quantity Delta", "Notes", "User ID", "Timestamp"};
        transactionTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        transactionTable = new JTable(transactionTableModel);
        transactionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        transactionTable.getTableHeader().setReorderingAllowed(false);
        transactionTable.setDefaultRenderer(Object.class, new TransactionTableCellRenderer());

        TableColumnModel cm = transactionTable.getColumnModel();
        cm.getColumn(0).setPreferredWidth(55);
        cm.getColumn(0).setMaxWidth(75);
        cm.getColumn(1).setPreferredWidth(240);
        cm.getColumn(2).setPreferredWidth(155);
        cm.getColumn(3).setPreferredWidth(110);
        cm.getColumn(4).setPreferredWidth(220);
        cm.getColumn(5).setPreferredWidth(70);
        cm.getColumn(6).setPreferredWidth(155);

        JScrollPane scrollPane = new JScrollPane(transactionTable);
        scrollPane.setBorder(new MatteBorder(1, 1, 1, 1, new Color(226, 232, 240)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStatusBarPanel() {
        JPanel status = new JPanel(new BorderLayout(15, 0));
        status.setBackground(new Color(248, 249, 250));
        status.setPreferredSize(new Dimension(0, 30));
        status.setBorder(new MatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        dbStatusLabel = new JLabel("Database: Online [MySQL Server]",
                IconFactory.statusDot(10, new Color(25, 135, 84)), SwingConstants.LEFT);
        dbStatusLabel.setIconTextGap(6);
        dbStatusLabel.setBorder(new EmptyBorder(0, 14, 0, 0));
        dbStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        dbStatusLabel.setForeground(new Color(25, 135, 84));

        statusMessageLabel = new JLabel("System Ready", SwingConstants.CENTER);
        statusMessageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusMessageLabel.setForeground(new Color(100, 116, 139));

        clockLabel = new JLabel(" ", SwingConstants.RIGHT);
        clockLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        clockLabel.setForeground(new Color(71, 85, 105));
        clockLabel.setBorder(new EmptyBorder(0, 0, 0, 14));

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
        navDashboardBtn.setActive(activeKey.equals(CARD_DASHBOARD));
        navProductsBtn.setActive(activeKey.equals(CARD_PRODUCTS));
        navTransactionsBtn.setActive(activeKey.equals(CARD_TRANSACTIONS));
        navSuppliersBtn.setActive(activeKey.equals(CARD_SUPPLIERS));
        navReportsBtn.setActive(activeKey.equals(CARD_REPORTS));
    }

    public void setMetrics(int totalProducts, int lowStockCount, BigDecimal valuation) {
        kpiTotalProductsVal.setText(String.valueOf(totalProducts));
        kpiLowStockVal.setText(String.valueOf(lowStockCount));
        kpiValuationVal.setText(CURRENCY.format(valuation != null ? valuation : BigDecimal.ZERO));
    }

    public void populateDashboardWatchlists(List<Product> lowStockProducts,
                                            List<StockTransaction> recentTransactions,
                                            Map<Integer, Product> productMap) {
        watchlistTableModel.setRowCount(0);
        if (lowStockProducts != null) {
            for (Product p : lowStockProducts) {
                int deficit = Math.max(0, p.getReorderLevel() - p.getQuantityInStock());
                watchlistTableModel.addRow(new Object[]{
                        p.getSku(),
                        p.getName(),
                        p.getQuantityInStock(),
                        p.getReorderLevel(),
                        deficit
                });
            }
        }

        recentMovementsTableModel.setRowCount(0);
        if (recentTransactions != null) {
            int limit = Math.min(15, recentTransactions.size());
            for (int i = 0; i < limit; i++) {
                StockTransaction tx = recentTransactions.get(i);
                recentMovementsTableModel.addRow(new Object[]{
                        formatTimestamp(tx.getCreatedAt()),
                        resolveProductLabel(tx.getProductId(), productMap),
                        tx.getTransactionType().name(),
                        (tx.getQuantityChange() > 0 ? "+" : "") + tx.getQuantityChange()
                });
            }
        }
    }

    public void populateProductTable(List<Product> products) {
        productTableModel.setRowCount(0);
        for (Product p : products) {
            String status = p.isLowStock() ? "LOW STOCK" : "IN STOCK";
            productTableModel.addRow(new Object[]{
                    p.getId(),
                    p.getSku(),
                    p.getName(),
                    CURRENCY.format(p.getPurchasePrice() != null ? p.getPurchasePrice() : BigDecimal.ZERO),
                    CURRENCY.format(p.getUnitPrice() != null ? p.getUnitPrice() : BigDecimal.ZERO),
                    p.getQuantityInStock(),
                    p.getReorderLevel(),
                    status
            });
        }
    }

    public void populateTransactionTable(List<StockTransaction> transactions, Map<Integer, Product> productMap) {
        transactionTableModel.setRowCount(0);
        for (StockTransaction tx : transactions) {
            transactionTableModel.addRow(new Object[]{
                    tx.getId(),
                    resolveProductLabel(tx.getProductId(), productMap),
                    tx.getTransactionType().name(),
                    (tx.getQuantityChange() > 0 ? "+" : "") + tx.getQuantityChange(),
                    tx.getNotes() != null ? tx.getNotes() : "-",
                    tx.getPerformedByUserId() != null ? tx.getPerformedByUserId() : "System",
                    formatTimestamp(tx.getCreatedAt())
            });
        }
    }

    private String resolveProductLabel(int productId, Map<Integer, Product> productMap) {
        if (productMap != null) {
            Product p = productMap.get(productId);
            if (p != null) {
                return "[" + p.getSku() + "] " + p.getName();
            }
        }
        return "Product #" + productId;
    }

    private String formatTimestamp(LocalDateTime ts) {
        return ts != null ? ts.format(TS_FORMAT) : "N/A";
    }

    public void setStatusMessage(String message) {
        statusMessageLabel.setText(message);
    }

    // Accessors for Event Listeners & Controllers
    public JButton getNavDashboardBtn() { return navDashboardBtn; }
    public JButton getNavProductsBtn() { return navProductsBtn; }
    public JButton getNavTransactionsBtn() { return navTransactionsBtn; }
    public JButton getNavSuppliersBtn() { return navSuppliersBtn; }
    public JButton getNavReportsBtn() { return navReportsBtn; }
    public JButton getLogoutBtn() { return logoutBtn; }

    public JButton getRefreshProductsBtn() { return refreshProductsBtn; }
    public JButton getExportProductsBtn() { return exportProductsBtn; }
    public JButton getAddProductBtn() { return addProductBtn; }
    public JButton getEditProductBtn() { return editProductBtn; }
    public JButton getDeleteProductBtn() { return deleteProductBtn; }
    public JTable getProductTable() { return productTable; }
    public DefaultTableModel getProductTableModel() { return productTableModel; }
    public JTextField getProductSearchField() { return productSearchField; }

    public JButton getStockInBtn() { return stockInBtn; }
    public JButton getStockOutBtn() { return stockOutBtn; }
    public JButton getRefreshTransactionsBtn() { return refreshTransactionsBtn; }
    public JButton getExportTransactionsBtn() { return exportTransactionsBtn; }

    public SuppliersPanel getSuppliersPanel() { return suppliersPanel; }
    public ReportsPanel getReportsPanel() { return reportsPanel; }
    public User getCurrentUser() { return currentUser; }

    // =========================================================================
    // Custom Modern Swing Components & Cell Renderers
    // =========================================================================

    private static class SidebarButton extends JButton {
        private final Icon activeIcon;
        private final Icon inactiveIcon;
        private boolean active;
        private boolean hovered = false;

        SidebarButton(String label, Icon activeIcon, Icon inactiveIcon, boolean active) {
            super(label);
            this.activeIcon = activeIcon;
            this.inactiveIcon = inactiveIcon;
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setHorizontalAlignment(SwingConstants.LEFT);
            setIconTextGap(12);
            setBorder(new EmptyBorder(10, 18, 10, 14));
            setBorderPainted(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setActive(active);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }
            });
        }

        void setActive(boolean active) {
            this.active = active;
            setIcon(active ? activeIcon : inactiveIcon);
            setForeground(active ? Color.WHITE : new Color(203, 213, 225));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                if (active) {
                    g2.setColor(new Color(13, 110, 253));
                    g2.fill(new RoundRectangle2D.Float(0, 1, w, h - 2, 10, 10));
                    g2.setColor(new Color(147, 197, 253));
                    g2.fill(new RoundRectangle2D.Float(0, 7, 4, h - 14, 4, 4));
                } else if (hovered) {
                    g2.setColor(new Color(38, 45, 58));
                    g2.fill(new RoundRectangle2D.Float(0, 1, w, h - 2, 10, 10));
                }
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    private static class ProductTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object val, boolean isSel,
                                                       boolean hasFocus, int row, int col) {
            JLabel cell = (JLabel) super.getTableCellRendererComponent(table, val, isSel, hasFocus, row, col);
            cell.setBorder(new EmptyBorder(0, 12, 0, 12));

            boolean rightAlign = col == 0 || (col >= 3 && col <= 6);
            cell.setHorizontalAlignment(rightAlign ? SwingConstants.RIGHT : SwingConstants.LEFT);

            if (col == 1 || col == 2) {
                cell.setFont(cell.getFont().deriveFont(col == 1 ? Font.BOLD : Font.PLAIN));
            }

            if (!isSel) {
                String status = String.valueOf(table.getModel().getValueAt(row, 7));
                if ("LOW STOCK".equals(status)) {
                    cell.setBackground(new Color(254, 247, 247));
                } else {
                    cell.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                }
                cell.setForeground(new Color(30, 41, 59));
            }
            return cell;
        }
    }

    private static class StatusBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object val, boolean isSel,
                                                       boolean hasFocus, int row, int col) {
            String status = val != null ? val.toString() : "";
            boolean isLow = "LOW STOCK".equals(status);

            JPanel wrapper = new JPanel(new GridBagLayout());
            wrapper.setOpaque(true);
            if (isSel) {
                wrapper.setBackground(table.getSelectionBackground());
            } else {
                wrapper.setBackground(isLow ? new Color(254, 247, 247)
                        : (row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252)));
            }

            Color pillBg = isLow ? new Color(254, 226, 226) : new Color(220, 252, 231);
            Color pillFg = isLow ? new Color(185, 28, 28) : new Color(21, 128, 61);

            JLabel pill = new JLabel(status) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    try {
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(pillBg);
                        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                    } finally {
                        g2.dispose();
                    }
                    super.paintComponent(g);
                }
            };
            pill.setFont(new Font("Segoe UI", Font.BOLD, 11));
            pill.setForeground(pillFg);
            pill.setOpaque(false);
            pill.setBorder(new EmptyBorder(3, 10, 3, 10));

            wrapper.add(pill);
            return wrapper;
        }
    }

    private static class TransactionTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object val, boolean isSel,
                                                       boolean hasFocus, int row, int col) {
            JLabel cell = (JLabel) super.getTableCellRendererComponent(table, val, isSel, hasFocus, row, col);
            cell.setBorder(new EmptyBorder(0, 12, 0, 12));
            cell.setHorizontalAlignment((col == 0 || col == 3 || col == 5) ? SwingConstants.RIGHT : SwingConstants.LEFT);

            if (!isSel) {
                cell.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                cell.setForeground(new Color(30, 41, 59));
            }

            if (col == 3 && val != null) {
                String text = val.toString();
                cell.setFont(cell.getFont().deriveFont(Font.BOLD));
                if (!isSel) {
                    cell.setForeground(text.startsWith("+")
                            ? new Color(21, 128, 61)
                            : new Color(185, 28, 28));
                }
            }
            return cell;
        }
    }

    private static class WatchlistCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object val, boolean isSel,
                                                       boolean hasFocus, int row, int col) {
            JLabel cell = (JLabel) super.getTableCellRendererComponent(table, val, isSel, hasFocus, row, col);
            cell.setBorder(new EmptyBorder(0, 10, 0, 10));
            cell.setHorizontalAlignment(col >= 2 ? SwingConstants.RIGHT : SwingConstants.LEFT);
            if (!isSel) {
                cell.setBackground(row % 2 == 0 ? Color.WHITE : new Color(254, 247, 247));
                cell.setForeground(col == 4 ? new Color(185, 28, 28) : new Color(30, 41, 59));
                if (col == 0 || col == 4) {
                    cell.setFont(cell.getFont().deriveFont(Font.BOLD));
                }
            }
            return cell;
        }
    }

    private static class RecentMovementRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object val, boolean isSel,
                                                       boolean hasFocus, int row, int col) {
            JLabel cell = (JLabel) super.getTableCellRendererComponent(table, val, isSel, hasFocus, row, col);
            cell.setBorder(new EmptyBorder(0, 10, 0, 10));
            cell.setHorizontalAlignment(col == 3 ? SwingConstants.RIGHT : SwingConstants.LEFT);
            if (!isSel) {
                cell.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                cell.setForeground(new Color(30, 41, 59));
                if (col == 3 && val != null) {
                    cell.setFont(cell.getFont().deriveFont(Font.BOLD));
                    cell.setForeground(val.toString().startsWith("+")
                            ? new Color(21, 128, 61)
                            : new Color(185, 28, 28));
                }
            }
            return cell;
        }
    }
}
