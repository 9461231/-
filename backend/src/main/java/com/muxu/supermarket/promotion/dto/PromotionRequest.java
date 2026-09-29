package com.muxu.supermarket.promotion.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PromotionRequest {

    @NotBlank(message = "活动名称不能为空")
    @Size(max = 100, message = "活动名称不能超过100个字符")
    private String name;

    /** 1-直接折扣 2-满减 3-第二件优惠 4-会员专享价 */
    @NotNull(message = "促销类型不能为空")
    @Min(value = 1, message = "促销类型不正确")
    @Max(value = 4, message = "促销类型不正确")
    private Integer type;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @DecimalMin(value = "0.01", message = "折扣率必须在0~1之间")
    @DecimalMax(value = "1.00", message = "折扣率必须在0~1之间")
    private BigDecimal discountRate;

    @DecimalMin(value = "0.00", message = "门槛金额不能为负数")
    private BigDecimal minAmount;

    @DecimalMin(value = "0.01", message = "减免金额必须大于0")
    private BigDecimal reduceAmount;

    @DecimalMin(value = "0.01", message = "第二件折扣率必须在0~1之间")
    @DecimalMax(value = "1.00", message = "第二件折扣率必须在0~1之间")
    private BigDecimal secondRate;

    @DecimalMin(value = "0.00", message = "会员价不能为负数")
    private BigDecimal memberPrice;

    private String remark;

    /** 参与商品 */
    @NotEmpty(message = "至少选择一个参与商品")
    private List<Long> productIds;
}
