package com.nexus.ims.view;

import com.nexus.ims.model.Product;
import com.nexus.ims.util.IconFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Financial & Inventory Reports view featuring 4 valuation KPI cards,
 * low-stock reorder deficit analysis table, and CSV report export.
 */
public class ReportsPanel extends JPanel {
    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(Locale.US);

    private JLabel totalCostValueLabel;
    private JLabel totalRetailValueLabel;
    private JLabel projectedProfitLabel;
    private JLabel lowStockCountLabel;

    private JButton refreshReportsBtn;
    private JButton exportCsvBtn;
    private JTable reorderTable;
    private DefaultTableModel reorderTableModel;

    public ReportsPanel() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(16, 16));
        setOpaque(false);
        setBorder(new EmptyBorder(18, 20, 18, 20));

        // 1. Top 4 Financial KPI Cards
        JPanel kpiGrid = new JPanel(new GridLayout(1, 4, 16, 0));
        kpiGrid.setOpaque(false);
        kpiGrid.setPreferredSize(new Dimension(0, 112));

        totalCostValueLabel = new JLabel("$0.00");
        totalRetailValueLabel = new JLabel("$0.00");
        projectedProfitLabel = new JLabel("$0.00");
        lowStockCountLabel = new JLabel("0");

        kpiGrid.add(createReportKpiCard("TOTAL INVENTORY COST", totalCostValueLabel,
                new Color(13, 110, 253), new Color(231, 241, 255), IconFactory.products(20, new Color(13, 110, 253))));
        kpiGrid.add(createReportKpiCard("TOTAL RETAIL VALUE", totalRetailValueLabel,
                new Color(99, 102, 241), new Color(238, 242, 255), IconFactory.reports(20, new Color(99, 102, 241))));
        kpiGrid.add(createReportKpiCard("PROJECTED GROSS PROFIT", projectedProfitLabel,
                new Color(25, 135, 84), new Color(220, 252, 231), IconFactory.stockIn(20, new Color(25, 135, 84))));
        kpiGrid.add(createReportKpiCard("LOW-STOCK ITEMS", lowStockCountLabel,
                new Color(220, 53, 69), new Color(254, 226, 226), IconFactory.alert(20, new Color(220, 53, 69))));

        add(kpiGrid, BorderLayout.NORTH);

        // 2. Center Reorder Action Schedule Panel
        JPanel tableContainer = new JPanel(new BorderLayout(10, 10));
        tableContainer.setBackground(Color.WHITE);
        tableContainer.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 1, 1, 1, new Color(222, 226, 230)),
                new EmptyBorder(16, 16, 16, 16)
        ));

        JPanel sectionHeader = new JPanel(new BorderLayout(10, 0));
        sectionHeader.setOpaque(false);

        JLabel sectionTitle = new JLabel("Reorder Action Schedule (Low-Stock & Deficit Analysis)");
        sectionTitle.setIcon(IconFactory.alert(16, new Color(220, 53, 69)));
        sectionTitle.setIconTextGap(8);
        sectionTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        sectionTitle.setForeground(new Color(33, 37, 41));
        sectionHeader.add(sectionTitle, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        refreshReportsBtn = new JButton("Refresh", IconFactory.refresh(15, new Color(73, 80, 87)));
        refreshReportsBtn.setIconTextGap(6);

        exportCsvBtn = new JButton("Export Report", IconFactory.exportDropdown(15, Color.WHITE));
        exportCsvBtn.setIconTextGap(8);
        exportCsvBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        exportCsvBtn.setBackground(new Color(25, 135, 84));
        exportCsvBtn.setForeground(Color.WHITE);

        actions.add(refreshReportsBtn);
        actions.add(exportCsvBtn);
        sectionHeader.add(actions, BorderLayout.EAST);

        tableContainer.add(sectionHeader, BorderLayout.NORTH);

        String[] columns = {
                "ID", "SKU", "Product Name", "Current Stock", "Reorder Level",
                "Deficit Units", "Unit Cost", "Est. Restock Cost"
        };
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

        TableColumnModel cm = reorderTable.getColumnModel();
        cm.getColumn(0).setPreferredWidth(50);
        cm.getColumn(0).setMaxWidth(70);
        cm.getColumn(1).setPreferredWidth(130);
        cm.getColumn(2).setPreferredWidth(250);

        JScrollPane scrollPane = new JScrollPane(reorderTable);
        scrollPane.setBorder(new MatteBorder(1, 1, 1, 1, new Color(222, 226, 230)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        tableContainer.add(scrollPane, BorderLayout.CENTER);

        add(tableContainer, BorderLayout.CENTER);
    }

    private JPanel createReportKpiCard(String title, JLabel valueLabel, Color accent, Color badgeBg, Icon icon) {
        JPanel card = new JPanel(new BorderLayout(12, 6));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 4, 0, 0, accent),
                BorderFactory.createCompoundBorder(
                        new MatteBorder(1, 0, 1, 1, new Color(222, 226, 230)),
                        new EmptyBorder(14, 16, 14, 16)
                )
        ));

        JPanel textCol = new JPanel();
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
        textCol.setOpaque(false);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        titleLbl.setForeground(new Color(108, 117, 125));

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueLabel.setForeground(new Color(33, 37, 41));

        textCol.add(titleLbl);
        textCol.add(Box.createVerticalStrut(6));
        textCol.add(valueLabel);

        JLabel iconBadge = new JLabel(icon, SwingConstants.CENTER);
        iconBadge.setOpaque(true);
        iconBadge.setBackground(badgeBg);
        iconBadge.setPreferredSize(new Dimension(42, 42));

        JPanel badgeWrapper = new JPanel(new GridBagLayout());
        badgeWrapper.setOpaque(false);
        badgeWrapper.add(iconBadge);

        card.add(textCol, BorderLayout.CENTER);
        card.add(badgeWrapper, BorderLayout.EAST);
        return card;
    }

    /**
     * Recomputes financial KPIs and populates the low-stock reorder action schedule.
     */
    public void updateReportData(List<Product> allProducts) {
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

        BigDecimal projectedProfit = totalRetail.subtract(totalCost);
        totalCostValueLabel.setText(CURRENCY.format(totalCost));
        totalRetailValueLabel.setText(CURRENCY.format(totalRetail));
        projectedProfitLabel.setText(CURRENCY.format(projectedProfit));
        lowStockCountLabel.setText(String.valueOf(lowStockCount));
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
            cell.setHorizontalAlignment(numericCol ? SwingConstants.RIGHT : SwingConstants.LEFT);

            if (!isSelected) {
                cell.setBackground(row % 2 == 0 ? Color.WHITE : new Color(254, 249, 249));
                if (column == 5) {
                    cell.setForeground(new Color(185, 28, 28));
                    cell.setFont(cell.getFont().deriveFont(Font.BOLD));
                } else if (column == 1) {
                    cell.setForeground(new Color(33, 37, 41));
                    cell.setFont(cell.getFont().deriveFont(Font.BOLD));
                } else {
                    cell.setForeground(new Color(33, 37, 41));
                }
            }
            return cell;
        }
    }
}
