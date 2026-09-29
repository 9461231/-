package com.muxu.supermarket.stocktake.dto;

import com.muxu.supermarket.stocktake.entity.Stocktake;
import com.muxu.supermarket.stocktake.entity.StocktakeItem;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class StocktakeVO {

    private Long id;
    private String taskNo;
    private Integer status;
    private String statusLabel;
    private String remark;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private List<Item> items;

    @Data
    public static class Item {
        private Long id;
        private Long productId;
        private String productName;
        private String sku;
        private String unit;
        private Integer systemQty;
        private Integer actualQty;
        private Integer diff;
    }

    public static StocktakeVO from(Stocktake t, List<Item> items) {
        StocktakeVO vo = new StocktakeVO();
        vo.setId(t.getId());
        vo.setTaskNo(t.getTaskNo());
        vo.setStatus(t.getStatus());
        vo.setStatusLabel(switch (t.getStatus()) {
            case Stocktake.STATUS_IN_PROGRESS -> "进行中";
            case Stocktake.STATUS_COMPLETED -> "已完成";
            case Stocktake.STATUS_CANCELLED -> "已取消";
            default -> "未知";
        });
        vo.setRemark(t.getRemark());
        vo.setCompletedAt(t.getCompletedAt());
        vo.setCreatedAt(t.getCreatedAt());
        vo.setItems(items);
        return vo;
    }

    public static Item toItem(StocktakeItem item, String productName, String sku, String unit) {
        StocktakeVO.Item vi = new StocktakeVO.Item();
        vi.setId(item.getId());
        vi.setProductId(item.getProductId());
        vi.setProductName(productName);
        vi.setSku(sku);
        vi.setUnit(unit);
        vi.setSystemQty(item.getSystemQty());
        vi.setActualQty(item.getActualQty());
        vi.setDiff(item.getDiff());
        return vi;
    }
}
