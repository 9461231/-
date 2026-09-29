package com.muxu.supermarket.product.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductRequest {

    @NotNull(message = "商品分类不能为空")
    private Long categoryId;

    @NotBlank(message = "SKU 编码不能为空")
    @Size(max = 64, message = "SKU 编码不能超过64个字符")
    private String sku;

    @Size(max = 64, message = "条码不能超过64个字符")
    private String barcode;

    @NotBlank(message = "商品名称不能为空")
    @Size(max = 100, message = "商品名称不能超过100个字符")
    private String name;

    @Size(max = 64, message = "品牌不能超过64个字符")
    private String brand;

    @Size(max = 64, message = "规格不能超过64个字符")
    private String spec;

    @NotBlank(message = "单位不能为空")
    @Size(max = 16, message = "单位不能超过16个字符")
    private String unit;

    @NotNull(message = "采购价不能为空")
    @DecimalMin(value = "0.00", message = "采购价不能为负数")
    @Digits(integer = 8, fraction = 2, message = "采购价格式不正确")
    private BigDecimal purchasePrice;

    @NotNull(message = "销售价不能为空")
    @DecimalMin(value = "0.00", message = "销售价不能为负数")
    @Digits(integer = 8, fraction = 2, message = "销售价格式不正确")
    private BigDecimal salePrice;

    @DecimalMin(value = "0.00", message = "会员价不能为负数")
    private BigDecimal memberPrice;

    @DecimalMin(value = "0.00", message = "最低销售价不能为负数")
    private BigDecimal minSalePrice;

    @NotNull(message = "最低库存不能为空")
    @Min(value = 0, message = "最低库存不能为负数")
    private Integer minStock;

    @Min(value = 0, message = "最高库存不能为负数")
    private Integer maxStock;

    /** 1-在售 0-停售 */
    private Integer status = 1;

    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;
}
