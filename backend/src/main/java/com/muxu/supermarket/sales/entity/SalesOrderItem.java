package com.muxu.supermarket.sales.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 销售订单明细
 */
@Data
@Entity
@Table(name = "sales_order_item", indexes = @Index(name = "idx_soi_order", columnList = "order_id"))
public class SalesOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    /** 成交单价（原价） */
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /** 该行促销优惠金额 */
    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /** 该行实收金额 */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /** 匹配到的促销活动 */
    @Column(name = "promotion_id")
    private Long promotionId;

    /** 已退货数量 */
    @Column(name = "returned_quantity", nullable = false)
    private Integer returnedQuantity = 0;
}
