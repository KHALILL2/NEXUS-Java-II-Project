package com.nexus.ims.service;

import com.nexus.ims.dao.ProductDao;
import com.nexus.ims.model.Product;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Service encapsulating business logic, calculations, and validations for Product inventory.
 */
public class ProductService {
    private final ProductDao productDao;

    public ProductService() {
        this.productDao = new ProductDao();
    }

    public ProductService(ProductDao productDao) {
        this.productDao = productDao;
    }

    public List<Product> getAllProducts() throws SQLException {
        return productDao.findAll();
    }

    public List<Product> getLowStockAlerts() throws SQLException {
        return productDao.findLowStockProducts();
    }

    public Optional<Product> getProductById(int id) throws SQLException {
        return Optional.ofNullable(productDao.findById(id));
    }

    public boolean createProduct(Product product) throws SQLException {
        validateProduct(product);
        if (productDao.findBySku(product.getSku()) != null) {
            throw new IllegalArgumentException("Product with SKU '" + product.getSku() + "' already exists.");
        }
        return productDao.save(product);
    }

    public boolean updateProduct(Product product) throws SQLException {
        validateProduct(product);
        return productDao.update(product);
    }

    public boolean deleteProduct(int id) throws SQLException {
        return productDao.deleteById(id);
    }

    private void validateProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null");
        }
        if (product.getSku() == null || product.getSku().trim().isEmpty()) {
            throw new IllegalArgumentException("Product SKU is required.");
        }
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Product name is required.");
        }
        if (product.getPurchasePrice() == null || product.getPurchasePrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Purchase price must be non-negative.");
        }
        if (product.getUnitPrice() == null || product.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit selling price must be non-negative.");
        }
        if (product.getQuantityInStock() < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative.");
        }
    }
}

