package com.muxu.supermarket.stocktake.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 盘点明细
 */
@Data
@Entity
@Table(name = "stocktake_item")
public class StocktakeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stocktake_id", nullable = false)
    private Long stocktakeId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    /** 创建任务时的系统库存快照 */
    @Column(name = "system_qty", nullable = false)
    private Integer systemQty;

    /** 实盘数量（未盘点为 null） */
    @Column(name = "actual_qty")
    private Integer actualQty;

    /** 差异 = 实际 - 系统（正=盘盈，负=盘亏） */
    @Column
    private Integer diff;
}
