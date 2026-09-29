package com.muxu.supermarket.product.dto;

import com.muxu.supermarket.product.entity.ProductCategory;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CategoryVO {

    private Long id;
    private String name;
    private Integer sortOrder;
    private Integer status;
    /** 该分类下的商品数量 */
    private long productCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CategoryVO from(ProductCategory category, long productCount) {
        CategoryVO vo = new CategoryVO();
        vo.setId(category.getId());
        vo.setName(category.getName());
        vo.setSortOrder(category.getSortOrder());
        vo.setStatus(category.getStatus());
        vo.setProductCount(productCount);
        vo.setCreatedAt(category.getCreatedAt());
        vo.setUpdatedAt(category.getUpdatedAt());
        return vo;
    }
}
