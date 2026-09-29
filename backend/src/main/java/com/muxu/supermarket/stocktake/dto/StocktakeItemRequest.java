package com.muxu.supermarket.stocktake.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StocktakeItemRequest {

    @NotNull(message = "实盘数量不能为空")
    private Integer actualQty;
}
