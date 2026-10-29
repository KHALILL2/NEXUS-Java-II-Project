package com.nexus.ims.service;

import com.nexus.ims.dao.ProductDAO;
import com.nexus.ims.dao.StockTransactionDAO;
import com.nexus.ims.model.Product;
import com.nexus.ims.model.StockTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying core business logic rules in InventoryService.
 */
class InventoryServiceTest {

    private InventoryService inventoryService;
    private MockProductDAO mockProductDAO;
    private MockStockTransactionDAO mockTransactionDAO;

    @BeforeEach
    void setUp() {
        mockProductDAO = new MockProductDAO();
        mockTransactionDAO = new MockStockTransactionDAO();
        inventoryService = new InventoryService(mockProductDAO, mockTransactionDAO);
    }

    @Test
    @DisplayName("Stock deduction must throw exception when requested quantity exceeds available stock")
    void testOutboundSaleThrowsWhenInsufficientStock() {
        Product p = new Product("TEST-SKU-1", "Test Widget", new BigDecimal("10.00"), new BigDecimal("15.00"), 5);
        p.setId(1);
        mockProductDAO.save(p);

        // Attempt to deduct 10 units when only 5 exist
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            inventoryService.recordOutboundSale(1, 10, "Order 999", 1);
        });

        assertTrue(exception.getMessage().contains("Insufficient stock"));
    }

    @Test
    @DisplayName("Stock deduction must succeed when available quantity is sufficient")
    void testOutboundSaleSucceedsWithSufficientStock() {
        Product p = new Product("TEST-SKU-2", "Test Gadget", new BigDecimal("20.00"), new BigDecimal("30.00"), 10);
        p.setId(2);
        p.setReorderLevel(3);
        mockProductDAO.save(p);

        boolean result = inventoryService.recordOutboundSale(2, 4, "Order 1000", 1);
        assertTrue(result);
        assertEquals(1, mockTransactionDAO.recordedTransactions.size());
        assertEquals(-4, mockTransactionDAO.recordedTransactions.get(0).getQuantityChange());
    }

    @Test
    @DisplayName("Total inventory asset valuation must compute SUM(quantity * purchasePrice)")
    void testCalculateTotalInventoryValuation() {
        Product p1 = new Product("SKU-A", "Item A", new BigDecimal("100.00"), new BigDecimal("150.00"), 5);
        Product p2 = new Product("SKU-B", "Item B", new BigDecimal("50.00"), new BigDecimal("75.00"), 10);
        mockProductDAO.save(p1);
        mockProductDAO.save(p2);

        // Valuation: (5 * 100) + (10 * 50) = 500 + 500 = 1000.00
        BigDecimal totalValuation = inventoryService.calculateTotalInventoryValuation();
        assertEquals(new BigDecimal("1000.00"), totalValuation);
    }

    // Lightweight in-memory test doubles
    static class MockProductDAO extends ProductDAO {
        private final List<Product> storage = new java.util.concurrent.CopyOnWriteArrayList<>();

        @Override
        public boolean save(Product p) {
            storage.add(p);
            return true;
        }

        @Override
        public Product findById(int id) {
            return storage.stream().filter(p -> p.getId() == id).findFirst().orElse(null);
        }

        @Override
        public List<Product> findAll() {
            return storage;
        }
    }

    static class MockStockTransactionDAO extends StockTransactionDAO {
        final List<StockTransaction> recordedTransactions = new java.util.ArrayList<>();

        @Override
        public boolean executeTransaction(StockTransaction tx) {
            recordedTransactions.add(tx);
            return true;
        }
    }
}

