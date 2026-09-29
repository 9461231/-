package com.muxu.supermarket.purchase.dto;

import com.muxu.supermarket.purchase.entity.PurchaseOrderItem;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PurchaseOrderItemVO {

    private Long id;
    private Long productId;
    private String productName;
    private String sku;
    private String unit;
    private Integer quantity;
    private BigDecimal purchasePrice;
    private BigDecimal amount;
    private Integer receivedQuantity;

    public static PurchaseOrderItemVO from(PurchaseOrderItem item, String productName, String sku, String unit) {
        PurchaseOrderItemVO vo = new PurchaseOrderItemVO();
        vo.setId(item.getId());
        vo.setProductId(item.getProductId());
        vo.setProductName(productName);
        vo.setSku(sku);
        vo.setUnit(unit);
        vo.setQuantity(item.getQuantity());
        vo.setPurchasePrice(item.getPurchasePrice());
        vo.setAmount(item.getAmount());
        vo.setReceivedQuantity(item.getReceivedQuantity());
        return vo;
    }
}
