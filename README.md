# NEXUS Inventory Management System

Desktop inventory and stock ledger application built with Java 17, Swing (FlatLaf), and MySQL 8 via JDBC. It tracks catalog SKUs, suppliers, categories, stock movements, financial valuations, and role-based operator accounts.

## Tech Stack

- **Language**: Java 17 (`--release 17`)
- **UI**: Java Swing + FlatLaf 3.4.1 / FlatLaf Extras + Java2D vector icons (`IconFactory`) and custom category chart (`CategoryValuationChartPanel`)
- **Database**: MySQL 8.0+ via `mysql-connector-j` 8.0.33 (`PreparedStatement` + `SELECT ... FOR UPDATE` row locking)
- **Security**: `at.favre.lib:bcrypt` 0.10.2 (cost factor 12)
- **Exports**: LibrePDF OpenPDF 2.0.3 (`.pdf`), Apache POI 5.3.0 (`.xlsx`), UTF-8 BOM `.csv`, and `.json`
- **Build & Test**: Apache Maven / `mvnw.cmd` + JUnit Jupiter 5.10.2

## Features

- **Dashboard Overview**: Four interactive KPI cards (catalog count, warehouse units, low-stock alerts, cost/retail valuation), a Low Stock Watchlist with one-click quick restock and healthy empty state, and recent stock movements.
- **Catalog & Categories**: Live search, stock status filtering (`All Items`, `In Stock Only`, `Low Stock Only`), calculated profit margin column, summary footer bar, right-click table context menu (`Edit`, `Stock IN`, `Stock OUT`, `Copy SKU`, `Delete`), and modal `CategoryManagementDialog` with foreign-key protection.
- **Stock Transactions**: Atomic Stock IN/OUT workflows (`INBOUND_PURCHASE`, `RETURN_RESTOCK`, `OUTBOUND_SALE`, `ADJUSTMENT_DAMAGE`, `ADJUSTMENT_COUNT`) with negative-stock prevention, operator attribution, and timeframe filtering (`All Time`, `Today`, `Last 7 Days`, `Last 30 Days`).
- **Suppliers Directory**: Vendor CRUD with supplied SKU counts per vendor and instant search.
- **Reports & Exports**: Financial valuation KPIs, horizontal Cost vs. Retail Java2D bar chart by category, reorder deficit schedule, and background export to PDF, Excel (`.xlsx`), CSV, and JSON.
- **Users & RBAC**: Admin-only user management with account creation, role updates (`ADMIN`, `MANAGER`, `STAFF`), password resets, and active/inactive toggling (blocking self-demotion/self-deactivation).
- **Theme Engine & Localization**: Live Light, Dark, and Windows System theme switching via `ThemeManager`, paired with English (LTR) and Arabic (RTL) bundles via `LanguageManager`.

## Prerequisites & Database Setup

1. Install **JDK 17+** and start **MySQL Server 8.0+** on `localhost:3306`.
2. Edit [db.properties](src/main/resources/db.properties) with your local MySQL credentials:
   ```properties
   db.driver=com.mysql.cj.jdbc.Driver
   db.url=jdbc:mysql://localhost:3306/nexus_ims_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
   db.username=root
   db.password=YOUR_MYSQL_PASSWORD
   ```
3. Run the Java migrator to create the schema, load seed data, and generate BCrypt hashes for default accounts:
   ```powershell
   .\mvnw.cmd clean compile
   .\mvnw.cmd exec:java -Dexec.mainClass="com.nexus.ims.util.DatabaseMigrator"
   ```

## Build, Test & Run

```powershell
# Compile and run unit tests
.\mvnw.cmd clean test

# Launch the desktop client
.\mvnw.cmd exec:java

# Build shaded executable JAR
.\mvnw.cmd clean package
java -jar target/nexus-inventory-management-1.0.0-SNAPSHOT.jar
```

## Default Accounts & Permissions

| Username | Password | Role | Access Scope |
| :--- | :--- | :--- | :--- |
| `admin` | `admin123` | **ADMIN** | Full catalog/category/supplier CRUD, Stock IN/OUT, reports/exports, and user administration. |
| `manager` | `manager123` | **MANAGER** | Dashboard, catalog/category/supplier add & edit, Stock IN/OUT, and reports/exports. Delete disabled; Users hidden. |
| `staff` | `staff123` | **STAFF** | Read-only catalog and suppliers; full access to Dashboard, Stock IN/OUT, and reports/exports. |

## Project Structure

```text
├── .gitignore
├── CHANGELOG.md
├── README.md
├── mvnw.cmd
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/nexus/ims/
    │   │   ├── Main.java                       # App initialization & EDT startup
    │   │   ├── controller/                     # AuthController, DashboardController
    │   │   ├── dao/                            # BaseDAO, ProductDAO, CategoryDAO, SupplierDAO, StockTransactionDAO, UserDAO
    │   │   ├── model/                          # Category, Product, StockTransaction, Supplier, User
    │   │   ├── service/                        # AuthService, InventoryService, ReportExportService
    │   │   ├── util/                           # DatabaseConnection, DatabaseMigrator, PasswordHasher, IconFactory, ThemeManager, LanguageManager
    │   │   └── view/                           # Frames, module panels, chart panel, and modal dialogs
    │   └── resources/
    │       ├── db.properties
    │       ├── database/                       # schema.sql, data.sql
    │       └── i18n/                           # messages_en.properties, messages_ar.properties
    └── test/java/com/nexus/ims/service/
        └── InventoryServiceTest.java           # JUnit 5 tests for stock rules & valuation
```
