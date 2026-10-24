
package com.stockwise.stockwise.product;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/sales")
@CrossOrigin(origins = {
        "http://127.0.0.1:5500",
        "http://localhost:5500"
})
public class SalesController {

    private final JdbcTemplate jdbcTemplate;

    public SalesController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public static class SaleRequest {
        private Integer productId;
        private Integer quantity;

        public Integer getProductId() {
            return productId;
        }

        public void setProductId(Integer productId) {
            this.productId = productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> createSale(
            @RequestBody SaleRequest request) {

        if (request == null
                || request.getProductId() == null
                || request.getProductId() <= 0
                || request.getQuantity() == null
                || request.getQuantity() <= 0) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            "Valid product ID and positive quantity are required."
                    ));
        }

        Integer productId = request.getProductId();
        int quantity = request.getQuantity();

        // Lock inventory row to prevent overselling.
        var stockRows = jdbcTemplate.queryForList(
                "SELECT quantity FROM inventory "
                        + "WHERE product_id = ? FOR UPDATE",
                productId
        );

        if (stockRows.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error",
                            "Inventory record not found for this product."));
        }

        int availableStock =
                ((Number) stockRows.get(0).get("quantity")).intValue();

        if (availableStock < quantity) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "Insufficient stock.",
                            "availableStock", availableStock
                    ));
        }

        var productRows = jdbcTemplate.queryForList(
                "SELECT cost_price, selling_price "
                        + "FROM products WHERE product_id = ?",
                productId
        );

        if (productRows.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Product not found."));
        }

        BigDecimal unitCost =
                (BigDecimal) productRows.get(0).get("cost_price");

        BigDecimal unitPrice =
                (BigDecimal) productRows.get(0).get("selling_price");

        BigDecimal total = unitPrice.multiply(
                BigDecimal.valueOf(quantity));

        // 1. Create sale header.
        jdbcTemplate.update(
                "INSERT INTO sales (total_amount) VALUES (?)",
                total
        );

        Integer saleId = jdbcTemplate.queryForObject(
                "SELECT LAST_INSERT_ID()",
                Integer.class
        );

        if (saleId == null) {
            throw new IllegalStateException("Could not create sale.");
        }

        // 2. Save the sold product and its price/cost snapshot.
        jdbcTemplate.update(
                "INSERT INTO sale_items "
                        + "(sale_id, product_id, quantity, "
                        + "unit_selling_price, unit_cost_at_sale) "
                        + "VALUES (?, ?, ?, ?, ?)",
                saleId, productId, quantity, unitPrice, unitCost
        );

        // 3. Reduce inventory.
        int updated = jdbcTemplate.update(
                "UPDATE inventory "
                        + "SET quantity = quantity - ? "
                        + "WHERE product_id = ? AND quantity >= ?",
                quantity, productId, quantity
        );

        if (updated != 1) {
            throw new IllegalStateException(
                    "Stock update failed. Sale has been rolled back.");
        }

        // 4. Record stock movement.
        jdbcTemplate.update(
                "INSERT INTO stock_movements "
                        + "(product_id, movement_type, quantity, notes) "
                        + "VALUES (?, 'OUT', ?, ?)",
                productId,
                quantity,
                "Sale ID: " + saleId
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Sale recorded successfully.",
                        "saleId", saleId,
                        "productId", productId,
                        "quantitySold", quantity,
                        "unitSellingPrice", unitPrice,
                        "totalAmount", total,
                        "remainingStock", availableStock - quantity
                ));
    }

    
    
    // GET: Fetch Sales History, optionally filtered by date
    @GetMapping("/history")
    public ResponseEntity<?> getSalesHistory(
            @RequestParam(required = false) String date) {

        String sql =
                "SELECT " +
                "s.sale_id AS saleId, " +
                "s.sale_date AS saleDate, " +
                "p.product_name AS productName, " +
                "si.quantity AS quantitySold, " +
                "si.unit_selling_price AS sellingPrice, " +
                "(si.quantity * si.unit_selling_price) AS totalAmount " +
                "FROM sales s " +
                "JOIN sale_items si ON s.sale_id = si.sale_id " +
                "JOIN products p ON si.product_id = p.product_id " +
                "WHERE (? IS NULL OR s.sale_date >= ? AND s.sale_date < DATE_ADD(?, INTERVAL 1 DAY)) " +
                "ORDER BY s.sale_id DESC";

        var salesHistory = jdbcTemplate.queryForList(
                sql, date, date, date
        );

        return ResponseEntity.ok(salesHistory);
    }


}
