
package com.stockwise.stockwise.product;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = {
        "http://127.0.0.1:5500",
        "http://localhost:5500"
})
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // 1. Get all products
    @GetMapping
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // 2. Get product by ID
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(
            @PathVariable Integer id) {

        return productRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 3. Add a new product
    @PostMapping
    public ResponseEntity<?> addProduct(
            @RequestBody Product product) {

        if (!isValid(product)) {
            return ResponseEntity.badRequest()
                    .body("Please enter valid product details.");
        }

        if (productRepository.existsBySku(product.getSku())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("This SKU already exists.");
        }

        Product savedProduct = productRepository.save(product);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savedProduct);
    }

    // 4. Update an existing product
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(
            @PathVariable Integer id,
            @RequestBody Product updatedProduct) {

        if (!isValid(updatedProduct)) {
            return ResponseEntity.badRequest()
                    .body("Please enter valid product details.");
        }

        return productRepository.findById(id)
                .map(existingProduct -> {

                    if (productRepository.existsBySkuAndProductIdNot(
                            updatedProduct.getSku(), id)) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body("This SKU already exists.");
                    }

                    existingProduct.setProductName(
                            updatedProduct.getProductName());
                    existingProduct.setSku(updatedProduct.getSku());
                    existingProduct.setCategoryId(
                            updatedProduct.getCategoryId());
                    existingProduct.setCostPrice(
                            updatedProduct.getCostPrice());
                    existingProduct.setSellingPrice(
                            updatedProduct.getSellingPrice());
                    existingProduct.setAttributes(
                            updatedProduct.getAttributes());

                    Product savedProduct =
                            productRepository.save(existingProduct);

                    return ResponseEntity.ok(savedProduct);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 5. Delete a product
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(
            @PathVariable Integer id) {

        if (!productRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Product not found.");
        }

        productRepository.deleteById(id);

        return ResponseEntity.ok("Product deleted successfully.");
    }

    // Validate product details
    private boolean isValid(Product product) {

        return product != null
                && product.getProductName() != null
                && !product.getProductName().isBlank()
                && product.getSku() != null
                && !product.getSku().isBlank()
                && product.getCategoryId() != null
                && product.getCostPrice() != null
                && product.getSellingPrice() != null
                && product.getCostPrice().signum() >= 0
                && product.getSellingPrice().signum() >= 0;
    }
}
