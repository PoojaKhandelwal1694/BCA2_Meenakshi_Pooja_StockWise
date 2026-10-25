package com.stockwise.stockwise.batch;

import com.stockwise.stockwise.category.Category;
import com.stockwise.stockwise.category.CategoryRepository;
import com.stockwise.stockwise.product.Product;
import com.stockwise.stockwise.product.ProductRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/batches")
@CrossOrigin(origins = {
        "http://127.0.0.1:5500",
        "http://localhost:5500"
})
public class ProductBatchController {

    private final ProductBatchRepository batchRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final JdbcTemplate jdbcTemplate;

    public ProductBatchController(
            ProductBatchRepository batchRepository,
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            JdbcTemplate jdbcTemplate) {
        this.batchRepository = batchRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public List<ProductBatch> getAllBatches() {
        return batchRepository.findAll();
    }

    
@GetMapping("/expiry-alerts")
public List<ProductBatch> getExpiryAlerts() {
    LocalDate today = LocalDate.now();
    LocalDate next30Days = today.plusDays(30);

    return batchRepository.findAll().stream()
            .filter(batch -> batch.getExpiryDate() != null)
            .filter(batch ->
                    !batch.getExpiryDate().isAfter(next30Days))
            .filter(batch -> batch.getQuantity() != null
                    && batch.getQuantity() > 0)
            .toList();
}

    @GetMapping("/product/{productId}")
    public List<ProductBatch> getBatchesByProduct(
            @PathVariable Integer productId) {
        return batchRepository.findByProductId(productId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductBatch> getBatchById(
            @PathVariable Integer id) {

        return batchRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> addBatch(@RequestBody ProductBatch batch) {

        ResponseEntity<?> validationError = validateBatch(batch);
        if (validationError != null) {
            return validationError;
        }

        Product product = productRepository
                .findById(batch.getProductId()).orElse(null);

        if (product == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Selected product does not exist."));
        }

        ResponseEntity<?> expiryError = validateExpiry(
                product.getCategoryId(), batch.getExpiryDate());

        if (expiryError != null) {
            return expiryError;
        }

        batch.setBatchId(null);
        batch.setBatchNumber(batch.getBatchNumber().trim());

        ProductBatch savedBatch = batchRepository.save(batch);

        adjustInventory(savedBatch.getProductId(), savedBatch.getQuantity());

        return ResponseEntity.status(HttpStatus.CREATED).body(savedBatch);
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateBatch(
            @PathVariable Integer id,
            @RequestBody ProductBatch updatedBatch) {

        Optional<ProductBatch> result = batchRepository.findById(id);

        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ResponseEntity<?> validationError = validateBatch(updatedBatch);
        if (validationError != null) {
            return validationError;
        }

        Product newProduct = productRepository
                .findById(updatedBatch.getProductId()).orElse(null);

        if (newProduct == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Selected product does not exist."));
        }

        ResponseEntity<?> expiryError = validateExpiry(
                newProduct.getCategoryId(), updatedBatch.getExpiryDate());

        if (expiryError != null) {
            return expiryError;
        }

        ProductBatch existing = result.get();

        Integer oldProductId = existing.getProductId();
        Integer oldQuantity = existing.getQuantity();

        existing.setProductId(updatedBatch.getProductId());
        existing.setBatchNumber(updatedBatch.getBatchNumber().trim());
        existing.setExpiryDate(updatedBatch.getExpiryDate());
        existing.setQuantity(updatedBatch.getQuantity());

        ProductBatch savedBatch = batchRepository.save(existing);

        if (oldProductId.equals(savedBatch.getProductId())) {
            // Only apply the difference to the existing product's stock.
            int difference = savedBatch.getQuantity() - oldQuantity;
            adjustInventory(savedBatch.getProductId(), difference);
        } else {
            // Move this batch's stock from the old product to the new one.
            adjustInventory(oldProductId, -oldQuantity);
            adjustInventory(savedBatch.getProductId(), savedBatch.getQuantity());
        }

        return ResponseEntity.ok(savedBatch);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteBatch(@PathVariable Integer id) {

        Optional<ProductBatch> result = batchRepository.findById(id);

        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ProductBatch batch = result.get();

        adjustInventory(batch.getProductId(), -batch.getQuantity());
        batchRepository.delete(batch);

        return ResponseEntity.ok(
                Map.of("message", "Batch deleted and inventory updated."));
    }

    private ResponseEntity<?> validateBatch(ProductBatch batch) {
        if (batch == null
                || batch.getProductId() == null
                || batch.getProductId() <= 0
                || batch.getBatchNumber() == null
                || batch.getBatchNumber().isBlank()
                || batch.getQuantity() == null
                || batch.getQuantity() < 0) {

            return ResponseEntity.badRequest().body(
                    Map.of("message",
                            "Product, batch number and valid quantity are required."));
        }

        return null;
    }

    private ResponseEntity<?> validateExpiry(
            Integer categoryId, LocalDate expiryDate) {

        Category category = categoryRepository.findById(categoryId).orElse(null);

        if (category == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Product category does not exist."));
        }

        if (category.isExpiryTracked() && expiryDate == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("message",
                            "Expiry date is required for this category."));
        }

       

        return null;
    }

   private void adjustInventory(Integer productId, int quantityChange) {

    Integer inventoryCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM inventory WHERE product_id = ?",
            Integer.class,
            productId
    );

    if (inventoryCount != null && inventoryCount > 0) {

        jdbcTemplate.update(
                "UPDATE inventory SET quantity = quantity + ? WHERE product_id = ?",
                quantityChange,
                productId
        );

    } else {

        jdbcTemplate.update("""
            INSERT INTO inventory (product_id, quantity, reorder_level)
            VALUES (?, ?, 0)
            """,
            productId,
            quantityChange
        );
    }
}
}