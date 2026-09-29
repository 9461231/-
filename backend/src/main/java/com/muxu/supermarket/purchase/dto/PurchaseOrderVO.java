package com.muxu.supermarket.purchase.dto;

import com.muxu.supermarket.purchase.entity.PurchaseOrder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PurchaseOrderVO {

    private Long id;
    private String orderNo;
    private Long supplierId;
    private String supplierName;
    private Integer status;
    private String statusLabel;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private Integer itemCount;
    private String remark;
    private String auditRemark;
    private LocalDateTime auditAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PurchaseOrderVO from(PurchaseOrder o, String supplierName, Integer itemCount) {
        PurchaseOrderVO vo = new PurchaseOrderVO();
        vo.setId(o.getId());
        vo.setOrderNo(o.getOrderNo());
        vo.setSupplierId(o.getSupplierId());
        vo.setSupplierName(supplierName);
        vo.setStatus(o.getStatus());
        vo.setStatusLabel(statusLabel(o.getStatus()));
        vo.setTotalAmount(o.getTotalAmount());
        vo.setPaidAmount(o.getPaidAmount());
        vo.setItemCount(itemCount);
        vo.setRemark(o.getRemark());
        vo.setAuditRemark(o.getAuditRemark());
        vo.setAuditAt(o.getAuditAt());
        vo.setCompletedAt(o.getCompletedAt());
        vo.setCreatedAt(o.getCreatedAt());
        vo.setUpdatedAt(o.getUpdatedAt());
        return vo;
    }

    public static String statusLabel(int status) {
        return switch (status) {
            case PurchaseOrder.STATUS_DRAFT -> "草稿";
            case PurchaseOrder.STATUS_PENDING_AUDIT -> "待审核";
            case PurchaseOrder.STATUS_AUDITED -> "已审核";
            case PurchaseOrder.STATUS_PARTIAL_RECEIVED -> "部分入库";
            case PurchaseOrder.STATUS_COMPLETED -> "已完成";
            case PurchaseOrder.STATUS_CANCELLED -> "已取消";
            default -> "未知";
        };
    }
}
