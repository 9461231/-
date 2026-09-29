package com.muxu.supermarket.inventory.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class InventoryVO {

    private Long inventoryId;
    private Long productId;
    private String productName;
    private String sku;
    private String unit;
    private String categoryName;
    private Integer quantity;
    private Integer minStock;
    private Integer maxStock;
    /** 库存金额 = quantity * purchasePrice */
    private BigDecimal inventoryValue;
    /** 预警：OUT_OF_STOCK / LOW / HIGH / null */
    private String alertType;
    private LocalDateTime updatedAt;
}
