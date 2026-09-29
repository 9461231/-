package com.muxu.supermarket.stocktake.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class StocktakeCreateRequest {

    @NotEmpty(message = "至少选择一个盘点商品")
    private List<Long> productIds;

    private String remark;
}
