package com.muxu.supermarket.supplier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SupplierRequest {

    @NotBlank(message = "供应商名称不能为空")
    @Size(max = 100, message = "供应商名称不能超过100个字符")
    private String name;

    @Size(max = 50, message = "联系人不能超过50个字符")
    private String contactPerson;

    @Size(max = 32, message = "联系方式不能超过32个字符")
    private String phone;

    @Size(max = 100, message = "邮箱不能超过100个字符")
    private String email;

    @Size(max = 200, message = "地址不能超过200个字符")
    private String address;

    /** 1-合作中 0-已停止合作 */
    private Integer status = 1;

    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;
}
