
package com.stockwise.stockwise.batch;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductBatchRepository
        extends JpaRepository<ProductBatch, Integer> {

    List<ProductBatch> findByProductId(Integer productId);
}
