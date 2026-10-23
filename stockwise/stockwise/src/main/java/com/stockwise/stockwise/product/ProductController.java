package com.stockwise.stockwise.product;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = {
        "http://127.0.0.1:5500",
        "http://localhost:5500"
})
public class ProductController {

    private final ProductRepository productRepository;
    private final JdbcTemplate jdbcTemplate;

    public ProductController(
            ProductRepository productRepository,
            JdbcTemplate jdbcTemplate) {
        this.productRepository = productRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    // 1. Get all products
    
@GetMapping
public List<Map<String, Object>> getAllProducts() {

    String sql = """
        SELECT
            p.product_id AS productId,
            p.product_name AS productName,
            p.sku AS sku,
            p.category_id AS categoryId,
            p.cost_price AS costPrice,
            p.selling_price AS sellingPrice,
            i.quantity AS quantity,
            i.reorder_level AS reorderLevel
        FROM products p
        LEFT JOIN inventory i
            ON p.product_id = i.product_id
        ORDER BY p.product_id DESC
        """;

    return jdbcTemplate.queryForList(sql);
}

        
    // Get products with category details
    @GetMapping("/grouped")
    public List<ProductCategoryResponse> getProductsWithCategories() {

        
String sql = """
    SELECT
        p.product_id,
        p.product_name,
        p.sku,
        p.category_id,
        c.category_name,
        i.quantity,
        i.reorder_level
    FROM products p
    LEFT JOIN categories c
        ON p.category_id = c.category_id
    LEFT JOIN inventory i
        ON p.product_id = i.product_id
    ORDER BY c.category_name, p.product_name
    """;

return jdbcTemplate.query(sql, (rs, rowNum) ->
        new ProductCategoryResponse(
                rs.getInt("product_id"),
                rs.getString("product_name"),
                rs.getString("sku"),
                rs.getInt("category_id"),
                rs.getString("category_name"),
                (Integer) rs.getObject("quantity"),
                (Integer) rs.getObject("reorder_level")
        )
);

    }



    // 2. Get product by ID
    @GetMapping("/{id}")
public ResponseEntity<Map<String, Object>> getProductById(
        @PathVariable Integer id) {

    String sql = """
        SELECT
            p.product_id AS productId,
            p.product_name AS productName,
            p.sku AS sku,
            p.category_id AS categoryId,
            p.cost_price AS costPrice,
            p.selling_price AS sellingPrice,
            p.attributes AS attributes,
            i.quantity AS quantity,
            i.reorder_level AS reorderLevel
        FROM products p
        LEFT JOIN inventory i
            ON p.product_id = i.product_id
        WHERE p.product_id = ?
        """;

    List<Map<String, Object>> products =
            jdbcTemplate.queryForList(sql, id);

    if (products.isEmpty()) {
        return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok(products.get(0));
}

    // 3. Add product and inventory together
    @PostMapping
    @Transactional
    public ResponseEntity<?> addProduct(
            @RequestBody ProductCreateRequest request) {

        if (!isValid(request)) {
            return ResponseEntity.badRequest()
                    .body("Please enter valid product and inventory details.");
        }

        if (productRepository.existsBySku(request.getSku().trim())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("This SKU already exists.");
        }

        Product product = new Product();
        product.setProductName(request.getProductName().trim());
        product.setSku(request.getSku().trim());
        product.setCategoryId(request.getCategoryId());
        product.setCostPrice(request.getCostPrice());
        product.setSellingPrice(request.getSellingPrice());
        product.setAttributes(request.getAttributes());

        // Save product first; database generates product_id
        Product savedProduct = productRepository.save(product);

        // Save stock against the generated product_id
        jdbcTemplate.update(
                "INSERT INTO inventory (product_id, quantity, reorder_level) " +
                        "VALUES (?, ?, ?)",
                savedProduct.getProductId(),
                request.getQuantity(),
                request.getReorderLevel()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savedProduct);
    }

    // 4. Update product and its inventory values
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateProduct(
            @PathVariable Integer id,
            @RequestBody ProductCreateRequest request) {

        if (!isValid(request)) {
            return ResponseEntity.badRequest()
                    .body("Please enter valid product and inventory details.");
        }

        return productRepository.findById(id)
                .map(existingProduct -> {

                    if (productRepository.existsBySkuAndProductIdNot(
                            request.getSku().trim(), id)) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body("This SKU already exists.");
                    }

                    existingProduct.setProductName(
                            request.getProductName().trim());
                    existingProduct.setSku(request.getSku().trim());
                    existingProduct.setCategoryId(request.getCategoryId());
                    existingProduct.setCostPrice(request.getCostPrice());
                    existingProduct.setSellingPrice(request.getSellingPrice());
                    existingProduct.setAttributes(request.getAttributes());

                    Product savedProduct =
                            productRepository.save(existingProduct);

                    Integer inventoryCount = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM inventory WHERE product_id = ?",
                            Integer.class,
                            id
                    );

                    if (inventoryCount != null && inventoryCount > 0) {
                        jdbcTemplate.update(
                                "UPDATE inventory " +
                                        "SET quantity = ?, reorder_level = ? " +
                                        "WHERE product_id = ?",
                                request.getQuantity(),
                                request.getReorderLevel(),
                                id
                        );
                    } else {
                        jdbcTemplate.update(
                                "INSERT INTO inventory " +
                                        "(product_id, quantity, reorder_level) " +
                                        "VALUES (?, ?, ?)",
                                id,
                                request.getQuantity(),
                                request.getReorderLevel()
                        );
                    }

                    return ResponseEntity.ok(savedProduct);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 5. Delete inventory first, then product
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteProduct(
            @PathVariable Integer id) {

        if (!productRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Product not found.");
        }

        jdbcTemplate.update(
                "DELETE FROM inventory WHERE product_id = ?", id);

        productRepository.deleteById(id);

        return ResponseEntity.ok("Product deleted successfully.");
    }

    // Validate product and inventory details
    private boolean isValid(ProductCreateRequest request) {
        return request != null
                && request.getProductName() != null
                && !request.getProductName().isBlank()
                && request.getSku() != null
                && !request.getSku().isBlank()
                && request.getCategoryId() != null
                && request.getCategoryId() > 0
                && request.getCostPrice() != null
                && request.getSellingPrice() != null
                && request.getCostPrice().signum() >= 0
                && request.getSellingPrice().signum() >= 0
                && request.getQuantity() != null
                && request.getQuantity() >= 0
                && request.getReorderLevel() != null
                && request.getReorderLevel() >= 0;
    }
}

