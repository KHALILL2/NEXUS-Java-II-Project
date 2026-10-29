package com.nexus.ims.dao;

import com.nexus.ims.model.Product;
import com.nexus.ims.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Production-grade Data Access Object for the Product entity.
 * Implements BaseDAO with PreparedStatements and custom inventory retrieval queries.
 */
public class ProductDAO implements BaseDAO<Product> {
    private static final Logger LOGGER = Logger.getLogger(ProductDAO.class.getName());

    @Override
    public boolean save(Product product) {
        String sql = "INSERT INTO products (sku, name, description, category_id, supplier_id, " +
                     "purchase_price, unit_price, quantity_in_stock, reorder_level) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, product.getSku());
            stmt.setString(2, product.getName());
            stmt.setString(3, product.getDescription());

            if (product.getCategoryId() != null && product.getCategoryId() > 0) {
                stmt.setInt(4, product.getCategoryId());
            } else {
                stmt.setNull(4, Types.INTEGER);
            }

            if (product.getSupplierId() != null && product.getSupplierId() > 0) {
                stmt.setInt(5, product.getSupplierId());
            } else {
                stmt.setNull(5, Types.INTEGER);
            }

            stmt.setBigDecimal(6, product.getPurchasePrice() != null ? product.getPurchasePrice() : BigDecimal.ZERO);
            stmt.setBigDecimal(7, product.getUnitPrice() != null ? product.getUnitPrice() : BigDecimal.ZERO);
            stmt.setInt(8, product.getQuantityInStock());
            stmt.setInt(9, product.getReorderLevel());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        product.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to persist product SKU: " + product.getSku(), e);
        }
        return false;
    }

    @Override
    public boolean update(Product product) {
        String sql = "UPDATE products SET sku = ?, name = ?, description = ?, category_id = ?, " +
                     "supplier_id = ?, purchase_price = ?, unit_price = ?, quantity_in_stock = ?, " +
                     "reorder_level = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, product.getSku());
            stmt.setString(2, product.getName());
            stmt.setString(3, product.getDescription());

            if (product.getCategoryId() != null && product.getCategoryId() > 0) {
                stmt.setInt(4, product.getCategoryId());
            } else {
                stmt.setNull(4, Types.INTEGER);
            }

            if (product.getSupplierId() != null && product.getSupplierId() > 0) {
                stmt.setInt(5, product.getSupplierId());
            } else {
                stmt.setNull(5, Types.INTEGER);
            }

            stmt.setBigDecimal(6, product.getPurchasePrice());
            stmt.setBigDecimal(7, product.getUnitPrice());
            stmt.setInt(8, product.getQuantityInStock());
            stmt.setInt(9, product.getReorderLevel());
            stmt.setInt(10, product.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update product ID: " + product.getId(), e);
            return false;
        }
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM products WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to delete product ID: " + id, e);
            return false;
        }
    }

    public boolean deleteById(int id) {
        return delete(id);
    }

    @Override
    public Product findById(int id) {
        String sql = "SELECT * FROM products WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProduct(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding product by ID: " + id, e);
        }
        return null;
    }

    public Product findBySku(String sku) {
        if (sku == null || sku.trim().isEmpty()) {
            return null;
        }
        String sql = "SELECT * FROM products WHERE sku = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sku.trim().toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProduct(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding product by SKU: " + sku, e);
        }
        return null;
    }

    @Override
    public List<Product> findAll() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products ORDER BY name ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching all products", e);
        }
        return products;
    }

    public List<Product> findLowStock(int threshold) {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE quantity_in_stock <= ? ORDER BY quantity_in_stock ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, threshold);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapResultSetToProduct(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching low stock products with threshold " + threshold, e);
        }
        return products;
    }

    public List<Product> findLowStockProducts() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE quantity_in_stock <= reorder_level ORDER BY quantity_in_stock ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching low stock reorder alerts", e);
        }
        return products;
    }

    private Product mapResultSetToProduct(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getInt("id"));
        p.setSku(rs.getString("sku"));
        p.setName(rs.getString("name"));
        p.setDescription(rs.getString("description"));

        int catId = rs.getInt("category_id");
        p.setCategoryId(rs.wasNull() ? null : catId);

        int supId = rs.getInt("supplier_id");
        p.setSupplierId(rs.wasNull() ? null : supId);

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
