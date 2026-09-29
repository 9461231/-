package com.muxu.supermarket.sales.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板
 */
@Data
@Entity
@Table(name = "coupon")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    /** 使用门槛金额 */
    @Column(name = "min_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal minAmount;

    /** 抵扣金额 */
    @Column(name = "reduce_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal reduceAmount;

    /** 有效期至 */
    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;

    /** 发放总量 */
    @Column(name = "total_count", nullable = false)
    private Integer totalCount;

    /** 已发放数量 */
    @Column(name = "issued_count", nullable = false)
    private Integer issuedCount = 0;

    /** 0-停用 1-启用 */
    @Column(nullable = false)
    private Integer status = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
