
package com.stockwise.stockwise.product;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reorder")
@CrossOrigin(origins = "*")
public class SmartReorderController {

    private final JdbcTemplate jdbcTemplate;

    public SmartReorderController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/recommendations")
    public List<Map<String, Object>> getRecommendations() {

        String sql = """
            SELECT
                p.product_id,
                p.product_name,
                p.cost_price,
                p.selling_price,
                i.quantity,
                i.reorder_level
            FROM products p
            INNER JOIN inventory i
                ON p.product_id = i.product_id
            ORDER BY
                CASE
                    WHEN i.quantity <= i.reorder_level THEN 0
                    ELSE 1
                END,
                i.quantity ASC
            """;

        List<Map<String, Object>> products =
                jdbcTemplate.queryForList(sql);

        for (Map<String, Object> product : products) {

            int quantity =
                    ((Number) product.get("quantity")).intValue();

            int reorderLevel =
                    ((Number) product.get("reorder_level")).intValue();

            double costPrice =
                    ((Number) product.get("cost_price")).doubleValue();

            double sellingPrice =
                    ((Number) product.get("selling_price")).doubleValue();

            int targetStock = reorderLevel * 3;

            int suggestedQuantity = Math.max(
                    0, targetStock - quantity
            );

            String status;

            if (quantity < reorderLevel) {
                status = "Reorder Now";
            } else if (quantity == reorderLevel) {
                status = "Reorder Soon";
            } else {
                status = "Stock Available";
            }

            product.put("status", status);
            product.put("suggestedQuantity", suggestedQuantity);
            product.put("estimatedCost",
                    Math.round(suggestedQuantity * costPrice * 100.0) / 100.0);
            product.put("grossProfitPerUnit",
                    Math.round((sellingPrice - costPrice) * 100.0) / 100.0);
        }

        return products;
    }
}
