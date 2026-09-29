package com.muxu.supermarket.supplier.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 供应商-商品合作关系（含供货价）
 */
@Data
@Entity
@Table(name = "supplier_product",
        uniqueConstraints = @UniqueConstraint(name = "uk_supplier_product", columnNames = {"supplier_id", "product_id"}))
public class SupplierProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    /** 供货价（可能不同于商品档案中的采购价） */
    @Column(name = "supply_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal supplyPrice;

    /** 是否主供货商 */
    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
