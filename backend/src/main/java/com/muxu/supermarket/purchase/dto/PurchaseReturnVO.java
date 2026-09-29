package com.muxu.supermarket.purchase.dto;

import com.muxu.supermarket.purchase.entity.PurchaseReturn;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PurchaseReturnVO {

    private Long id;
    private String returnNo;
    private Long supplierId;
    private String supplierName;
    private Long orderId;
    private BigDecimal totalAmount;
    private String reason;
    private LocalDateTime createdAt;
    private List<PurchaseReturnVOItem> items;

    @Data
    public static class PurchaseReturnVOItem {
        private Long productId;
        private String productName;
        private Integer quantity;
        private BigDecimal purchasePrice;
    }

    public static PurchaseReturnVO from(PurchaseReturn r, String supplierName) {
        PurchaseReturnVO vo = new PurchaseReturnVO();
        vo.setId(r.getId());
        vo.setReturnNo(r.getReturnNo());
        vo.setSupplierId(r.getSupplierId());
        vo.setSupplierName(supplierName);
        vo.setOrderId(r.getOrderId());
        vo.setTotalAmount(r.getTotalAmount());
        vo.setReason(r.getReason());
        vo.setCreatedAt(r.getCreatedAt());
        return vo;
    }
}
