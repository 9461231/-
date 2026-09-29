package com.muxu.supermarket.sales.repository;

import com.muxu.supermarket.sales.entity.SalesReturnItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalesReturnItemRepository extends JpaRepository<SalesReturnItem, Long> {

    List<SalesReturnItem> findByReturnId(Long returnId);
}
