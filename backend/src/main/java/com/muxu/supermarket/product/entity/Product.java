package com.muxu.supermarket.product.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品主数据
 * 注意：不维护 current_stock，库存数量由 Step 04 库存模块统一维护
 */
@Data
@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(nullable = false, length = 64, unique = true)
    private String sku;

    /** 条码可为空（如称重商品），但存在时必须唯一 */
    @Column(length = 64, unique = true)
    private String barcode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 64)
    private String brand;

    @Column(length = 64)
    private String spec;

    @Column(nullable = false, length = 16)
    private String unit;

    @Column(name = "purchase_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "sale_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal salePrice;

    /** 会员价（可选，低于销售价时会员结算使用） */
    @Column(name = "member_price", precision = 10, scale = 2)
    private BigDecimal memberPrice;

    /** 最低销售价（手工改价/促销不得低于该价） */
    @Column(name = "min_sale_price", precision = 10, scale = 2)
    private BigDecimal minSalePrice;

    @Column(name = "min_stock", nullable = false)
    private Integer minStock = 0;

    @Column(name = "max_stock")
    private Integer maxStock;

    /** 状态：1-在售 0-停售 */
    @Column(nullable = false)
    private Integer status = ProductStatus.ON_SALE.getCode();

    @Column(length = 255)
    private String remark;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
