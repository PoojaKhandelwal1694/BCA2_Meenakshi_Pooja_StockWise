
package com.stockwise.stockwise.product;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    // Check whether a SKU already exists
    boolean existsBySku(String sku);

    // Check whether the SKU belongs to another product
    boolean existsBySkuAndProductIdNot(String sku, Integer productId);
}
