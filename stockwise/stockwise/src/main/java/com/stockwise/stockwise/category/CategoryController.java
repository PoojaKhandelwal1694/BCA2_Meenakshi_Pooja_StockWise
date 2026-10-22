
package com.stockwise.stockwise.category;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = {
        "http://127.0.0.1:5500",
        "http://localhost:5500"
})
public class CategoryController {

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    // 1. Get all categories
    @GetMapping
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    // 2. Get category by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getCategoryById(
            @PathVariable Integer id) {

        return categoryRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 3. Add category
    @PostMapping
    public ResponseEntity<?> addCategory(
            @RequestBody Category category) {

        if (category == null
                || category.getCategoryName() == null
                || category.getCategoryName().isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Category name is required."));
        }

        String name = category.getCategoryName().trim();

        if (categoryRepository.existsByCategoryNameIgnoreCase(name)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("message", "This category already exists."));
        }

        category.setCategoryId(null);
        category.setCategoryName(name);

        if (category.getDescription() != null) {
            category.setDescription(category.getDescription().trim());
        }

        Category saved = categoryRepository.save(category);

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // 4. Update category
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(
            @PathVariable Integer id,
            @RequestBody Category updatedCategory) {

        if (updatedCategory == null
                || updatedCategory.getCategoryName() == null
                || updatedCategory.getCategoryName().isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Category name is required."));
        }

        return categoryRepository.findById(id)
                .<ResponseEntity<?>>map(existing -> {

                    String name = updatedCategory.getCategoryName().trim();

                    if (categoryRepository
                            .existsByCategoryNameIgnoreCaseAndCategoryIdNot(
                                    name, id)) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Map.of(
                                        "message",
                                        "Another category already uses this name."));
                    }

                    existing.setCategoryName(name);

                    String description = updatedCategory.getDescription();
                    existing.setDescription(
                            description == null ? null : description.trim());

                    Category saved = categoryRepository.save(existing);

                    return ResponseEntity.ok(saved);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 5. Delete category
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(
            @PathVariable Integer id) {

        if (!categoryRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("message", "Category not found."));
        }

        try {
            categoryRepository.deleteById(id);
            return ResponseEntity.ok(
                    Map.of("message", "Category deleted successfully."));
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("message",
                            "This category is used by products and cannot be deleted."));
        }
    }
}
