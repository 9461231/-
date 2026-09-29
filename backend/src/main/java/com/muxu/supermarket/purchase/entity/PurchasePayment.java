package com.muxu.supermarket.purchase.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购付款记录（采购应付与结算）
 */
@Data
@Entity
@Table(name = "purchase_payment", indexes = @Index(name = "idx_pp_order", columnList = "order_id"))
public class PurchasePayment {

    public static final String METHOD_CASH = "CASH";
    public static final String METHOD_BANK = "BANK";
    public static final String METHOD_WECHAT = "WECHAT";
    public static final String METHOD_ALIPAY = "ALIPAY";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "order_no", length = 64)
    private String orderNo;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /** CASH/BANK/WECHAT/ALIPAY */
    @Column(nullable = false, length = 16)
    private String method;

    @Column(length = 255)
    private String remark;

    @Column(name = "paid_by", length = 50)
    private String paidBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
