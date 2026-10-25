package com.stockwise.stockwise.product;

import com.stockwise.stockwise.category.Category;
import com.stockwise.stockwise.category.CategoryRepository;
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
    private final CategoryRepository categoryRepository;
    private final JdbcTemplate jdbcTemplate;

    public ProductController(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            JdbcTemplate jdbcTemplate) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

   @GetMapping
public List<Map<String, Object>> getAllProducts() {
    String sql = """
        SELECT p.product_id AS productId,
               p.product_name AS productName,
               p.sku AS sku,
               p.category_id AS categoryId,
               p.cost_price AS costPrice,
               p.selling_price AS sellingPrice,

               CASE
                   WHEN c.expiry_tracked = TRUE THEN
                       COALESCE((
                           SELECT SUM(pb.quantity)
                           FROM product_batches pb
                           WHERE pb.product_id = p.product_id
                       ), 0)
                   ELSE
                       COALESCE(i.quantity, 0)
               END AS quantity,

               i.reorder_level AS reorderLevel

        FROM products p
        LEFT JOIN categories c
            ON p.category_id = c.category_id
        LEFT JOIN inventory i
            ON p.product_id = i.product_id
        ORDER BY p.product_id DESC
        """;

    return jdbcTemplate.queryForList(sql);
}

    @GetMapping("/grouped")
    public List<ProductCategoryResponse> getProductsWithCategories() {
        String sql = """
            SELECT p.product_id, p.product_name, p.sku, p.category_id,
                   c.category_name, i.quantity, i.reorder_level
            FROM products p
            LEFT JOIN categories c ON p.category_id = c.category_id
            LEFT JOIN inventory i ON p.product_id = i.product_id
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

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getProductById(
            @PathVariable Integer id) {

        String sql = """
            SELECT p.product_id AS productId,
                   p.product_name AS productName,
                   p.sku AS sku,
                   p.category_id AS categoryId,
                   p.cost_price AS costPrice,
                   p.selling_price AS sellingPrice,
                   p.attributes AS attributes,
                  CASE
    WHEN c.expiry_tracked = TRUE THEN
        COALESCE((
            SELECT SUM(pb.quantity)
            FROM product_batches pb
            WHERE pb.product_id = p.product_id
        ), 0)
    ELSE
        COALESCE(i.quantity, 0)
END AS quantity,
                   i.reorder_level AS reorderLevel
           FROM products p
LEFT JOIN categories c ON p.category_id = c.category_id
LEFT JOIN inventory i ON p.product_id = i.product_id
WHERE p.product_id = ?
            """;

        List<Map<String, Object>> products =
                jdbcTemplate.queryForList(sql, id);

        if (products.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(products.get(0));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> addProduct(
            @RequestBody ProductCreateRequest request) {

        if (!isValid(request)) {
            return ResponseEntity.badRequest()
                    .body("Please enter valid product details.");
        }

        if (!categoryRepository.existsById(request.getCategoryId())) {
            return ResponseEntity.badRequest()
                    .body("Selected category does not exist.");
        }

        String sku = request.getSku().trim();

        if (productRepository.existsBySku(sku)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("This SKU already exists.");
        }

        Product product = new Product();
        product.setProductName(request.getProductName().trim());
        product.setSku(sku);
        product.setCategoryId(request.getCategoryId());
        product.setCostPrice(request.getCostPrice());
        product.setSellingPrice(request.getSellingPrice());
        product.setAttributes(request.getAttributes());

        Product savedProduct = productRepository.save(product);

        
Category selectedCategory = categoryRepository.findById(
        request.getCategoryId()
).orElseThrow();

int initialQuantity;

if (selectedCategory.isExpiryTracked()) {
    // Expiry-tracked products get stock through batches.
    initialQuantity = 0;
} else {
    // Non-expiry products use the quantity from Add Product form.
    initialQuantity = request.getQuantity() == null
            ? 0
            : request.getQuantity();
}

jdbcTemplate.update("""
    INSERT INTO inventory (product_id, quantity, reorder_level)
    VALUES (?, ?, ?)
    """,
    savedProduct.getProductId(),
    initialQuantity,
    request.getReorderLevel()
);


        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savedProduct);
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateProduct(
            @PathVariable Integer id,
            @RequestBody ProductCreateRequest request) {

        if (!isValid(request)) {
            return ResponseEntity.badRequest()
                    .body("Please enter valid product details.");
        }

        if (!categoryRepository.existsById(request.getCategoryId())) {
            return ResponseEntity.badRequest()
                    .body("Selected category does not exist.");
        }

        if (!productRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        String sku = request.getSku().trim();

        if (productRepository.existsBySkuAndProductIdNot(sku, id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("This SKU already exists.");
        }

        Product existingProduct = productRepository.findById(id).orElseThrow();

        existingProduct.setProductName(request.getProductName().trim());
        existingProduct.setSku(sku);
        existingProduct.setCategoryId(request.getCategoryId());
        existingProduct.setCostPrice(request.getCostPrice());
        existingProduct.setSellingPrice(request.getSellingPrice());
        existingProduct.setAttributes(request.getAttributes());

        Product savedProduct = productRepository.save(existingProduct);

       Category selectedCategory = categoryRepository.findById(
        request.getCategoryId()
).orElseThrow();

Integer inventoryCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM inventory WHERE product_id = ?",
        Integer.class,
        id
);

if (inventoryCount != null && inventoryCount > 0) {

    if (selectedCategory.isExpiryTracked()) {

        // Expiry-tracked product stock is managed through batches.
        jdbcTemplate.update(
                "UPDATE inventory SET reorder_level = ? WHERE product_id = ?",
                request.getReorderLevel(), id
        );

    } else {

        // Non-expiry product quantity comes from the Edit Product form.
        jdbcTemplate.update(
                "UPDATE inventory SET quantity = ?, reorder_level = ? WHERE product_id = ?",
                request.getQuantity() == null ? 0 : request.getQuantity(),
                request.getReorderLevel(),
                id
        );
    }

} else {

    int initialQuantity = selectedCategory.isExpiryTracked()
            ? 0
            : (request.getQuantity() == null ? 0 : request.getQuantity());

    jdbcTemplate.update("""
        INSERT INTO inventory (product_id, quantity, reorder_level)
        VALUES (?, ?, ?)
        """,
        id,
        initialQuantity,
        request.getReorderLevel()
    );
}

        return ResponseEntity.ok(savedProduct);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteProduct(@PathVariable Integer id) {

        if (!productRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Product not found.");
        }

        // Delete dependent batches before deleting the product.
        jdbcTemplate.update(
                "DELETE FROM product_batches WHERE product_id = ?", id
        );

        jdbcTemplate.update(
                "DELETE FROM inventory WHERE product_id = ?", id
        );

        productRepository.deleteById(id);

        return ResponseEntity.ok("Product deleted successfully.");
    }

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
                && request.getReorderLevel() != null
                && request.getReorderLevel() >= 0
                && request.getReorderLevel() != null
                && request.getReorderLevel() >= 0
                && (request.getQuantity() == null
                        || request.getQuantity() >= 0);
                    }
}