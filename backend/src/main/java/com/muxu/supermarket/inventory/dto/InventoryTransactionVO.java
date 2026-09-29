package com.muxu.supermarket.inventory.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InventoryTransactionVO {

    private Long id;
    private Long productId;
    private String productName;
    private String sku;
    private String type;
    private Integer changeQty;
    private Integer beforeQty;
    private Integer afterQty;
    private String refType;
    private String refNo;
    private String remark;
    private LocalDateTime createdAt;
}
