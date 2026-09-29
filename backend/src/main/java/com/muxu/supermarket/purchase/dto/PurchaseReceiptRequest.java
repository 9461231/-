package com.muxu.supermarket.purchase.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.List;

@Data
public class PurchaseReceiptRequest {

    @Valid
    @NotEmpty(message = "入库明细不能为空")
    private List<Item> items;

    private String remark;

    /** 可选：生产日期（yyyy-MM-dd），提供保质期天数时自动生成批次 */
    private String productionDate;

    /** 可选：保质期（天） */
    private Integer shelfLifeDays;

    @Data
    public static class Item {

        /** 采购订单明细ID */
        private Long orderItemId;

        private Long productId;

        @Min(value = 1, message = "入库数量必须大于0")
        private Integer quantity;
    }
}
