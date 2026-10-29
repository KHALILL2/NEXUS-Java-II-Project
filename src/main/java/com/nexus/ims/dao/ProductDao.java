package com.nexus.ims.dao;

import com.nexus.ims.model.Product;
import com.nexus.ims.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC DAO implementation for Product entity management.
 */
public class ProductDao implements BaseDao<Product, Integer> {

    @Override
    public Optional<Product> findById(Integer id) throws SQLException {
        String sql = "SELECT * FROM products WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToProduct(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Product> findBySku(String sku) throws SQLException {
        String sql = "SELECT * FROM products WHERE sku = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sku);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToProduct(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Product> findAll() throws SQLException {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products ORDER BY name ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
        }
        return products;
    }

    public List<Product> findLowStockProducts() throws SQLException {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE quantity_in_stock <= reorder_level ORDER BY quantity_in_stock ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
        }
        return products;
    }

    @Override
    public boolean save(Product product) throws SQLException {
        String sql = "INSERT INTO products (sku, name, description, category_id, supplier_id, " +
                     "purchase_price, unit_price, quantity_in_stock, reorder_level) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, product.getSku());
            stmt.setString(2, product.getName());
            stmt.setString(3, product.getDescription());
            stmt.setInt(4, product.getCategoryId());
            stmt.setInt(5, product.getSupplierId());
            stmt.setBigDecimal(6, product.getPurchasePrice());
            stmt.setBigDecimal(7, product.getUnitPrice());
            stmt.setInt(8, product.getQuantityInStock());
            stmt.setInt(9, product.getReorderLevel());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        product.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean update(Product product) throws SQLException {
        String sql = "UPDATE products SET sku = ?, name = ?, description = ?, category_id = ?, " +
                     "supplier_id = ?, purchase_price = ?, unit_price = ?, quantity_in_stock = ?, " +
                     "reorder_level = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, product.getSku());
            stmt.setString(2, product.getName());
            stmt.setString(3, product.getDescription());
            stmt.setInt(4, product.getCategoryId());
            stmt.setInt(5, product.getSupplierId());
            stmt.setBigDecimal(6, product.getPurchasePrice());
            stmt.setBigDecimal(7, product.getUnitPrice());
            stmt.setInt(8, product.getQuantityInStock());
            stmt.setInt(9, product.getReorderLevel());
            stmt.setInt(10, product.getId());

            return stmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean deleteById(Integer id) throws SQLException {
        String sql = "DELETE FROM products WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    private Product mapResultSetToProduct(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getInt("id"));
        p.setSku(rs.getString("sku"));
        p.setName(rs.getString("name"));
        p.setDescription(rs.getString("description"));
        p.setCategoryId(rs.getInt("category_id"));
        p.setSupplierId(rs.getInt("supplier_id"));
        p.setPurchasePrice(rs.getBigDecimal("purchase_price"));
        p.setUnitPrice(rs.getBigDecimal("unit_price"));
        p.setQuantityInStock(rs.getInt("quantity_in_stock"));
        p.setReorderLevel(rs.getInt("reorder_level"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) p.setCreatedAt(created.toLocalDateTime());
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) p.setUpdatedAt(updated.toLocalDateTime());
        return p;
    }
}

