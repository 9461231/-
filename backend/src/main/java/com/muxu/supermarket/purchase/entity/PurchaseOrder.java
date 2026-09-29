package com.muxu.supermarket.purchase.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购订单
 * 状态流转：0 草稿 → 1 待审核 → 2 已审核(待入库) → 3 部分入库 → 4 已完成；草稿可取消 5
 */
@Data
@Entity
@Table(name = "purchase_order", indexes = @Index(name = "idx_po_no", columnList = "order_no", unique = true))
public class PurchaseOrder {

    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_PENDING_AUDIT = 1;
    public static final int STATUS_AUDITED = 2;
    public static final int STATUS_PARTIAL_RECEIVED = 3;
    public static final int STATUS_COMPLETED = 4;
    public static final int STATUS_CANCELLED = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, length = 64, unique = true)
    private String orderNo;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(nullable = false)
    private Integer status = STATUS_DRAFT;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /** 已支付金额 */
    @Column(name = "paid_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(length = 255)
    private String remark;

    @Column(name = "audit_remark", length = 255)
    private String auditRemark;

    @Column(name = "audit_at")
    private LocalDateTime auditAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

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
}
