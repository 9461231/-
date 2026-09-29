package com.muxu.supermarket.supplier.dto;

import com.muxu.supermarket.supplier.entity.Supplier;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SupplierVO {

    private Long id;
    private String name;
    private String contactPerson;
    private String phone;
    private String email;
    private String address;
    private Integer status;
    private String remark;
    /** 合作商品数量 */
    private long productCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SupplierVO from(Supplier s, long productCount) {
        SupplierVO vo = new SupplierVO();
        vo.setId(s.getId());
        vo.setName(s.getName());
        vo.setContactPerson(s.getContactPerson());
        vo.setPhone(s.getPhone());
        vo.setEmail(s.getEmail());
        vo.setAddress(s.getAddress());
        vo.setStatus(s.getStatus());
        vo.setRemark(s.getRemark());
        vo.setProductCount(productCount);
        vo.setCreatedAt(s.getCreatedAt());
        vo.setUpdatedAt(s.getUpdatedAt());
        return vo;
    }
}
