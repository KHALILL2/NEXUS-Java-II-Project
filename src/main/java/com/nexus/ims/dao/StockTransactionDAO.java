package com.nexus.ims.dao;

import com.nexus.ims.model.StockTransaction;
import com.nexus.ims.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object managing Stock Transactions with strict ACID compliance.
 * Guarantees atomic updates across stock_transactions and products tables.
 */
public class StockTransactionDAO implements BaseDAO<StockTransaction> {
    private static final Logger LOGGER = Logger.getLogger(StockTransactionDAO.class.getName());

    @Override
    public boolean save(StockTransaction transaction) {
        return executeTransaction(transaction);
    }

    /**
     * Executes an atomic stock adjustment:
     * 1. Locks the product row (SELECT ... FOR UPDATE) to prevent race conditions.
     * 2. Validates that deduction does not drive stock below zero.
     * 3. Adjusts quantity_in_stock in the products table.
     * 4. Logs the immutable audit record into stock_transactions.
     *
     * @param transaction The stock transaction domain object to execute
     * @return true if atomic commit succeeded, false otherwise
     */
    public boolean executeTransaction(StockTransaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null.");
        }

        String lockProductSql = "SELECT quantity_in_stock, name FROM products WHERE id = ? FOR UPDATE";
        String updateProductSql = "UPDATE products SET quantity_in_stock = ? WHERE id = ?";
        String insertAuditSql = "INSERT INTO stock_transactions (product_id, transaction_type, quantity_change, notes, performed_by_user_id) " +
                               "VALUES (?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Begin ACID Transaction

            int currentStock = 0;
            String productName = "";

            // 1. Lock and fetch current stock
            try (PreparedStatement lockStmt = conn.prepareStatement(lockProductSql)) {
                lockStmt.setInt(1, transaction.getProductId());
                try (ResultSet rs = lockStmt.executeQuery()) {
                    if (!rs.next()) {
                        LOGGER.warning("Product ID " + transaction.getProductId() + " not found for transaction.");
                        conn.rollback();
                        return false;
                    }
                    currentStock = rs.getInt("quantity_in_stock");
                    productName = rs.getString("name");
                }
            }

            // 2. Validate resulting stock level
            int delta = transaction.getQuantityChange();
            int newStock = currentStock + delta;
            if (newStock < 0) {
                LOGGER.warning(String.format("Stock deduction rejected! Product '%s' (ID %d) has %d in stock, requested delta %+d.",
                        productName, transaction.getProductId(), currentStock, delta));
                conn.rollback();
                return false;
            }

            // 3. Update products current stock
            try (PreparedStatement updateStmt = conn.prepareStatement(updateProductSql)) {
                updateStmt.setInt(1, newStock);
                updateStmt.setInt(2, transaction.getProductId());
                updateStmt.executeUpdate();
            }

            // 4. Record stock_transactions audit record
            try (PreparedStatement auditStmt = conn.prepareStatement(insertAuditSql, Statement.RETURN_GENERATED_KEYS)) {
                auditStmt.setInt(1, transaction.getProductId());
                auditStmt.setString(2, transaction.getTransactionType().name());
                auditStmt.setInt(3, delta);
                auditStmt.setString(4, transaction.getNotes());

                if (transaction.getPerformedByUserId() != null && transaction.getPerformedByUserId() > 0) {
                    auditStmt.setInt(5, transaction.getPerformedByUserId());
                } else {
                    auditStmt.setNull(5, Types.INTEGER);
                }

                auditStmt.executeUpdate();
                try (ResultSet rs = auditStmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        transaction.setId(rs.getInt(1));
                    }
                }
            }

            conn.commit(); // Commit atomic transaction
            LOGGER.info(String.format("Atomic stock transaction committed for Product ID %d (%s -> %s). Delta: %+d",
                    transaction.getProductId(), currentStock, newStock, delta));
            return true;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL Exception during atomic stock transaction, initiating rollback", e);
            if (conn != null) {
                try {
                    conn.rollback();
                    LOGGER.info("Transaction rolled back successfully.");
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Rollback failed", ex);
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    LOGGER.log(Level.WARNING, "Failed to restore auto-commit or close connection", ex);
                }
            }
        }
    }

    @Override
    public boolean update(StockTransaction entity) {
        // Transactions are immutable audit records; updates are disallowed by architectural design
        throw new UnsupportedOperationException("StockTransaction records are immutable and cannot be updated.");
    }

    @Override
    public boolean delete(int id) {
        // Audit records should never be deleted under standard business practices
        String sql = "DELETE FROM stock_transactions WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to delete audit transaction " + id, e);
            return false;
        }
    }

    @Override
    public StockTransaction findById(int id) {
        String sql = "SELECT * FROM stock_transactions WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToTransaction(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding transaction ID " + id, e);
        }
        return null;
    }

    public List<StockTransaction> findByProductId(int productId) {
        List<StockTransaction> list = new ArrayList<>();
        String sql = "SELECT * FROM stock_transactions WHERE product_id = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching transactions for product " + productId, e);
        }
        return list;
    }

    @Override
    public List<StockTransaction> findAll() {
        List<StockTransaction> list = new ArrayList<>();
        String sql = "SELECT * FROM stock_transactions ORDER BY created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToTransaction(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving all stock transactions", e);
        }
        return list;
    }

    private StockTransaction mapResultSetToTransaction(ResultSet rs) throws SQLException {
        StockTransaction tx = new StockTransaction();
        tx.setId(rs.getInt("id"));
        tx.setProductId(rs.getInt("product_id"));
        tx.setTransactionType(StockTransaction.TransactionType.valueOf(rs.getString("transaction_type")));
        tx.setQuantityChange(rs.getInt("quantity_change"));
        tx.setNotes(rs.getString("notes"));

        int userId = rs.getInt("performed_by_user_id");
        tx.setPerformedByUserId(rs.wasNull() ? null : userId);

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) tx.setCreatedAt(ts.toLocalDateTime());

        return tx;
    }
}

