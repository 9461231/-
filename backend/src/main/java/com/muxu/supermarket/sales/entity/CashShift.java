package com.muxu.supermarket.sales.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收银交班单
 */
@Data
@Entity
@Table(name = "cash_shift")
public class CashShift {

    public static final int STATUS_OPEN = 0;
    public static final int STATUS_CLOSED = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cashier_id", nullable = false)
    private Long cashierId;

    @Column(name = "cashier_name", length = 50)
    private String cashierName;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(nullable = false)
    private Integer status = STATUS_OPEN;

    @Column(name = "order_count")
    private Integer orderCount = 0;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "cash_amount", precision = 12, scale = 2)
    private BigDecimal cashAmount = BigDecimal.ZERO;

    @Column(name = "wechat_amount", precision = 12, scale = 2)
    private BigDecimal wechatAmount = BigDecimal.ZERO;

    @Column(name = "alipay_amount", precision = 12, scale = 2)
    private BigDecimal alipayAmount = BigDecimal.ZERO;

    @Column(name = "card_amount", precision = 12, scale = 2)
    private BigDecimal cardAmount = BigDecimal.ZERO;

    @Column(name = "balance_amount", precision = 12, scale = 2)
    private BigDecimal balanceAmount = BigDecimal.ZERO;

    @Column(name = "refund_amount", precision = 12, scale = 2)
    private BigDecimal refundAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(length = 255)
    private String remark;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
