package com.muxu.supermarket.purchase.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PurchaseReturnRequest {

    @NotNull(message = "供应商不能为空")
    private Long supplierId;

    /** 关联采购订单（可选） */
    private Long orderId;

    private String reason;

    @Valid
    @NotEmpty(message = "退货明细不能为空")
    private List<Item> items;

    @Data
    public static class Item {

        @NotNull(message = "商品不能为空")
        private Long productId;

        @Min(value = 1, message = "退货数量必须大于0")
        private Integer quantity;

        @DecimalMin(value = "0.00", message = "退货价格不能为负数")
        private java.math.BigDecimal purchasePrice;
    }
}
