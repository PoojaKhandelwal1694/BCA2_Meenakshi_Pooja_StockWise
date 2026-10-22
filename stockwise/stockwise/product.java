package com.stockwise.stockwise.product;

import java.sql.*;
import java.util.Scanner;
import java.math.BigDecimal;

public class product {

    static final String URL = "jdbc:mysql://localhost:3306/stockwise";
    static final String DB_USER = "root";
    static final String DB_PASSWORD = "";

    // Database connection
    static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
            URL, DB_USER, DB_PASSWORD
        );
    }

    // Display available categories
    static void showCategories() throws SQLException {

        String sql = "SELECT category_id, category_name FROM categories";

        try (Connection con = getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            System.out.println("\n--- Available Categories ---");

            boolean found = false;

            while (rs.next()) {
                found = true;
                System.out.println(
                    rs.getInt("category_id") + " - "
                    + rs.getString("category_name")
                );
            }

            if (!found) {
                System.out.println("No categories found. Add a category first.");
            }
        }
    
    }



   

    // 1. Add product
    static void addProduct(Scanner sc) throws SQLException {

        showCategories();

        System.out.print("\nEnter product name: ");
        String name = sc.nextLine().trim();

        System.out.print("Enter SKU: ");
        String sku = sc.nextLine().trim();

        System.out.print("Enter category ID: ");
        int categoryId = Integer.parseInt(sc.nextLine());

        System.out.print("Enter cost price: ");
        BigDecimal cost = new BigDecimal(sc.nextLine());

        System.out.print("Enter selling price: ");
        BigDecimal selling = new BigDecimal(sc.nextLine());

        System.out.print("Enter attributes: ");
        String attributes = sc.nextLine().trim();

        if (name.isEmpty() || sku.isEmpty()
                || cost.signum() < 0 || selling.signum() < 0) {
            System.out.println("Invalid product details.");
            return;
        }

        String sql = "INSERT INTO products "
            + "(product_name, sku, category_id, cost_price, "
            + "selling_price, attributes) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, sku);
            ps.setInt(3, categoryId);
            ps.setBigDecimal(4, cost);
            ps.setBigDecimal(5, selling);
            ps.setString(6, attributes);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Product added successfully!");
            }
        }
    }

    // 2. View all products with category
    static void viewProducts() throws SQLException {

        String sql =
            "SELECT p.product_id, p.product_name, p.sku, "
            + "c.category_name, p.cost_price, p.selling_price, "
            + "p.attributes "
            + "FROM products p "
            + "LEFT JOIN categories c "
            + "ON p.category_id = c.category_id "
            + "ORDER BY p.product_id";

        try (Connection con = getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            boolean found = false;

            System.out.println("\n--- StockWise Products ---");

            while (rs.next()) {
                found = true;

                System.out.println(
                    "\nProduct ID: " + rs.getInt("product_id")
                    + "\nName: " + rs.getString("product_name")
                    + "\nSKU: " + rs.getString("sku")
                    + "\nCategory: " + rs.getString("category_name")
                    + "\nCost Price: " + rs.getBigDecimal("cost_price")
                    + "\nSelling Price: " + rs.getBigDecimal("selling_price")
                    + "\nAttributes: " + rs.getString("attributes")
                );
            }

            if (!found) {
                System.out.println("No products found.");
            }
        }
    }

    // 3. Update product
    static void updateProduct(Scanner sc) throws SQLException {

        viewProducts();

        System.out.print("\nEnter product ID to update: ");
        int id = Integer.parseInt(sc.nextLine());

        showCategories();

        System.out.print("Enter new product name: ");
        String name = sc.nextLine().trim();

        System.out.print("Enter new SKU: ");
        String sku = sc.nextLine().trim();

        System.out.print("Enter new category ID: ");
        int categoryId = Integer.parseInt(sc.nextLine());

        System.out.print("Enter new cost price: ");
        BigDecimal cost = new BigDecimal(sc.nextLine());

        System.out.print("Enter new selling price: ");
        BigDecimal selling = new BigDecimal(sc.nextLine());

        System.out.print("Enter new attributes: ");
        String attributes = sc.nextLine().trim();

        if (name.isEmpty() || sku.isEmpty()
                || cost.signum() < 0 || selling.signum() < 0) {
            System.out.println("Invalid product details.");
            return;
        }

        String sql = "UPDATE products SET product_name = ?, "
            + "sku = ?, category_id = ?, cost_price = ?, "
            + "selling_price = ?, attributes = ? "
            + "WHERE product_id = ?";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, sku);
            ps.setInt(3, categoryId);
            ps.setBigDecimal(4, cost);
            ps.setBigDecimal(5, selling);
            ps.setString(6, attributes);
            ps.setInt(7, id);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Product updated successfully!");
            } else {
                System.out.println("Product ID not found.");
            }
        }
    }

    // 4. Delete product
    static void deleteProduct(Scanner sc) throws SQLException {

        viewProducts();

        System.out.print("\nEnter product ID to delete: ");
        int id = Integer.parseInt(sc.nextLine());

        System.out.print("Are you sure? Enter YES to delete: ");
        String confirm = sc.nextLine();

        if (!confirm.equalsIgnoreCase("YES")) {
            System.out.println("Deletion cancelled.");
            return;
        }

        String sql = "DELETE FROM products WHERE product_id = ?";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Product deleted successfully!");
            } else {
                System.out.println("Product ID not found.");
            }
        }
    }



 // Add Categories in Database


    static void addCategory() {
    Scanner sc = new Scanner(System.in);

    System.out.print("Enter category name: ");
    String name = sc.nextLine();

    System.out.print("Enter category description: ");
    String description = sc.nextLine();

    String sql = "INSERT INTO categories (category_name, description) VALUES (?, ?)";

    try (Connection con = DriverManager.getConnection(URL, DB_USER, DB_PASSWORD);
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, name);
        ps.setString(2, description);

        int rows = ps.executeUpdate();

        if (rows > 0) {
            System.out.println("Category added successfully!");
        }

    } catch (SQLException e) {
        System.out.println("Error adding category: " + e.getMessage());
    }
}


    // Main menu
    public static void main(String[] args) {

        try (Scanner sc = new Scanner(System.in)) {

            while (true) {
                System.out.println("1. Add Product");
                System.out.println("2. View Products");
                System.out.println("3. Update Product");
                System.out.println("4. Delete Product");
                System.out.println("5. View Categories");
                System.out.println("6. Add Category");
                System.out.println("7. Exit");

                String choice = sc.nextLine();

                try {
                    switch (choice) {
                        case "1":
                            addProduct(sc);
                            break;
                        case "2":
                            viewProducts();
                            break;
                        case "3":
                            updateProduct(sc);
                            break;
                        case "4":
                            deleteProduct(sc);
                            break;
                        case "5":
                            showCategories();
                            break;

                            case "6":
                                addCategory();
                                break;
                        case "7":
                            System.out.println("Exiting Product Management.");
                            return;
                        default:
                            System.out.println("Invalid choice.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Please enter a valid number or price.");
                } catch (SQLIntegrityConstraintViolationException e) {
                    System.out.println(
                        "Operation failed: duplicate SKU, invalid category, "
                        + "or product is referenced by another table."
                    );
                } 
                catch (SQLException e) {
                    System.out.println("Database error: " + e.getMessage());
                }
            }

        } 
        catch (SQLException e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }
}
