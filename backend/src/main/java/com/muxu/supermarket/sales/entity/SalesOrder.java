package com.muxu.supermarket.sales.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售订单
 */
@Data
@Entity
@Table(name = "sales_order", indexes = {
        @Index(name = "idx_so_no", columnList = "order_no", unique = true),
        @Index(name = "idx_so_created", columnList = "created_at")
})
public class SalesOrder {

    /** 已完成 */
    public static final int STATUS_COMPLETED = 1;
    /** 部分退货 */
    public static final int STATUS_PARTIAL_RETURNED = 2;
    /** 全部退货 */
    public static final int STATUS_RETURNED = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, length = 64, unique = true)
    private String orderNo;

    @Column(name = "member_id")
    private Long memberId;

    @Column(nullable = false)
    private Integer status = STATUS_COMPLETED;

    /** 原价合计 */
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    /** 促销优惠合计 */
    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /** 应收金额 */
    @Column(name = "payable_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal payableAmount;

    /** 支付方式：CASH/WECHAT/ALIPAY/CARD */
    @Column(name = "pay_method", nullable = false, length = 16)
    private String payMethod;

    @Column(name = "points_earned", nullable = false)
    private Integer pointsEarned = 0;

    /** 优惠券抵扣金额 */
    @Column(name = "coupon_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal couponAmount = BigDecimal.ZERO;

    /** 关联交班单 */
    @Column(name = "shift_id")
    private Long shiftId;

    @Column(length = 255)
    private String remark;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
