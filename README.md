# NEXUS Inventory Management System (IMS)

[![Java Version](https://img.shields.io/badge/Java-17%20LTS-007396?logo=openjdk&logoColor=white)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Build Tool](https://img.shields.io/badge/Build-Maven%203.9-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![UI Framework](https://img.shields.io/badge/UI-FlatLaf%203.4-4A90E2)](https://www.formdev.com/flatlaf/)
[![Database](https://img.shields.io/badge/Database-MySQL%208.0-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Architecture](https://img.shields.io/badge/Architecture-MVC%20%2B%20DAO-brightgreen)](#system-architecture)

---

## 1. Project Overview

The **NEXUS Inventory Management System (IMS)** is a desktop inventory operations application built with **Java 17 LTS**, **Swing (FlatLaf)**, **Java2D Vector Icons**, and **MySQL 8.0** using raw JDBC `PreparedStatement` persistence. It implements strict **Model-View-Controller (MVC)** and **Data Access Object (DAO)** patterns with asynchronous `SwingWorker` execution across all database operations.

### Key Capabilities
- **Modern Desktop UI & Vector Icons**: FlatLaf rounded styling, custom anti-aliased `Graphics2D` vector icons (`IconFactory`), right-aligned currency columns, rounded `IN STOCK` / `LOW STOCK` pill badges, and live system clock.
- **Dashboard Overview Watchlists**: Live KPI cards alongside a split **Low Stock Watchlist** and **Recent Stock Movements** panel.
- **Product Catalog & Modal CRUD**: Instant in-memory SKU/Name search filtering, Add/Edit `ProductFormDialog` populated from `CategoryDAO` and `SupplierDAO`, and unique SKU enforcement.
- **ACID-Compliant Stock Transactions**: Dedicated `StockTransactionDialog` supporting Stock IN (`INBOUND_PURCHASE`, `RETURN_RESTOCK`, `ADJUSTMENT_COUNT`) and Stock OUT (`OUTBOUND_SALE`, `ADJUSTMENT_DAMAGE`, `ADJUSTMENT_COUNT`) backed by `SELECT ... FOR UPDATE` row locks and automatic negative-stock rejection.
- **Suppliers Management**: Full vendor directory CRUD (`SuppliersPanel` & `SupplierFormDialog`) with instant search filtering.
- **Financial Reports & Multi-Format Export**: Valuation breakdown (`Total Inventory Cost`, `Total Retail Value`, `Projected Gross Profit`, `Low-Stock Items`), reorder deficit schedule, and unified **Export** dropdown (`ReportExportService`) generating native **PDF** (OpenPDF), multi-sheet **Excel `.xlsx`** (Apache POI), **UTF-8 BOM CSV**, and structured **JSON** with one-click file launch.
- **Role-Based Access Control (RBAC)**: Enforced at runtime across `ADMIN`, `MANAGER`, and `STAFF` roles with salted BCrypt (Cost 12) authentication.

---

## 2. System Architecture

```text
C:\.vscode\NEXUS Java II Project\
├── .gitignore
├── CHANGELOG.md
├── pom.xml                                     # Java 17 (--release 17), mysql-connector-j, FlatLaf, BCrypt, OpenPDF, Apache POI, JUnit 5
├── README.md
├── mvnw.cmd                                    # Zero-install Windows Maven Wrapper
└── src/
    ├── main/
    │   ├── resources/
    │   │   ├── db.properties                   # Active JDBC credentials
    │   │   └── database/
    │   │       ├── schema.sql                  # MySQL 8.0 DDL (users, categories, suppliers, products, stock_transactions)
    │   │       └── data.sql                    # Initial catalog & transaction seed data
    │   └── java/com/nexus/ims/
    │       ├── Main.java                       # FlatLaf UIManager configuration & EDT bootstrap
    │       ├── model/                          # Encapsulated Entities (Product, Category, Supplier, StockTransaction, User)
    │       ├── dao/                            # JDBC DAOs (BaseDAO, ProductDAO, CategoryDAO, SupplierDAO, StockTransactionDAO, UserDAO)
    │       ├── service/                        # Business Logic & Reporting (AuthService, InventoryService, ReportExportService)
    │       ├── view/                           # Swing UI (LoginFrame, DashboardFrame, SuppliersPanel, ReportsPanel, Modal Dialogs)
    │       ├── controller/                     # MVC Mediators (AuthController, DashboardController)
    │       └── util/                           # Infrastructure (DatabaseConnection, DatabaseMigrator, PasswordHasher, IconFactory)
    └── test/java/com/nexus/ims/service/
        └── InventoryServiceTest.java           # JUnit 5 unit tests
```

---

## 3. Prerequisites & Setup

1. **Java Development Kit (JDK)**: JDK 17+ (`java -version`).
2. **MySQL Server**: MySQL 8.0+ running on `localhost:3306`.
3. **Configure JDBC Credentials**: Update `src/main/resources/db.properties` with your MySQL username and password:
   ```properties
   db.driver=com.mysql.cj.jdbc.Driver
   db.url=jdbc:mysql://localhost:3306/nexus_ims_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
   db.username=root
   db.password=YOUR_MYSQL_PASSWORD
   ```

4. **Initialize Database & Seed Valid BCrypt Credentials**:
   ```powershell
   cd "C:\.vscode\NEXUS Java II Project"
   .\mvnw.cmd clean compile
   .\mvnw.cmd exec:java -Dexec.mainClass="com.nexus.ims.util.DatabaseMigrator"
   ```
   *(The migrator applies `schema.sql`, `data.sql`, and verifies/generates cryptographic BCrypt hashes for the 3 default accounts).*

---

## 4. Build, Test & Run Commands

```powershell
cd "C:\.vscode\NEXUS Java II Project"

# Compile and run JUnit 5 unit tests
.\mvnw.cmd clean test

# Launch the desktop application
.\mvnw.cmd exec:java

# Package executable fat-JAR
.\mvnw.cmd clean package
java -jar target/nexus-inventory-management-1.0.0-SNAPSHOT.jar
```

---

## 5. Role-Based Access Control (RBAC) & Test Credentials

| Username | Password | Role | Permitted Operations |
| :--- | :--- | :--- | :--- |
| `admin` | `admin123` | **ADMIN** | Full access across Dashboard, Products (Add/Edit/Delete), Suppliers (Add/Edit/Delete), Stock IN/OUT, and Reports/Multi-Format Export. |
| `manager` | `manager123` | **MANAGER** | Full operational access to Dashboard, Products (Add/Edit), Suppliers (Add/Edit), Stock IN/OUT, and Reports/Multi-Format Export. Delete actions disabled. |
| `staff` | `staff123` | **STAFF** | Read-only access on Products and Suppliers (Add/Edit/Delete hidden); full access to Dashboard, Stock IN/OUT Transactions, and Reports/Multi-Format Export. |
