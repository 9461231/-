package com.muxu.supermarket.purchase.repository;

import com.muxu.supermarket.purchase.entity.PurchasePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface PurchasePaymentRepository
        extends JpaRepository<PurchasePayment, Long>, JpaSpecificationExecutor<PurchasePayment> {

    List<PurchasePayment> findByOrderId(Long orderId);

    List<PurchasePayment> findBySupplierIdOrderByIdDesc(Long supplierId);
}
