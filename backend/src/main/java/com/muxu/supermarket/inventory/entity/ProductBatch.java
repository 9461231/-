package com.muxu.supermarket.inventory.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 商品批次（保质期/临期管理）
 */
@Data
@Entity
@Table(name = "product_batch", indexes = {
        @Index(name = "idx_batch_product", columnList = "product_id"),
        @Index(name = "idx_batch_expire", columnList = "expire_date")
})
public class ProductBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_no", nullable = false, length = 64)
    private String batchNo;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "production_date")
    private LocalDate productionDate;

    /** 保质期（天） */
    @Column(name = "shelf_life_days")
    private Integer shelfLifeDays;

    @Column(name = "expire_date")
    private LocalDate expireDate;

    @Column(name = "purchase_price", precision = 10, scale = 2)
    private BigDecimal purchasePrice;

    @Column(length = 255)
    private String remark;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    /** 临期状态：EXPIRED / URGENT(≤7天) / NEAR(≤30天) / OK */
    public String expiryStatus() {
        if (expireDate == null) {
            return "OK";
        }
        long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expireDate);
        if (days < 0) {
            return "EXPIRED";
        }
        if (days <= 7) {
            return "URGENT";
        }
        if (days <= 30) {
            return "NEAR";
        }
        return "OK";
    }

    public long daysToExpire() {
        return expireDate == null ? Long.MAX_VALUE
                : java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expireDate);
    }
}
