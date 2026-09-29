package com.muxu.supermarket.sales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SalesOrderRequest {

    /** 会员（可选） */
    private Long memberId;

    /** 会员优惠券码（可选） */
    private String couponCode;

    @NotBlank(message = "支付方式不能为空")
    private String payMethod;

    @Valid
    @NotEmpty(message = "购物车不能为空")
    private List<Item> items;

    private String remark;

    @Data
    public static class Item {

        @NotNull(message = "商品不能为空")
        private Long productId;

        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量必须大于0")
        private Integer quantity;
    }
}
