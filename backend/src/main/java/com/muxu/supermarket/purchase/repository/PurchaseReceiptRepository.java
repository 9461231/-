package com.muxu.supermarket.purchase.repository;

import com.muxu.supermarket.purchase.entity.PurchaseReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PurchaseReceiptRepository
        extends JpaRepository<PurchaseReceipt, Long>, JpaSpecificationExecutor<PurchaseReceipt> {
}
