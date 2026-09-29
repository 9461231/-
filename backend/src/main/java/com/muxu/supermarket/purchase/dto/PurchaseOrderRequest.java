package com.muxu.supermarket.purchase.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.List;

@Data
public class PurchaseOrderRequest {

    @NotNull(message = "供应商不能为空")
    private Long supplierId;

    @Valid
    @NotEmpty(message = "采购明细不能为空")
    private List<Item> items;

    private String remark;

    @Data
    public static class Item {

        @NotNull(message = "商品不能为空")
        private Long productId;

        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "采购数量必须大于0")
        private Integer quantity;

        @NotNull(message = "采购价不能为空")
        @DecimalMin(value = "0.00", message = "采购价不能为负数")
        private java.math.BigDecimal purchasePrice;
    }
}
