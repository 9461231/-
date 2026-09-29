package com.muxu.supermarket.product.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 价格变更记录（采购价/销售价/会员价）
 */
@Data
@Entity
@Table(name = "price_change_record", indexes = @Index(name = "idx_pcr_product", columnList = "product_id"))
public class PriceChangeRecord {

    public static final String FIELD_PURCHASE_PRICE = "purchasePrice";
    public static final String FIELD_SALE_PRICE = "salePrice";
    public static final String FIELD_MEMBER_PRICE = "memberPrice";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    /** purchasePrice / salePrice / memberPrice */
    @Column(name = "field_name", nullable = false, length = 32)
    private String fieldName;

    @Column(name = "old_value", precision = 10, scale = 2)
    private BigDecimal oldValue;

    @Column(name = "new_value", precision = 10, scale = 2)
    private BigDecimal newValue;

    @Column(name = "changed_by", length = 50)
    private String changedBy;

    /** WEB / AGENT / IMPORT */
    @Column(length = 16)
    private String source = "WEB";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
