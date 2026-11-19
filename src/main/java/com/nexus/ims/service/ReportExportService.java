package com.nexus.ims.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.nexus.ims.model.Product;
import com.nexus.ims.model.StockTransaction;
import com.nexus.ims.model.Supplier;
import com.nexus.ims.model.User;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Centralized multi-format report export engine supporting native PDF (OpenPDF),
 * multi-sheet Excel .xlsx (Apache POI), UTF-8 BOM CSV, and pretty-printed JSON.
 */
public class ReportExportService {
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(Locale.US);

    public enum ExportFormat {
        PDF("PDF Document (*.pdf)", "pdf"),
        EXCEL("Excel Workbook (*.xlsx)", "xlsx"),
        CSV("CSV Spreadsheet (*.csv)", "csv"),
        JSON("JSON Data (*.json)", "json");

        private final String description;
        private final String extension;

        ExportFormat(String description, String extension) {
            this.description = description;
            this.extension = extension;
        }

        public String getDescription() {
            return description;
        }

        public String getExtension() {
            return extension;
        }
    }

    private record ValuationSummary(
            int totalItems,
            int totalUnits,
            int lowStockCount,
            BigDecimal totalCost,
            BigDecimal totalRetail,
            BigDecimal projectedProfit,
            List<Product> lowStockProducts
    ) {}

    public void exportReport(ExportFormat format, File file, User currentUser,
                             List<Product> products, List<StockTransaction> transactions,
                             List<Supplier> suppliers) throws Exception {
        if (format == null || file == null) {
            throw new IllegalArgumentException("Export format and target file must not be null.");
        }

        List<Product> safeProducts = products != null ? products : Collections.emptyList();
        List<StockTransaction> safeTransactions = transactions != null ? transactions : Collections.emptyList();
        List<Supplier> safeSuppliers = suppliers != null ? suppliers : Collections.emptyList();

        switch (format) {
            case PDF -> exportToPdf(file, currentUser, safeProducts, safeTransactions, safeSuppliers);
            case EXCEL -> exportToExcel(file, currentUser, safeProducts, safeTransactions, safeSuppliers);
            case CSV -> exportToCsv(file, safeProducts);
            case JSON -> exportToJson(file, currentUser, safeProducts, safeTransactions, safeSuppliers);
        }
    }

    // =========================================================================
    // 1. Native PDF Export (LibrePDF OpenPDF)
    // =========================================================================

    public void exportToPdf(File file, User currentUser, List<Product> products,
                            List<StockTransaction> transactions, List<Supplier> suppliers) throws Exception {
        ValuationSummary summary = computeSummary(products);
        Map<Integer, Product> productMap = buildProductMap(products);
        String generatedAt = LocalDateTime.now().format(TS_FORMAT);
        String operator = currentUser != null
                ? currentUser.getFullName() + " (" + currentUser.getRole().name() + ")"
                : "System Operator";

        Document document = new Document(PageSize.A4.rotate(), 28, 28, 28, 40);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            PdfWriter writer = PdfWriter.getInstance(document, fos);
            writer.setPageEvent(new PdfFooterHelper(generatedAt));
            document.open();

            com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.WHITE);
            com.lowagie.text.Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(203, 213, 225));
            com.lowagie.text.Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(30, 41, 59));
            com.lowagie.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            com.lowagie.text.Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, new Color(30, 41, 59));
            com.lowagie.text.Font boldBodyFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, new Color(30, 41, 59));
            com.lowagie.text.Font alertFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, new Color(185, 28, 28));
            com.lowagie.text.Font okFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, new Color(21, 128, 61));

            // Branded Header Banner
            PdfPTable banner = new PdfPTable(1);
            banner.setWidthPercentage(100);
            banner.setSpacingAfter(14f);
            PdfPCell bannerCell = new PdfPCell();
            bannerCell.setBackgroundColor(new Color(24, 28, 36));
            bannerCell.setBorder(PdfPCell.NO_BORDER);
            bannerCell.setPadding(14f);
            bannerCell.addElement(new Paragraph("NEXUS IMS - Financial & Inventory Valuation Report", titleFont));
            Paragraph metaPara = new Paragraph(
                    "Generated: " + generatedAt + "   |   Exported By: " + operator
                    + "   |   Active Suppliers: " + suppliers.size(), subTitleFont);
            metaPara.setSpacingBefore(4f);
            bannerCell.addElement(metaPara);
            banner.addCell(bannerCell);
            document.add(banner);

            // Financial KPI Summary Table
            Paragraph kpiTitle = new Paragraph("Executive Financial KPI Summary", sectionFont);
            kpiTitle.setSpacingAfter(6f);
            document.add(kpiTitle);

            PdfPTable kpiTable = new PdfPTable(4);
            kpiTable.setWidthPercentage(100);
            kpiTable.setSpacingAfter(16f);
            addKpiPdfCell(kpiTable, "TOTAL INVENTORY COST", CURRENCY.format(summary.totalCost()), new Color(13, 110, 253));
            addKpiPdfCell(kpiTable, "TOTAL RETAIL VALUE", CURRENCY.format(summary.totalRetail()), new Color(79, 70, 229));
            addKpiPdfCell(kpiTable, "PROJECTED GROSS PROFIT", CURRENCY.format(summary.projectedProfit()), new Color(25, 135, 84));
            addKpiPdfCell(kpiTable, "LOW-STOCK ALERTS",
                    summary.lowStockCount() + " / " + summary.totalItems() + " SKUs", new Color(220, 53, 69));
            document.add(kpiTable);

            // Low-Stock Reorder Alert Schedule
            Paragraph lowTitle = new Paragraph("Low-Stock Reorder Alert Schedule", sectionFont);
            lowTitle.setSpacingAfter(6f);
            document.add(lowTitle);

            PdfPTable lowTable = new PdfPTable(new float[]{0.8f, 1.8f, 3.4f, 1.2f, 1.2f, 1.2f, 1.4f, 1.6f});
            lowTable.setWidthPercentage(100);
            lowTable.setHeaderRows(1);
            lowTable.setSpacingAfter(16f);
            String[] lowHeaders = {"ID", "SKU", "Product Name", "Stock", "Reorder", "Deficit", "Unit Cost", "Est. Restock"};
            for (String h : lowHeaders) {
                lowTable.addCell(createPdfHeaderCell(h, headerFont, new Color(185, 28, 28)));
            }

            if (summary.lowStockProducts().isEmpty()) {
                PdfPCell emptyCell = new PdfPCell(new Phrase("All catalog items are stocked above reorder levels.", okFont));
                emptyCell.setColspan(8);
                emptyCell.setPadding(8f);
                emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                emptyCell.setBorderColor(new Color(226, 232, 240));
                lowTable.addCell(emptyCell);
            } else {
                for (int i = 0; i < summary.lowStockProducts().size(); i++) {
                    Product p = summary.lowStockProducts().get(i);
                    Color rowBg = i % 2 == 0 ? Color.WHITE : new Color(254, 247, 247);
                    BigDecimal cost = safeBigDecimal(p.getPurchasePrice());
                    int deficit = Math.max(0, p.getReorderLevel() - p.getQuantityInStock());
                    BigDecimal restockCost = cost.multiply(BigDecimal.valueOf(deficit));

                    lowTable.addCell(createPdfCell(String.valueOf(p.getId()), bodyFont, rowBg, Element.ALIGN_RIGHT));
                    lowTable.addCell(createPdfCell(safeStr(p.getSku()), boldBodyFont, rowBg, Element.ALIGN_LEFT));
                    lowTable.addCell(createPdfCell(safeStr(p.getName()), bodyFont, rowBg, Element.ALIGN_LEFT));
                    lowTable.addCell(createPdfCell(String.valueOf(p.getQuantityInStock()), alertFont, rowBg, Element.ALIGN_RIGHT));
                    lowTable.addCell(createPdfCell(String.valueOf(p.getReorderLevel()), bodyFont, rowBg, Element.ALIGN_RIGHT));
                    lowTable.addCell(createPdfCell(String.valueOf(deficit), alertFont, rowBg, Element.ALIGN_RIGHT));
                    lowTable.addCell(createPdfCell(CURRENCY.format(cost), bodyFont, rowBg, Element.ALIGN_RIGHT));
                    lowTable.addCell(createPdfCell(CURRENCY.format(restockCost), boldBodyFont, rowBg, Element.ALIGN_RIGHT));
                }
            }
            document.add(lowTable);

            // Full Product Catalog Table
            Paragraph catTitle = new Paragraph("Full Product Catalog Valuation (" + products.size() + " Items)", sectionFont);
            catTitle.setSpacingAfter(6f);
            document.add(catTitle);

            PdfPTable catTable = new PdfPTable(new float[]{0.7f, 1.8f, 3.2f, 1.2f, 1.2f, 1.0f, 1.0f, 1.5f, 1.3f});
            catTable.setWidthPercentage(100);
            catTable.setHeaderRows(1);
            catTable.setSpacingAfter(16f);
            String[] catHeaders = {"ID", "SKU", "Product Name", "Unit Cost", "Sell Price", "Qty", "Min", "Cost Value", "Status"};
            for (String h : catHeaders) {
                catTable.addCell(createPdfHeaderCell(h, headerFont, new Color(30, 41, 59)));
            }

            for (int i = 0; i < products.size(); i++) {
                Product p = products.get(i);
                boolean low = p.isLowStock();
                Color rowBg = low ? new Color(254, 242, 242) : (i % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                BigDecimal cost = safeBigDecimal(p.getPurchasePrice());
                BigDecimal sell = safeBigDecimal(p.getUnitPrice());
                BigDecimal totalCostVal = cost.multiply(BigDecimal.valueOf(Math.max(0, p.getQuantityInStock())));

                catTable.addCell(createPdfCell(String.valueOf(p.getId()), bodyFont, rowBg, Element.ALIGN_RIGHT));
                catTable.addCell(createPdfCell(safeStr(p.getSku()), boldBodyFont, rowBg, Element.ALIGN_LEFT));
                catTable.addCell(createPdfCell(safeStr(p.getName()), bodyFont, rowBg, Element.ALIGN_LEFT));
                catTable.addCell(createPdfCell(CURRENCY.format(cost), bodyFont, rowBg, Element.ALIGN_RIGHT));
                catTable.addCell(createPdfCell(CURRENCY.format(sell), bodyFont, rowBg, Element.ALIGN_RIGHT));
                catTable.addCell(createPdfCell(String.valueOf(p.getQuantityInStock()), bodyFont, rowBg, Element.ALIGN_RIGHT));
                catTable.addCell(createPdfCell(String.valueOf(p.getReorderLevel()), bodyFont, rowBg, Element.ALIGN_RIGHT));
                catTable.addCell(createPdfCell(CURRENCY.format(totalCostVal), boldBodyFont, rowBg, Element.ALIGN_RIGHT));
                catTable.addCell(createPdfCell(low ? "LOW STOCK" : "IN STOCK", low ? alertFont : okFont, rowBg, Element.ALIGN_CENTER));
            }
            document.add(catTable);

            // Recent Stock Audit Ledger
            if (!transactions.isEmpty()) {
                Paragraph txTitle = new Paragraph("Recent Stock Audit Movements", sectionFont);
                txTitle.setSpacingAfter(6f);
                document.add(txTitle);

                PdfPTable txTable = new PdfPTable(new float[]{0.7f, 2.8f, 1.9f, 1.0f, 3.0f, 1.8f});
                txTable.setWidthPercentage(100);
                txTable.setHeaderRows(1);
                String[] txHeaders = {"ID", "Product", "Movement Type", "Delta", "Notes", "Timestamp"};
                for (String h : txHeaders) {
                    txTable.addCell(createPdfHeaderCell(h, headerFont, new Color(51, 65, 85)));
                }

                int maxRows = Math.min(30, transactions.size());
                for (int i = 0; i < maxRows; i++) {
                    StockTransaction tx = transactions.get(i);
                    Color rowBg = i % 2 == 0 ? Color.WHITE : new Color(248, 250, 252);
                    int delta = tx.getQuantityChange();
                    String deltaText = (delta > 0 ? "+" : "") + delta;

                    txTable.addCell(createPdfCell(String.valueOf(tx.getId()), bodyFont, rowBg, Element.ALIGN_RIGHT));
                    txTable.addCell(createPdfCell(resolveProductLabel(tx.getProductId(), productMap), bodyFont, rowBg, Element.ALIGN_LEFT));
                    txTable.addCell(createPdfCell(tx.getTransactionType().name(), bodyFont, rowBg, Element.ALIGN_LEFT));
                    txTable.addCell(createPdfCell(deltaText, delta > 0 ? okFont : alertFont, rowBg, Element.ALIGN_RIGHT));
                    txTable.addCell(createPdfCell(tx.getNotes() != null ? tx.getNotes() : "-", bodyFont, rowBg, Element.ALIGN_LEFT));
                    txTable.addCell(createPdfCell(formatTimestamp(tx.getCreatedAt()), bodyFont, rowBg, Element.ALIGN_LEFT));
                }
                document.add(txTable);
            }

            document.close();
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }

    private void addKpiPdfCell(PdfPTable table, String label, String value, Color accent) {
        com.lowagie.text.Font lblFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new Color(100, 116, 139));
        com.lowagie.text.Font valFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, accent);

        PdfPCell cell = new PdfPCell();
        cell.setPadding(10f);
        cell.setBackgroundColor(new Color(248, 250, 252));
        cell.setBorderColor(new Color(226, 232, 240));
        cell.addElement(new Paragraph(label, lblFont));
        Paragraph pVal = new Paragraph(value, valFont);
        pVal.setSpacingBefore(3f);
        cell.addElement(pVal);
        table.addCell(cell);
    }

    private PdfPCell createPdfHeaderCell(String text, com.lowagie.text.Font font, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(7f);
        cell.setBorderColor(bg);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private PdfPCell createPdfCell(String text, com.lowagie.text.Font font, Color bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(5.5f);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorderColor(new Color(226, 232, 240));
        return cell;
    }

    private static class PdfFooterHelper extends PdfPageEventHelper {
        private final String generatedAt;
        private final com.lowagie.text.Font footerFont =
                FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(100, 116, 139));

        PdfFooterHelper(String generatedAt) {
            this.generatedAt = generatedAt;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase("NEXUS IMS - Financial & Inventory Valuation Report (" + generatedAt + ")", footerFont),
                    document.leftMargin(), document.bottom() - 18, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    new Phrase("Page " + writer.getPageNumber(), footerFont),
                    document.right(), document.bottom() - 18, 0);
        }
    }

    // =========================================================================
    // 2. Native Excel .xlsx Export (Apache POI XSSFWorkbook)
    // =========================================================================

    public void exportToExcel(File file, User currentUser, List<Product> products,
                              List<StockTransaction> transactions, List<Supplier> suppliers) throws IOException {
        ValuationSummary summary = computeSummary(products);
        Map<Integer, Product> productMap = buildProductMap(products);
        String generatedAt = LocalDateTime.now().format(TS_FORMAT);
        String operator = currentUser != null
                ? currentUser.getFullName() + " (" + currentUser.getRole().name() + ")"
                : "System Operator";

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            DataFormat dataFormat = workbook.createDataFormat();

            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());

            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            org.apache.poi.ss.usermodel.Font boldFont = workbook.createFont();
            boldFont.setBold(true);

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.LEFT);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle textStyle = createBorderedStyle(workbook);
            textStyle.setAlignment(HorizontalAlignment.LEFT);

            CellStyle boldTextStyle = createBorderedStyle(workbook);
            boldTextStyle.setFont(boldFont);

            CellStyle intStyle = createBorderedStyle(workbook);
            intStyle.setDataFormat(dataFormat.getFormat("#,##0"));
            intStyle.setAlignment(HorizontalAlignment.RIGHT);

            CellStyle signedIntStyle = createBorderedStyle(workbook);
            signedIntStyle.setDataFormat(dataFormat.getFormat("+#,##0;-#,##0;0"));
            signedIntStyle.setAlignment(HorizontalAlignment.RIGHT);

            CellStyle currencyStyle = createBorderedStyle(workbook);
            currencyStyle.setDataFormat(dataFormat.getFormat("$#,##0.00"));
            currencyStyle.setAlignment(HorizontalAlignment.RIGHT);

            CellStyle boldCurrencyStyle = createBorderedStyle(workbook);
            boldCurrencyStyle.setFont(boldFont);
            boldCurrencyStyle.setDataFormat(dataFormat.getFormat("$#,##0.00"));
            boldCurrencyStyle.setAlignment(HorizontalAlignment.RIGHT);

            // Sheet 1: Executive Summary
            Sheet summarySheet = workbook.createSheet("Executive Summary");
            int r = 0;
            Row titleRow = summarySheet.createRow(r++);
            Cell tCell = titleRow.createCell(0);
            tCell.setCellValue("NEXUS IMS - Executive Inventory Valuation Summary");
            tCell.setCellStyle(titleStyle);

            createKvRow(summarySheet, r++, "Generated At", generatedAt, boldTextStyle, textStyle);
            createKvRow(summarySheet, r++, "Exported By", operator, boldTextStyle, textStyle);
            createKvRow(summarySheet, r++, "Registered Suppliers", suppliers.size(), boldTextStyle, intStyle);
            r++;

            Row kpiHdr = summarySheet.createRow(r++);
            kpiHdr.setHeightInPoints(24);
            createStyledCell(kpiHdr, 0, "Metric", headerStyle);
            createStyledCell(kpiHdr, 1, "Value", headerStyle);

            createKvRow(summarySheet, r++, "Total Catalog Items (SKUs)", summary.totalItems(), boldTextStyle, intStyle);
            createKvRow(summarySheet, r++, "Total Units in Warehouse", summary.totalUnits(), boldTextStyle, intStyle);
            createKvRow(summarySheet, r++, "Low-Stock Items Requiring Attention", summary.lowStockCount(), boldTextStyle, intStyle);
            createKvRow(summarySheet, r++, "Total Inventory Cost", summary.totalCost().doubleValue(), boldTextStyle, boldCurrencyStyle);
            createKvRow(summarySheet, r++, "Total Retail Value", summary.totalRetail().doubleValue(), boldTextStyle, boldCurrencyStyle);
            createKvRow(summarySheet, r++, "Projected Gross Profit", summary.projectedProfit().doubleValue(), boldTextStyle, boldCurrencyStyle);
            autoSizeColumns(summarySheet, 2);

            // Sheet 2: Product Catalog
            Sheet catalogSheet = workbook.createSheet("Product Catalog");
            String[] catCols = {"ID", "SKU", "Product Name", "Category ID", "Supplier ID",
                    "Purchase Cost", "Unit Price", "Stock Qty", "Reorder Level", "Status",
                    "Total Cost Value", "Total Retail Value"};
            createHeaderRow(catalogSheet, catCols, headerStyle);

            int catRowIdx = 1;
            for (Product p : products) {
                Row row = catalogSheet.createRow(catRowIdx++);
                BigDecimal cost = safeBigDecimal(p.getPurchasePrice());
                BigDecimal sell = safeBigDecimal(p.getUnitPrice());
                BigDecimal qty = BigDecimal.valueOf(Math.max(0, p.getQuantityInStock()));

                createNumericCell(row, 0, p.getId(), intStyle);
                createStyledCell(row, 1, safeStr(p.getSku()), boldTextStyle);
                createStyledCell(row, 2, safeStr(p.getName()), textStyle);
                createStyledCell(row, 3, p.getCategoryId() != null ? String.valueOf(p.getCategoryId()) : "", textStyle);
                createStyledCell(row, 4, p.getSupplierId() != null ? String.valueOf(p.getSupplierId()) : "", textStyle);
                createNumericCell(row, 5, cost.doubleValue(), currencyStyle);
                createNumericCell(row, 6, sell.doubleValue(), currencyStyle);
                createNumericCell(row, 7, p.getQuantityInStock(), intStyle);
                createNumericCell(row, 8, p.getReorderLevel(), intStyle);
                createStyledCell(row, 9, p.isLowStock() ? "LOW STOCK" : "IN STOCK", textStyle);
                createNumericCell(row, 10, cost.multiply(qty).doubleValue(), currencyStyle);
                createNumericCell(row, 11, sell.multiply(qty).doubleValue(), currencyStyle);
            }

            Row totalRow = catalogSheet.createRow(catRowIdx);
            createStyledCell(totalRow, 1, "TOTAL", boldTextStyle);
            createNumericCell(totalRow, 7, summary.totalUnits(), intStyle);
            createNumericCell(totalRow, 10, summary.totalCost().doubleValue(), boldCurrencyStyle);
            createNumericCell(totalRow, 11, summary.totalRetail().doubleValue(), boldCurrencyStyle);
            autoSizeColumns(catalogSheet, catCols.length);

            // Sheet 3: Low Stock Alerts
            Sheet lowSheet = workbook.createSheet("Low Stock Alerts");
            String[] lowCols = {"ID", "SKU", "Product Name", "Current Stock", "Reorder Level", "Deficit Units", "Unit Cost", "Est. Restock Cost"};
            createHeaderRow(lowSheet, lowCols, headerStyle);

            int lowRowIdx = 1;
            for (Product p : summary.lowStockProducts()) {
                Row row = lowSheet.createRow(lowRowIdx++);
                BigDecimal cost = safeBigDecimal(p.getPurchasePrice());
                int deficit = Math.max(0, p.getReorderLevel() - p.getQuantityInStock());

                createNumericCell(row, 0, p.getId(), intStyle);
                createStyledCell(row, 1, safeStr(p.getSku()), boldTextStyle);
                createStyledCell(row, 2, safeStr(p.getName()), textStyle);
                createNumericCell(row, 3, p.getQuantityInStock(), intStyle);
                createNumericCell(row, 4, p.getReorderLevel(), intStyle);
                createNumericCell(row, 5, deficit, intStyle);
                createNumericCell(row, 6, cost.doubleValue(), currencyStyle);
                createNumericCell(row, 7, cost.multiply(BigDecimal.valueOf(deficit)).doubleValue(), currencyStyle);
            }
            autoSizeColumns(lowSheet, lowCols.length);

            // Sheet 4: Stock Ledger
            Sheet ledgerSheet = workbook.createSheet("Stock Ledger");
            String[] ledgerCols = {"ID", "Product ID", "Product", "Movement Type", "Quantity Delta", "Operator ID", "Timestamp", "Notes"};
            createHeaderRow(ledgerSheet, ledgerCols, headerStyle);

            int txRowIdx = 1;
            for (StockTransaction tx : transactions) {
                Row row = ledgerSheet.createRow(txRowIdx++);
                createNumericCell(row, 0, tx.getId(), intStyle);
                createNumericCell(row, 1, tx.getProductId(), intStyle);
                createStyledCell(row, 2, resolveProductLabel(tx.getProductId(), productMap), textStyle);
                createStyledCell(row, 3, tx.getTransactionType().name(), textStyle);
                createNumericCell(row, 4, tx.getQuantityChange(), signedIntStyle);
                createStyledCell(row, 5, tx.getPerformedByUserId() != null ? String.valueOf(tx.getPerformedByUserId()) : "System", textStyle);
                createStyledCell(row, 6, formatTimestamp(tx.getCreatedAt()), textStyle);
                createStyledCell(row, 7, tx.getNotes() != null ? tx.getNotes() : "", textStyle);
            }
            autoSizeColumns(ledgerSheet, ledgerCols.length);

            try (FileOutputStream out = new FileOutputStream(file)) {
                workbook.write(out);
            }
        }
    }

    private CellStyle createBorderedStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private void createHeaderRow(Sheet sheet, String[] headers, CellStyle headerStyle) {
        Row row = sheet.createRow(0);
        row.setHeightInPoints(24);
        for (int i = 0; i < headers.length; i++) {
            createStyledCell(row, i, headers[i], headerStyle);
        }
    }

    private void createKvRow(Sheet sheet, int rowIdx, String key, String val, CellStyle kStyle, CellStyle vStyle) {
        Row row = sheet.createRow(rowIdx);
        createStyledCell(row, 0, key, kStyle);
        createStyledCell(row, 1, val, vStyle);
    }

    private void createKvRow(Sheet sheet, int rowIdx, String key, double val, CellStyle kStyle, CellStyle vStyle) {
        Row row = sheet.createRow(rowIdx);
        createStyledCell(row, 0, key, kStyle);
        createNumericCell(row, 1, val, vStyle);
    }

    private void createStyledCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void createNumericCell(Row row, int col, double value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void autoSizeColumns(Sheet sheet, int colCount) {
        for (int i = 0; i < colCount; i++) {
            sheet.autoSizeColumn(i);
            int width = sheet.getColumnWidth(i) + 768;
            sheet.setColumnWidth(i, Math.min(width, 255 * 256));
        }
    }

    // =========================================================================
    // 3. UTF-8 with BOM CSV Export
    // =========================================================================

    public void exportToCsv(File file, List<Product> products) throws IOException {
        ValuationSummary summary = computeSummary(products);

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            // UTF-8 Byte Order Mark so Microsoft Excel renders UTF-8 (English/Arabic) cleanly
            writer.write('\uFEFF');
            writer.write("ID,SKU,Product Name,Category ID,Supplier ID,Purchase Cost,Unit Price,Stock Qty,Reorder Level,Status,Total Cost Value,Total Retail Value");
            writer.newLine();

            for (Product p : products) {
                BigDecimal qty = BigDecimal.valueOf(Math.max(0, p.getQuantityInStock()));
                BigDecimal cost = safeBigDecimal(p.getPurchasePrice());
                BigDecimal sell = safeBigDecimal(p.getUnitPrice());
                BigDecimal totalCost = cost.multiply(qty);
                BigDecimal totalRetail = sell.multiply(qty);
                String status = p.isLowStock() ? "LOW STOCK" : "IN STOCK";

                writer.write(String.join(",",
                        String.valueOf(p.getId()),
                        csvEscape(p.getSku()),
                        csvEscape(p.getName()),
                        p.getCategoryId() != null ? String.valueOf(p.getCategoryId()) : "",
                        p.getSupplierId() != null ? String.valueOf(p.getSupplierId()) : "",
                        cost.toPlainString(),
                        sell.toPlainString(),
                        String.valueOf(p.getQuantityInStock()),
                        String.valueOf(p.getReorderLevel()),
                        status,
                        totalCost.toPlainString(),
                        totalRetail.toPlainString()
                ));
                writer.newLine();
            }

            writer.write(String.join(",",
                    "",
                    "\"TOTAL\"",
                    "\"Warehouse Summary\"",
                    "", "", "", "",
                    String.valueOf(summary.totalUnits()),
                    "",
                    "\"" + summary.lowStockCount() + " Low Stock\"",
                    summary.totalCost().toPlainString(),
                    summary.totalRetail().toPlainString()
            ));
            writer.newLine();
        }
    }

    private String csvEscape(String raw) {
        if (raw == null) return "\"\"";
        return "\"" + raw.replace("\"", "\"\"") + "\"";
    }

    // =========================================================================
    // 4. Pretty-Printed JSON Export
    // =========================================================================

    public void exportToJson(File file, User currentUser, List<Product> products,
                             List<StockTransaction> transactions, List<Supplier> suppliers) throws IOException {
        ValuationSummary summary = computeSummary(products);
        Map<Integer, Product> productMap = buildProductMap(products);
        String generatedAt = LocalDateTime.now().format(TS_FORMAT);

        StringBuilder json = new StringBuilder(4096);
        json.append("{\n");

        // Metadata
        json.append("  \"metadata\": {\n");
        json.append("    \"reportTitle\": \"NEXUS IMS - Financial & Inventory Valuation Report\",\n");
        json.append("    \"generatedAt\": \"").append(jsonEscape(generatedAt)).append("\",\n");
        json.append("    \"exportedBy\": \"").append(jsonEscape(currentUser != null ? currentUser.getFullName() : "System")).append("\",\n");
        json.append("    \"role\": \"").append(jsonEscape(currentUser != null ? currentUser.getRole().name() : "GUEST")).append("\"\n");
        json.append("  },\n");

        // Financial KPIs
        json.append("  \"financialKpis\": {\n");
        json.append("    \"totalCatalogItems\": ").append(summary.totalItems()).append(",\n");
        json.append("    \"totalUnitsInStock\": ").append(summary.totalUnits()).append(",\n");
        json.append("    \"lowStockItemsCount\": ").append(summary.lowStockCount()).append(",\n");
        json.append("    \"totalInventoryCost\": ").append(summary.totalCost().toPlainString()).append(",\n");
        json.append("    \"totalRetailValue\": ").append(summary.totalRetail().toPlainString()).append(",\n");
        json.append("    \"projectedGrossProfit\": ").append(summary.projectedProfit().toPlainString()).append("\n");
        json.append("  },\n");

        // Low-Stock Items
        json.append("  \"lowStockAlerts\": [\n");
        for (int i = 0; i < summary.lowStockProducts().size(); i++) {
            Product p = summary.lowStockProducts().get(i);
            BigDecimal cost = safeBigDecimal(p.getPurchasePrice());
            int deficit = Math.max(0, p.getReorderLevel() - p.getQuantityInStock());
            json.append("    {\n");
            json.append("      \"id\": ").append(p.getId()).append(",\n");
            json.append("      \"sku\": \"").append(jsonEscape(p.getSku())).append("\",\n");
            json.append("      \"name\": \"").append(jsonEscape(p.getName())).append("\",\n");
            json.append("      \"currentStock\": ").append(p.getQuantityInStock()).append(",\n");
            json.append("      \"reorderLevel\": ").append(p.getReorderLevel()).append(",\n");
            json.append("      \"deficitUnits\": ").append(deficit).append(",\n");
            json.append("      \"unitCost\": ").append(cost.toPlainString()).append(",\n");
            json.append("      \"estimatedRestockCost\": ").append(cost.multiply(BigDecimal.valueOf(deficit)).toPlainString()).append("\n");
            json.append("    }").append(i + 1 < summary.lowStockProducts().size() ? "," : "").append("\n");
        }
        json.append("  ],\n");

        // Full Product Catalog
        json.append("  \"productCatalog\": [\n");
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            BigDecimal cost = safeBigDecimal(p.getPurchasePrice());
            BigDecimal sell = safeBigDecimal(p.getUnitPrice());
            BigDecimal qty = BigDecimal.valueOf(Math.max(0, p.getQuantityInStock()));
            json.append("    {\n");
            json.append("      \"id\": ").append(p.getId()).append(",\n");
            json.append("      \"sku\": \"").append(jsonEscape(p.getSku())).append("\",\n");
            json.append("      \"name\": \"").append(jsonEscape(p.getName())).append("\",\n");
            json.append("      \"categoryId\": ").append(p.getCategoryId() != null ? p.getCategoryId() : "null").append(",\n");
            json.append("      \"supplierId\": ").append(p.getSupplierId() != null ? p.getSupplierId() : "null").append(",\n");
            json.append("      \"purchasePrice\": ").append(cost.toPlainString()).append(",\n");
            json.append("      \"unitPrice\": ").append(sell.toPlainString()).append(",\n");
            json.append("      \"quantityInStock\": ").append(p.getQuantityInStock()).append(",\n");
            json.append("      \"reorderLevel\": ").append(p.getReorderLevel()).append(",\n");
            json.append("      \"status\": \"").append(p.isLowStock() ? "LOW STOCK" : "IN STOCK").append("\",\n");
            json.append("      \"totalCostValue\": ").append(cost.multiply(qty).toPlainString()).append(",\n");
            json.append("      \"totalRetailValue\": ").append(sell.multiply(qty).toPlainString()).append("\n");
            json.append("    }").append(i + 1 < products.size() ? "," : "").append("\n");
        }
        json.append("  ],\n");

        // Stock Ledger
        json.append("  \"stockLedger\": [\n");
        for (int i = 0; i < transactions.size(); i++) {
            StockTransaction tx = transactions.get(i);
            json.append("    {\n");
            json.append("      \"id\": ").append(tx.getId()).append(",\n");
            json.append("      \"productId\": ").append(tx.getProductId()).append(",\n");
            json.append("      \"productLabel\": \"").append(jsonEscape(resolveProductLabel(tx.getProductId(), productMap))).append("\",\n");
            json.append("      \"transactionType\": \"").append(tx.getTransactionType().name()).append("\",\n");
            json.append("      \"quantityChange\": ").append(tx.getQuantityChange()).append(",\n");
            json.append("      \"performedByUserId\": ").append(tx.getPerformedByUserId() != null ? tx.getPerformedByUserId() : "null").append(",\n");
            json.append("      \"notes\": ").append(tx.getNotes() != null ? "\"" + jsonEscape(tx.getNotes()) + "\"" : "null").append(",\n");
            json.append("      \"timestamp\": \"").append(jsonEscape(formatTimestamp(tx.getCreatedAt()))).append("\"\n");
            json.append("    }").append(i + 1 < transactions.size() ? "," : "").append("\n");
        }
        json.append("  ],\n");

        // Suppliers
        json.append("  \"suppliers\": [\n");
        for (int i = 0; i < suppliers.size(); i++) {
            Supplier s = suppliers.get(i);
            json.append("    {\n");
            json.append("      \"id\": ").append(s.getId()).append(",\n");
            json.append("      \"name\": \"").append(jsonEscape(s.getName())).append("\",\n");
            json.append("      \"contactPerson\": ").append(s.getContactPerson() != null ? "\"" + jsonEscape(s.getContactPerson()) + "\"" : "null").append(",\n");
            json.append("      \"email\": ").append(s.getEmail() != null ? "\"" + jsonEscape(s.getEmail()) + "\"" : "null").append(",\n");
            json.append("      \"phone\": ").append(s.getPhone() != null ? "\"" + jsonEscape(s.getPhone()) + "\"" : "null").append("\n");
            json.append("    }").append(i + 1 < suppliers.size() ? "," : "").append("\n");
        }
        json.append("  ]\n");
        json.append("}\n");

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.write(json.toString());
        }
    }

    private String jsonEscape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    // =========================================================================
    // Shared Computation Helpers
    // =========================================================================

    private ValuationSummary computeSummary(List<Product> products) {
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalRetail = BigDecimal.ZERO;
        int totalUnits = 0;
        List<Product> lowStock = new ArrayList<>();

        for (Product p : products) {
            int qtyInt = Math.max(0, p.getQuantityInStock());
            totalUnits += qtyInt;
            BigDecimal qty = BigDecimal.valueOf(qtyInt);
            totalCost = totalCost.add(safeBigDecimal(p.getPurchasePrice()).multiply(qty));
            totalRetail = totalRetail.add(safeBigDecimal(p.getUnitPrice()).multiply(qty));
            if (p.isLowStock()) {
                lowStock.add(p);
            }
        }
        return new ValuationSummary(
                products.size(),
                totalUnits,
                lowStock.size(),
                totalCost,
                totalRetail,
                totalRetail.subtract(totalCost),
                lowStock
        );
    }

    private Map<Integer, Product> buildProductMap(List<Product> products) {
        Map<Integer, Product> map = new HashMap<>();
        for (Product p : products) {
            map.put(p.getId(), p);
        }
        return map;
    }

    private String resolveProductLabel(int productId, Map<Integer, Product> map) {
        Product p = map.get(productId);
        return p != null ? "[" + p.getSku() + "] " + p.getName() : "Product #" + productId;
    }

    private String formatTimestamp(LocalDateTime ts) {
        return ts != null ? ts.format(TS_FORMAT) : "N/A";
    }

    private BigDecimal safeBigDecimal(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }

    private String safeStr(String s) {
        return s != null ? s : "";
    }
}
