package com.muxu.supermarket.promotion.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 促销活动
 * 类型：1-直接折扣 2-满减 3-第二件优惠 4-会员专享价
 */
@Data
@Entity
@Table(name = "promotion")
public class Promotion {

    public static final int TYPE_DISCOUNT = 1;
    public static final int TYPE_FULL_REDUCTION = 2;
    public static final int TYPE_SECOND_HALF = 3;
    public static final int TYPE_MEMBER_PRICE = 4;

    public static final int STATUS_DISABLED = 0;
    public static final int STATUS_ENABLED = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer type;

    /** 0-停用（含草稿） 1-启用 */
    @Column(nullable = false)
    private Integer status = STATUS_DISABLED;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    /** 折扣率（类型1），如 0.80 表示8折 */
    @Column(name = "discount_rate", precision = 3, scale = 2)
    private BigDecimal discountRate;

    /** 满减门槛金额（类型2） */
    @Column(name = "min_amount", precision = 10, scale = 2)
    private BigDecimal minAmount;

    /** 满减减免金额（类型2） */
    @Column(name = "reduce_amount", precision = 10, scale = 2)
    private BigDecimal reduceAmount;

    /** 第二件折扣率（类型3），如 0.50 表示第二件半价 */
    @Column(name = "second_rate", precision = 3, scale = 2)
    private BigDecimal secondRate;

    /** 会员专享价（类型4，单价） */
    @Column(name = "member_price", precision = 10, scale = 2)
    private BigDecimal memberPrice;

    @Column(length = 255)
    private String remark;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public String typeLabel() {
        return switch (type) {
            case TYPE_DISCOUNT -> "直接折扣";
            case TYPE_FULL_REDUCTION -> "满减";
            case TYPE_SECOND_HALF -> "第二件优惠";
            case TYPE_MEMBER_PRICE -> "会员专享价";
            default -> "未知";
        };
    }
}
