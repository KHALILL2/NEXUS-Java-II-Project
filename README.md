# Inventory Management System (IMS)

[![Java Version](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Build Tool](https://img.shields.io/badge/Build-Maven-orange.svg)](https://maven.apache.org/)
[![Database](https://img.shields.io/badge/Database-MySQL-informational.svg)](https://www.mysql.com/)
[![Architecture](https://img.shields.io/badge/Architecture-MVC%20Desktop-green.svg)](#architecture-overview)

---

## 1. Project Overview

The **NEXUS Inventory Management System (IMS)** is a desktop application developed for the Java II curriculum. Built with standard **Java 17+**, **Swing (FlatLaf UI)**, and a **MySQL** relational database, it follows standard **Model-View-Controller (MVC)** and **Data Access Object (DAO)** enterprise architectural patterns.

### Core Capabilities
- **Product & Inventory Management**: Stock tracking, low-stock threshold alerting, categorisation.
- **Supplier & Order Management**: Supplier records, automated purchase recording.
- **Stock Transactions**: Audit logging for inbound shipments, sales stock reductions, adjustments.
- **Role-Based Access Control (RBAC)**: Secure multi-tier authentication (Admin, Manager, Staff) with salted BCrypt password hashing.
- **Interactive UI**: Responsive desktop GUI with modern desktop theming.

---

## 2. Team NEXUS Members & Project Roles

| Member Name | Role & Core Responsibilities |
| :--- | :--- |
| **Team Lead / DevOps** | Architecture definition, Git repository lifecycle, CI/CD configuration, security standards. |
| **Backend & Database Engineer** | Database schema design (`schema.sql`), JDBC connection pooling, DAO implementation. |
| **Business Logic & Service Layer** | Service transaction handling, stock level algorithms, input validation, reporting logic. |
| **GUI & UI/UX Developer** | Swing components, FlatLaf theme integration, MVC View event bindings and dialogs. |
| **QA & Verification Engineer** | Unit tests (JUnit 5), data integrity verification, manual system testing. |

---

## 3. Architecture Overview

The system strictly decouples user interface from business rules and persistence:

```
src/main/java/com/nexus/ims/
├── Main.java                 # Application Bootstrap & Entry Point
├── model/                    # Domain Entities (POJOs representing DB tables)
│   ├── Product.java
│   ├── Category.java
│   ├── Supplier.java
│   ├── StockTransaction.java
│   └── User.java
├── dao/                      # Data Access Objects (CRUD & JDBC Transactions)
│   ├── BaseDao.java
│   ├── ProductDao.java
│   └── UserDao.java
├── service/                  # Business Logic & Workflow Validations
│   ├── ProductService.java
│   └── AuthService.java
├── view/                     # Presentation Tier (Swing Frames, Panels, Tables)
│   ├── LoginFrame.java
│   └── MainFrame.java
├── controller/               # Event Listeners & Mediators (View <-> Service)
│   ├── AuthController.java
│   └── ProductController.java
└── util/                     # Cross-Cutting Infrastructure Utilities
    ├── Constants.java
    ├── DatabaseConnection.java
    └── PasswordHasher.java
```

---

## 4. Prerequisites

Before setting up and running the application, ensure your environment meets the following specifications:

1. **Java Development Kit (JDK)**: Version 17 or higher (`jdk-17+`).
   ```bash
   java -version
   javac -version
   ```
2. **Apache Maven**: Version 3.8+ (included in most IDE distributions or via wrapper).
   ```bash
   mvn -version
   ```
3. **Database Server**: MySQL Server 8.0+ running locally or remotely on default port `3306`.
4. **IDE**: VS Code (with Extension Pack for Java), Antigravity IDE, or IntelliJ IDEA.

---

## 5. Database Setup

1. Launch your MySQL command-line client or MySQL Workbench:
   ```bash
   mysql -u root -p
   ```
2. Execute the schema initialization script located at `src/main/resources/schema.sql` (or copy into your SQL console):
   ```sql
   CREATE DATABASE IF NOT EXISTS nexus_ims_db;
   USE nexus_ims_db;
   ```
3. Configure your database credentials in `src/main/resources/application.properties` (or copy from `application.properties.example`):
   ```properties
   db.url=jdbc:mysql://localhost:3306/nexus_ims_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
   db.username=root
   db.password=YOUR_PASSWORD_HERE
   ```

---

## 6. Build & Execution Instructions

### A. Clone and Setup
```bash
git clone https://github.com/KHALILL2/NEXUS-Java-II-Project.git
cd "NEXUS-Java-II-Project"
```

### B. Compile the Project
```bash
mvn clean compile
```

### C. Run the Application
```bash
mvn exec:java
```

### D. Package into a Standalone Executable Fat-JAR
```bash
mvn clean package
```
The compiled self-contained executable JAR will be located at:
```bash
java -jar target/nexus-inventory-management-1.0.0-SNAPSHOT.jar
```

---

## 7. Version Control & Git Strategy

- **Default Branch**: `main` (Production-ready and reviewed codebase).
- **Feature Branches**: `feature/<feature-name>` (e.g., `feature/login-auth`, `feature/product-crud`).
- **Commit Convention**: Conventional Commits (e.g., `feat: implement user authentication`, `fix: handle null product category`).

