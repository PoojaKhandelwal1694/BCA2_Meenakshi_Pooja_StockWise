
package com.stockwise.stockwise.batch;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/batches")
@CrossOrigin(origins = "*")
public class ProductBatchController {

    private final ProductBatchRepository batchRepository;

    public ProductBatchController(ProductBatchRepository batchRepository) {
        this.batchRepository = batchRepository;
    }

    // Get all batches
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
            !batch.getExpiryDate().isAfter(next30Days)
        )
        .toList();
}


    // Get batches by product ID
    @GetMapping("/product/{productId}")
    public List<ProductBatch> getBatchesByProduct(
            @PathVariable Integer productId) {
        return batchRepository.findByProductId(productId);
    }

    // Get batch by ID
    @GetMapping("/{id}")
    public ResponseEntity<ProductBatch> getBatchById(
            @PathVariable Integer id) {
        return batchRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Add a new batch
    @PostMapping
    public ResponseEntity<?> addBatch(
            @RequestBody ProductBatch batch) {

        if (batch.getProductId() == null
                || batch.getBatchNumber() == null
                || batch.getBatchNumber().isBlank()
                || batch.getQuantity() == null
                || batch.getQuantity() < 0) {
            return ResponseEntity.badRequest()
                    .body("Product ID, batch number and valid quantity are required.");
        }

        ProductBatch savedBatch = batchRepository.save(batch);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savedBatch);
    }

    // Update a batch
    @PutMapping("/{id}")
    public ResponseEntity<?> updateBatch(
            @PathVariable Integer id,
            @RequestBody ProductBatch updatedBatch) {

        Optional<ProductBatch> existing =
                batchRepository.findById(id);

        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ProductBatch batch = existing.get();
        batch.setProductId(updatedBatch.getProductId());
        batch.setBatchNumber(updatedBatch.getBatchNumber());
        batch.setExpiryDate(updatedBatch.getExpiryDate());
        batch.setQuantity(updatedBatch.getQuantity());

        return ResponseEntity.ok(batchRepository.save(batch));
    }

    // Delete a batch
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBatch(
            @PathVariable Integer id) {

        if (!batchRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        batchRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
