package com.muxu.supermarket.inventory.dto;

import lombok.Data;

@Data
public class InventoryAlertVO {

    private Long productId;
    private String productName;
    private String sku;
    private Integer quantity;
    private Integer minStock;
    private Integer maxStock;
    /** OUT_OF_STOCK / LOW / HIGH */
    private String alertType;
    /** 建议补货数量 = max(minStock*2 - quantity, minStock - quantity) */
    private Integer suggestQty;
}
