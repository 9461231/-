package com.muxu.supermarket.promotion.repository;

import com.muxu.supermarket.promotion.entity.PromotionProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PromotionProductRepository extends JpaRepository<PromotionProduct, Long> {

    List<PromotionProduct> findByPromotionId(Long promotionId);

    List<PromotionProduct> findByProductIdIn(List<Long> productIds);

    boolean existsByPromotionId(Long promotionId);
}
