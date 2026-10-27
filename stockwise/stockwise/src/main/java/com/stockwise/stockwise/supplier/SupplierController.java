
package com.stockwise.stockwise.supplier;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;
import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
@RequestMapping("/api/suppliers")
@CrossOrigin(origins = {
    "http://127.0.0.1:5500",
    "http://localhost:5500"
})
public class SupplierController {

    private final SupplierRepository repository;

    public SupplierController(SupplierRepository repository) {
        this.repository = repository;
    }

    // Get all suppliers
    @GetMapping
    public List<Supplier> getAllSuppliers() {
        return repository.findAll();
    }

    // Get supplier by ID
    @GetMapping("/{id}")
    public ResponseEntity<Supplier> getSupplier(@PathVariable Integer id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Add supplier
    @PostMapping
    public ResponseEntity<?> addSupplier(@RequestBody Supplier supplier) {

        if (supplier.getSupplierName() == null ||
            supplier.getSupplierName().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Supplier name is required"));
        }

        supplier.setSupplierName(supplier.getSupplierName().trim());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(repository.save(supplier));
    }

    // Update supplier
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSupplier(
            @PathVariable Integer id,
            @RequestBody Supplier details) {

        if (details.getSupplierName() == null ||
            details.getSupplierName().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Supplier name is required"));
        }

        return repository.findById(id).map(supplier -> {

            supplier.setSupplierName(details.getSupplierName().trim());
            supplier.setCompanyName(details.getCompanyName());
            supplier.setPhone(details.getPhone());
            supplier.setEmail(details.getEmail());
            supplier.setAddress(details.getAddress());

            return ResponseEntity.ok(repository.save(supplier));

        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Delete supplier
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSupplier(@PathVariable Integer id) {

        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
