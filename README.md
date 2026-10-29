# NEXUS Inventory Management System (IMS)

[![Java Version](https://img.shields.io/badge/Java-17%20LTS-007396?logo=openjdk&logoColor=white)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Build Tool](https://img.shields.io/badge/Build-Maven%203.9-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![UI Framework](https://img.shields.io/badge/UI-FlatLaf%203.4-4A90E2)](https://www.formdev.com/flatlaf/)
[![Database](https://img.shields.io/badge/Database-MySQL%208.0-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Architecture](https://img.shields.io/badge/Architecture-MVC%20%2B%20DAO-brightgreen)](#system-architecture)
[![License](https://img.shields.io/badge/License-Academic%20Use-blue.svg)](#)

---

## 1. Project Overview

The **NEXUS Inventory Management System (IMS)** is an enterprise desktop application designed for modern warehouse operations, catalog tracking, and inventory auditing. Built with **Java 17 LTS**, **Swing (FlatLaf Look & Feel)**, and **MySQL 8.0**, the system adheres to strict **Model-View-Controller (MVC)** and **Data Access Object (DAO)** architectural standards.

### Key Capabilities
- **Role-Based Access Control (RBAC)**: Multi-tier operator authentication (`ADMIN`, `MANAGER`, `STAFF`) secured via salted BCrypt password hashing (Cost Factor 12).
- **Product Catalog Management**: Track inventory items with unique SKU constraints, real-time live search, categorization, and pricing calculations.
- **ACID-Compliant Stock Movement**: Atomic stock increments (receipts) and decrements (sales dispatch) protected by database row-level locking (`SELECT ... FOR UPDATE`) with automatic rollback on error.
- **Stock Threshold Monitoring**: Automatic low-stock visual badging and warnings whenever `quantity_in_stock <= reorder_level`.
- **Financial Asset Valuation**: Real-time warehouse valuation computing total financial asset worth using high-precision `BigDecimal` arithmetic (`SUM(quantity * purchasePrice)`).
- **Modern Desktop UI**: Polished FlatLaf theme, High-DPI screen scaling, dynamic `CardLayout` view switching, live KPI cards, and live system status clock.

---

## 2. System Architecture

The application strictly separates presentation, business rules, and persistence:

```
[ User Interaction: Desktop UI ]
               │
               ▼
┌──────────────────────────────┐
│  Presentation Tier (View)     │  Swing Frames (LoginFrame, DashboardFrame)
└──────────────┬───────────────┘  Pure visual renderers with no business logic
               │ ActionEvents / Delegation
               ▼
┌──────────────────────────────┐
│  Controller Tier (MVC)       │  AuthController, DashboardController
└──────────────┬───────────────┘  Asynchronous SwingWorker task execution
               │
               ▼
┌──────────────────────────────┐
│  Service Tier (Logic)        │  InventoryService, AuthService, ProductService
└──────────────┬───────────────┘  Validations, non-negative stock guards, valuation
               │
               ▼
┌──────────────────────────────┐
│  Persistence Tier (DAO)      │  ProductDAO, StockTransactionDAO, UserDao
└──────────────┬───────────────┘  Parameterized PreparedStatement queries & transactions
               │
               ▼
┌──────────────────────────────┐
│  Relational Database         │  MySQL 8.0 Server (nexus_ims_db)
└──────────────────────────────┘
```

### Complete Codebase Layout:
```
C:\.vscode\NEXUS Java II Project\
├── .gitignore                                      # Comprehensive production ignore rules
├── CHANGELOG.md                                    # Semantic version release history
├── pom.xml                                         # Maven project descriptor (Java 17, FlatLaf, MySQL, JUnit)
├── README.md                                       # System technical manual & documentation
├── mvnw.cmd                                        # Embedded zero-install Maven Wrapper for Windows
├── run.ps1                                         # Local PowerShell launcher script
├── migrate-db.ps1                                  # Automated MySQL detection and database migrator
├── install-maven-permanently.ps1                   # Permanent global Maven installer
└── src/
    ├── main/
    │   ├── resources/
    │   │   ├── db.properties                       # Active JDBC configuration (URL, user, password)
    │   │   ├── application.properties.example      # Credential configuration template
    │   │   └── database/
    │   │       ├── schema.sql                      # DDL: Tables, foreign keys, check constraints, indexes
    │   │       └── data.sql                        # DML: Default roles, categories, and initial products
    │   └── java/
    │       └── com/nexus/ims/
    │           ├── Main.java                       # Application Entry Point & FlatLaf Theme Bootstrap
    │           ├── model/                          # Domain Entities (POJOs)
    │           │   ├── Product.java                # SKU, pricing, stock, reorder boundaries
    │           │   ├── Category.java               # Inventory category classification
    │           │   ├── Supplier.java               # Vendor contact & address metadata
    │           │   ├── StockTransaction.java       # Immutable stock movement audit ledger
    │           │   └── User.java                   # User entity with Role-Based Access Control
    │           ├── dao/                            # Data Access Objects (Raw JDBC)
    │           │   ├── BaseDAO.java                # Generic CRUD contract interface
    │           │   ├── ProductDAO.java             # Product catalog persistence & low-stock queries
    │           │   ├── StockTransactionDAO.java    # Atomic ACID stock movement transaction manager
    │           │   └── UserDao.java                # User lookups and credential persistence
    │           ├── service/                        # Business Logic & Rules
    │           │   ├── InventoryService.java       # Stock bounds validation & financial valuation
    │           │   ├── AuthService.java            # BCrypt credential verification & user creation
    │           │   └── ProductService.java         # Catalog validation workflows
    │           ├── view/                           # Presentation Tier (Swing / FlatLaf)
    │           │   ├── LoginFrame.java             # Centered login window with password toggle
    │           │   └── DashboardFrame.java         # Multi-pane dashboard with sidebar, cards, and status bar
    │           ├── controller/                     # Event Mediators (View <-> Service)
    │           │   ├── AuthController.java         # Authentication flow and view transitions
    │           │   └── DashboardController.java    # Navigation, search filter, and transaction modal bindings
    │           └── util/                           # Infrastructure Utilities
    │               ├── Constants.java              # Application constants & configuration keys
    │               ├── DatabaseConnection.java     # Thread-safe Singleton JDBC connection factory
    │               ├── DatabaseMigrator.java       # Native Java/JDBC schema and seed executor
    │               └── PasswordHasher.java         # Salted BCrypt password hasher (Cost 12)
    └── test/
        └── java/
            └── com/nexus/ims/service/
                └── InventoryServiceTest.java       # JUnit 5 unit tests for business validation rules
```

---

## 3. Prerequisites

Ensure your system meets the following specifications:
1. **Java Development Kit (JDK)**: Version 17 LTS or higher.
   ```powershell
   java -version
   ```
2. **Database Server**: MySQL Server 8.0+ running on port `3306` (or via XAMPP / WAMP / Laragon).
3. **Build Tool**: Apache Maven (or use the included `mvnw.cmd` wrapper with zero local installation).

---

## 4. Database Setup & Migration

### Step A: Configure Credentials
Ensure your database credentials in [`src/main/resources/db.properties`](file:///C:/.vscode/NEXUS%20Java%20II%20Project/src/main/resources/db.properties) match your local MySQL configuration:
```properties
db.driver=com.mysql.cj.jdbc.Driver
db.url=jdbc:mysql://localhost:3306/nexus_ims_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
db.username=root
db.password=YOUR_MYSQL_PASSWORD
```

### Step B: Execute Migration
Run **either** of the following automated migration solutions:

#### Option 1: Native Java Migrator (Recommended — No `mysql.exe` CLI required)
```powershell
cd "C:\.vscode\NEXUS Java II Project"
.\mvnw.cmd clean compile
.\mvnw.cmd exec:java -Dexec.mainClass="com.nexus.ims.util.DatabaseMigrator"
```

#### Option 2: Automated PowerShell Script (Locates MySQL and registers PATH)
```powershell
cd "C:\.vscode\NEXUS Java II Project"
.\migrate-db.ps1
```

---

## 5. Build, Test, & Run Instructions

All commands can be executed using the included Maven Wrapper (`.\mvnw.cmd`) without needing Maven installed in your system PATH.

### 1. Compile the Project
```powershell
.\mvnw.cmd clean compile
```

### 2. Execute Automated Unit Tests
```powershell
.\mvnw.cmd test
```

### 3. Launch the Application
```powershell
.\mvnw.cmd exec:java
```

### 4. Build an Executable Standalone Fat-JAR
```powershell
.\mvnw.cmd clean package
```
The compiled self-contained executable JAR will be located at:
```powershell
java -jar target/nexus-inventory-management-1.0.0-SNAPSHOT.jar
```

---

## 6. Default Login Credentials

| Username | Password | Role | Access Level |
| :--- | :--- | :--- | :--- |
| `admin` | `admin123` | **ADMIN** | Full administrative rights, user management, and stock adjustments. |
| `manager` | `manager123` | **MANAGER** | Inventory updates, purchase receipts, and sales dispatch. |
| `staff` | `staff123` | **STAFF** | Catalog viewing, stock inquiries, and basic operational logging. |

---

## 7. Team NEXUS Members & Project Roles

| Member Name | Role & Core Responsibilities |
| :--- | :--- |
| **Team Lead / DevOps** | Git lifecycle, Maven infrastructure, environment configuration, documentation. |
| **Backend & Database Engineer** | Relational schema design (`schema.sql`), JDBC connection pooling, DAO implementation. |
| **Business Logic Specialist** | Business validations, stock bounds enforcement, financial valuation algorithms. |
| **UI/UX Desktop Engineer** | Swing presentation layer, FlatLaf modern theming, CardLayout view transitions. |
| **QA & Verification Engineer** | JUnit 5 unit testing, data integrity verification, manual test workflows. |

---

## 8. Version Control & Git Strategy

- **Default Branch**: `main` (Production-ready and reviewed codebase).
- **Feature Branches**: `feature/<feature-name>` (e.g. `feature/login-auth`, `feature/inventory-service`).
- **Commit Convention**: Conventional Commits (e.g. `feat: implement stock transaction dao`, `fix: handle null supplier id`).
