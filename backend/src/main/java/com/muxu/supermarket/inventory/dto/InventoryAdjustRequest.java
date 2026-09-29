package com.muxu.supermarket.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InventoryAdjustRequest {

    @NotNull(message = "商品不能为空")
    private Long productId;

    /** 调整数量：正数增加，负数减少 */
    @NotNull(message = "调整数量不能为空")
    private Integer changeQty;

    @NotBlank(message = "调整原因不能为空")
    private String reason;
}
