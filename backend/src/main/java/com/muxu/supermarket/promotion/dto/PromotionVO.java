package com.muxu.supermarket.promotion.dto;

import com.muxu.supermarket.promotion.entity.Promotion;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PromotionVO {

    private Long id;
    private String name;
    private Integer type;
    private String typeLabel;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal discountRate;
    private BigDecimal minAmount;
    private BigDecimal reduceAmount;
    private BigDecimal secondRate;
    private BigDecimal memberPrice;
    private String remark;
    private List<Long> productIds;
    private LocalDateTime createdAt;

    public static PromotionVO from(Promotion p, List<Long> productIds) {
        PromotionVO vo = new PromotionVO();
        vo.setId(p.getId());
        vo.setName(p.getName());
        vo.setType(p.getType());
        vo.setTypeLabel(p.typeLabel());
        vo.setStatus(p.getStatus());
        vo.setStartTime(p.getStartTime());
        vo.setEndTime(p.getEndTime());
        vo.setDiscountRate(p.getDiscountRate());
        vo.setMinAmount(p.getMinAmount());
        vo.setReduceAmount(p.getReduceAmount());
        vo.setSecondRate(p.getSecondRate());
        vo.setMemberPrice(p.getMemberPrice());
        vo.setRemark(p.getRemark());
        vo.setProductIds(productIds);
        vo.setCreatedAt(p.getCreatedAt());
        return vo;
    }
}
