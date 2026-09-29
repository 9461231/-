package com.muxu.supermarket.stocktake.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LossRecordRequest {

    @NotNull(message = "商品不能为空")
    private Long productId;

    @NotNull(message = "损耗数量不能为空")
    @Min(value = 1, message = "损耗数量必须大于0")
    private Integer quantity;

    @NotNull(message = "损耗原因不能为空")
    @NotBlank(message = "损耗原因不能为空")
    private String reason;

    /** 单位损耗成本（默认取商品采购价） */
    @DecimalMin(value = "0.00", message = "单位成本不能为负数")
    private BigDecimal unitCost;
}
