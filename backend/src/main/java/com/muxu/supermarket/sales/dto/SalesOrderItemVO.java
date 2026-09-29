package com.muxu.supermarket.sales.dto;

import com.muxu.supermarket.sales.entity.SalesOrderItem;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalesOrderItemVO {

    private Long id;
    private Long productId;
    private String productName;
    private String sku;
    private String unit;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal discountAmount;
    private BigDecimal amount;
    private Long promotionId;
    private Integer returnedQuantity;

    public static SalesOrderItemVO from(SalesOrderItem item, String productName, String sku, String unit) {
        SalesOrderItemVO vo = new SalesOrderItemVO();
        vo.setId(item.getId());
        vo.setProductId(item.getProductId());
        vo.setProductName(productName);
        vo.setSku(sku);
        vo.setUnit(unit);
        vo.setQuantity(item.getQuantity());
        vo.setUnitPrice(item.getUnitPrice());
        vo.setDiscountAmount(item.getDiscountAmount());
        vo.setAmount(item.getAmount());
        vo.setPromotionId(item.getPromotionId());
        vo.setReturnedQuantity(item.getReturnedQuantity());
        return vo;
    }
}
