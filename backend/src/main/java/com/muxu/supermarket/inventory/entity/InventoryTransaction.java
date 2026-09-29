package com.muxu.supermarket.inventory.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存流水
 */
@Data
@Entity
@Table(name = "inventory_transaction", indexes = {
        @Index(name = "idx_inv_tx_product", columnList = "product_id"),
        @Index(name = "idx_inv_tx_created", columnList = "created_at")
})
public class InventoryTransaction {

    /** 采购入库 */
    public static final String TYPE_PURCHASE_IN = "PURCHASE_IN";
    /** 销售出库 */
    public static final String TYPE_SALE_OUT = "SALE_OUT";
    /** 销售退货入库 */
    public static final String TYPE_SALE_RETURN_IN = "SALE_RETURN_IN";
    /** 采购退货出库 */
    public static final String TYPE_PURCHASE_RETURN_OUT = "PURCHASE_RETURN_OUT";
    /** 盘点调整 */
    public static final String TYPE_STOCKTAKE_ADJUST = "STOCKTAKE_ADJUST";
    /** 手工调整 */
    public static final String TYPE_MANUAL_ADJUST = "MANUAL_ADJUST";
    /** 损耗出库 */
    public static final String TYPE_LOSS_OUT = "LOSS_OUT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    /** 变动类型，见常量 */
    @Column(nullable = false, length = 32)
    private String type;

    /** 变动数量：正数增加，负数减少 */
    @Column(name = "change_qty", nullable = false)
    private Integer changeQty;

    @Column(name = "before_qty", nullable = false)
    private Integer beforeQty;

    @Column(name = "after_qty", nullable = false)
    private Integer afterQty;

    /** 关联单据类型，如 PURCHASE_ORDER / SALES_ORDER / STOCKTAKE */
    @Column(name = "ref_type", length = 32)
    private String refType;

    /** 关联单据号 */
    @Column(name = "ref_no", length = 64)
    private String refNo;

    @Column(length = 255)
    private String remark;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
