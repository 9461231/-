package com.muxu.supermarket.purchase.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 采购订单明细
 */
@Data
@Entity
@Table(name = "purchase_order_item", indexes = @Index(name = "idx_poi_order", columnList = "order_id"))
public class PurchaseOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "purchase_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal purchasePrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /** 已入库数量 */
    @Column(name = "received_quantity", nullable = false)
    private Integer receivedQuantity = 0;
}
