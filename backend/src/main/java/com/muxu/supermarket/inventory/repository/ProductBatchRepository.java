package com.muxu.supermarket.inventory.repository;

import com.muxu.supermarket.inventory.entity.ProductBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ProductBatchRepository extends JpaRepository<ProductBatch, Long> {

    List<ProductBatch> findByProductIdOrderByExpireDateAsc(Long productId);

    List<ProductBatch> findByQuantityGreaterThanOrderByExpireDateAsc(int qty);

    List<ProductBatch> findByExpireDateBeforeAndQuantityGreaterThan(LocalDate date, int qty);

    List<ProductBatch> findByExpireDateBetweenAndQuantityGreaterThan(LocalDate start, LocalDate end, int qty);
}
