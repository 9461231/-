package com.muxu.supermarket.sales.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 销售退货单明细
 */
@Data
@Entity
@Table(name = "sales_return_item")
public class SalesReturnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "return_id", nullable = false)
    private Long returnId;

    @Column(name = "order_item_id", nullable = false)
    private Long orderItemId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    /** 退款金额（按实收单价折算） */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal refundAmount;
}
