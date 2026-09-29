package com.muxu.supermarket.supplier.repository;

import com.muxu.supermarket.supplier.entity.SupplierProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierProductRepository extends JpaRepository<SupplierProduct, Long> {

    List<SupplierProduct> findBySupplierId(Long supplierId);

    List<SupplierProduct> findByProductId(Long productId);

    Optional<SupplierProduct> findBySupplierIdAndProductId(Long supplierId, Long productId);

    boolean existsBySupplierId(Long supplierId);

    long countBySupplierId(Long supplierId);

    void deleteBySupplierId(Long supplierId);
}
