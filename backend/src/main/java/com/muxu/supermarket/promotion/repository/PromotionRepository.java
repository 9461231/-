package com.muxu.supermarket.promotion.repository;

import com.muxu.supermarket.promotion.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PromotionRepository
        extends JpaRepository<Promotion, Long>, JpaSpecificationExecutor<Promotion> {
}
