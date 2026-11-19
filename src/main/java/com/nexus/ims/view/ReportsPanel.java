package com.nexus.ims.view;

import com.nexus.ims.model.Product;
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
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Financial valuation and reorder deficit view with category chart, empty states, and multi-format export.
 */
public class ReportsPanel extends JPanel {
    private static final String CARD_TABLE = "TABLE";
    private static final String CARD_EMPTY = "EMPTY";
    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(Locale.US);

    private JLabel reportHeaderTitle;
    private JButton refreshReportsBtn;
    private JButton exportCsvBtn;

    private JLabel totalCostValueLabel;
    private JLabel totalRetailValueLabel;
    private JLabel projectedProfitLabel;
    private JLabel lowStockCountLabel;

    private JPanel costCard;
    private JPanel retailCard;
    private JPanel profitCard;
    private JPanel lowStockCard;
    private JLabel costCardTitle;
    private JLabel retailCardTitle;
    private JLabel profitCardTitle;
    private JLabel lowStockCardTitle;
    private JLabel costIconBadge;
    private JLabel retailIconBadge;
    private JLabel profitIconBadge;
    private JLabel lowStockIconBadge;

    private JPanel tableContainer;
    private JLabel sectionTitle;
    private CategoryValuationChartPanel categoryValuationChartPanel;
    private JTable reorderTable;
    private DefaultTableModel reorderTableModel;
    private JScrollPane scrollPane;
    private CardLayout reorderCardLayout;
    private JPanel reorderContentPanel;
    private JPanel emptyStatePanel;
    private JLabel emptyIconLabel;
    private JLabel emptyTitleLabel;
    private JLabel emptySubLabel;
    private int lastLowStockCount = 0;

    public ReportsPanel() {
        initComponents();
        refreshThemeAndLanguage();
    }

    private void initComponents() {
        setLayout(new BorderLayout(16, 14));
        setOpaque(false);
        setBorder(new EmptyBorder(18, 20, 18, 20));

        // 1. Top Bar (Title + Export / Refresh Actions) + 4 Financial KPI Cards
        JPanel northContainer = new JPanel(new BorderLayout(0, 12));
        northContainer.setOpaque(false);

        JPanel topBar = new JPanel(new BorderLayout(10, 0));
        topBar.setOpaque(false);

        reportHeaderTitle = new JLabel("", SwingConstants.LEADING);
        reportHeaderTitle.setIconTextGap(8);
        reportHeaderTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        topBar.add(reportHeaderTitle, BorderLayout.LINE_START);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
        actions.setOpaque(false);

        refreshReportsBtn = new JButton();
        refreshReportsBtn.setIconTextGap(6);

        exportCsvBtn = new JButton();
        exportCsvBtn.setIconTextGap(8);
        exportCsvBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));

        actions.add(refreshReportsBtn);
        actions.add(exportCsvBtn);
        topBar.add(actions, BorderLayout.LINE_END);

        northContainer.add(topBar, BorderLayout.NORTH);

        JPanel kpiGrid = new JPanel(new GridLayout(1, 4, 16, 0));
        kpiGrid.setOpaque(false);
        kpiGrid.setPreferredSize(new Dimension(0, 108));

        totalCostValueLabel = new JLabel("$0.00");
        totalRetailValueLabel = new JLabel("$0.00");
        projectedProfitLabel = new JLabel("$0.00");
        lowStockCountLabel = new JLabel("0");

        costCardTitle = new JLabel();
        retailCardTitle = new JLabel();
        profitCardTitle = new JLabel();
        lowStockCardTitle = new JLabel();

        costIconBadge = new JLabel("", SwingConstants.CENTER);
        retailIconBadge = new JLabel("", SwingConstants.CENTER);
        profitIconBadge = new JLabel("", SwingConstants.CENTER);
        lowStockIconBadge = new JLabel("", SwingConstants.CENTER);

        costCard = createReportKpiCard(costCardTitle, totalCostValueLabel, costIconBadge);
        retailCard = createReportKpiCard(retailCardTitle, totalRetailValueLabel, retailIconBadge);
        profitCard = createReportKpiCard(profitCardTitle, projectedProfitLabel, profitIconBadge);
        lowStockCard = createReportKpiCard(lowStockCardTitle, lowStockCountLabel, lowStockIconBadge);

        kpiGrid.add(costCard);
        kpiGrid.add(retailCard);
        kpiGrid.add(profitCard);
        kpiGrid.add(lowStockCard);

        northContainer.add(kpiGrid, BorderLayout.CENTER);
        add(northContainer, BorderLayout.NORTH);

        // 2. Center Stack: Category Valuation Chart + Reorder Action Schedule (Table / Empty State)
        tableContainer = new JPanel(new BorderLayout(10, 10));

        JPanel sectionHeader = new JPanel(new BorderLayout(10, 0));
        sectionHeader.setOpaque(false);

        sectionTitle = new JLabel("", SwingConstants.LEADING);
        sectionTitle.setIconTextGap(8);
        sectionTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        sectionHeader.add(sectionTitle, BorderLayout.LINE_START);
        tableContainer.add(sectionHeader, BorderLayout.NORTH);

        String[] columns = new String[8];
        reorderTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        reorderTable = new JTable(reorderTableModel);
        reorderTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        reorderTable.getTableHeader().setReorderingAllowed(false);
        reorderTable.setDefaultRenderer(Object.class, new ReorderCellRenderer());

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(reorderTableModel);
        sorter.setComparator(0, DashboardFrame.NUMERIC_COMPARATOR);
        sorter.setComparator(3, DashboardFrame.NUMERIC_COMPARATOR);
        sorter.setComparator(4, DashboardFrame.NUMERIC_COMPARATOR);
        sorter.setComparator(5, DashboardFrame.NUMERIC_COMPARATOR);
        sorter.setComparator(6, DashboardFrame.NUMERIC_COMPARATOR);
        sorter.setComparator(7, DashboardFrame.NUMERIC_COMPARATOR);
        reorderTable.setRowSorter(sorter);

        TableColumnModel cm = reorderTable.getColumnModel();
        cm.getColumn(0).setPreferredWidth(50);
        cm.getColumn(0).setMaxWidth(70);
        cm.getColumn(1).setPreferredWidth(130);
        cm.getColumn(2).setPreferredWidth(250);

        scrollPane = new JScrollPane(reorderTable);

        emptyStatePanel = buildHealthyEmptyState();
        reorderCardLayout = new CardLayout();
        reorderContentPanel = new JPanel(reorderCardLayout);
        reorderContentPanel.setOpaque(false);
        reorderContentPanel.add(scrollPane, CARD_TABLE);
        reorderContentPanel.add(emptyStatePanel, CARD_EMPTY);
        tableContainer.add(reorderContentPanel, BorderLayout.CENTER);

        categoryValuationChartPanel = new CategoryValuationChartPanel();
        categoryValuationChartPanel.setPreferredSize(new Dimension(0, 280));

        JPanel centerStack = new JPanel(new BorderLayout(0, 14));
        centerStack.setOpaque(false);
        centerStack.add(categoryValuationChartPanel, BorderLayout.NORTH);
        centerStack.add(tableContainer, BorderLayout.CENTER);

        add(centerStack, BorderLayout.CENTER);
    }

    private JPanel buildHealthyEmptyState() {
        JPanel empty = new JPanel(new GridBagLayout());
        empty.setOpaque(true);

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);

        emptyIconLabel = new JLabel();
        emptyIconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        emptyTitleLabel = new JLabel("", SwingConstants.CENTER);
        emptyTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        emptyTitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        emptySubLabel = new JLabel("", SwingConstants.CENTER);
        emptySubLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        emptySubLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        inner.add(emptyIconLabel);
        inner.add(Box.createVerticalStrut(10));
        inner.add(emptyTitleLabel);
        inner.add(Box.createVerticalStrut(4));
        inner.add(emptySubLabel);
        empty.add(inner);
        return empty;
    }

    private JPanel createReportKpiCard(JLabel titleLbl, JLabel valueLabel, JLabel iconBadge) {
        JPanel card = new JPanel(new BorderLayout(12, 6));

        JPanel textCol = new JPanel(new GridBagLayout());
        textCol.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.LINE_START;

        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        titleLbl.setHorizontalAlignment(SwingConstants.LEADING);
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 6, 0);
        textCol.add(titleLbl, gbc);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueLabel.setHorizontalAlignment(SwingConstants.LEADING);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        textCol.add(valueLabel, gbc);

        iconBadge.setOpaque(true);
        iconBadge.setPreferredSize(new Dimension(42, 42));

        JPanel badgeWrapper = new JPanel(new GridBagLayout());
        badgeWrapper.setOpaque(false);
        badgeWrapper.add(iconBadge);

        card.add(textCol, BorderLayout.CENTER);
        card.add(badgeWrapper, BorderLayout.LINE_END);
        return card;
    }

    public void refreshThemeAndLanguage() {
        boolean rtl = LanguageManager.isRTL();

        reportHeaderTitle.setText(LanguageManager.get("reports.header.title"));
        reportHeaderTitle.setIcon(IconFactory.reports(18, ThemeManager.accentPrimary()));
        reportHeaderTitle.setForeground(ThemeManager.textPrimary());

        refreshReportsBtn.setText(LanguageManager.get("common.refresh"));
        refreshReportsBtn.setBackground(ThemeManager.btnSecondaryBg());
        refreshReportsBtn.setForeground(ThemeManager.btnSecondaryFg());
        refreshReportsBtn.setIcon(IconFactory.refresh(15, ThemeManager.btnSecondaryFg()));

        exportCsvBtn.setText(LanguageManager.get("export.button"));
        exportCsvBtn.setIcon(IconFactory.exportDropdown(15, Color.WHITE));
        exportCsvBtn.setBackground(ThemeManager.accentSuccess());
        exportCsvBtn.setForeground(Color.WHITE);

        styleKpiCard(costCard, costCardTitle, totalCostValueLabel, costIconBadge,
                LanguageManager.get("reports.kpi.cost"), ThemeManager.accentPrimary(),
                ThemeManager.badgeInfo().background(), IconFactory.products(20, ThemeManager.accentPrimary()), rtl);
        styleKpiCard(retailCard, retailCardTitle, totalRetailValueLabel, retailIconBadge,
                LanguageManager.get("reports.kpi.retail"), ThemeManager.accentSecondary(),
                ThemeManager.badgePurple().background(), IconFactory.reports(20, ThemeManager.accentSecondary()), rtl);
        styleKpiCard(profitCard, profitCardTitle, projectedProfitLabel, profitIconBadge,
                LanguageManager.get("reports.kpi.profit"), ThemeManager.accentSuccess(),
                ThemeManager.badgeSuccess().background(), IconFactory.dollarWallet(20, ThemeManager.accentSuccess()), rtl);
        styleKpiCard(lowStockCard, lowStockCardTitle, lowStockCountLabel, lowStockIconBadge,
                LanguageManager.get("reports.kpi.lowStock"), ThemeManager.accentDanger(),
                ThemeManager.badgeDanger().background(), IconFactory.alert(20, ThemeManager.accentDanger()), rtl);

        tableContainer.setBackground(ThemeManager.bgSurface());
        tableContainer.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 1, 1, 1, ThemeManager.borderSubtle()),
                new EmptyBorder(16, 16, 16, 16)
        ));

        sectionTitle.setText(LanguageManager.get("reports.reorder.title"));
        sectionTitle.setIcon(IconFactory.alert(16, ThemeManager.accentDanger()));
        sectionTitle.setForeground(ThemeManager.textPrimary());

        String[] headers = {
                LanguageManager.get("common.id"),
                LanguageManager.get("reports.reorder.col.sku"),
                LanguageManager.get("reports.reorder.col.name"),
                LanguageManager.get("reports.reorder.col.current"),
                LanguageManager.get("reports.reorder.col.min"),
                LanguageManager.get("reports.reorder.col.deficit"),
                LanguageManager.get("reports.reorder.col.unitCost"),
                LanguageManager.get("reports.reorder.col.estCost")
        };
        TableColumnModel cm = reorderTable.getColumnModel();
        for (int i = 0; i < headers.length && i < cm.getColumnCount(); i++) {
            cm.getColumn(i).setHeaderValue(headers[i]);
        }
        if (reorderTable.getTableHeader() != null) {
            reorderTable.getTableHeader().repaint();
        }

        scrollPane.setBorder(new MatteBorder(1, 1, 1, 1, ThemeManager.borderSubtle()));
        scrollPane.getViewport().setBackground(ThemeManager.bgSurface());
        reorderTable.setBackground(ThemeManager.bgSurface());
        reorderTable.setForeground(ThemeManager.textPrimary());
        reorderTable.setGridColor(ThemeManager.borderSubtle());

        emptyStatePanel.setBackground(ThemeManager.bgSurfaceAlt());
        emptyStatePanel.setBorder(new MatteBorder(1, 1, 1, 1, ThemeManager.borderSubtle()));
        emptyIconLabel.setIcon(IconFactory.checkShield(36, ThemeManager.accentSuccess()));
        emptyTitleLabel.setText(LanguageManager.get("reports.reorder.emptyTitle"));
        emptyTitleLabel.setForeground(ThemeManager.textPrimary());
        emptySubLabel.setText(LanguageManager.get("reports.reorder.emptySub"));
        emptySubLabel.setForeground(ThemeManager.textSecondary());

        reorderCardLayout.show(reorderContentPanel, lastLowStockCount == 0 ? CARD_EMPTY : CARD_TABLE);

        categoryValuationChartPanel.refreshThemeAndLanguage();
        LanguageManager.applyOrientation(this);
        reorderTable.repaint();
    }

    private void styleKpiCard(JPanel card, JLabel titleLbl, JLabel valueLabel, JLabel iconBadge,
                              String titleText, Color accent, Color badgeBg, Icon icon, boolean rtl) {
        card.setBackground(ThemeManager.bgSurface());
        card.setBorder(BorderFactory.createCompoundBorder(
                ThemeManager.createDirectionalMatteBorder(0, 4, 0, 0, accent, rtl),
                BorderFactory.createCompoundBorder(
                        ThemeManager.createDirectionalMatteBorder(1, 0, 1, 1, ThemeManager.borderSubtle(), rtl),
                        new EmptyBorder(14, 16, 14, 16)
                )
        ));
        titleLbl.setText(titleText);
        titleLbl.setForeground(ThemeManager.textSecondary());
        valueLabel.setForeground(ThemeManager.textPrimary());
        iconBadge.setIcon(icon);
        iconBadge.setBackground(badgeBg);
    }

    public void updateReportData(List<Product> allProducts) {
        updateReportData(allProducts, Collections.emptyMap());
    }

    public void updateReportData(List<Product> allProducts, Map<Integer, String> categoryNames) {
        if (categoryValuationChartPanel != null) {
            categoryValuationChartPanel.setCategoryMetrics(allProducts, categoryNames);
        }
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalRetail = BigDecimal.ZERO;
        int lowStockCount = 0;

        reorderTableModel.setRowCount(0);

        if (allProducts != null) {
            for (Product p : allProducts) {
                BigDecimal qty = BigDecimal.valueOf(Math.max(0, p.getQuantityInStock()));
                BigDecimal cost = p.getPurchasePrice() != null ? p.getPurchasePrice() : BigDecimal.ZERO;
                BigDecimal sell = p.getUnitPrice() != null ? p.getUnitPrice() : BigDecimal.ZERO;

                totalCost = totalCost.add(cost.multiply(qty));
                totalRetail = totalRetail.add(sell.multiply(qty));

                if (p.isLowStock()) {
                    lowStockCount++;
                    int deficit = Math.max(0, p.getReorderLevel() - p.getQuantityInStock());
                    BigDecimal estRestockCost = cost.multiply(BigDecimal.valueOf(deficit));

                    reorderTableModel.addRow(new Object[]{
                            p.getId(),
                            p.getSku(),
                            p.getName(),
                            p.getQuantityInStock(),
                            p.getReorderLevel(),
                            deficit,
                            CURRENCY.format(cost),
                            CURRENCY.format(estRestockCost)
                    });
                }
            }
        }

        this.lastLowStockCount = lowStockCount;
        reorderCardLayout.show(reorderContentPanel, lowStockCount == 0 ? CARD_EMPTY : CARD_TABLE);

        BigDecimal projectedProfit = totalRetail.subtract(totalCost);
        totalCostValueLabel.setText(LanguageManager.isolateLTR(CURRENCY.format(totalCost)));
        totalRetailValueLabel.setText(LanguageManager.isolateLTR(CURRENCY.format(totalRetail)));
        projectedProfitLabel.setText(LanguageManager.isolateLTR(CURRENCY.format(projectedProfit)));
        lowStockCountLabel.setText(LanguageManager.isolateLTR(String.valueOf(lowStockCount)));
    }

    public JButton getRefreshReportsBtn() { return refreshReportsBtn; }
    public JButton getExportReportBtn() { return exportCsvBtn; }
    public JButton getExportCsvBtn() { return exportCsvBtn; }

    private static class ReorderCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            JLabel cell = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            cell.setBorder(new EmptyBorder(0, 12, 0, 12));

            boolean numericCol = column == 0 || column >= 3;
            cell.setHorizontalAlignment(numericCol ? SwingConstants.TRAILING : SwingConstants.LEADING);

            if ((numericCol || column == 1) && value != null) {
                cell.setText(LanguageManager.isolateLTR(value.toString()));
            }

            if (!isSelected) {
                cell.setBackground(row % 2 == 0 ? ThemeManager.bgSurface() : ThemeManager.bgSurfaceAlt());
                if (column == 5) {
                    cell.setForeground(ThemeManager.accentDanger());
                    cell.setFont(cell.getFont().deriveFont(Font.BOLD));
                } else if (column == 1) {
                    cell.setForeground(ThemeManager.textPrimary());
                    cell.setFont(cell.getFont().deriveFont(Font.BOLD));
                } else {
                    cell.setForeground(ThemeManager.textPrimary());
                }
            }
            return cell;
        }
    }
}
