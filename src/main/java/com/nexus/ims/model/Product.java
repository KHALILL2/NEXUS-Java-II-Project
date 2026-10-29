package com.nexus.ims.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain entity representing an inventory product item.
 * Strictly encapsulated with validation rules and business logic helpers.
 */
public class Product {
    private int id;
    private String sku;
    private String name;
    private String description;
    private Integer categoryId;
    private Integer supplierId;
    private BigDecimal purchasePrice;
    private BigDecimal unitPrice;
    private int quantityInStock;
    private int reorderLevel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Product() {
        this.purchasePrice = BigDecimal.ZERO;
        this.unitPrice = BigDecimal.ZERO;
        this.quantityInStock = 0;
        this.reorderLevel = 10;
    }

    public Product(String sku, String name, BigDecimal purchasePrice, BigDecimal unitPrice, int quantityInStock) {
        this();
        setSku(sku);
        setName(name);
        setPurchasePrice(purchasePrice);
        setUnitPrice(unitPrice);
        setQuantityInStock(quantityInStock);
    }

    public Product(int id, String sku, String name, String description, Integer categoryId, 
                   Integer supplierId, BigDecimal purchasePrice, BigDecimal unitPrice, 
                   int quantityInStock, int reorderLevel, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        setSku(sku);
        setName(name);
        this.description = description;
        this.categoryId = categoryId;
        this.supplierId = supplierId;
        setPurchasePrice(purchasePrice);
        setUnitPrice(unitPrice);
        setQuantityInStock(quantityInStock);
        setReorderLevel(reorderLevel);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters with Validations
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        if (sku == null || sku.trim().isEmpty()) {
            throw new IllegalArgumentException("SKU cannot be null or empty.");
        }
        this.sku = sku.trim().toUpperCase();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be null or empty.");
        }
        this.name = name.trim();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public Integer getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Integer supplierId) {
        this.supplierId = supplierId;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        if (purchasePrice == null || purchasePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Purchase price (cost) must be non-negative.");
        }
        this.purchasePrice = purchasePrice;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit selling price must be non-negative.");
        }
        this.unitPrice = unitPrice;
    }

    public int getQuantityInStock() {
        return quantityInStock;
    }

    public void setQuantityInStock(int quantityInStock) {
        if (quantityInStock < 0) {
            throw new IllegalArgumentException("Quantity in stock cannot be negative.");
        }
        this.quantityInStock = quantityInStock;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(int reorderLevel) {
        if (reorderLevel < 0) {
            throw new IllegalArgumentException("Reorder level cannot be negative.");
        }
        this.reorderLevel = reorderLevel;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Checks if product stock is at or below its reorder threshold.
     */
    public boolean isLowStock() {
        return this.quantityInStock <= this.reorderLevel;
    }

    /**
     * Calculates total inventory valuation for this product item (Quantity * Cost Price).
     */
    public BigDecimal calculateAssetValuation() {
        if (purchasePrice == null || quantityInStock <= 0) {
            return BigDecimal.ZERO;
        }
        return purchasePrice.multiply(BigDecimal.valueOf(quantityInStock));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product product)) return false;
        return id == product.id && Objects.equals(sku, product.sku);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sku);
    }

    @Override
    public String toString() {
        return String.format("Product [id=%d, sku='%s', name='%s', stock=%d, cost=$%.2f, price=$%.2f, lowStock=%s]",
                id, sku, name, quantityInStock, purchasePrice, unitPrice, isLowStock());
    }
}
