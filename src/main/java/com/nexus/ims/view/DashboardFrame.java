package com.nexus.ims.view;

import com.nexus.ims.model.Product;
import com.nexus.ims.model.StockTransaction;
import com.nexus.ims.model.User;
import com.nexus.ims.util.IconFactory;
import com.nexus.ims.util.LanguageManager;
import com.nexus.ims.util.LanguageManager.Language;
import com.nexus.ims.util.ThemeManager;
import com.nexus.ims.util.ThemeManager.BadgeColors;
import com.nexus.ims.util.ThemeManager.ThemeMode;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Main desktop window hosting the collapsible sidebar navigation, header breadcrumbs,
 * theme/language switchers, KPI overview, and module views.
 */
public class DashboardFrame extends JFrame {
    public static final String CARD_DASHBOARD = "CARD_DASHBOARD";
    public static final String CARD_PRODUCTS = "CARD_PRODUCTS";
    public static final String CARD_TRANSACTIONS = "CARD_TRANSACTIONS";
    public static final String CARD_SUPPLIERS = "CARD_SUPPLIERS";
    public static final String CARD_REPORTS = "CARD_REPORTS";
    public static final String CARD_USERS = "CARD_USERS";

    private static final String WATCHLIST_TABLE = "TABLE";
    private static final String WATCHLIST_EMPTY = "EMPTY";
    private static final int SIDEBAR_EXPANDED_WIDTH = 236;
    private static final int SIDEBAR_COLLAPSED_WIDTH = 72;

    public static final Comparator<Object> NUMERIC_COMPARATOR = (a, b) ->
            Double.compare(parseNumeric(a), parseNumeric(b));

    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(Locale.US);
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd  HH:mm:ss");

    private final User currentUser;
    private String currentCardKey = CARD_DASHBOARD;
    private boolean sidebarCollapsed = false;
    private boolean updatingControls = false;
    private boolean canEditProducts = true;
    private boolean canDeleteProducts = true;

    // Frame Structural Containers
    private JPanel sidebarPanel;
    private JPanel sidebarBrandHeader;
    private JPanel brandLeftGroup;
    private JPanel navStackPanel;
    private JPanel sidebarFooterPanel;
    private JPanel profileCardPanel;
    private JPanel profileTextCol;
    private JPanel headerPanel;
    private JPanel cardsPanel;
    private JPanel statusBarPanel;
    private JSeparator sidebarDivider;
    private CardLayout cardLayout;

    // Sidebar Controls & Profile
    private JLabel brandLogoLabel;
    private JLabel brandSubLabel;
    private JLabel brandIconLabel;
    private JButton sidebarToggleBtn;
    private JLabel operationsSectionLabel;
    private JLabel adminSectionLabel;
    private AvatarCircle profileAvatar;
    private JLabel profileNameLabel;
    private JLabel profileRolePill;

    // Header Breadcrumb & Switchers
    private JLabel viewTitleLabel;
    private JLabel breadcrumbLabel;
    private JComboBox<ThemeMode> themeCombo;
    private JComboBox<Language> languageCombo;

    // Navigation Controls
    private SidebarButton navDashboardBtn;
    private SidebarButton navProductsBtn;
    private SidebarButton navTransactionsBtn;
    private SidebarButton navSuppliersBtn;
    private SidebarButton navReportsBtn;
    private SidebarButton navUsersBtn;
    private JButton logoutBtn;

    // Dashboard Overview KPIs & Watchlists
    private InteractiveKpiCard productsKpiCard;
    private InteractiveKpiCard unitsKpiCard;
    private InteractiveKpiCard lowStockKpiCard;
    private InteractiveKpiCard valuationKpiCard;
    private JLabel kpiTotalProductsVal;
    private JLabel kpiTotalUnitsVal;
    private JLabel kpiLowStockVal;
    private JLabel kpiValuationVal;
    private BigDecimal lastCostValuation = BigDecimal.ZERO;
    private BigDecimal lastRetailValuation = BigDecimal.ZERO;
    private int lastTotalProducts = 0;
    private int lastTotalCategories = 0;
    private int lastTotalUnits = 0;
    private int lastLowStockCount = 0;

    private JPanel watchlistCard;
    private JLabel watchlistTitleLabel;
    private JScrollPane watchlistScroll;
    private CardLayout watchlistCardLayout;
    private JPanel watchlistContentPanel;
    private JPanel watchlistEmptyPanel;
    private JLabel watchlistEmptyIcon;
    private JLabel watchlistEmptyTitle;
    private JLabel watchlistEmptySub;
    private JTable watchlistTable;
    private DefaultTableModel watchlistTableModel;
    private JButton quickRestockBtn;

    private JPanel recentCard;
    private JLabel recentTitleLabel;
    private JScrollPane recentScroll;
    private JTable recentTable;
    private DefaultTableModel recentMovementsTableModel;

    // Products View Components
    private JTable productTable;
    private DefaultTableModel productTableModel;
    private JScrollPane productScroll;
    private JPanel productSummaryBar;
    private JLabel productSummaryLabel;
    private JTextField productSearchField;
    private JComboBox<String> productStatusFilterCombo;
    private JButton addProductBtn;
    private JButton editProductBtn;
    private JButton deleteProductBtn;
    private JButton refreshProductsBtn;
    private JButton exportProductsBtn;
    private JButton manageCategoriesBtn;
    private int lastTotalCatalogCount = 0;
    private int lastShownUnits = 0;
    private BigDecimal lastShownCostValue = BigDecimal.ZERO;

    // Product Right-Click Context Menu
    private JPopupMenu productContextMenu;
    private JMenuItem ctxEditProductItem;
    private JMenuItem ctxStockInItem;
    private JMenuItem ctxStockOutItem;
    private JMenuItem ctxCopySkuItem;
    private JMenuItem ctxDeleteProductItem;

    // Transactions View Components
    private JTable transactionTable;
    private DefaultTableModel transactionTableModel;
    private JScrollPane transactionScroll;
    private JTextField transactionSearchField;
    private JComboBox<String> transactionTypeFilterCombo;
    private JComboBox<String> transactionTimeframeFilterCombo;
    private JButton stockInBtn;
    private JButton stockOutBtn;
    private JButton refreshTransactionsBtn;
    private JButton exportTransactionsBtn;

    // Dedicated Module Panels
    private SuppliersPanel suppliersPanel;
    private ReportsPanel reportsPanel;
    private UsersPanel usersPanel;

    // Status Bar Components
    private JLabel dbStatusLabel;
    private JLabel statusMessageLabel;
    private JProgressBar statusProgressBar;
    private JLabel clockLabel;
    private Timer clockTimer;

    // Cached DTO Snapshots for Instant Live Language Re-rendering
    private List<Product> lastLowStockProducts = Collections.emptyList();
    private List<StockTransaction> lastRecentTransactions = Collections.emptyList();
    private Map<Integer, Product> lastRecentProductMap = Collections.emptyMap();
    private List<Product> lastCatalogProducts = Collections.emptyList();
    private Map<Integer, String> lastCategoryNames = Collections.emptyMap();
    private Map<Integer, String> lastSupplierNames = Collections.emptyMap();
    private List<StockTransaction> lastTransactions = Collections.emptyList();
    private Map<Integer, Product> lastTxProductMap = Collections.emptyMap();
    private Map<Integer, User> lastTxUserMap = Collections.emptyMap();

    private final Runnable themeListener = this::refreshThemeAndLanguage;
    private final Runnable languageListener = this::refreshThemeAndLanguage;

    public DashboardFrame(User currentUser) {
        this.currentUser = currentUser != null
                ? currentUser
                : new User("guest", "", "Guest User", "guest@nexus.local", User.Role.STAFF);
        initComponents();
        ThemeManager.addThemeListener(themeListener);
        LanguageManager.addLanguageListener(languageListener);
        refreshThemeAndLanguage();
        registerSearchShortcut();
        startClockTimer();
    }

    private void initComponents() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1360, 860);
        setMinimumSize(new Dimension(1120, 700));
        setLocationRelativeTo(null);

        JPanel mainLayout = new JPanel(new BorderLayout());
        mainLayout.add(createSidebarPanel(), BorderLayout.LINE_START);

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
        sidebarPanel = new JPanel(new BorderLayout(0, 14));
        sidebarPanel.setPreferredSize(new Dimension(SIDEBAR_EXPANDED_WIDTH, 0));
        sidebarPanel.setBorder(new EmptyBorder(18, 12, 16, 12));

        // 1. Brand Header + Collapse/Expand Toggle
        sidebarBrandHeader = new JPanel(new BorderLayout(8, 0));
        sidebarBrandHeader.setOpaque(false);
        sidebarBrandHeader.setBorder(new EmptyBorder(0, 6, 4, 2));

        brandLeftGroup = new JPanel(new BorderLayout(10, 0));
        brandLeftGroup.setOpaque(false);

        brandIconLabel = new JLabel();
        JPanel brandText = new JPanel(new GridBagLayout());
        brandText.setOpaque(false);
        GridBagConstraints bgc = new GridBagConstraints();
        bgc.gridx = 0;
        bgc.weightx = 1.0;
        bgc.fill = GridBagConstraints.HORIZONTAL;
        bgc.anchor = GridBagConstraints.LINE_START;

        brandLogoLabel = new JLabel();
        brandLogoLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        brandLogoLabel.setForeground(Color.WHITE);
        brandLogoLabel.setHorizontalAlignment(SwingConstants.LEADING);
        bgc.gridy = 0;
        brandText.add(brandLogoLabel, bgc);

        brandSubLabel = new JLabel();
        brandSubLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        brandSubLabel.setHorizontalAlignment(SwingConstants.LEADING);
        bgc.gridy = 1;
        brandText.add(brandSubLabel, bgc);

        brandLeftGroup.add(brandIconLabel, BorderLayout.LINE_START);
        brandLeftGroup.add(brandText, BorderLayout.CENTER);

        sidebarToggleBtn = new JButton();
        sidebarToggleBtn.setContentAreaFilled(false);
        sidebarToggleBtn.setBorderPainted(false);
        sidebarToggleBtn.setFocusPainted(false);
        sidebarToggleBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        sidebarToggleBtn.setPreferredSize(new Dimension(32, 32));
        sidebarToggleBtn.addActionListener(e -> toggleSidebarCollapsed());

        sidebarBrandHeader.add(brandLeftGroup, BorderLayout.CENTER);
        sidebarBrandHeader.add(sidebarToggleBtn, BorderLayout.LINE_END);
        sidebarPanel.add(sidebarBrandHeader, BorderLayout.NORTH);

        // 2. Grouped Navigation Stack (100% uniform width)
        navStackPanel = new JPanel(new GridBagLayout());
        navStackPanel.setOpaque(false);
        GridBagConstraints ngc = new GridBagConstraints();
        ngc.gridx = 0;
        ngc.weightx = 1.0;
        ngc.fill = GridBagConstraints.HORIZONTAL;
        ngc.anchor = GridBagConstraints.LINE_START;

        int row = 0;
        operationsSectionLabel = new JLabel();
        operationsSectionLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        operationsSectionLabel.setHorizontalAlignment(SwingConstants.LEADING);
        ngc.gridy = row++;
        ngc.insets = new Insets(6, 0, 6, 0);
        navStackPanel.add(operationsSectionLabel, ngc);

        navDashboardBtn = new SidebarButton(true);
        navProductsBtn = new SidebarButton(false);
        navTransactionsBtn = new SidebarButton(false);
        navSuppliersBtn = new SidebarButton(false);
        navReportsBtn = new SidebarButton(false);

        ngc.insets = new Insets(0, 0, 4, 0);
        ngc.gridy = row++;
        navStackPanel.add(navDashboardBtn, ngc);
        ngc.gridy = row++;
        navStackPanel.add(navProductsBtn, ngc);
        ngc.gridy = row++;
        navStackPanel.add(navTransactionsBtn, ngc);
        ngc.gridy = row++;
        navStackPanel.add(navSuppliersBtn, ngc);
        ngc.gridy = row++;
        navStackPanel.add(navReportsBtn, ngc);

        adminSectionLabel = new JLabel();
        adminSectionLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        adminSectionLabel.setHorizontalAlignment(SwingConstants.LEADING);
        navUsersBtn = new SidebarButton(false);

        boolean isAdmin = currentUser.getRole() == User.Role.ADMIN;
        adminSectionLabel.setVisible(isAdmin);
        navUsersBtn.setVisible(isAdmin);

        ngc.gridy = row++;
        ngc.insets = new Insets(14, 0, 6, 0);
        navStackPanel.add(adminSectionLabel, ngc);

        ngc.gridy = row++;
        ngc.insets = new Insets(0, 0, 4, 0);
        navStackPanel.add(navUsersBtn, ngc);

        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        ngc.gridy = row;
        ngc.weighty = 1.0;
        ngc.fill = GridBagConstraints.BOTH;
        navStackPanel.add(spacer, ngc);

        sidebarPanel.add(navStackPanel, BorderLayout.CENTER);

        // 3. Footer: Divider + User Profile Card + Sign Out Button
        sidebarFooterPanel = new JPanel(new GridBagLayout());
        sidebarFooterPanel.setOpaque(false);
        GridBagConstraints fgc = new GridBagConstraints();
        fgc.gridx = 0;
        fgc.weightx = 1.0;
        fgc.fill = GridBagConstraints.HORIZONTAL;

        sidebarDivider = new JSeparator();
        sidebarDivider.setPreferredSize(new Dimension(0, 1));
        fgc.gridy = 0;
        fgc.insets = new Insets(0, 0, 10, 0);
        sidebarFooterPanel.add(sidebarDivider, fgc);

        profileCardPanel = new JPanel(new BorderLayout(10, 0));
        profileCardPanel.setOpaque(true);

        profileAvatar = new AvatarCircle(extractInitials(currentUser.getFullName()));
        profileTextCol = new JPanel(new GridBagLayout());
        profileTextCol.setOpaque(false);
        GridBagConstraints pgc = new GridBagConstraints();
        pgc.gridx = 0;
        pgc.weightx = 1.0;
        pgc.fill = GridBagConstraints.HORIZONTAL;
        pgc.anchor = GridBagConstraints.LINE_START;

        profileNameLabel = new JLabel(currentUser.getFullName());
        profileNameLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        profileNameLabel.setForeground(Color.WHITE);
        profileNameLabel.setHorizontalAlignment(SwingConstants.LEADING);
        pgc.gridy = 0;
        pgc.insets = new Insets(0, 0, 3, 0);
        profileTextCol.add(profileNameLabel, pgc);

        JPanel pillHolder = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        pillHolder.setOpaque(false);
        profileRolePill = new JLabel();
        profileRolePill.setFont(new Font("Segoe UI", Font.BOLD, 10));
        profileRolePill.setOpaque(true);
        profileRolePill.setBorder(new EmptyBorder(2, 7, 2, 7));
        pillHolder.add(profileRolePill);
        pgc.gridy = 1;
        pgc.insets = new Insets(0, 0, 0, 0);
        profileTextCol.add(pillHolder, pgc);

        profileCardPanel.add(profileAvatar, BorderLayout.LINE_START);
        profileCardPanel.add(profileTextCol, BorderLayout.CENTER);

        fgc.gridy = 1;
        fgc.insets = new Insets(0, 0, 8, 0);
        sidebarFooterPanel.add(profileCardPanel, fgc);

        logoutBtn = new JButton();
        logoutBtn.setIconTextGap(10);
        logoutBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        logoutBtn.setBorderPainted(false);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutBtn.setPreferredSize(new Dimension(0, 38));

        fgc.gridy = 2;
        fgc.insets = new Insets(0, 0, 0, 0);
        sidebarFooterPanel.add(logoutBtn, fgc);

        sidebarPanel.add(sidebarFooterPanel, BorderLayout.SOUTH);
        return sidebarPanel;
    }

    private void toggleSidebarCollapsed() {
        sidebarCollapsed = !sidebarCollapsed;
        applySidebarCollapsedState();
    }

    private void applySidebarCollapsedState() {
        boolean rtl = LanguageManager.isRTL();
        int width = sidebarCollapsed ? SIDEBAR_COLLAPSED_WIDTH : SIDEBAR_EXPANDED_WIDTH;
        sidebarPanel.setPreferredSize(new Dimension(width, 0));
        sidebarPanel.setBorder(sidebarCollapsed
                ? new EmptyBorder(18, 10, 16, 10)
                : new EmptyBorder(18, 12, 16, 12));

        brandLeftGroup.setVisible(!sidebarCollapsed);
        sidebarBrandHeader.remove(sidebarToggleBtn);
        if (sidebarCollapsed) {
            sidebarBrandHeader.add(sidebarToggleBtn, BorderLayout.CENTER);
        } else {
            sidebarBrandHeader.add(sidebarToggleBtn, BorderLayout.LINE_END);
        }

        boolean isAdmin = currentUser.getRole() == User.Role.ADMIN;
        operationsSectionLabel.setVisible(!sidebarCollapsed);
        adminSectionLabel.setVisible(!sidebarCollapsed && isAdmin);

        navDashboardBtn.setCollapsed(sidebarCollapsed);
        navProductsBtn.setCollapsed(sidebarCollapsed);
        navTransactionsBtn.setCollapsed(sidebarCollapsed);
        navSuppliersBtn.setCollapsed(sidebarCollapsed);
        navReportsBtn.setCollapsed(sidebarCollapsed);
        navUsersBtn.setCollapsed(sidebarCollapsed);

        profileTextCol.setVisible(!sidebarCollapsed);
        profileCardPanel.remove(profileAvatar);
        if (sidebarCollapsed) {
            profileCardPanel.setBorder(new EmptyBorder(8, 6, 8, 6));
            profileCardPanel.add(profileAvatar, BorderLayout.CENTER);
            profileCardPanel.setToolTipText(currentUser.getFullName() + " (" + LanguageManager.get("role." + currentUser.getRole().name()) + ")");
            logoutBtn.setText("");
            logoutBtn.setHorizontalAlignment(SwingConstants.CENTER);
            logoutBtn.setBorder(new EmptyBorder(8, 8, 8, 8));
            logoutBtn.setToolTipText(LanguageManager.get("nav.signOut"));
        } else {
            profileCardPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
            profileCardPanel.add(profileAvatar, BorderLayout.LINE_START);
            profileCardPanel.setToolTipText(null);
            logoutBtn.setText(LanguageManager.get("nav.signOut"));
            logoutBtn.setHorizontalAlignment(SwingConstants.LEADING);
            logoutBtn.setBorder(ThemeManager.createDirectionalBorder(8, 14, 8, 14, rtl));
            logoutBtn.setToolTipText(null);
        }

        sidebarPanel.revalidate();
        sidebarPanel.repaint();
    }

    private JPanel createHeaderPanel() {
        headerPanel = new JPanel(new BorderLayout(12, 0));
        headerPanel.setPreferredSize(new Dimension(0, 62));

        JPanel titleStack = new JPanel(new GridBagLayout());
        titleStack.setOpaque(false);
        titleStack.setBorder(new EmptyBorder(8, 20, 8, 20));
        GridBagConstraints hgc = new GridBagConstraints();
        hgc.gridx = 0;
        hgc.weightx = 1.0;
        hgc.fill = GridBagConstraints.HORIZONTAL;
        hgc.anchor = GridBagConstraints.LINE_START;

        viewTitleLabel = new JLabel();
        viewTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        viewTitleLabel.setHorizontalAlignment(SwingConstants.LEADING);
        hgc.gridy = 0;
        hgc.insets = new Insets(0, 0, 2, 0);
        titleStack.add(viewTitleLabel, hgc);

        breadcrumbLabel = new JLabel();
        breadcrumbLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        breadcrumbLabel.setHorizontalAlignment(SwingConstants.LEADING);
        hgc.gridy = 1;
        hgc.insets = new Insets(0, 0, 0, 0);
        titleStack.add(breadcrumbLabel, hgc);

        headerPanel.add(titleStack, BorderLayout.LINE_START);

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.TRAILING, 10, 15));
        rightControls.setOpaque(false);

        themeCombo = new JComboBox<>(ThemeMode.values());
        themeCombo.setPreferredSize(new Dimension(112, 30));
        themeCombo.setFocusable(false);
        themeCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ThemeMode mode) {
                    label.setText(getThemeModeLabel(mode));
                    Color iconColor = isSelected ? Color.WHITE : ThemeManager.textPrimary();
                    label.setIcon(ThemeManager.modeIcon(mode, 14, iconColor));
                    label.setIconTextGap(6);
                }
                label.setHorizontalAlignment(SwingConstants.LEADING);
                return label;
            }
        });
        themeCombo.addActionListener(e -> {
            if (!updatingControls && themeCombo.getSelectedItem() instanceof ThemeMode mode) {
                ThemeManager.setMode(mode);
            }
        });

        languageCombo = new JComboBox<>(Language.values());
        languageCombo.setPreferredSize(new Dimension(106, 30));
        languageCombo.setFocusable(false);
        languageCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Language lang) {
                    label.setText(lang.getDisplayName());
                    Color iconColor = isSelected ? Color.WHITE : ThemeManager.accentPrimary();
                    label.setIcon(IconFactory.globe(14, iconColor));
                    label.setIconTextGap(6);
                }
                label.setHorizontalAlignment(SwingConstants.LEADING);
                return label;
            }
        });
        languageCombo.addActionListener(e -> {
            if (!updatingControls && languageCombo.getSelectedItem() instanceof Language lang) {
                LanguageManager.setLanguage(lang);
            }
        });

        rightControls.add(themeCombo);
        rightControls.add(languageCombo);
        headerPanel.add(rightControls, BorderLayout.LINE_END);

        return headerPanel;
    }

    private JPanel createCenterCardsPanel() {
        cardLayout = new CardLayout();
        cardsPanel = new JPanel(cardLayout);

        suppliersPanel = new SuppliersPanel();
        reportsPanel = new ReportsPanel();
        usersPanel = new UsersPanel();

        cardsPanel.add(buildDashboardOverviewPanel(), CARD_DASHBOARD);
        cardsPanel.add(buildProductsPanel(), CARD_PRODUCTS);
        cardsPanel.add(buildTransactionsPanel(), CARD_TRANSACTIONS);
        cardsPanel.add(suppliersPanel, CARD_SUPPLIERS);
        cardsPanel.add(reportsPanel, CARD_REPORTS);
        cardsPanel.add(usersPanel, CARD_USERS);

        return cardsPanel;
    }

    private JPanel buildDashboardOverviewPanel() {
        JPanel panel = new JPanel(new BorderLayout(16, 16));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(18, 20, 18, 20));

        JPanel metricsGrid = new JPanel(new GridLayout(1, 4, 16, 0));
        metricsGrid.setOpaque(false);
        metricsGrid.setPreferredSize(new Dimension(0, 124));

        kpiTotalProductsVal = new JLabel("0");
        kpiTotalUnitsVal = new JLabel("0");
        kpiLowStockVal = new JLabel("0");
        kpiValuationVal = new JLabel("$0.00");

        productsKpiCard = new InteractiveKpiCard(kpiTotalProductsVal, () -> showCard(CARD_PRODUCTS));
        unitsKpiCard = new InteractiveKpiCard(kpiTotalUnitsVal, () -> showCard(CARD_PRODUCTS));
        lowStockKpiCard = new InteractiveKpiCard(kpiLowStockVal, () -> {
            productStatusFilterCombo.setSelectedItem("LOW_STOCK");
            showCard(CARD_PRODUCTS);
        });
        valuationKpiCard = new InteractiveKpiCard(kpiValuationVal, () -> showCard(CARD_REPORTS));

        metricsGrid.add(productsKpiCard);
        metricsGrid.add(unitsKpiCard);
        metricsGrid.add(lowStockKpiCard);
        metricsGrid.add(valuationKpiCard);
        panel.add(metricsGrid, BorderLayout.NORTH);

        JPanel splitGrid = new JPanel(new GridLayout(1, 2, 18, 0));
        splitGrid.setOpaque(false);

        // Left Card: Low Stock Watchlist (Table <-> Healthy Empty State)
        String[] watchCols = new String[6];
        watchlistTableModel = new DefaultTableModel(watchCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        watchlistTable = new JTable(watchlistTableModel);
        watchlistTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        watchlistTable.getTableHeader().setReorderingAllowed(false);
        watchlistTable.setDefaultRenderer(Object.class, new WatchlistCellRenderer());

        TableColumnModel wcm = watchlistTable.getColumnModel();
        wcm.getColumn(0).setPreferredWidth(44);
        wcm.getColumn(0).setMaxWidth(60);
        wcm.getColumn(1).setPreferredWidth(115);
        wcm.getColumn(2).setPreferredWidth(180);

        TableRowSorter<DefaultTableModel> watchSorter = new TableRowSorter<>(watchlistTableModel);
        watchSorter.setComparator(0, NUMERIC_COMPARATOR);
        watchSorter.setComparator(3, NUMERIC_COMPARATOR);
        watchSorter.setComparator(4, NUMERIC_COMPARATOR);
        watchSorter.setComparator(5, NUMERIC_COMPARATOR);
        watchlistTable.setRowSorter(watchSorter);

        quickRestockBtn = new JButton();
        quickRestockBtn.setIconTextGap(6);
        quickRestockBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));

        watchlistTitleLabel = new JLabel("", SwingConstants.LEADING);
        watchlistScroll = new JScrollPane(watchlistTable);
        watchlistEmptyPanel = buildHealthyWatchlistEmptyPanel();

        watchlistCardLayout = new CardLayout();
        watchlistContentPanel = new JPanel(watchlistCardLayout);
        watchlistContentPanel.setOpaque(false);
        watchlistContentPanel.add(watchlistScroll, WATCHLIST_TABLE);
        watchlistContentPanel.add(watchlistEmptyPanel, WATCHLIST_EMPTY);

        watchlistCard = createOverviewTableCard(watchlistTitleLabel, watchlistContentPanel, quickRestockBtn);
        splitGrid.add(watchlistCard);

        // Right Card: Recent Movements
        String[] recentCols = new String[4];
        recentMovementsTableModel = new DefaultTableModel(recentCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        recentTable = new JTable(recentMovementsTableModel);
        recentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        recentTable.getTableHeader().setReorderingAllowed(false);
        recentTable.setDefaultRenderer(Object.class, new RecentMovementRenderer());
        recentTable.getColumnModel().getColumn(2).setCellRenderer(new MovementTypeBadgeRenderer());

        TableRowSorter<DefaultTableModel> recentSorter = new TableRowSorter<>(recentMovementsTableModel);
        recentSorter.setComparator(3, NUMERIC_COMPARATOR);
        recentTable.setRowSorter(recentSorter);

        recentTitleLabel = new JLabel("", SwingConstants.LEADING);
        recentScroll = new JScrollPane(recentTable);
        recentCard = createOverviewTableCard(recentTitleLabel, recentScroll, null);
        splitGrid.add(recentCard);

        panel.add(splitGrid, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildHealthyWatchlistEmptyPanel() {
        JPanel empty = new JPanel(new GridBagLayout());
        empty.setOpaque(true);

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);

        watchlistEmptyIcon = new JLabel();
        watchlistEmptyIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        watchlistEmptyTitle = new JLabel("", SwingConstants.CENTER);
        watchlistEmptyTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        watchlistEmptyTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        watchlistEmptySub = new JLabel("", SwingConstants.CENTER);
        watchlistEmptySub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        watchlistEmptySub.setAlignmentX(Component.CENTER_ALIGNMENT);

        inner.add(watchlistEmptyIcon);
        inner.add(Box.createVerticalStrut(10));
        inner.add(watchlistEmptyTitle);
        inner.add(Box.createVerticalStrut(4));
        inner.add(watchlistEmptySub);
        empty.add(inner);
        return empty;
    }

    private JPanel createOverviewTableCard(JLabel titleLbl, JComponent content, JComponent headerAction) {
        JPanel card = new JPanel(new BorderLayout(0, 12));

        JPanel headerRow = new JPanel(new BorderLayout(8, 0));
        headerRow.setOpaque(false);

        titleLbl.setIconTextGap(8);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        headerRow.add(titleLbl, BorderLayout.LINE_START);

        if (headerAction != null) {
            headerRow.add(headerAction, BorderLayout.LINE_END);
        }
        card.add(headerRow, BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);

        return card;
    }

    private JPanel buildProductsPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(18, 20, 18, 20));

        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setOpaque(false);

        JPanel filterBox = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
        filterBox.setOpaque(false);

        productSearchField = new JTextField(20);
        productSearchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        productSearchField.setPreferredSize(new Dimension(250, 36));
        productSearchField.putClientProperty("JTextField.showClearButton", true);

        productStatusFilterCombo = new JComboBox<>(new String[]{"ALL", "IN_STOCK", "LOW_STOCK"});
        productStatusFilterCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        productStatusFilterCombo.setPreferredSize(new Dimension(150, 36));
        productStatusFilterCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null) {
                    String key = value.toString();
                    String text = switch (key) {
                        case "IN_STOCK" -> LanguageManager.get("products.filter.inStock");
                        case "LOW_STOCK" -> LanguageManager.get("products.filter.lowStock");
                        default -> LanguageManager.get("products.filter.all");
                    };
                    label.setText(text);
                }
                label.setHorizontalAlignment(SwingConstants.LEADING);
                return label;
            }
        });

        filterBox.add(productSearchField);
        filterBox.add(productStatusFilterCombo);
        toolbar.add(filterBox, BorderLayout.LINE_START);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
        actions.setOpaque(false);

        refreshProductsBtn = new JButton();
        refreshProductsBtn.setIconTextGap(6);

        addProductBtn = new JButton();
        addProductBtn.setIconTextGap(6);
        addProductBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));

        editProductBtn = new JButton();
        editProductBtn.setIconTextGap(6);

        deleteProductBtn = new JButton();
        deleteProductBtn.setIconTextGap(6);

        exportProductsBtn = new JButton();
        exportProductsBtn.setIconTextGap(6);

        manageCategoriesBtn = new JButton();
        manageCategoriesBtn.setIconTextGap(6);

        actions.add(refreshProductsBtn);
        actions.add(manageCategoriesBtn);
        actions.add(exportProductsBtn);
        actions.add(addProductBtn);
        actions.add(editProductBtn);
        actions.add(deleteProductBtn);
        toolbar.add(actions, BorderLayout.LINE_END);

        panel.add(toolbar, BorderLayout.NORTH);

        // 11-column Product Table (including Profit Margin at col 7, Status at col 10)
        String[] columns = new String[11];
        productTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        productTable = new JTable(productTableModel);
        productTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        productTable.getTableHeader().setReorderingAllowed(false);
        productTable.setDefaultRenderer(Object.class, new ProductTableCellRenderer());
        productTable.getColumnModel().getColumn(10).setCellRenderer(new StatusBadgeRenderer());

        TableRowSorter<DefaultTableModel> productSorter = new TableRowSorter<>(productTableModel);
        productSorter.setComparator(0, NUMERIC_COMPARATOR);
        productSorter.setComparator(5, NUMERIC_COMPARATOR);
        productSorter.setComparator(6, NUMERIC_COMPARATOR);
        productSorter.setComparator(7, NUMERIC_COMPARATOR);
        productSorter.setComparator(8, NUMERIC_COMPARATOR);
        productSorter.setComparator(9, NUMERIC_COMPARATOR);
        productTable.setRowSorter(productSorter);

        TableColumnModel cm = productTable.getColumnModel();
        cm.getColumn(0).setPreferredWidth(48);
        cm.getColumn(0).setMaxWidth(65);
        cm.getColumn(1).setPreferredWidth(115);
        cm.getColumn(2).setPreferredWidth(205);
        cm.getColumn(3).setPreferredWidth(130);
        cm.getColumn(4).setPreferredWidth(140);
        cm.getColumn(5).setPreferredWidth(85);
        cm.getColumn(6).setPreferredWidth(85);
        cm.getColumn(7).setPreferredWidth(75);
        cm.getColumn(8).setPreferredWidth(80);
        cm.getColumn(9).setPreferredWidth(88);
        cm.getColumn(10).setPreferredWidth(105);

        productTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateProductSelectionState();
            }
        });

        buildProductContextMenu();

        productScroll = new JScrollPane(productTable);
        panel.add(productScroll, BorderLayout.CENTER);

        // Compact Summary Footer Strip
        productSummaryBar = new JPanel(new BorderLayout());
        productSummaryBar.setPreferredSize(new Dimension(0, 32));
        productSummaryLabel = new JLabel("", SwingConstants.LEADING);
        productSummaryLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        productSummaryBar.add(productSummaryLabel, BorderLayout.LINE_START);
        panel.add(productSummaryBar, BorderLayout.SOUTH);

        return panel;
    }

    private void buildProductContextMenu() {
        productContextMenu = new JPopupMenu();
        ctxEditProductItem = new JMenuItem();
        ctxEditProductItem.setIconTextGap(8);
        ctxStockInItem = new JMenuItem();
        ctxStockInItem.setIconTextGap(8);
        ctxStockOutItem = new JMenuItem();
        ctxStockOutItem.setIconTextGap(8);
        ctxCopySkuItem = new JMenuItem();
        ctxCopySkuItem.setIconTextGap(8);
        ctxDeleteProductItem = new JMenuItem();
        ctxDeleteProductItem.setIconTextGap(8);

        productContextMenu.add(ctxEditProductItem);
        productContextMenu.add(ctxStockInItem);
        productContextMenu.add(ctxStockOutItem);
        productContextMenu.addSeparator();
        productContextMenu.add(ctxCopySkuItem);
        productContextMenu.addSeparator();
        productContextMenu.add(ctxDeleteProductItem);

        MouseAdapter contextListener = new MouseAdapter() {
            private void maybeShowPopup(MouseEvent e) {
                if (!e.isPopupTrigger()) {
                    return;
                }
                int row = productTable.rowAtPoint(e.getPoint());
                if (row >= 0 && row < productTable.getRowCount()) {
                    productTable.setRowSelectionInterval(row, row);
                    ctxEditProductItem.setVisible(canEditProducts);
                    ctxDeleteProductItem.setVisible(canEditProducts);
                    ctxDeleteProductItem.setEnabled(canDeleteProducts);
                    LanguageManager.applyOrientation(productContextMenu);
                    productContextMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }
        };
        productTable.addMouseListener(contextListener);
    }

    private JPanel buildTransactionsPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(18, 20, 18, 20));

        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setOpaque(false);

        JPanel filterBox = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
        filterBox.setOpaque(false);

        transactionSearchField = new JTextField(16);
        transactionSearchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        transactionSearchField.setPreferredSize(new Dimension(210, 36));
        transactionSearchField.putClientProperty("JTextField.showClearButton", true);

        transactionTypeFilterCombo = new JComboBox<>(new String[]{
                "ALL",
                "INBOUND_PURCHASE",
                "RETURN_RESTOCK",
                "OUTBOUND_SALE",
                "ADJUSTMENT_DAMAGE",
                "ADJUSTMENT_COUNT"
        });
        transactionTypeFilterCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        transactionTypeFilterCombo.setPreferredSize(new Dimension(165, 36));
        transactionTypeFilterCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null) {
                    label.setText(formatMovementFilterLabel(value.toString()));
                }
                label.setHorizontalAlignment(SwingConstants.LEADING);
                return label;
            }
        });

        transactionTimeframeFilterCombo = new JComboBox<>(new String[]{"ALL", "TODAY", "DAYS_7", "DAYS_30"});
        transactionTimeframeFilterCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        transactionTimeframeFilterCombo.setPreferredSize(new Dimension(145, 36));
        transactionTimeframeFilterCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null) {
                    String key = value.toString();
                    String text = switch (key) {
                        case "TODAY" -> LanguageManager.get("transactions.time.today");
                        case "DAYS_7" -> LanguageManager.get("transactions.time.days7");
                        case "DAYS_30" -> LanguageManager.get("transactions.time.days30");
                        default -> LanguageManager.get("transactions.time.all");
                    };
                    label.setText(text);
                    Color iconColor = isSelected ? Color.WHITE : ThemeManager.textSecondary();
                    label.setIcon(IconFactory.calendar(14, iconColor));
                    label.setIconTextGap(6);
                }
                label.setHorizontalAlignment(SwingConstants.LEADING);
                return label;
            }
        });

        filterBox.add(transactionSearchField);
        filterBox.add(transactionTypeFilterCombo);
        filterBox.add(transactionTimeframeFilterCombo);
        toolbar.add(filterBox, BorderLayout.LINE_START);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
        actions.setOpaque(false);

        stockInBtn = new JButton();
        stockInBtn.setIconTextGap(6);
        stockInBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));

        stockOutBtn = new JButton();
        stockOutBtn.setIconTextGap(6);
        stockOutBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));

        refreshTransactionsBtn = new JButton();
        refreshTransactionsBtn.setIconTextGap(6);

        exportTransactionsBtn = new JButton();
        exportTransactionsBtn.setIconTextGap(6);

        actions.add(stockInBtn);
        actions.add(stockOutBtn);
        actions.add(refreshTransactionsBtn);
        actions.add(exportTransactionsBtn);
        toolbar.add(actions, BorderLayout.LINE_END);

        panel.add(toolbar, BorderLayout.NORTH);

        String[] columns = new String[7];
        transactionTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        transactionTable = new JTable(transactionTableModel);
        transactionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        transactionTable.getTableHeader().setReorderingAllowed(false);
        transactionTable.setDefaultRenderer(Object.class, new TransactionTableCellRenderer());
        transactionTable.getColumnModel().getColumn(2).setCellRenderer(new MovementTypeBadgeRenderer());

        TableRowSorter<DefaultTableModel> txSorter = new TableRowSorter<>(transactionTableModel);
        txSorter.setComparator(0, NUMERIC_COMPARATOR);
        txSorter.setComparator(3, NUMERIC_COMPARATOR);
        transactionTable.setRowSorter(txSorter);

        TableColumnModel cm = transactionTable.getColumnModel();
        cm.getColumn(0).setPreferredWidth(50);
        cm.getColumn(0).setMaxWidth(70);
        cm.getColumn(1).setPreferredWidth(235);
        cm.getColumn(2).setPreferredWidth(155);
        cm.getColumn(3).setPreferredWidth(100);
        cm.getColumn(4).setPreferredWidth(205);
        cm.getColumn(5).setPreferredWidth(165);
        cm.getColumn(6).setPreferredWidth(150);

        transactionScroll = new JScrollPane(transactionTable);
        panel.add(transactionScroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStatusBarPanel() {
        statusBarPanel = new JPanel(new BorderLayout(15, 0));
        statusBarPanel.setPreferredSize(new Dimension(0, 32));

        dbStatusLabel = new JLabel("", SwingConstants.LEADING);
        dbStatusLabel.setIconTextGap(6);
        dbStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        JPanel centerStatus = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        centerStatus.setOpaque(false);

        statusProgressBar = new JProgressBar();
        statusProgressBar.setIndeterminate(true);
        statusProgressBar.setVisible(false);
        statusProgressBar.setPreferredSize(new Dimension(110, 8));

        statusMessageLabel = new JLabel("", SwingConstants.CENTER);
        statusMessageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        centerStatus.add(statusProgressBar);
        centerStatus.add(statusMessageLabel);

        clockLabel = new JLabel(" ", SwingConstants.TRAILING);
        clockLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));

        statusBarPanel.add(dbStatusLabel, BorderLayout.LINE_START);
        statusBarPanel.add(centerStatus, BorderLayout.CENTER);
        statusBarPanel.add(clockLabel, BorderLayout.LINE_END);

        return statusBarPanel;
    }

    private void startClockTimer() {
        clockTimer = new Timer(1000, e ->
                clockLabel.setText(LanguageManager.isolateLTR(LocalDateTime.now().format(CLOCK_FORMAT))));
        clockTimer.setInitialDelay(0);
        clockTimer.start();
    }

    private void registerSearchShortcut() {
        getRootPane().registerKeyboardAction(
                e -> focusActiveSearchField(),
                KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    private void focusActiveSearchField() {
        switch (currentCardKey) {
            case CARD_PRODUCTS -> {
                productSearchField.requestFocusInWindow();
                productSearchField.selectAll();
            }
            case CARD_TRANSACTIONS -> {
                transactionSearchField.requestFocusInWindow();
                transactionSearchField.selectAll();
            }
            case CARD_SUPPLIERS -> {
                suppliersPanel.getSearchField().requestFocusInWindow();
                suppliersPanel.getSearchField().selectAll();
            }
            case CARD_USERS -> {
                usersPanel.getSearchField().requestFocusInWindow();
                usersPanel.getSearchField().selectAll();
            }
            default -> {
                navProductsBtn.doClick();
                SwingUtilities.invokeLater(() -> {
                    productSearchField.requestFocusInWindow();
                    productSearchField.selectAll();
                });
            }
        }
    }

    public void registerRefreshShortcut(Runnable refreshAction) {
        if (refreshAction == null) return;
        getRootPane().registerKeyboardAction(
                e -> refreshAction.run(),
                KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    // =========================================================================
    // Smart Selection & Role Permission Controls
    // =========================================================================

    public void setProductRolePermissions(boolean canEdit, boolean canDelete) {
        this.canEditProducts = canEdit;
        this.canDeleteProducts = canDelete;
        updateProductSelectionState();
    }

    private void updateProductSelectionState() {
        boolean hasSelection = productTable.getSelectedRow() != -1;
        boolean editEnabled = canEditProducts && hasSelection;
        boolean deleteEnabled = canDeleteProducts && hasSelection;

        editProductBtn.setEnabled(editEnabled);
        Color editFg = editEnabled ? ThemeManager.btnSecondaryFg() : ThemeManager.textMuted();
        editProductBtn.setForeground(editFg);
        editProductBtn.setIcon(IconFactory.edit(15, editFg));

        deleteProductBtn.setEnabled(deleteEnabled);
        Color deleteFg = deleteEnabled ? ThemeManager.accentDanger() : ThemeManager.textMuted();
        deleteProductBtn.setForeground(deleteFg);
        deleteProductBtn.setIcon(IconFactory.delete(15, deleteFg));
    }

    // =========================================================================
    // Live Theme & Bilingual RTL Synchronization
    // =========================================================================

    public void refreshThemeAndLanguage() {
        boolean rtl = LanguageManager.isRTL();
        updatingControls = true;
        try {
            setTitle(LanguageManager.get("app.title"));
            cardsPanel.setBackground(ThemeManager.bgCanvas());

            // Sidebar
            sidebarPanel.setBackground(ThemeManager.bgSidebar());
            brandIconLabel.setIcon(IconFactory.brandLogo(24, ThemeManager.accentPrimary()));
            brandLogoLabel.setText(LanguageManager.get("app.brand"));
            brandSubLabel.setText(LanguageManager.get("app.subtitle.short"));
            brandSubLabel.setForeground(ThemeManager.sidebarTextMuted());

            sidebarToggleBtn.setIcon(IconFactory.sidebarToggle(17, ThemeManager.sidebarText()));
            sidebarToggleBtn.setToolTipText(LanguageManager.get("nav.toggleTooltip"));

            operationsSectionLabel.setText(LanguageManager.get("nav.section.operations"));
            operationsSectionLabel.setForeground(ThemeManager.sidebarTextMuted());
            operationsSectionLabel.setBorder(ThemeManager.createDirectionalBorder(0, 14, 0, 14, rtl));

            adminSectionLabel.setText(LanguageManager.get("nav.section.admin"));
            adminSectionLabel.setForeground(ThemeManager.sidebarTextMuted());
            adminSectionLabel.setBorder(ThemeManager.createDirectionalBorder(0, 14, 0, 14, rtl));

            Color activeIcon = ThemeManager.sidebarTextActive();
            Color inactiveIcon = ThemeManager.sidebarTextMuted();

            navDashboardBtn.updateAppearance(LanguageManager.get("nav.dashboard"),
                    IconFactory.dashboard(17, activeIcon), IconFactory.dashboard(17, inactiveIcon), rtl);
            navProductsBtn.updateAppearance(LanguageManager.get("nav.products"),
                    IconFactory.products(17, activeIcon), IconFactory.products(17, inactiveIcon), rtl);
            navTransactionsBtn.updateAppearance(LanguageManager.get("nav.transactions"),
                    IconFactory.transactions(17, activeIcon), IconFactory.transactions(17, inactiveIcon), rtl);
            navSuppliersBtn.updateAppearance(LanguageManager.get("nav.suppliers"),
                    IconFactory.suppliers(17, activeIcon), IconFactory.suppliers(17, inactiveIcon), rtl);
            navReportsBtn.updateAppearance(LanguageManager.get("nav.reports"),
                    IconFactory.reports(17, activeIcon), IconFactory.reports(17, inactiveIcon), rtl);
            navUsersBtn.updateAppearance(LanguageManager.get("nav.users"),
                    IconFactory.users(17, activeIcon), IconFactory.users(17, inactiveIcon), rtl);

            sidebarDivider.setForeground(ThemeManager.sidebarDivider());
            sidebarDivider.setBackground(ThemeManager.sidebarDivider());

            profileCardPanel.setBackground(ThemeManager.sidebarHover());
            profileAvatar.setFillColor(ThemeManager.accentPrimary());
            profileNameLabel.setForeground(ThemeManager.sidebarTextActive());
            BadgeColors roleColors = switch (currentUser.getRole()) {
                case ADMIN -> ThemeManager.badgeWarning();
                case MANAGER -> ThemeManager.badgePurple();
                case STAFF -> ThemeManager.badgeSuccess();
            };
            profileRolePill.setText(LanguageManager.get("role." + currentUser.getRole().name()));
            profileRolePill.setBackground(roleColors.background());
            profileRolePill.setForeground(roleColors.foreground());

            logoutBtn.setIcon(IconFactory.signOut(16, ThemeManager.accentDanger()));
            logoutBtn.setForeground(ThemeManager.sidebarText());
            logoutBtn.setBackground(ThemeManager.sidebarHover());

            applySidebarCollapsedState();

            // Header Breadcrumbs & Controls
            headerPanel.setBackground(ThemeManager.bgHeader());
            headerPanel.setBorder(new MatteBorder(0, 0, 1, 0, ThemeManager.borderSubtle()));
            viewTitleLabel.setForeground(ThemeManager.textPrimary());
            breadcrumbLabel.setForeground(ThemeManager.textSecondary());
            updateViewTitle();

            themeCombo.setBackground(ThemeManager.bgSurface());
            themeCombo.setForeground(ThemeManager.textPrimary());
            languageCombo.setBackground(ThemeManager.bgSurface());
            languageCombo.setForeground(ThemeManager.textPrimary());
            themeCombo.setSelectedItem(ThemeManager.getMode());
            languageCombo.setSelectedItem(LanguageManager.getLanguage());

            // Dashboard KPI Cards (Semantic Icons & Subtitles)
            productsKpiCard.refreshStyle(LanguageManager.get("dashboard.kpi.products"),
                    ThemeManager.accentPrimary(), ThemeManager.badgeInfo().background(),
                    IconFactory.products(20, ThemeManager.accentPrimary()));
            unitsKpiCard.refreshStyle(LanguageManager.get("dashboard.kpi.units"),
                    ThemeManager.accentSecondary(), ThemeManager.badgePurple().background(),
                    IconFactory.warehouseBoxes(20, ThemeManager.accentSecondary()));
            lowStockKpiCard.refreshStyle(LanguageManager.get("dashboard.kpi.lowStock"),
                    ThemeManager.accentDanger(), ThemeManager.badgeDanger().background(),
                    IconFactory.alert(20, ThemeManager.accentDanger()));
            valuationKpiCard.refreshStyle(LanguageManager.get("dashboard.kpi.value"),
                    ThemeManager.accentSuccess(), ThemeManager.badgeSuccess().background(),
                    IconFactory.dollarWallet(20, ThemeManager.accentSuccess()));
            setMetrics(lastTotalProducts, lastTotalCategories, lastTotalUnits, lastLowStockCount,
                    lastCostValuation, lastRetailValuation);

            // Watchlist Card & Healthy Empty State
            styleOverviewCard(watchlistCard, watchlistTitleLabel, watchlistScroll, watchlistTable,
                    LanguageManager.get("dashboard.watchlist.title"),
                    IconFactory.alert(16, ThemeManager.accentDanger()));
            watchlistEmptyPanel.setBackground(ThemeManager.bgSurfaceAlt());
            watchlistEmptyPanel.setBorder(new MatteBorder(1, 1, 1, 1, ThemeManager.borderSubtle()));
            watchlistEmptyIcon.setIcon(IconFactory.checkShield(38, ThemeManager.accentSuccess()));
            watchlistEmptyTitle.setText(LanguageManager.get("dashboard.watchlist.emptyTitle"));
            watchlistEmptyTitle.setForeground(ThemeManager.textPrimary());
            watchlistEmptySub.setText(LanguageManager.get("dashboard.watchlist.emptySub"));
            watchlistEmptySub.setForeground(ThemeManager.textSecondary());

            boolean emptyWatchlist = lastLowStockProducts.isEmpty();
            watchlistCardLayout.show(watchlistContentPanel, emptyWatchlist ? WATCHLIST_EMPTY : WATCHLIST_TABLE);

            quickRestockBtn.setText(LanguageManager.get("dashboard.quickRestock"));
            quickRestockBtn.setIcon(IconFactory.stockIn(14, Color.WHITE));
            quickRestockBtn.setBackground(ThemeManager.accentSuccess());
            quickRestockBtn.setForeground(Color.WHITE);
            quickRestockBtn.setToolTipText(LanguageManager.get("dashboard.watchlist.quickRestockTooltip"));
            quickRestockBtn.setEnabled(!emptyWatchlist);

            styleOverviewCard(recentCard, recentTitleLabel, recentScroll, recentTable,
                    LanguageManager.get("dashboard.recent.title"),
                    IconFactory.transactions(16, ThemeManager.accentPrimary()));

            setTableHeaders(watchlistTable, new String[]{
                    LanguageManager.get("common.id"),
                    LanguageManager.get("products.col.sku"),
                    LanguageManager.get("products.col.name"),
                    LanguageManager.get("dashboard.watchlist.col.stock"),
                    LanguageManager.get("dashboard.watchlist.col.min"),
                    LanguageManager.get("dashboard.watchlist.col.deficit")
            });
            setTableHeaders(recentTable, new String[]{
                    LanguageManager.get("dashboard.recent.col.timestamp"),
                    LanguageManager.get("dashboard.recent.col.product"),
                    LanguageManager.get("dashboard.recent.col.type"),
                    LanguageManager.get("dashboard.recent.col.delta")
            });

            // Products Toolbar, Table, Context Menu & Summary Bar
            productSearchField.putClientProperty("JTextField.leadingIcon", IconFactory.search(15, ThemeManager.textSecondary()));
            productSearchField.putClientProperty("JTextField.placeholderText", LanguageManager.get("products.searchPlaceholder"));
            productSearchField.setBackground(ThemeManager.bgSurface());
            productSearchField.setForeground(ThemeManager.textPrimary());
            productStatusFilterCombo.setBackground(ThemeManager.bgSurface());
            productStatusFilterCombo.setForeground(ThemeManager.textPrimary());

            styleSecondaryButton(refreshProductsBtn, LanguageManager.get("common.refresh"),
                    IconFactory.refresh(15, ThemeManager.btnSecondaryFg()));
            styleSecondaryButton(manageCategoriesBtn, LanguageManager.get("products.categories"),
                    IconFactory.categories(15, ThemeManager.btnSecondaryFg()));
            styleSecondaryButton(exportProductsBtn, LanguageManager.get("export.button"),
                    IconFactory.exportDropdown(15, ThemeManager.btnSecondaryFg()));
            addProductBtn.setText(LanguageManager.get("products.add"));
            addProductBtn.setBackground(ThemeManager.accentPrimary());
            addProductBtn.setForeground(Color.WHITE);
            addProductBtn.setIcon(IconFactory.add(15, Color.WHITE));

            editProductBtn.setText(LanguageManager.get("common.edit"));
            editProductBtn.setBackground(ThemeManager.btnSecondaryBg());
            deleteProductBtn.setText(LanguageManager.get("common.delete"));
            deleteProductBtn.setBackground(ThemeManager.btnSecondaryBg());
            updateProductSelectionState();

            ctxEditProductItem.setText(LanguageManager.get("products.ctx.edit"));
            ctxEditProductItem.setIcon(IconFactory.edit(14, ThemeManager.textPrimary()));
            ctxStockInItem.setText(LanguageManager.get("products.ctx.stockIn"));
            ctxStockInItem.setIcon(IconFactory.stockIn(14, ThemeManager.accentSuccess()));
            ctxStockOutItem.setText(LanguageManager.get("products.ctx.stockOut"));
            ctxStockOutItem.setIcon(IconFactory.stockOut(14, ThemeManager.accentDanger()));
            ctxCopySkuItem.setText(LanguageManager.get("products.ctx.copySku"));
            ctxCopySkuItem.setIcon(IconFactory.copy(14, ThemeManager.textSecondary()));
            ctxDeleteProductItem.setText(LanguageManager.get("products.ctx.delete"));
            ctxDeleteProductItem.setIcon(IconFactory.delete(14, ThemeManager.accentDanger()));

            setTableHeaders(productTable, new String[]{
                    LanguageManager.get("common.id"),
                    LanguageManager.get("products.col.sku"),
                    LanguageManager.get("products.col.name"),
                    LanguageManager.get("products.col.category"),
                    LanguageManager.get("products.col.supplier"),
                    LanguageManager.get("products.col.cost"),
                    LanguageManager.get("products.col.selling"),
                    LanguageManager.get("products.col.margin"),
                    LanguageManager.get("products.col.stock"),
                    LanguageManager.get("products.col.minThreshold"),
                    LanguageManager.get("products.col.status")
            });
            styleTablePane(productScroll, productTable);

            productSummaryBar.setBackground(ThemeManager.bgSurfaceAlt());
            productSummaryBar.setBorder(BorderFactory.createCompoundBorder(
                    new MatteBorder(1, 1, 1, 1, ThemeManager.borderSubtle()),
                    new EmptyBorder(0, 14, 0, 14)
            ));
            productSummaryLabel.setForeground(ThemeManager.textSecondary());
            updateProductSummaryText();

            // Transactions Toolbar & Table
            transactionSearchField.putClientProperty("JTextField.leadingIcon", IconFactory.search(15, ThemeManager.textSecondary()));
            transactionSearchField.putClientProperty("JTextField.placeholderText", LanguageManager.get("transactions.searchPlaceholder"));
            transactionSearchField.setBackground(ThemeManager.bgSurface());
            transactionSearchField.setForeground(ThemeManager.textPrimary());
            transactionTypeFilterCombo.setBackground(ThemeManager.bgSurface());
            transactionTypeFilterCombo.setForeground(ThemeManager.textPrimary());
            transactionTimeframeFilterCombo.setBackground(ThemeManager.bgSurface());
            transactionTimeframeFilterCombo.setForeground(ThemeManager.textPrimary());

            stockInBtn.setText(LanguageManager.get("transactions.stockIn"));
            stockInBtn.setIcon(IconFactory.stockIn(15, Color.WHITE));
            stockInBtn.setBackground(ThemeManager.accentSuccess());
            stockInBtn.setForeground(Color.WHITE);
            stockInBtn.setToolTipText(LanguageManager.get("transactions.stockInTooltip"));

            stockOutBtn.setText(LanguageManager.get("transactions.stockOut"));
            stockOutBtn.setIcon(IconFactory.stockOut(15, Color.WHITE));
            stockOutBtn.setBackground(ThemeManager.accentDanger());
            stockOutBtn.setForeground(Color.WHITE);
            stockOutBtn.setToolTipText(LanguageManager.get("transactions.stockOutTooltip"));

            styleSecondaryButton(refreshTransactionsBtn, LanguageManager.get("common.refresh"),
                    IconFactory.refresh(15, ThemeManager.btnSecondaryFg()));
            styleSecondaryButton(exportTransactionsBtn, LanguageManager.get("export.button"),
                    IconFactory.exportDropdown(15, ThemeManager.btnSecondaryFg()));

            setTableHeaders(transactionTable, new String[]{
                    LanguageManager.get("common.id"),
                    LanguageManager.get("transactions.col.product"),
                    LanguageManager.get("transactions.col.type"),
                    LanguageManager.get("transactions.col.delta"),
                    LanguageManager.get("transactions.col.notes"),
                    LanguageManager.get("transactions.col.user"),
                    LanguageManager.get("transactions.col.timestamp")
            });
            styleTablePane(transactionScroll, transactionTable);

            // Sub-Panels
            suppliersPanel.refreshThemeAndLanguage();
            reportsPanel.refreshThemeAndLanguage();
            usersPanel.refreshThemeAndLanguage();

            // Status Bar
            statusBarPanel.setBackground(ThemeManager.statusBarBg());
            statusBarPanel.setBorder(new MatteBorder(1, 0, 0, 0, ThemeManager.borderSubtle()));
            dbStatusLabel.setText(LanguageManager.get("db.online"));
            dbStatusLabel.setIcon(IconFactory.statusDot(10, ThemeManager.accentSuccess()));
            dbStatusLabel.setForeground(ThemeManager.accentSuccess());
            dbStatusLabel.setBorder(ThemeManager.createDirectionalBorder(0, 14, 0, 0, rtl));
            statusMessageLabel.setForeground(ThemeManager.textSecondary());
            if (!statusProgressBar.isVisible() && (statusMessageLabel.getText() == null || statusMessageLabel.getText().isBlank())) {
                statusMessageLabel.setText(LanguageManager.get("status.ready"));
            }
            clockLabel.setForeground(ThemeManager.textSecondary());
            clockLabel.setBorder(ThemeManager.createDirectionalBorder(0, 0, 0, 14, rtl));
            clockLabel.setText(LanguageManager.isolateLTR(LocalDateTime.now().format(CLOCK_FORMAT)));

            repopulateCachedSnapshots();
        } finally {
            updatingControls = false;
        }

        LanguageManager.applyOrientation(this);
        themeCombo.repaint();
        languageCombo.repaint();
        productStatusFilterCombo.repaint();
        transactionTypeFilterCombo.repaint();
        transactionTimeframeFilterCombo.repaint();
        repaint();
    }

    private void repopulateCachedSnapshots() {
        if (!lastLowStockProducts.isEmpty() || !lastRecentTransactions.isEmpty()) {
            populateDashboardWatchlists(lastLowStockProducts, lastRecentTransactions, lastRecentProductMap);
        }
        if (!lastCatalogProducts.isEmpty()) {
            populateProductTable(lastCatalogProducts, lastCategoryNames, lastSupplierNames, lastTotalCatalogCount);
        }
        if (!lastTransactions.isEmpty()) {
            populateTransactionTable(lastTransactions, lastTxProductMap, lastTxUserMap);
        }
    }

    private void styleOverviewCard(JPanel card, JLabel titleLbl, JScrollPane scroll, JTable table, String text, Icon icon) {
        card.setBackground(ThemeManager.bgSurface());
        card.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 1, 1, 1, ThemeManager.borderSubtle()),
                new EmptyBorder(16, 16, 16, 16)
        ));
        titleLbl.setText(text);
        titleLbl.setIcon(icon);
        titleLbl.setForeground(ThemeManager.textPrimary());
        styleTablePane(scroll, table);
    }

    private void styleTablePane(JScrollPane scroll, JTable table) {
        scroll.setBorder(new MatteBorder(1, 1, 1, 1, ThemeManager.borderSubtle()));
        scroll.getViewport().setBackground(ThemeManager.bgSurface());
        table.setBackground(ThemeManager.bgSurface());
        table.setForeground(ThemeManager.textPrimary());
        table.setGridColor(ThemeManager.borderSubtle());
    }

    private void styleSecondaryButton(JButton btn, String text, Icon icon) {
        btn.setText(text);
        btn.setIcon(icon);
        btn.setBackground(ThemeManager.btnSecondaryBg());
        btn.setForeground(ThemeManager.btnSecondaryFg());
    }

    private void setTableHeaders(JTable table, String[] headers) {
        TableColumnModel cm = table.getColumnModel();
        for (int i = 0; i < headers.length && i < cm.getColumnCount(); i++) {
            cm.getColumn(i).setHeaderValue(headers[i]);
        }
        if (table.getTableHeader() != null) {
            table.getTableHeader().repaint();
        }
    }

    private String getThemeModeLabel(ThemeMode mode) {
        return switch (mode) {
            case LIGHT -> LanguageManager.get("theme.light");
            case DARK -> LanguageManager.get("theme.dark");
            case SYSTEM -> LanguageManager.get("theme.system");
        };
    }

    private static String extractInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "NX";
        }
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase(Locale.ROOT);
        }
        String single = parts[0].toUpperCase(Locale.ROOT);
        return single.length() >= 2 ? single.substring(0, 2) : single;
    }

    // =========================================================================
    // Public View API & Controller Delegation Methods
    // =========================================================================

    public void showCard(String cardKey) {
        this.currentCardKey = cardKey;
        cardLayout.show(cardsPanel, cardKey);
        updateActiveNavStyle(cardKey);
        updateViewTitle();
    }

    private void updateViewTitle() {
        String root = LanguageManager.get("breadcrumb.root");
        String section = CARD_USERS.equals(currentCardKey)
                ? LanguageManager.get("breadcrumb.admin")
                : LanguageManager.get("breadcrumb.operations");

        String title;
        String leaf;
        switch (currentCardKey) {
            case CARD_PRODUCTS -> {
                title = LanguageManager.get("nav.title.products");
                leaf = LanguageManager.get("nav.products");
            }
            case CARD_TRANSACTIONS -> {
                title = LanguageManager.get("nav.title.transactions");
                leaf = LanguageManager.get("nav.transactions");
            }
            case CARD_SUPPLIERS -> {
                title = LanguageManager.get("nav.title.suppliers");
                leaf = LanguageManager.get("nav.suppliers");
            }
            case CARD_REPORTS -> {
                title = LanguageManager.get("nav.title.reports");
                leaf = LanguageManager.get("nav.reports");
            }
            case CARD_USERS -> {
                title = LanguageManager.get("nav.title.users");
                leaf = LanguageManager.get("nav.users");
            }
            default -> {
                title = LanguageManager.get("nav.title.dashboard");
                leaf = LanguageManager.get("nav.dashboard");
            }
        }
        viewTitleLabel.setText(title);
        breadcrumbLabel.setText(root + "  /  " + section + "  /  " + leaf);
    }

    private void updateActiveNavStyle(String activeKey) {
        navDashboardBtn.setActive(activeKey.equals(CARD_DASHBOARD));
        navProductsBtn.setActive(activeKey.equals(CARD_PRODUCTS));
        navTransactionsBtn.setActive(activeKey.equals(CARD_TRANSACTIONS));
        navSuppliersBtn.setActive(activeKey.equals(CARD_SUPPLIERS));
        navReportsBtn.setActive(activeKey.equals(CARD_REPORTS));
        navUsersBtn.setActive(activeKey.equals(CARD_USERS));
    }

    public void setMetrics(int totalProducts, int lowStockCount, BigDecimal valuation) {
        setMetrics(totalProducts, 0, totalProducts == 0 ? 0 : lastTotalUnits, lowStockCount, valuation, valuation);
    }

    public void setMetrics(int totalProducts, int totalUnits, int lowStockCount, BigDecimal valuation) {
        setMetrics(totalProducts, lastTotalCategories, totalUnits, lowStockCount, valuation, valuation);
    }

    public void setMetrics(int totalProducts, int totalCategories, int totalUnits, int lowStockCount,
                           BigDecimal costValuation, BigDecimal retailValuation) {
        this.lastTotalProducts = totalProducts;
        this.lastTotalCategories = totalCategories;
        this.lastTotalUnits = totalUnits;
        this.lastLowStockCount = lowStockCount;
        this.lastCostValuation = costValuation != null ? costValuation : BigDecimal.ZERO;
        this.lastRetailValuation = retailValuation != null ? retailValuation : BigDecimal.ZERO;

        kpiTotalProductsVal.setText(LanguageManager.isolateLTR(String.valueOf(totalProducts)));
        productsKpiCard.setSubtitle(
                LanguageManager.format("dashboard.kpi.products.sub", totalCategories),
                ThemeManager.textSecondary());

        NumberFormat intFmt = NumberFormat.getIntegerInstance(Locale.US);
        kpiTotalUnitsVal.setText(LanguageManager.isolateLTR(intFmt.format(totalUnits)));
        long avgUnits = totalProducts > 0 ? Math.round(totalUnits / (double) totalProducts) : 0;
        unitsKpiCard.setSubtitle(
                LanguageManager.format("dashboard.kpi.units.sub", avgUnits),
                ThemeManager.textSecondary());

        kpiLowStockVal.setText(LanguageManager.isolateLTR(String.valueOf(lowStockCount)));
        if (lowStockCount == 0) {
            lowStockKpiCard.setSubtitle(
                    LanguageManager.get("dashboard.kpi.lowStock.healthy"),
                    ThemeManager.accentSuccess());
        } else {
            lowStockKpiCard.setSubtitle(
                    LanguageManager.get("dashboard.kpi.lowStock.action"),
                    ThemeManager.accentDanger());
        }

        kpiValuationVal.setText(LanguageManager.isolateLTR(CURRENCY.format(this.lastCostValuation)));
        double marginPct = 0.0;
        if (this.lastRetailValuation.compareTo(BigDecimal.ZERO) > 0) {
            marginPct = (this.lastRetailValuation.subtract(this.lastCostValuation).doubleValue()
                    / this.lastRetailValuation.doubleValue()) * 100.0;
        }
        String retailStr = LanguageManager.isolateLTR(CURRENCY.format(this.lastRetailValuation));
        String marginStr = LanguageManager.isolateLTR(String.format(Locale.US, "%.1f", marginPct));
        valuationKpiCard.setSubtitle(
                LanguageManager.format("dashboard.kpi.value.sub", retailStr, marginStr),
                ThemeManager.textSecondary());
    }

    public void populateDashboardWatchlists(List<Product> lowStockProducts,
                                            List<StockTransaction> recentTransactions,
                                            Map<Integer, Product> productMap) {
        this.lastLowStockProducts = lowStockProducts != null ? lowStockProducts : Collections.emptyList();
        this.lastRecentTransactions = recentTransactions != null ? recentTransactions : Collections.emptyList();
        this.lastRecentProductMap = productMap != null ? productMap : Collections.emptyMap();

        watchlistTableModel.setRowCount(0);
        for (Product p : this.lastLowStockProducts) {
            int deficit = Math.max(0, p.getReorderLevel() - p.getQuantityInStock());
            watchlistTableModel.addRow(new Object[]{
                    p.getId(),
                    p.getSku(),
                    p.getName(),
                    p.getQuantityInStock(),
                    p.getReorderLevel(),
                    deficit
            });
        }

        boolean emptyWatchlist = this.lastLowStockProducts.isEmpty();
        watchlistCardLayout.show(watchlistContentPanel, emptyWatchlist ? WATCHLIST_EMPTY : WATCHLIST_TABLE);
        quickRestockBtn.setEnabled(!emptyWatchlist);

        recentMovementsTableModel.setRowCount(0);
        int limit = Math.min(15, this.lastRecentTransactions.size());
        for (int i = 0; i < limit; i++) {
            StockTransaction tx = this.lastRecentTransactions.get(i);
            recentMovementsTableModel.addRow(new Object[]{
                    formatTimestamp(tx.getCreatedAt()),
                    resolveProductLabel(tx.getProductId(), this.lastRecentProductMap),
                    tx.getTransactionType().name(),
                    (tx.getQuantityChange() > 0 ? "+" : "") + tx.getQuantityChange()
            });
        }
    }

    public void populateProductTable(List<Product> products) {
        populateProductTable(products, Collections.emptyMap(), Collections.emptyMap());
    }

    public void populateProductTable(List<Product> products,
                                     Map<Integer, String> categoryNames,
                                     Map<Integer, String> supplierNames) {
        populateProductTable(products, categoryNames, supplierNames, products != null ? products.size() : 0);
    }

    public void populateProductTable(List<Product> products,
                                     Map<Integer, String> categoryNames,
                                     Map<Integer, String> supplierNames,
                                     int totalCatalogCount) {
        this.lastCatalogProducts = products != null ? products : Collections.emptyList();
        this.lastCategoryNames = categoryNames != null ? categoryNames : Collections.emptyMap();
        this.lastSupplierNames = supplierNames != null ? supplierNames : Collections.emptyMap();
        this.lastTotalCatalogCount = Math.max(totalCatalogCount, this.lastCatalogProducts.size());

        String unassigned = LanguageManager.get("common.unassigned");
        productTableModel.setRowCount(0);
        int totalUnits = 0;
        BigDecimal totalCost = BigDecimal.ZERO;

        for (Product p : this.lastCatalogProducts) {
            String categoryLabel = p.getCategoryId() != null
                    ? this.lastCategoryNames.getOrDefault(p.getCategoryId(), unassigned)
                    : unassigned;
            String supplierLabel = p.getSupplierId() != null
                    ? this.lastSupplierNames.getOrDefault(p.getSupplierId(), unassigned)
                    : unassigned;
            BigDecimal cost = p.getPurchasePrice() != null ? p.getPurchasePrice() : BigDecimal.ZERO;
            BigDecimal sell = p.getUnitPrice() != null ? p.getUnitPrice() : BigDecimal.ZERO;

            double marginPct = 0.0;
            if (sell.compareTo(BigDecimal.ZERO) > 0) {
                marginPct = ((sell.doubleValue() - cost.doubleValue()) / sell.doubleValue()) * 100.0;
            }
            String marginLabel = String.format(Locale.US, "%.1f%%", marginPct);
            String status = p.isLowStock() ? "LOW STOCK" : "IN STOCK";

            int qty = Math.max(0, p.getQuantityInStock());
            totalUnits += qty;
            totalCost = totalCost.add(cost.multiply(BigDecimal.valueOf(qty)));

            productTableModel.addRow(new Object[]{
                    p.getId(),
                    p.getSku(),
                    p.getName(),
                    categoryLabel,
                    supplierLabel,
                    CURRENCY.format(cost),
                    CURRENCY.format(sell),
                    marginLabel,
                    p.getQuantityInStock(),
                    p.getReorderLevel(),
                    status
            });
        }

        this.lastShownUnits = totalUnits;
        this.lastShownCostValue = totalCost;
        updateProductSummaryText();
        updateProductSelectionState();
    }

    private void updateProductSummaryText() {
        String unitsStr = LanguageManager.isolateLTR(NumberFormat.getIntegerInstance(Locale.US).format(lastShownUnits));
        String costStr = LanguageManager.isolateLTR(CURRENCY.format(lastShownCostValue));
        productSummaryLabel.setText(LanguageManager.format("products.summary",
                lastCatalogProducts.size(), lastTotalCatalogCount, unitsStr, costStr));
    }

    public void populateTransactionTable(List<StockTransaction> transactions, Map<Integer, Product> productMap) {
        populateTransactionTable(transactions, productMap, Collections.emptyMap());
    }

    public void populateTransactionTable(List<StockTransaction> transactions,
                                         Map<Integer, Product> productMap,
                                         Map<Integer, User> userMap) {
        this.lastTransactions = transactions != null ? transactions : Collections.emptyList();
        this.lastTxProductMap = productMap != null ? productMap : Collections.emptyMap();
        this.lastTxUserMap = userMap != null ? userMap : Collections.emptyMap();

        transactionTableModel.setRowCount(0);
        for (StockTransaction tx : this.lastTransactions) {
            transactionTableModel.addRow(new Object[]{
                    tx.getId(),
                    resolveProductLabel(tx.getProductId(), this.lastTxProductMap),
                    tx.getTransactionType().name(),
                    (tx.getQuantityChange() > 0 ? "+" : "") + tx.getQuantityChange(),
                    tx.getNotes() != null ? tx.getNotes() : "-",
                    resolveOperatorLabel(tx.getPerformedByUserId(), this.lastTxUserMap),
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

    private String resolveOperatorLabel(Integer userId, Map<Integer, User> userMap) {
        if (userId == null) {
            return LanguageManager.get("common.system");
        }
        if (userMap != null) {
            User u = userMap.get(userId);
            if (u != null) {
                return u.getUsername() + " (" + u.getFullName() + ")";
            }
        }
        return "User #" + userId;
    }

    private String formatTimestamp(LocalDateTime ts) {
        return ts != null ? ts.format(TS_FORMAT) : LanguageManager.get("common.na");
    }

    private static String formatMovementFilterLabel(String raw) {
        if ("ALL".equals(raw)) {
            return LanguageManager.get("transactions.filter.all");
        }
        return LanguageManager.get("transactions.type." + raw);
    }

    private static double parseNumeric(Object o) {
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        if (o == null) {
            return 0.0;
        }
        String cleaned = o.toString().replaceAll("[^0-9.\\-]", "");
        if (cleaned.isEmpty() || "-".equals(cleaned) || ".".equals(cleaned)) {
            return 0.0;
        }
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    public void setStatusMessage(String message) {
        statusMessageLabel.setText(message);
    }

    public void setBusy(boolean busy, String message) {
        statusProgressBar.setVisible(busy);
        if (message != null) {
            statusMessageLabel.setText(message);
        }
        statusBarPanel.revalidate();
        statusBarPanel.repaint();
    }

    @Override
    public void dispose() {
        ThemeManager.removeThemeListener(themeListener);
        LanguageManager.removeLanguageListener(languageListener);
        if (clockTimer != null) {
            clockTimer.stop();
        }
        super.dispose();
    }

    // Accessors for Event Listeners & Controllers
    public JButton getNavDashboardBtn() { return navDashboardBtn; }
    public JButton getNavProductsBtn() { return navProductsBtn; }
    public JButton getNavTransactionsBtn() { return navTransactionsBtn; }
    public JButton getNavSuppliersBtn() { return navSuppliersBtn; }
    public JButton getNavReportsBtn() { return navReportsBtn; }
    public JButton getNavUsersBtn() { return navUsersBtn; }
    public JButton getLogoutBtn() { return logoutBtn; }

    public JTable getWatchlistTable() { return watchlistTable; }
    public DefaultTableModel getWatchlistTableModel() { return watchlistTableModel; }
    public JButton getQuickRestockBtn() { return quickRestockBtn; }

    public JButton getRefreshProductsBtn() { return refreshProductsBtn; }
    public JButton getManageCategoriesBtn() { return manageCategoriesBtn; }
    public JButton getExportProductsBtn() { return exportProductsBtn; }
    public JButton getAddProductBtn() { return addProductBtn; }
    public JButton getEditProductBtn() { return editProductBtn; }
    public JButton getDeleteProductBtn() { return deleteProductBtn; }
    public JTable getProductTable() { return productTable; }
    public DefaultTableModel getProductTableModel() { return productTableModel; }
    public JTextField getProductSearchField() { return productSearchField; }
    public JComboBox<String> getProductStatusFilterCombo() { return productStatusFilterCombo; }

    public JMenuItem getCtxEditProductItem() { return ctxEditProductItem; }
    public JMenuItem getCtxStockInItem() { return ctxStockInItem; }
    public JMenuItem getCtxStockOutItem() { return ctxStockOutItem; }
    public JMenuItem getCtxCopySkuItem() { return ctxCopySkuItem; }
    public JMenuItem getCtxDeleteProductItem() { return ctxDeleteProductItem; }

    public JButton getStockInBtn() { return stockInBtn; }
    public JButton getStockOutBtn() { return stockOutBtn; }
    public JButton getRefreshTransactionsBtn() { return refreshTransactionsBtn; }
    public JButton getExportTransactionsBtn() { return exportTransactionsBtn; }
    public JTable getTransactionTable() { return transactionTable; }
    public JTextField getTransactionSearchField() { return transactionSearchField; }
    public JComboBox<String> getTransactionTypeFilterCombo() { return transactionTypeFilterCombo; }
    public JComboBox<String> getTransactionTimeframeFilterCombo() { return transactionTimeframeFilterCombo; }

    public SuppliersPanel getSuppliersPanel() { return suppliersPanel; }
    public ReportsPanel getReportsPanel() { return reportsPanel; }
    public UsersPanel getUsersPanel() { return usersPanel; }
    public User getCurrentUser() { return currentUser; }

    // =========================================================================
    // Custom 60fps Animated Components & RTL-Aware Cell Renderers
    // =========================================================================

    private static class AvatarCircle extends JComponent {
        private final String initials;
        private Color fillColor = ThemeManager.accentPrimary();

        AvatarCircle(String initials) {
            this.initials = initials;
            setPreferredSize(new Dimension(34, 34));
            setMinimumSize(new Dimension(34, 34));
            setMaximumSize(new Dimension(34, 34));
        }

        void setFillColor(Color fillColor) {
            this.fillColor = fillColor;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int size = Math.min(getWidth(), getHeight()) - 2;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;

                g2.setColor(fillColor);
                g2.fill(new Ellipse2D.Float(x, y, size, size));

                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                int tx = x + (size - fm.stringWidth(initials)) / 2;
                int ty = y + ((size - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(initials, tx, ty);
            } finally {
                g2.dispose();
            }
        }
    }

    private static class SidebarButton extends JButton {
        private String fullLabel = "";
        private Icon activeIcon;
        private Icon inactiveIcon;
        private boolean active;
        private boolean collapsed = false;
        private boolean rtl = false;
        private float hoverProgress = 0.0f;
        private float targetHover = 0.0f;
        private final Timer hoverTimer;

        SidebarButton(boolean active) {
            this.active = active;
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setIconTextGap(12);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(0, 40));

            hoverTimer = new Timer(16, e -> {
                float step = 0.12f;
                if (hoverProgress < targetHover) {
                    hoverProgress = Math.min(targetHover, hoverProgress + step);
                } else {
                    hoverProgress = Math.max(targetHover, hoverProgress - step);
                }
                repaint();
                if (Math.abs(hoverProgress - targetHover) < 0.005f) {
                    hoverProgress = targetHover;
                    ((Timer) e.getSource()).stop();
                }
            });

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    targetHover = 1.0f;
                    if (!hoverTimer.isRunning()) {
                        hoverTimer.start();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    targetHover = 0.0f;
                    if (!hoverTimer.isRunning()) {
                        hoverTimer.start();
                    }
                }
            });
        }

        void updateAppearance(String label, Icon activeIcon, Icon inactiveIcon, boolean rtl) {
            this.fullLabel = label;
            this.activeIcon = activeIcon;
            this.inactiveIcon = inactiveIcon;
            this.rtl = rtl;
            applyLayoutMode();
        }

        void setCollapsed(boolean collapsed) {
            this.collapsed = collapsed;
            applyLayoutMode();
        }

        private void applyLayoutMode() {
            if (collapsed) {
                setText("");
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(new EmptyBorder(8, 8, 8, 8));
                setToolTipText(fullLabel);
            } else {
                setText(fullLabel);
                setHorizontalAlignment(SwingConstants.LEADING);
                setBorder(ThemeManager.createDirectionalBorder(8, 14, 8, 14, rtl));
                setToolTipText(null);
            }
            applyActiveVisuals();
        }

        void setActive(boolean active) {
            this.active = active;
            applyActiveVisuals();
            repaint();
        }

        private void applyActiveVisuals() {
            setIcon(active ? activeIcon : inactiveIcon);
            setForeground(active ? ThemeManager.sidebarTextActive() : ThemeManager.sidebarText());
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                if (active) {
                    g2.setColor(ThemeManager.sidebarActive());
                    g2.fill(new RoundRectangle2D.Float(0, 1, w, h - 2, 10, 10));
                    int indicatorX = getComponentOrientation().isLeftToRight() ? 0 : (w - 4);
                    g2.setColor(ThemeManager.accentPrimaryHover());
                    g2.fill(new RoundRectangle2D.Float(indicatorX, 7, 4, h - 14, 4, 4));
                } else if (hoverProgress > 0.0f) {
                    Color idleBg = new Color(ThemeManager.bgSidebar().getRed(),
                            ThemeManager.bgSidebar().getGreen(), ThemeManager.bgSidebar().getBlue(), 0);
                    g2.setColor(ThemeManager.lerpColor(idleBg, ThemeManager.sidebarHover(), hoverProgress));
                    g2.fill(new RoundRectangle2D.Float(0, 1, w, h - 2, 10, 10));
                }
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    private static class InteractiveKpiCard extends JPanel {
        private final JLabel titleLbl;
        private final JLabel valueLabel;
        private final JLabel subtitleLbl;
        private final JLabel iconBadge;
        private Color accentColor = ThemeManager.accentPrimary();
        private float hoverProgress = 0.0f;
        private float targetHover = 0.0f;
        private final Timer hoverTimer;

        InteractiveKpiCard(JLabel valueLabel, Runnable onClick) {
            this.valueLabel = valueLabel;
            setLayout(new BorderLayout(10, 6));
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(12, 18, 12, 16));

            JPanel textCol = new JPanel(new GridBagLayout());
            textCol.setOpaque(false);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.LINE_START;

            titleLbl = new JLabel();
            titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
            titleLbl.setHorizontalAlignment(SwingConstants.LEADING);
            gbc.gridy = 0;
            gbc.insets = new Insets(0, 0, 4, 0);
            textCol.add(titleLbl, gbc);

            valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
            valueLabel.setHorizontalAlignment(SwingConstants.LEADING);
            gbc.gridy = 1;
            gbc.insets = new Insets(0, 0, 4, 0);
            textCol.add(valueLabel, gbc);

            subtitleLbl = new JLabel(" ");
            subtitleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            subtitleLbl.setHorizontalAlignment(SwingConstants.LEADING);
            gbc.gridy = 2;
            gbc.insets = new Insets(0, 0, 0, 0);
            textCol.add(subtitleLbl, gbc);

            iconBadge = new JLabel("", SwingConstants.CENTER);
            iconBadge.setOpaque(true);
            iconBadge.setPreferredSize(new Dimension(42, 42));

            JPanel badgeWrapper = new JPanel(new GridBagLayout());
            badgeWrapper.setOpaque(false);
            badgeWrapper.add(iconBadge);

            add(textCol, BorderLayout.CENTER);
            add(badgeWrapper, BorderLayout.LINE_END);

            hoverTimer = new Timer(16, e -> {
                float step = 0.12f;
                if (hoverProgress < targetHover) {
                    hoverProgress = Math.min(targetHover, hoverProgress + step);
                } else {
                    hoverProgress = Math.max(targetHover, hoverProgress - step);
                }
                repaint();
                if (Math.abs(hoverProgress - targetHover) < 0.005f) {
                    hoverProgress = targetHover;
                    ((Timer) e.getSource()).stop();
                }
            });

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    targetHover = 1.0f;
                    if (!hoverTimer.isRunning()) {
                        hoverTimer.start();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    targetHover = 0.0f;
                    if (!hoverTimer.isRunning()) {
                        hoverTimer.start();
                    }
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e) && onClick != null) {
                        onClick.run();
                    }
                }
            });
        }

        void refreshStyle(String title, Color accentColor, Color badgeBg, Icon badgeIcon) {
            this.accentColor = accentColor;
            boolean rtl = LanguageManager.isRTL();
            setBorder(ThemeManager.createDirectionalBorder(12, 20, 12, 16, rtl));
            titleLbl.setText(title);
            titleLbl.setForeground(ThemeManager.textSecondary());
            valueLabel.setForeground(ThemeManager.textPrimary());
            iconBadge.setIcon(badgeIcon);
            iconBadge.setBackground(badgeBg);
            repaint();
        }

        void setSubtitle(String text, Color color) {
            subtitleLbl.setText(text);
            subtitleLbl.setForeground(color);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                Color bg = ThemeManager.lerpColor(ThemeManager.bgSurface(), ThemeManager.bgHover(), hoverProgress);
                Color border = ThemeManager.lerpColor(ThemeManager.borderSubtle(), accentColor, hoverProgress * 0.55f);

                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 8, 8));

                g2.setColor(border);
                g2.draw(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 8, 8));

                int stripeX = getComponentOrientation().isLeftToRight() ? 0 : (w - 4);
                g2.setColor(accentColor);
                g2.fill(new RoundRectangle2D.Float(stripeX, 1, 4, h - 2, 4, 4));
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

            boolean numericCol = col == 0 || (col >= 5 && col <= 9);
            cell.setHorizontalAlignment(numericCol ? SwingConstants.TRAILING : SwingConstants.LEADING);

            if ((numericCol || col == 1) && val != null) {
                cell.setText(LanguageManager.isolateLTR(val.toString()));
            }
            if (col == 1 || col == 7) {
                cell.setFont(cell.getFont().deriveFont(Font.BOLD));
            }

            if (!isSel) {
                int modelRow = table.convertRowIndexToModel(row);
                String status = String.valueOf(table.getModel().getValueAt(modelRow, 10));
                if ("LOW STOCK".equals(status)) {
                    cell.setBackground(ThemeManager.isDark() ? new Color(0x2D191E) : new Color(0xFEF7F7));
                } else {
                    cell.setBackground(row % 2 == 0 ? ThemeManager.bgSurface() : ThemeManager.bgSurfaceAlt());
                }

                if (col == 7 && val != null) {
                    double pct = parseNumeric(val);
                    if (pct > 0.0) {
                        cell.setForeground(ThemeManager.accentSuccess());
                    } else if (pct < 0.0) {
                        cell.setForeground(ThemeManager.accentDanger());
                    } else {
                        cell.setForeground(ThemeManager.textSecondary());
                    }
                } else {
                    cell.setForeground(ThemeManager.textPrimary());
                }
            }
            return cell;
        }
    }

    private static class StatusBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object val, boolean isSel,
                                                       boolean hasFocus, int row, int col) {
            String rawStatus = val != null ? val.toString() : "";
            boolean isLow = "LOW STOCK".equals(rawStatus);
            String label = isLow ? LanguageManager.get("status.lowStock") : LanguageManager.get("status.inStock");

            JPanel wrapper = new JPanel(new GridBagLayout());
            wrapper.setOpaque(true);
            if (isSel) {
                wrapper.setBackground(table.getSelectionBackground());
            } else {
                wrapper.setBackground(isLow
                        ? (ThemeManager.isDark() ? new Color(0x2D191E) : new Color(0xFEF7F7))
                        : (row % 2 == 0 ? ThemeManager.bgSurface() : ThemeManager.bgSurfaceAlt()));
            }

            BadgeColors colors = isLow ? ThemeManager.badgeDanger() : ThemeManager.badgeSuccess();
            wrapper.add(createRoundedPillLabel(label, colors.background(), colors.foreground()));
            return wrapper;
        }
    }

    private static class MovementTypeBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object val, boolean isSel,
                                                       boolean hasFocus, int row, int col) {
            String rawType = val != null ? val.toString() : "";
            String label = formatMovementFilterLabel(rawType);

            BadgeColors colors = switch (rawType) {
                case "INBOUND_PURCHASE" -> ThemeManager.badgeSuccess();
                case "RETURN_RESTOCK" -> ThemeManager.badgeInfo();
                case "OUTBOUND_SALE" -> ThemeManager.badgeDanger();
                default -> ThemeManager.badgeWarning();
            };

            JPanel wrapper = new JPanel(new GridBagLayout());
            wrapper.setOpaque(true);
            wrapper.setBackground(isSel ? table.getSelectionBackground()
                    : (row % 2 == 0 ? ThemeManager.bgSurface() : ThemeManager.bgSurfaceAlt()));
            wrapper.add(createRoundedPillLabel(label, colors.background(), colors.foreground()));
            return wrapper;
        }
    }

    private static JLabel createRoundedPillLabel(String text, Color bg, Color fg) {
        JLabel pill = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(bg);
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                } finally {
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        pill.setFont(new Font("Segoe UI", Font.BOLD, 11));
        pill.setForeground(fg);
        pill.setOpaque(false);
        pill.setBorder(new EmptyBorder(3, 10, 3, 10));
        return pill;
    }

    private static class TransactionTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object val, boolean isSel,
                                                       boolean hasFocus, int row, int col) {
            JLabel cell = (JLabel) super.getTableCellRendererComponent(table, val, isSel, hasFocus, row, col);
            cell.setBorder(new EmptyBorder(0, 12, 0, 12));
            boolean numericCol = col == 0 || col == 3;
            cell.setHorizontalAlignment(numericCol ? SwingConstants.TRAILING : SwingConstants.LEADING);

            if ((numericCol || col == 6) && val != null) {
                cell.setText(LanguageManager.isolateLTR(val.toString()));
            }

            if (!isSel) {
                cell.setBackground(row % 2 == 0 ? ThemeManager.bgSurface() : ThemeManager.bgSurfaceAlt());
                cell.setForeground(ThemeManager.textPrimary());
            }

            if (col == 3 && val != null) {
                String text = val.toString();
                cell.setFont(cell.getFont().deriveFont(Font.BOLD));
                if (!isSel) {
                    cell.setForeground(text.startsWith("+")
                            ? ThemeManager.accentSuccess()
                            : ThemeManager.accentDanger());
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
            boolean numericCol = col == 0 || col >= 3;
            cell.setHorizontalAlignment(numericCol ? SwingConstants.TRAILING : SwingConstants.LEADING);

            if ((numericCol || col == 1) && val != null) {
                cell.setText(LanguageManager.isolateLTR(val.toString()));
            }

            if (!isSel) {
                cell.setBackground(row % 2 == 0
                        ? ThemeManager.bgSurface()
                        : (ThemeManager.isDark() ? new Color(0x2D191E) : new Color(0xFEF7F7)));
                cell.setForeground(col == 5 ? ThemeManager.accentDanger() : ThemeManager.textPrimary());
                if (col == 1 || col == 5) {
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
            cell.setHorizontalAlignment(col == 3 ? SwingConstants.TRAILING : SwingConstants.LEADING);

            if ((col == 0 || col == 3) && val != null) {
                cell.setText(LanguageManager.isolateLTR(val.toString()));
            }

            if (!isSel) {
                cell.setBackground(row % 2 == 0 ? ThemeManager.bgSurface() : ThemeManager.bgSurfaceAlt());
                cell.setForeground(ThemeManager.textPrimary());
                if (col == 3 && val != null) {
                    cell.setFont(cell.getFont().deriveFont(Font.BOLD));
                    cell.setForeground(val.toString().startsWith("+")
                            ? ThemeManager.accentSuccess()
                            : ThemeManager.accentDanger());
                }
            }
            return cell;
        }
    }
}
