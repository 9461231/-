package com.muxu.supermarket.sales.dto;

import com.muxu.supermarket.sales.entity.SalesOrder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SalesOrderVO {

    private Long id;
    private String orderNo;
    private Long memberId;
    private String memberName;
    private Integer status;
    private String statusLabel;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal payableAmount;
    private String payMethod;
    private Integer pointsEarned;
    private String remark;
    private LocalDateTime createdAt;

    public static SalesOrderVO from(SalesOrder o, String memberName) {
        SalesOrderVO vo = new SalesOrderVO();
        vo.setId(o.getId());
        vo.setOrderNo(o.getOrderNo());
        vo.setMemberId(o.getMemberId());
        vo.setMemberName(memberName);
        vo.setStatus(o.getStatus());
        vo.setStatusLabel(switch (o.getStatus()) {
            case SalesOrder.STATUS_COMPLETED -> "已完成";
            case SalesOrder.STATUS_PARTIAL_RETURNED -> "部分退货";
            case SalesOrder.STATUS_RETURNED -> "已退货";
            default -> "未知";
        });
        vo.setTotalAmount(o.getTotalAmount());
        vo.setDiscountAmount(o.getDiscountAmount());
        vo.setPayableAmount(o.getPayableAmount());
        vo.setPayMethod(o.getPayMethod());
        vo.setPointsEarned(o.getPointsEarned());
        vo.setRemark(o.getRemark());
        vo.setCreatedAt(o.getCreatedAt());
        return vo;
    }
}
