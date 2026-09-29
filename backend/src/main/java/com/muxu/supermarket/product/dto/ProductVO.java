package com.muxu.supermarket.product.dto;

import com.muxu.supermarket.product.entity.Product;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductVO {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private String sku;
    private String barcode;
    private String name;
    private String brand;
    private String spec;
    private String unit;
    private BigDecimal purchasePrice;
    private BigDecimal salePrice;
    private BigDecimal memberPrice;
    private BigDecimal minSalePrice;
    private Integer minStock;
    private Integer maxStock;
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductVO from(Product product, String categoryName) {
        ProductVO vo = new ProductVO();
        vo.setId(product.getId());
        vo.setCategoryId(product.getCategoryId());
        vo.setCategoryName(categoryName);
        vo.setSku(product.getSku());
        vo.setBarcode(product.getBarcode());
        vo.setName(product.getName());
        vo.setBrand(product.getBrand());
        vo.setSpec(product.getSpec());
        vo.setUnit(product.getUnit());
        vo.setPurchasePrice(product.getPurchasePrice());
        vo.setSalePrice(product.getSalePrice());
        vo.setMemberPrice(product.getMemberPrice());
        vo.setMinSalePrice(product.getMinSalePrice());
        vo.setMinStock(product.getMinStock());
        vo.setMaxStock(product.getMaxStock());
        vo.setStatus(product.getStatus());
        vo.setRemark(product.getRemark());
        vo.setCreatedAt(product.getCreatedAt());
        vo.setUpdatedAt(product.getUpdatedAt());
        return vo;
    }
}
