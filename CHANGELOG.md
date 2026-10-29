# Changelog

All notable changes to the **NEXUS Inventory Management System (IMS)** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0] - 2026-10-29

### Added
- **Project Infrastructure & Build System**:
  - Configured `pom.xml` for Java 17 LTS with Maven plugins (`maven-compiler-plugin`, `exec-maven-plugin`, `maven-shade-plugin`).
  - Integrated dependencies: `mysql-connector-java` (8.0.33), `flatlaf` (3.4.1), `bcrypt` (0.10.2), and `junit-jupiter` (5.10.2).
  - Provided embedded zero-install Maven Wrapper (`mvnw.cmd`) and permanent Windows installer script (`install-maven-permanently.ps1`).
  - Added comprehensive production-grade `.gitignore` covering Java bytecode, IDE files, OS dumps, and environment configurations.

- **Database Persistence Tier**:
  - DDL Schema (`src/main/resources/database/schema.sql`) for MySQL 8.0+ covering `users`, `categories`, `suppliers`, `products`, and `stock_transactions`.
  - Implemented referential integrity with cascading deletes and null constraints, non-negative `CHECK` constraints on prices and quantities, and B-Tree indexes.
  - Mock seed dataset (`src/main/resources/database/data.sql`) containing default roles (`ADMIN`, `MANAGER`, `STAFF`) with BCrypt-hashed credentials.
  - Thread-safe Singleton `DatabaseConnection` using Double-Checked Locking (DCL) to parse `db.properties` and manage active JDBC connections.
  - Dual-mode database migrator:
    - `DatabaseMigrator.java`: Java/JDBC-native executor that reads SQL resources without requiring the external `mysql.exe` CLI.
    - `migrate-db.ps1`: Automated PowerShell migrator that detects system MySQL installations and registers them to `PATH`.

- **Domain Model & Data Access (DAO)**:
  - Strongly encapsulated entity models: `Product`, `Category`, `Supplier`, `StockTransaction`, and `User`.
  - Generic `BaseDAO<T>` contract providing parameterized CRUD operations (`save`, `update`, `delete`, `findById`, `findAll`).
  - `ProductDAO`: Production JDBC implementation featuring `findLowStock(int threshold)` and `findBySku(String sku)`.
  - `StockTransactionDAO`: Strict ACID-compliant transaction manager utilizing `conn.setAutoCommit(false)`, row-level locks (`SELECT ... FOR UPDATE`), rollbacks, and atomic inventory adjustments.
  - `UserDao`: Authentication and credential lookups with role-based access control.

- **Business Logic Layer**:
  - `InventoryService`:
    - Enforces business rule prohibiting stock deductions that result in negative stock.
    - Triggers automated low-stock warnings when inventory drops below individualized reorder levels.
    - Computes real-time warehouse asset valuation using high-precision `BigDecimal` arithmetic (`SUM(quantity * purchasePrice)`).
  - `AuthService`: Salted password hashing (BCrypt cost factor 12) and session verification.

- **Automated Verification**:
  - `InventoryServiceTest`: Comprehensive JUnit 5 unit test suite testing insufficient stock exceptions, outbound deductions, and valuation computations.

- **Presentation Layer (Modern Swing / FlatLaf)**:
  - `Main.java`: UI lifecycle bootstrap setting up FlatLaf light theme with High-DPI screen adjustments and EDT scheduling.
  - `LoginFrame`: Centered authentication window with password concealing, show/hide toggle, asynchronous `SwingWorker` verification, and error messaging.
  - `DashboardFrame`: Modular multi-pane desktop shell featuring:
    - Left navigation sidebar (`Dashboard`, `Products`, `Transactions`, `Suppliers`, `Reports`, `Sign Out`).
    - Top header displaying logged-in operator credentials and role badge pill.
    - Dynamic `CardLayout` center containing 3 live KPI metric cards (Total SKUs, Low Stock, Total Valuation).
    - Products catalog table with soft red row highlighting for low-stock items.
    - Stock Inbound (Receipt) and Outbound (Dispatch) fast-action transaction modals.
    - Status bar with database health indicator, status messenger, and 1-second system clock timer.
  - Pure MVC separation: `AuthController` and `DashboardController` mediating user gestures to backend services without UI locking.

---

[1.0.0]: https://github.com/KHALILL2/NEXUS-Java-II-Project/releases/tag/v1.0.0

