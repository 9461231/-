package com.muxu.supermarket.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

public class BatchDTOs {

    @Data
    public static class CreateRequest {
        @NotNull(message = "商品不能为空")
        private Long productId;
        private Long supplierId;
        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量必须大于0")
        private Integer quantity;
        private String productionDate;
        private Integer shelfLifeDays;
        private String expireDate;
        private java.math.BigDecimal purchasePrice;
        private String remark;
    }

    @Data
    public static class DisposeRequest {
        @NotNull(message = "处置数量不能为空")
        @Min(value = 1, message = "处置数量必须大于0")
        private Integer quantity;
        @NotBlank(message = "处置原因不能为空")
        private String reason;
    }

    @Data
    public static class BatchVO {
        private Long id;
        private String batchNo;
        private Long productId;
        private String productName;
        private String sku;
        private String unit;
        private Long supplierId;
        private String supplierName;
        private Integer quantity;
        private String productionDate;
        private Integer shelfLifeDays;
        private String expireDate;
        private long daysToExpire;
        private String expiryStatus;
        private java.math.BigDecimal purchasePrice;
        private String remark;
        private String createdAt;
    }
}
