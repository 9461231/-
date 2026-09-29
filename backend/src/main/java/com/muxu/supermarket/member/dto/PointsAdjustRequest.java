package com.muxu.supermarket.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PointsAdjustRequest {

    /** 正数累计，负数扣减 */
    @NotNull(message = "积分变动不能为空")
    private Integer change;

    @NotBlank(message = "变动原因不能为空")
    private String reason;
}
