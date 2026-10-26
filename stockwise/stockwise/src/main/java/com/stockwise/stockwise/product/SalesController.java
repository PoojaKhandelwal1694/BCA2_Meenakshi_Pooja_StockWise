
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

   
public static class SaleItemRequest {
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

public static class SaleRequest {
    private java.util.List<SaleItemRequest> items;

    public java.util.List<SaleItemRequest> getItems() {
        return items;
    }

    public void setItems(java.util.List<SaleItemRequest> items) {
        this.items = items;
    }
}


   
@PostMapping
@Transactional
public ResponseEntity<?> createSale(
        @RequestBody SaleRequest request) {

    if (request == null
            || request.getItems() == null
            || request.getItems().isEmpty()) {
        return ResponseEntity.badRequest().body(
                Map.of("error", "Please add at least one product.")
        );
    }

    // Merge repeated product IDs into one bill item.
    Map<Integer, Integer> requestedItems = new java.util.LinkedHashMap<>();

    for (SaleItemRequest item : request.getItems()) {
        if (item == null
                || item.getProductId() == null
                || item.getProductId() <= 0
                || item.getQuantity() == null
                || item.getQuantity() <= 0) {
            return ResponseEntity.badRequest().body(
                    Map.of("error",
                            "Every product needs a valid ID and positive quantity.")
            );
        }

        requestedItems.merge(
                item.getProductId(),
                item.getQuantity(),
                Math::addExact
        );
    }

    // Lock stock records and validate every item before saving the bill.
    java.util.List<Map<String, Object>> saleLines =
            new java.util.ArrayList<>();

    BigDecimal grandTotal = BigDecimal.ZERO;

    for (Map.Entry<Integer, Integer> entry : requestedItems.entrySet()) {
        int productId = entry.getKey();
        int quantity = entry.getValue();

        var productRows = jdbcTemplate.queryForList("""
            SELECT p.product_id, p.cost_price, p.selling_price,
                   c.expiry_tracked
            FROM products p
            JOIN categories c ON c.category_id = p.category_id
            WHERE p.product_id = ?
            """, productId);

        if (productRows.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", "Product not found: " + productId)
            );
        }

        Map<String, Object> product = productRows.get(0);

        BigDecimal unitCost =
                (BigDecimal) product.get("cost_price");
        BigDecimal unitPrice =
                (BigDecimal) product.get("selling_price");

        boolean expiryTracked =
                Boolean.TRUE.equals(product.get("expiry_tracked"));

        int availableStock;

        if (expiryTracked) {
            var batches = jdbcTemplate.queryForList("""
                SELECT batch_id, quantity
                FROM product_batches
                WHERE product_id = ?
                ORDER BY expiry_date ASC, batch_id ASC
                FOR UPDATE
                """, productId);

            availableStock = batches.stream()
                    .mapToInt(b ->
                            ((Number) b.get("quantity")).intValue())
                    .sum();

        } else {
            var stockRows = jdbcTemplate.queryForList("""
                SELECT quantity
                FROM inventory
                WHERE product_id = ?
                FOR UPDATE
                """, productId);

            if (stockRows.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        Map.of("error",
                                "Inventory record not found for product "
                                        + productId)
                );
            }

            availableStock = ((Number)
                    stockRows.get(0).get("quantity")).intValue();
        }

        if (availableStock < quantity) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "error", "Insufficient stock for product ID "
                                    + productId,
                            "availableStock", availableStock,
                            "requestedQuantity", quantity
                    )
            );
        }

        BigDecimal lineTotal = unitPrice.multiply(
                BigDecimal.valueOf(quantity));

        grandTotal = grandTotal.add(lineTotal);

        Map<String, Object> line = new java.util.HashMap<>();
        line.put("productId", productId);
        line.put("quantity", quantity);
        line.put("unitCost", unitCost);
        line.put("unitPrice", unitPrice);
        line.put("lineTotal", lineTotal);
        line.put("expiryTracked", expiryTracked);
        saleLines.add(line);
    }

    // All items passed validation. Create one bill.
    jdbcTemplate.update(
            "INSERT INTO sales (total_amount) VALUES (?)",
            grandTotal
    );

    Integer saleId = jdbcTemplate.queryForObject(
            "SELECT LAST_INSERT_ID()", Integer.class);

    if (saleId == null) {
        throw new IllegalStateException("Could not create sale.");
    }

    // Save each line and deduct stock.
    for (Map<String, Object> line : saleLines) {
        int productId = (Integer) line.get("productId");
        int quantity = (Integer) line.get("quantity");

        BigDecimal unitCost = (BigDecimal) line.get("unitCost");
        BigDecimal unitPrice = (BigDecimal) line.get("unitPrice");

        boolean expiryTracked = (Boolean) line.get("expiryTracked");

        jdbcTemplate.update("""
            INSERT INTO sale_items
                (sale_id, product_id, quantity,
                 unit_selling_price, unit_cost_at_sale)
            VALUES (?, ?, ?, ?, ?)
            """,
            saleId, productId, quantity, unitPrice, unitCost);

        if (expiryTracked) {
            int remaining = quantity;

            var batches = jdbcTemplate.queryForList("""
                SELECT batch_id, quantity
                FROM product_batches
                WHERE product_id = ?
                ORDER BY expiry_date ASC, batch_id ASC
                FOR UPDATE
                """, productId);

            for (Map<String, Object> batch : batches) {
                if (remaining == 0) {
                    break;
                }

                int batchId = ((Number) batch.get("batch_id")).intValue();
                int batchQuantity =
                        ((Number) batch.get("quantity")).intValue();

                int deduction = Math.min(batchQuantity, remaining);

                if (deduction > 0) {
                    int changed = jdbcTemplate.update("""
                        UPDATE product_batches
                        SET quantity = quantity - ?
                        WHERE batch_id = ? AND quantity >= ?
                        """, deduction, batchId, deduction);

                    if (changed != 1) {
                        throw new IllegalStateException(
                                "Batch stock update failed.");
                    }

                    remaining -= deduction;
                }
            }

            if (remaining > 0) {
                throw new IllegalStateException(
                        "Batch stock changed during sale.");
            }

        } else {
            int changed = jdbcTemplate.update("""
                UPDATE inventory
                SET quantity = quantity - ?
                WHERE product_id = ? AND quantity >= ?
                """, quantity, productId, quantity);

            if (changed != 1) {
                throw new IllegalStateException(
                        "Inventory stock update failed.");
            }
        }

        jdbcTemplate.update("""
            INSERT INTO stock_movements
                (product_id, movement_type, quantity, notes)
            VALUES (?, 'OUT', ?, ?)
            """,
            productId,
            quantity,
            "Sale ID: " + saleId);
    }

    return ResponseEntity.status(HttpStatus.CREATED).body(
            Map.of(
                    "message", "Sale bill recorded successfully.",
                    "saleId", saleId,
                    "items", saleLines,
                    "totalAmount", grandTotal
            )
    );
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
