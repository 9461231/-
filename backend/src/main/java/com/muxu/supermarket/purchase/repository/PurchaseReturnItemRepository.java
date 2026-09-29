package com.muxu.supermarket.purchase.repository;

import com.muxu.supermarket.purchase.entity.PurchaseReturnItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseReturnItemRepository extends JpaRepository<PurchaseReturnItem, Long> {

    List<PurchaseReturnItem> findByReturnId(Long returnId);
}
