package com.nexus.ims.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain entity recording immutable changes to inventory stock levels (Audit Ledger).
 */
public class StockTransaction {

    public enum TransactionType {
        INBOUND_PURCHASE(1, "Inbound shipment from supplier"),
        OUTBOUND_SALE(-1, "Customer sale deduction"),
        ADJUSTMENT_DAMAGE(-1, "Damaged stock write-off"),
        ADJUSTMENT_COUNT(1, "Inventory physical count adjustment"),
        RETURN_RESTOCK(1, "Customer returned restock");

        private final int defaultSign;
        private final String description;

        TransactionType(int defaultSign, String description) {
            this.defaultSign = defaultSign;
            this.description = description;
        }

        public int getDefaultSign() {
            return defaultSign;
        }

        public String getDescription() {
            return description;
        }
    }

    private int id;
    private int productId;
    private TransactionType transactionType;
    private int quantityChange;
    private String notes;
    private Integer performedByUserId;
    private LocalDateTime createdAt;

    public StockTransaction() {}

    public StockTransaction(int productId, TransactionType transactionType, 
                            int quantityChange, String notes, Integer performedByUserId) {
        this.productId = productId;
        setTransactionType(transactionType);
        setQuantityChange(quantityChange);
        this.notes = notes;
        this.performedByUserId = performedByUserId;
    }

    public StockTransaction(int id, int productId, TransactionType transactionType, 
                            int quantityChange, String notes, Integer performedByUserId, 
                            LocalDateTime createdAt) {
        this.id = id;
        this.productId = productId;
        setTransactionType(transactionType);
        setQuantityChange(quantityChange);
        this.notes = notes;
        this.performedByUserId = performedByUserId;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        if (transactionType == null) {
            throw new IllegalArgumentException("Transaction type cannot be null.");
        }
        this.transactionType = transactionType;
    }

    public int getQuantityChange() {
        return quantityChange;
    }

    public void setQuantityChange(int quantityChange) {
        if (quantityChange == 0) {
            throw new IllegalArgumentException("Quantity change cannot be zero.");
        }
        this.quantityChange = quantityChange;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Integer getPerformedByUserId() {
        return performedByUserId;
    }

    public void setPerformedByUserId(Integer performedByUserId) {
        this.performedByUserId = performedByUserId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StockTransaction that)) return false;
        return id == that.id && productId == that.productId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, productId);
    }

    @Override
    public String toString() {
        return String.format("StockTransaction [id=%d, productId=%d, type=%s, delta=%+d, notes='%s', time=%s]",
                id, productId, transactionType, quantityChange, notes, createdAt);
    }
}
