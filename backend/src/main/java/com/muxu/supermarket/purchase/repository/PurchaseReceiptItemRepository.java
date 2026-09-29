package com.muxu.supermarket.purchase.repository;

import com.muxu.supermarket.purchase.entity.PurchaseReceiptItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseReceiptItemRepository extends JpaRepository<PurchaseReceiptItem, Long> {

    List<PurchaseReceiptItem> findByReceiptId(Long receiptId);
}
