package com.nexus.ims.view;

import com.nexus.ims.model.Product;
import com.nexus.ims.util.IconFactory;
import com.nexus.ims.util.LanguageManager;
import com.nexus.ims.util.ThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Horizontal grouped bar chart (Java2D) comparing inventory cost and retail valuation by category.
 */
public class CategoryValuationChartPanel extends JPanel {
    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(Locale.US);

    private record CategoryEntry(String name, BigDecimal costTotal, BigDecimal retailTotal, double costSharePct) {
    }

    private List<CategoryEntry> entries = new ArrayList<>();
    private List<Product> lastProducts = Collections.emptyList();
    private Map<Integer, String> lastCategoryNames = Collections.emptyMap();

    private final JLabel titleLabel;
    private final JLabel costLegendLabel;
    private final JLabel retailLegendLabel;
    private final JScrollPane scrollPane;
    private final ChartCanvas canvas;

    public CategoryValuationChartPanel() {
        setLayout(new BorderLayout(0, 8));

        // Header + Legend using directional BorderLayout & FlowLayout
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);

        titleLabel = new JLabel("", SwingConstants.LEADING);
        titleLabel.setIconTextGap(8);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        header.add(titleLabel, BorderLayout.LINE_START);

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.TRAILING, 14, 0));
        legend.setOpaque(false);
        costLegendLabel = new JLabel("", SwingConstants.LEADING);
        costLegendLabel.setIconTextGap(5);
        costLegendLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));

        retailLegendLabel = new JLabel("", SwingConstants.LEADING);
        retailLegendLabel.setIconTextGap(5);
        retailLegendLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));

        legend.add(costLegendLabel);
        legend.add(retailLegendLabel);
        header.add(legend, BorderLayout.LINE_END);

        add(header, BorderLayout.NORTH);

        canvas = new ChartCanvas();
        scrollPane = new JScrollPane(canvas);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

        refreshThemeAndLanguage();
    }

    public void refreshThemeAndLanguage() {
        Color costColor = ThemeManager.accentPrimary();
        Color retailColor = ThemeManager.accentSuccess();

        setBackground(ThemeManager.bgSurface());
        setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 1, 1, 1, ThemeManager.borderSubtle()),
                new EmptyBorder(14, 16, 12, 16)
        ));

        titleLabel.setText(LanguageManager.get("reports.chart.title"));
        titleLabel.setIcon(IconFactory.reports(16, costColor));
        titleLabel.setForeground(ThemeManager.textPrimary());

        costLegendLabel.setText(LanguageManager.get("reports.chart.legendCost"));
        costLegendLabel.setIcon(IconFactory.statusDot(10, costColor));
        costLegendLabel.setForeground(ThemeManager.textSecondary());

        retailLegendLabel.setText(LanguageManager.get("reports.chart.legendRetail"));
        retailLegendLabel.setIcon(IconFactory.statusDot(10, retailColor));
        retailLegendLabel.setForeground(ThemeManager.textSecondary());

        canvas.setBackground(ThemeManager.bgSurface());
        scrollPane.getViewport().setBackground(ThemeManager.bgSurface());

        rebuildEntries();
        LanguageManager.applyOrientation(this);
        canvas.repaint();
    }

    /**
     * Aggregates inventory cost and retail valuation grouped by product category.
     */
    public void setCategoryMetrics(List<Product> products, Map<Integer, String> categoryNames) {
        this.lastProducts = products != null ? products : Collections.emptyList();
        this.lastCategoryNames = categoryNames != null ? categoryNames : Collections.emptyMap();
        rebuildEntries();
    }

    private void rebuildEntries() {
        Map<String, BigDecimal[]> grouped = new LinkedHashMap<>();
        BigDecimal grandCost = BigDecimal.ZERO;
        String unassigned = LanguageManager.get("common.unassigned");

        for (Product p : lastProducts) {
            String catName = p.getCategoryId() != null
                    ? lastCategoryNames.getOrDefault(p.getCategoryId(), unassigned)
                    : unassigned;

            BigDecimal qty = BigDecimal.valueOf(Math.max(0, p.getQuantityInStock()));
            BigDecimal cost = (p.getPurchasePrice() != null ? p.getPurchasePrice() : BigDecimal.ZERO).multiply(qty);
            BigDecimal retail = (p.getUnitPrice() != null ? p.getUnitPrice() : BigDecimal.ZERO).multiply(qty);

            BigDecimal[] pair = grouped.computeIfAbsent(catName,
                    k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            pair[0] = pair[0].add(cost);
            pair[1] = pair[1].add(retail);
            grandCost = grandCost.add(cost);
        }

        List<CategoryEntry> computed = new ArrayList<>();
        double grandCostDouble = grandCost.doubleValue();
        for (Map.Entry<String, BigDecimal[]> entry : grouped.entrySet()) {
            BigDecimal c = entry.getValue()[0];
            BigDecimal r = entry.getValue()[1];
            double share = grandCostDouble > 0 ? (c.doubleValue() / grandCostDouble) * 100.0 : 0.0;
            computed.add(new CategoryEntry(entry.getKey(), c, r, share));
        }

        computed.sort(Comparator.comparing(CategoryEntry::retailTotal).reversed());
        this.entries = computed;

        int preferredHeight = Math.max(150, entries.size() * 36 + 28);
        canvas.setPreferredSize(new Dimension(600, preferredHeight));
        canvas.revalidate();
        canvas.repaint();
    }

    private class ChartCanvas extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int width = getWidth();
                int height = getHeight();
                boolean rtl = LanguageManager.isRTL();

                Color costColor = ThemeManager.accentPrimary();
                Color retailColor = ThemeManager.accentSuccess();
                Color gridColor = ThemeManager.chartGridLine();
                Color labelColor = ThemeManager.textPrimary();
                Color mutedColor = ThemeManager.textSecondary();

                if (entries.isEmpty()) {
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                    g2.setColor(mutedColor);
                    String emptyMsg = LanguageManager.get("reports.chart.empty");
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(emptyMsg, (width - fm.stringWidth(emptyMsg)) / 2, height / 2);
                    return;
                }

                double maxVal = 0.0;
                for (CategoryEntry e : entries) {
                    maxVal = Math.max(maxVal, Math.max(e.costTotal().doubleValue(), e.retailTotal().doubleValue()));
                }
                if (maxVal <= 0.0) {
                    maxVal = 100.0;
                } else {
                    maxVal *= 1.08;
                }

                int labelMargin = 205;
                int valueMargin = 115;
                int topMargin = 6;
                int plotWidth = Math.max(120, width - labelMargin - valueMargin);
                int plotStartX = rtl ? (width - labelMargin) : labelMargin;
                int rowHeight = 36;
                int plotBottom = topMargin + entries.size() * rowHeight;

                Font tickFont = new Font("Segoe UI", Font.PLAIN, 10);
                g2.setFont(tickFont);
                FontMetrics tickFm = g2.getFontMetrics();
                Stroke dashed = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                        10f, new float[]{3f, 3f}, 0f);
                Stroke solid = new BasicStroke(1f);

                int ticks = 4;
                for (int t = 0; t <= ticks; t++) {
                    double frac = t / (double) ticks;
                    int offset = (int) Math.round(frac * plotWidth);
                    int gx = rtl ? (plotStartX - offset) : (plotStartX + offset);

                    g2.setColor(gridColor);
                    g2.setStroke(t == 0 ? solid : dashed);
                    g2.drawLine(gx, topMargin, gx, plotBottom);

                    String tickLabel = LanguageManager.isolateLTR(CURRENCY.format(frac * maxVal));
                    g2.setColor(mutedColor);
                    int tw = tickFm.stringWidth(tickLabel);
                    g2.drawString(tickLabel, gx - tw / 2, plotBottom + 16);
                }

                Font catFont = new Font("Segoe UI", Font.BOLD, 12);
                Font valFont = new Font("Segoe UI", Font.BOLD, 10);

                for (int i = 0; i < entries.size(); i++) {
                    CategoryEntry entry = entries.get(i);
                    int rowTop = topMargin + i * rowHeight;

                    g2.setFont(catFont);
                    g2.setColor(labelColor);
                    FontMetrics catFm = g2.getFontMetrics();
                    String pct = LanguageManager.isolateLTR(String.format(Locale.US, "%.1f%%", entry.costSharePct()));
                    String labelText = entry.name() + " (" + pct + ")";
                    labelText = truncateToFit(labelText, catFm, labelMargin - 16);
                    int labelY = rowTop + 21;
                    if (rtl) {
                        int labelX = width - 8 - catFm.stringWidth(labelText);
                        g2.drawString(labelText, labelX, labelY);
                    } else {
                        g2.drawString(labelText, 4, labelY);
                    }

                    int barHeight = 11;
                    int barY1 = rowTop + 5;
                    int barY2 = barY1 + barHeight + 2;

                    double costW = (entry.costTotal().doubleValue() / maxVal) * plotWidth;
                    double retailW = (entry.retailTotal().doubleValue() / maxVal) * plotWidth;
                    int cBarW = entry.costTotal().compareTo(BigDecimal.ZERO) > 0 ? Math.max(4, (int) Math.round(costW)) : 0;
                    int rBarW = entry.retailTotal().compareTo(BigDecimal.ZERO) > 0 ? Math.max(4, (int) Math.round(retailW)) : 0;

                    g2.setFont(valFont);
                    FontMetrics valFm = g2.getFontMetrics();

                    String costStr = LanguageManager.isolateLTR(CURRENCY.format(entry.costTotal()));
                    g2.setColor(costColor);
                    if (cBarW > 0) {
                        int cX = rtl ? (plotStartX - cBarW) : plotStartX;
                        g2.fill(new RoundRectangle2D.Float(cX, barY1, cBarW, barHeight, 5, 5));
                    }
                    int cTextX = rtl
                            ? (plotStartX - cBarW - 6 - valFm.stringWidth(costStr))
                            : (plotStartX + cBarW + 6);
                    g2.drawString(costStr, cTextX, barY1 + 10);

                    String retailStr = LanguageManager.isolateLTR(CURRENCY.format(entry.retailTotal()));
                    g2.setColor(retailColor);
                    if (rBarW > 0) {
                        int rX = rtl ? (plotStartX - rBarW) : plotStartX;
                        g2.fill(new RoundRectangle2D.Float(rX, barY2, rBarW, barHeight, 5, 5));
                    }
                    int rTextX = rtl
                            ? (plotStartX - rBarW - 6 - valFm.stringWidth(retailStr))
                            : (plotStartX + rBarW + 6);
                    g2.drawString(retailStr, rTextX, barY2 + 10);
                }
            } finally {
                g2.dispose();
            }
        }

        private String truncateToFit(String text, FontMetrics fm, int maxWidth) {
            if (fm.stringWidth(text) <= maxWidth) {
                return text;
            }
            String ellipsis = "...";
            int limit = maxWidth - fm.stringWidth(ellipsis);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < text.length(); i++) {
                if (fm.stringWidth(sb.toString() + text.charAt(i)) > limit) {
                    break;
                }
                sb.append(text.charAt(i));
            }
            return sb + ellipsis;
        }
    }
}
