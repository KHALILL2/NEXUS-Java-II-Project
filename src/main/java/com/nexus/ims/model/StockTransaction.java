package com.nexus.ims.model;

import java.time.LocalDateTime;

/**
 * Domain entity recording changes to inventory levels (Audit Trail).
 */
public class StockTransaction {
    public enum TransactionType {
        INBOUND_PURCHASE,
        OUTBOUND_SALE,
        ADJUSTMENT_DAMAGE,
        ADJUSTMENT_COUNT,
        RETURN_RESTOCK
    }

    private int id;
    private int productId;
    private TransactionType transactionType;
    private int quantityChange;
    private String notes;
    private int performedByUserId;
    private LocalDateTime timestamp;

    public StockTransaction() {}

    public StockTransaction(int id, int productId, TransactionType transactionType, 
                            int quantityChange, String notes, int performedByUserId, 
                            LocalDateTime timestamp) {
        this.id = id;
        this.productId = productId;
        this.transactionType = transactionType;
        this.quantityChange = quantityChange;
        this.notes = notes;
        this.performedByUserId = performedByUserId;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public TransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(TransactionType transactionType) { this.transactionType = transactionType; }

    public int getQuantityChange() { return quantityChange; }
    public void setQuantityChange(int quantityChange) { this.quantityChange = quantityChange; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public int getPerformedByUserId() { return performedByUserId; }
    public void setPerformedByUserId(int performedByUserId) { this.performedByUserId = performedByUserId; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}

