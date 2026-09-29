package com.muxu.supermarket.sales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SalesReturnRequest {

    @NotNull(message = "销售订单不能为空")
    private Long orderId;

    private String reason;

    @Valid
    @NotEmpty(message = "退货明细不能为空")
    private List<Item> items;

    @Data
    public static class Item {

        @NotNull(message = "订单明细不能为空")
        private Long orderItemId;

        @Min(value = 1, message = "退货数量必须大于0")
        private Integer quantity;
    }
}
