package com.muxu.supermarket.supplier.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SupplierProductVO {

    private Long productId;
    private String productName;
    private String sku;
    private String unit;
    private BigDecimal supplyPrice;
    /** 商品档案采购价，便于对比 */
    private BigDecimal purchasePrice;
    private Boolean isPrimary;
    private LocalDateTime boundAt;
}
