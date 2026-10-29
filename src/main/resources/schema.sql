-- =============================================================================
-- NEXUS IMS - Database Schema & Initial Seed
-- Target: MySQL 8.0+
-- =============================================================================

CREATE DATABASE IF NOT EXISTS nexus_ims_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE nexus_ims_db;

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    role ENUM('ADMIN', 'MANAGER', 'STAFF') NOT NULL DEFAULT 'STAFF',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 2. Categories Table
CREATE TABLE IF NOT EXISTS categories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
) ENGINE=InnoDB;

-- 3. Suppliers Table
CREATE TABLE IF NOT EXISTS suppliers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    contact_person VARCHAR(100),
    email VARCHAR(100),
    phone VARCHAR(30),
    address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 4. Products Table
CREATE TABLE IF NOT EXISTS products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sku VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    category_id INT,
    supplier_id INT,
    purchase_price DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    unit_price DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    quantity_in_stock INT NOT NULL DEFAULT 0,
    reorder_level INT NOT NULL DEFAULT 10,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE SET NULL,
    CONSTRAINT fk_product_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 5. Stock Transactions Table (Audit Log)
CREATE TABLE IF NOT EXISTS stock_transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    transaction_type ENUM('INBOUND_PURCHASE', 'OUTBOUND_SALE', 'ADJUSTMENT_DAMAGE', 'ADJUSTMENT_COUNT', 'RETURN_RESTOCK') NOT NULL,
    quantity_change INT NOT NULL,
    notes TEXT,
    performed_by_user_id INT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tx_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT fk_tx_user FOREIGN KEY (performed_by_user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- Seed Default Admin User (Password: 'admin123' hashed with BCrypt)
-- Hash generated via BCrypt cost factor 12: $2a$12$e80yq5pE2D3v3fL7ZvhPfejKskm7X0U2Y5eW4UuY7oN5k6t4/B2mK
INSERT INTO users (username, password_hash, full_name, email, role, is_active)
VALUES (
    'admin',
    '$2a$12$e80yq5pE2D3v3fL7ZvhPfejKskm7X0U2Y5eW4UuY7oN5k6t4/B2mK',
    'System Administrator',
    'admin@nexus.local',
    'ADMIN',
    TRUE
) ON DUPLICATE KEY UPDATE id=id;

-- Seed Sample Categories
INSERT INTO categories (name, description) VALUES
    ('Electronics', 'Hardware, accessories, and computing equipment'),
    ('Office Supplies', 'Paper, pens, desk organizers, and stationery'),
    ('Networking', 'Routers, switches, cables, and patch panels')
ON DUPLICATE KEY UPDATE id=id;

