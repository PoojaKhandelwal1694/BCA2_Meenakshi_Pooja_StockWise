
package com.stockwise.stockwise.inventory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = {
    "http://127.0.0.1:5500",
    "http://localhost:5500"
})
public class LowStockController {

    private final JdbcTemplate jdbcTemplate;

    public LowStockController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/low-stock")
    public List<Map<String, Object>> getLowStockProducts() {

        String sql = """
            SELECT
                p.product_id AS productId,
                p.product_name AS productName,
                i.quantity AS quantity,
                i.reorder_level AS reorderLevel
            FROM products p
            INNER JOIN inventory i
                ON p.product_id = i.product_id
            WHERE i.quantity <= i.reorder_level
            ORDER BY i.quantity ASC
            """;

        return jdbcTemplate.queryForList(sql);
    }
}
