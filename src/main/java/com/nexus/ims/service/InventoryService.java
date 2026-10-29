package com.nexus.ims.service;

import com.nexus.ims.dao.ProductDAO;
import com.nexus.ims.dao.StockTransactionDAO;
import com.nexus.ims.model.Product;
import com.nexus.ims.model.StockTransaction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Core business logic service handling inventory rules, valuation, and transaction flow.
 */
public class InventoryService {
    private static final Logger LOGGER = Logger.getLogger(InventoryService.class.getName());

    private final ProductDAO productDAO;
    private final StockTransactionDAO transactionDAO;

    public InventoryService() {
        this.productDAO = new ProductDAO();
        this.transactionDAO = new StockTransactionDAO();
    }

    public InventoryService(ProductDAO productDAO, StockTransactionDAO transactionDAO) {
        this.productDAO = productDAO;
        this.transactionDAO = transactionDAO;
    }

    /**
     * Records inbound stock shipment from a supplier (increases inventory).
     *
     * @param productId ID of product receiving stock
     * @param quantity Amount of units received (must be positive)
     * @param notes Receipt or PO reference notes
     * @param userId User logging the shipment
     * @return true if atomic transaction succeeded
     */
    public boolean recordInboundShipment(int productId, int quantity, String notes, Integer userId) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Inbound receipt quantity must be greater than zero.");
        }

        Product product = getProductOrThrow(productId);
        StockTransaction tx = new StockTransaction(
                productId,
                StockTransaction.TransactionType.INBOUND_PURCHASE,
                quantity,
                notes != null ? notes : "Inbound supplier shipment",
                userId
        );

        boolean success = transactionDAO.executeTransaction(tx);
        if (success) {
            LOGGER.info(String.format("Inbound shipment logged for '%s': +%d units.", product.getName(), quantity));
        }
        return success;
    }

    /**
     * Records outbound sale or inventory dispatch (decreases inventory).
     * Strictly enforces that current stock cannot be driven below zero.
     *
     * @param productId ID of product sold/dispatched
     * @param quantity Amount of units deducted (must be positive)
     * @param notes Sales Order or customer reference
     * @param userId User dispatching stock
     * @return true if atomic transaction succeeded
     */
    public boolean recordOutboundSale(int productId, int quantity, String notes, Integer userId) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Outbound deduction quantity must be greater than zero.");
        }

        Product product = getProductOrThrow(productId);

        // Validation Rule: Ensure stock deduction never results in a negative quantity
        if (product.getQuantityInStock() < quantity) {
            String errorMsg = String.format("Insufficient stock for product '%s' (SKU: %s). Available: %d, Requested: %d",
                    product.getName(), product.getSku(), product.getQuantityInStock(), quantity);
            LOGGER.warning(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        StockTransaction tx = new StockTransaction(
                productId,
                StockTransaction.TransactionType.OUTBOUND_SALE,
                -quantity, // Negative delta for deduction
                notes != null ? notes : "Outbound customer dispatch",
                userId
        );

        boolean success = transactionDAO.executeTransaction(tx);
        if (success) {
            int remainingStock = product.getQuantityInStock() - quantity;
            LOGGER.info(String.format("Outbound dispatch completed for '%s': -%d units (Remaining: %d).",
                    product.getName(), quantity, remainingStock));

            // Low-Stock Threshold Trigger Warning
            if (remainingStock <= product.getReorderLevel()) {
                triggerLowStockWarning(product, remainingStock);
            }
        }
        return success;
    }

    /**
     * Records a manual inventory adjustment (damaged goods write-off or cycle count).
     *
     * @param productId ID of product adjusted
     * @param delta Signed quantity change (+ for count found, - for damage/loss)
     * @param type Specific adjustment transaction type
     * @param reason Explanation for audit trail
     * @param userId Performing user
     * @return true if adjustment succeeded
     */
    public boolean recordAdjustment(int productId, int delta, StockTransaction.TransactionType type, String reason, Integer userId) {
        if (delta == 0) {
            throw new IllegalArgumentException("Adjustment delta cannot be zero.");
        }

        Product product = getProductOrThrow(productId);
        int projectedStock = product.getQuantityInStock() + delta;
        if (projectedStock < 0) {
            throw new IllegalStateException(String.format(
                    "Adjustment rejected: resulting stock cannot be negative. Current: %d, Delta: %+d",
                    product.getQuantityInStock(), delta));
        }

        StockTransaction tx = new StockTransaction(productId, type, delta, reason, userId);
        boolean success = transactionDAO.executeTransaction(tx);
        if (success && projectedStock <= product.getReorderLevel()) {
            triggerLowStockWarning(product, projectedStock);
        }
        return success;
    }

    /**
     * Checks if a specific product is currently below or at its reorder threshold.
     */
    public boolean isProductLowStock(int productId) {
        Product product = productDAO.findById(productId);
        return product != null && product.isLowStock();
    }

    /**
     * Retrieves all products that have breached their individualized reorder threshold.
     */
    public List<Product> getLowStockAlerts() {
        return productDAO.findLowStockProducts();
    }

    /**
     * Retrieves products below an arbitrary warehouse threshold level.
     */
    public List<Product> getProductsBelowThreshold(int threshold) {
        return productDAO.findLowStock(threshold);
    }

    /**
     * Computes the total cumulative valuation of all warehouse assets:
     * SUM(quantity_in_stock * purchase_price / cost_price).
     *
     * @return total asset valuation in USD
     */
    public BigDecimal calculateTotalInventoryValuation() {
        List<Product> products = productDAO.findAll();
        BigDecimal totalValuation = BigDecimal.ZERO;

        for (Product product : products) {
            if (product.getPurchasePrice() != null && product.getQuantityInStock() > 0) {
                BigDecimal itemValuation = product.getPurchasePrice().multiply(BigDecimal.valueOf(product.getQuantityInStock()));
                totalValuation = totalValuation.add(itemValuation);
            }
        }

        LOGGER.info(String.format("Calculated total inventory valuation across %d SKUs: $%s",
                products.size(), totalValuation.toPlainString()));
        return totalValuation;
    }

    /**
     * Computes inventory valuation filtered by a specific category.
     */
    public BigDecimal calculateCategoryValuation(int categoryId) {
        List<Product> products = productDAO.findAll();
        BigDecimal categoryTotal = BigDecimal.ZERO;

        for (Product product : products) {
            if (product.getCategoryId() != null && product.getCategoryId() == categoryId) {
                if (product.getPurchasePrice() != null && product.getQuantityInStock() > 0) {
                    BigDecimal itemValuation = product.getPurchasePrice().multiply(BigDecimal.valueOf(product.getQuantityInStock()));
                    categoryTotal = categoryTotal.add(itemValuation);
                }
            }
        }
        return categoryTotal;
    }

    /**
     * Emits a formatted low-stock alert log when a threshold breach is detected.
     */
    private void triggerLowStockWarning(Product product, int currentStock) {
        String warning = String.format(
                "[ALERT - LOW STOCK] Product '%s' (SKU: %s) has dropped to %d units (Reorder Threshold: %d)!",
                product.getName(), product.getSku(), currentStock, product.getReorderLevel());
        LOGGER.warning(warning);
        System.err.println(warning);
    }

    private Product getProductOrThrow(int productId) {
        Product product = productDAO.findById(productId);
        if (product == null) {
            throw new IllegalArgumentException("Product ID " + productId + " does not exist.");
        }
        return product;
    }
}

