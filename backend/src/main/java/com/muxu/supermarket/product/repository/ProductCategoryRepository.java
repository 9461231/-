package com.muxu.supermarket.product.repository;

import com.muxu.supermarket.product.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    List<ProductCategory> findAllByOrderBySortOrderAscIdAsc();
}
