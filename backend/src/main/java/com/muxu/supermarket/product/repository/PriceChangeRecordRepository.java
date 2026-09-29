package com.muxu.supermarket.product.repository;

import com.muxu.supermarket.product.entity.PriceChangeRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceChangeRecordRepository extends JpaRepository<PriceChangeRecord, Long> {

    List<PriceChangeRecord> findTop50ByProductIdOrderByIdDesc(Long productId);
}
