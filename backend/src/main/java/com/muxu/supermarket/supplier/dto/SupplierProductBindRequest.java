package com.muxu.supermarket.supplier.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SupplierProductBindRequest {

    @NotNull(message = "商品不能为空")
    private Long productId;

    @NotNull(message = "供货价不能为空")
    @DecimalMin(value = "0.00", message = "供货价不能为负数")
    private BigDecimal supplyPrice;

    private Boolean isPrimary = false;
}
