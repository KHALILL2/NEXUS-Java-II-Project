-- =============================================================================
-- NEXUS Inventory Management System (IMS) - Seed / Mock Data
-- DBMS: MySQL 8.0+
-- File: src/main/resources/database/data.sql
-- =============================================================================

USE nexus_ims_db;

-- 1. Seed Categories
INSERT INTO categories (id, name, description) VALUES
    (1, 'Laptops & Computers', 'Enterprise laptops, workstations, and thin clients'),
    (2, 'Computer Peripherals', 'Keyboards, mice, monitors, and docks'),
    (3, 'Networking Hardware', 'Routers, switches, access points, and CAT6 cabling'),
    (4, 'Storage Devices', 'NVMe SSDs, external hard drives, and NAS systems'),
    (5, 'Office Supplies', 'Standard stationery, filing, and office essentials')
ON DUPLICATE KEY UPDATE name=VALUES(name), description=VALUES(description);

-- 2. Seed Users
-- Passwords hashed via BCrypt (Cost Factor 12):
-- 'admin123'   -> $2a$12$e80yq5pE2D3v3fL7ZvhPfejKskm7X0U2Y5eW4UuY7oN5k6t4/B2mK
-- 'manager123' -> $2a$12$KkXzS3iA2rYl2lV3g.Z9s.l4Zc077k7tHqgGzEwhzH5p1V7A9d/mK
-- 'staff123'   -> $2a$12$O1122yPz6gE2vB2Qp6kYXe4g3T7pL0A2Xv9W7x9E1yB3a5c7D9eFe
INSERT INTO users (id, username, password_hash, full_name, email, role, is_active) VALUES
    (1, 'admin', '$2a$12$e80yq5pE2D3v3fL7ZvhPfejKskm7X0U2Y5eW4UuY7oN5k6t4/B2mK', 'System Administrator', 'admin@nexus.local', 'ADMIN', TRUE),
    (2, 'manager', '$2a$12$KkXzS3iA2rYl2lV3g.Z9s.l4Zc077k7tHqgGzEwhzH5p1V7A9d/mK', 'Inventory Manager', 'manager@nexus.local', 'MANAGER', TRUE),
    (3, 'staff', '$2a$12$O1122yPz6gE2vB2Qp6kYXe4g3T7pL0A2Xv9W7x9E1yB3a5c7D9eFe', 'Warehouse Staff', 'staff@nexus.local', 'STAFF', TRUE)
ON DUPLICATE KEY UPDATE full_name=VALUES(full_name), role=VALUES(role);

-- 3. Seed Suppliers
INSERT INTO suppliers (id, name, contact_person, email, phone, address) VALUES
    (1, 'Dell Technologies Direct', 'Marcus Vance', 'orders@dell-direct.com', '+1-800-555-0101', 'One Dell Way, Round Rock, TX 78682'),
    (2, 'Logitech B2B Distribution', 'Elena Rostova', 'b2b@logitech-supply.com', '+1-800-555-0102', '7700 Gateway Blvd, Newark, CA 94560'),
    (3, 'Cisco Systems Wholesale', 'David Chen', 'enterprise@cisco-dist.com', '+1-800-555-0103', '170 W Tasman Dr, San Jose, CA 95134'),
    (4, 'Samsung Semiconductor', 'Jin-Woo Park', 'sales@samsung-storage.com', '+1-800-555-0104', '85 Challenger Rd, Ridgefield Park, NJ 07660')
ON DUPLICATE KEY UPDATE name=VALUES(name), contact_person=VALUES(contact_person);

-- 4. Seed Products
INSERT INTO products (id, sku, name, description, category_id, supplier_id, purchase_price, unit_price, quantity_in_stock, reorder_level) VALUES
    (1, 'DELL-LAT-5540', 'Dell Latitude 5540 Laptop 15.6"', 'Core i7-1365U, 16GB RAM, 512GB SSD', 1, 1, 850.00, 1199.99, 25, 5),
    (2, 'DELL-XPS-15', 'Dell XPS 15 9530', 'Core i9-13900H, 32GB RAM, 1TB SSD, RTX 4060', 1, 1, 1650.00, 2299.99, 8, 3),
    (3, 'LOGI-MXM-3S', 'Logitech MX Master 3S Wireless Mouse', 'Quiet clicks, 8K DPI sensor, Bluetooth/USB', 2, 2, 62.00, 99.99, 65, 15),
    (4, 'LOGI-MXK-MECH', 'Logitech MX Mechanical Wireless Keyboard', 'Tactile quiet switches, dual OS layout', 2, 2, 98.00, 149.99, 42, 10),
    (5, 'CSCO-CAT-9200', 'Cisco Catalyst 9200L 24-Port Switch', '24x 1G Data, 4x 1G fixed uplinks, Network Essentials', 3, 3, 1100.00, 1650.00, 4, 5),
    (6, 'CSCO-CBS-350', 'Cisco Business 350 Managed Switch', '8-Port Gigabit PoE+, 2x SFP combo', 3, 3, 280.00, 420.00, 12, 4),
    (7, 'SAMS-990-PRO-2T', 'Samsung 990 PRO NVMe SSD 2TB', 'PCIe Gen 4.0 x4, up to 7450 MB/s read', 4, 4, 115.00, 179.99, 30, 8),
    (8, 'SAMS-T7-1TB', 'Samsung T7 Shield Portable SSD 1TB', 'Rugged USB 3.2 Gen 2 external storage', 4, 4, 75.00, 119.99, 2, 5) -- Low Stock Item
ON DUPLICATE KEY UPDATE name=VALUES(name), quantity_in_stock=VALUES(quantity_in_stock);

-- 5. Seed Initial Stock Audit Trail (Stock Transactions)
INSERT INTO stock_transactions (id, product_id, transaction_type, quantity_change, notes, performed_by_user_id) VALUES
    (1, 1, 'INBOUND_PURCHASE', 25, 'Initial batch receipt PO-2026-001', 1),
    (2, 2, 'INBOUND_PURCHASE', 10, 'Initial batch receipt PO-2026-001', 1),
    (3, 2, 'OUTBOUND_SALE', -2, 'Executive deployment SO-8831', 2),
    (4, 8, 'INBOUND_PURCHASE', 5, 'Initial trial stocking', 1),
    (5, 8, 'OUTBOUND_SALE', -3, 'Sales order SO-8840', 2)
ON DUPLICATE KEY UPDATE notes=VALUES(notes);

