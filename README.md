# StockWise 

**StockWise** is a Java-based inventory management application built with Spring Boot and MySQL. It is designed to help manage products, categories, inventory levels, sales, and low-stock alerts through a centralized backend application.

## 1. Problem or Frustration

Managing inventory manually or across disconnected systems can create several problems:

- Difficulty maintaining accurate stock records.
- Time-consuming product and category management.
- Errors when updating inventory and recording sales.
- Delays in identifying products with low stock.
- Limited visibility into inventory and sales information.
- Missed opportunities to reorder products before stock runs out.

These challenges make inventory management less efficient and can affect day-to-day business operations.

## 2. Solution

StockWise provides a centralized application for organizing inventory-related operations using Java, Spring Boot, and MySQL.

The application separates its functionality into components for products, categories, sales, authentication, inventory monitoring, and email notifications. Spring Data JPA supports database operations, while scheduled tasks support low-stock email alerts.

The goal is to simplify stock management, keep inventory records organized, and help identify products that may need replenishment.

## 3. Project Structure

The main application is located inside the `stockwise` subfolder.


JavaProject/
└── stockwise/
    ├── README.md
    └── stockwise/
        ├── pom.xml
        ├── mvnw
        ├── mvnw.cmd
        ├── HELP.md
        ├── product.java
        └── src/
            ├── main/
            │   ├── java/com/stockwise/stockwise/
            │   │   ├── StockwiseApplication.java
            │   │   ├── auth/
            │   │   ├── batch/
            │   │   ├── category/
            │   │   ├── config/
            │   │   ├── inventory/
            │   │   └── product/
            │   └── resources/
            │       ├── application.properties
            │       ├── static/
            │       └── templates/
            └── test/


### Main Components

- **auth/** — user model, repository, authentication controller, and security configuration.
- **batch/** — product batch management.
- **category/** — category model, repository, and controller.
- **config/** — email alert service and scheduled low-stock notifications.
- **inventory/** — low-stock inventory controller.
- **product/** — product management, category responses, sales, and smart reorder functionality.
- **StockwiseApplication.java** — Spring Boot application entry point.
- **application.properties** — application, database, and email configuration.
- **pom.xml** — Maven dependencies and build configuration.

## 4. Features

- **Product Management:** Components for working with product records.
- **Category Management:** Organize products by category.
- **Inventory Monitoring:** Identify products with low stock.
- **Sales Management:** Dedicated sales controller functionality.
- **Smart Reordering:** A dedicated smart reorder controller.
- **Product Batches:** Components for managing product batch records.
- **Authentication:** User-related components and security configuration.
- **Email Alerts:** Email service and scheduled low-stock notifications.
- **MySQL Integration:** Persistent database operations through Spring Data JPA.
- **REST Backend:** Spring Boot web controllers for application operations.

The exact behavior of each feature depends on its implementation and configuration.

## 5. How to Run the Project

### Prerequisites

Install or prepare the following:

- Java JDK 21
- MySQL Server
- Maven, or use the included Maven Wrapper
- Git (optional, for cloning the repository)

### Step 1: Open the Application Directory

From Windows Command Prompt:

```cmd
cd /d D:\JavaProject\stockwise\stockwise
```

### Step 2: Create the Database

Create the MySQL database:

```sql
CREATE DATABASE stockwise;
```

The application is configured to use a local MySQL database named `stockwise`.

Make sure the required tables and schema are available before starting the application.

### Step 3: Configure Database and Email Settings

Open:

`src/main/resources/application.properties`

Configure your database connection and email settings for your local environment.

Use environment variables or another secure configuration method for passwords and email credentials. Do not publish real credentials in GitHub.

### Step 4: Build the Application

On Windows, run:

```cmd
mvnw.cmd clean package
```

Alternatively, if Maven is installed:

```cmd
mvn clean package
```

### Step 5: Run the Application

Using the Maven Wrapper:

```cmd
mvnw.cmd spring-boot:run
```

Or with Maven installed:

```cmd
mvn spring-boot:run
```

The application is configured to use port **8080**.

### Step 6: Access the Application

After the application starts, its local base URL is:

`http://localhost:8080`

The available API endpoints depend on the controller mappings in the source code.

## Technologies Used

- Java 21
- Spring Boot
- Spring Web MVC
- Spring Data JPA
- Spring Security Crypto
- MySQL
- Maven
- Jakarta Bean Validation
- Spring Mail
- Git and GitHub


## Author

Developed as an academic project.
