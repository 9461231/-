package com.muxu.supermarket.purchase.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 采购入库单明细
 */
@Data
@Entity
@Table(name = "purchase_receipt_item")
public class PurchaseReceiptItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receipt_id", nullable = false)
    private Long receiptId;

    @Column(name = "order_item_id", nullable = false)
    private Long orderItemId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;
}
