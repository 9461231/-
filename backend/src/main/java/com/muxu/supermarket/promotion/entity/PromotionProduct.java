package com.muxu.supermarket.promotion.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 促销活动参与商品
 */
@Data
@Entity
@Table(name = "promotion_product",
        uniqueConstraints = @UniqueConstraint(name = "uk_promo_product", columnNames = {"promotion_id", "product_id"}))
public class PromotionProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "promotion_id", nullable = false)
    private Long promotionId;

    @Column(name = "product_id", nullable = false)
    private Long productId;
}
