# Changelog

All notable changes to **NEXUS Inventory Management System (IMS)** are documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project uses [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-11-20

### Added
- **Build & Project Setup**:
  - Configured [pom.xml](pom.xml) for Java 17 (`--release 17`) with `mysql-connector-j` (8.0.33), `flatlaf` (3.4.1), `flatlaf-extras` (3.4.1), `bcrypt` (0.10.2), `openpdf` (2.0.3), `poi-ooxml` (5.3.0), `log4j-core` (2.23.1), and `junit-jupiter` (5.10.2).
  - Included Windows Maven wrapper (`mvnw.cmd`) and repository ignore rules (`.gitignore`).

- **Database & Persistence Layer**:
  - Defined MySQL 8 schema (`src/main/resources/database/schema.sql`) and seed dataset (`src/main/resources/database/data.sql`) covering `users`, `categories`, `suppliers`, `products`, and `stock_transactions`.
  - Built `DatabaseConnection` singleton and `DatabaseMigrator` to initialize tables, seed data, and ensure valid BCrypt password hashes via JDBC.
  - Implemented `BaseDAO<T>`, `ProductDAO`, `CategoryDAO`, `SupplierDAO`, `UserDAO`, and `StockTransactionDAO` (`SELECT ... FOR UPDATE` row locking and rollback safety).

- **Services & Automated Tests**:
  - Added `AuthService` with BCrypt (cost factor 12) password verification and user registration.
  - Added `InventoryService` enforcing positive stock balances, inbound/outbound/adjustment movements, low-stock checks, and `BigDecimal` cost valuation.
  - Added `ReportExportService` generating PDF (OpenPDF), multi-sheet Excel (`.xlsx` via Apache POI), UTF-8 BOM CSV, and formatted JSON exports with one-click file launch.
  - Added `InventoryServiceTest` JUnit 5 suite validating stock deductions, inbound receipts, count adjustments, low-stock checks, and valuation math.

- **Desktop UI, Theme Engine & Localization**:
  - Built Java2D vector icon set (`IconFactory`) replacing Unicode symbol fallbacks across buttons, tables, menus, and empty states.
  - Implemented `ThemeManager` with `Light`, `Dark`, and Windows OS `System` modes (`AppsUseLightTheme` detection), persisted preferences, animated FlatLaf transitions, and high-contrast color tokens/badges.
  - Implemented `LanguageManager` with English (LTR) and Arabic (RTL) resource bundles (`messages_en.properties`, `messages_ar.properties`), live UI switching, recursive `ComponentOrientation` mirroring, and `\u2066...\u2069` LTR isolation on numbers, SKUs, dates, and currency amounts.
  - Built `LoginFrame`, `DashboardFrame`, `SuppliersPanel`, `ReportsPanel`, `UsersPanel`, `CategoryValuationChartPanel`, and modal forms (`ProductFormDialog`, `StockTransactionDialog`, `SupplierFormDialog`, `UserFormDialog`, `CategoryManagementDialog`) driven asynchronously by `AuthController` and `DashboardController`.
  - Added collapsible sidebar (`236px` expanded / `72px` compact rail) with grouped sections and user profile card, header breadcrumbs, interactive KPI cards with 60fps hover transitions, healthy inventory empty states, profit margin and supplied-SKU columns, smart row-selection button states, right-click context menu on products, and timeframe filtering on transactions.
