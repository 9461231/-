package com.muxu.supermarket.purchase.controller;

import com.muxu.supermarket.common.ApiResponse;
import com.muxu.supermarket.common.PageVO;
import com.muxu.supermarket.purchase.dto.PurchaseOrderRequest;
import com.muxu.supermarket.purchase.dto.PurchaseOrderVO;
import com.muxu.supermarket.purchase.dto.PurchaseReceiptRequest;
import com.muxu.supermarket.purchase.entity.PurchasePayment;
import com.muxu.supermarket.purchase.service.PurchaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseService purchaseService;

    @GetMapping
    public ApiResponse<PageVO<PurchaseOrderVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.ok(purchaseService.page(page, size, keyword, supplierId, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.ok(purchaseService.detail(id));
    }

    @PostMapping
    public ApiResponse<PurchaseOrderVO> create(@Valid @RequestBody PurchaseOrderRequest request) {
        return ApiResponse.ok(purchaseService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<PurchaseOrderVO> update(@PathVariable Long id,
                                               @Valid @RequestBody PurchaseOrderRequest request) {
        return ApiResponse.ok(purchaseService.update(id, request));
    }

    @PostMapping("/{id}/submit")
    public ApiResponse<Void> submit(@PathVariable Long id) {
        purchaseService.submit(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/audit")
    public ApiResponse<Void> audit(@PathVariable Long id,
                                   @RequestParam boolean approved,
                                   @RequestParam(required = false) String auditRemark) {
        purchaseService.audit(id, approved, auditRemark);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        purchaseService.cancel(id);
        return ApiResponse.ok();
    }

    /** 采购入库：完成入库单 + 库存增加（同一事务） */
    @PostMapping("/{id}/receipts")
    public ApiResponse<Map<String, Object>> receipt(@PathVariable Long id,
                                                    @Valid @RequestBody PurchaseReceiptRequest request) {
        return ApiResponse.ok(purchaseService.receipt(id, request));
    }

    // ==================== 采购结算 ====================

    /** 登记付款 */
    @PostMapping("/{id}/payments")
    public ApiResponse<PurchasePayment> pay(@PathVariable Long id,
                                            @RequestBody Map<String, Object> body) {
        java.math.BigDecimal amount = new java.math.BigDecimal(String.valueOf(body.get("amount")));
        String method = (String) body.getOrDefault("method", "CASH");
        String remark = (String) body.get("remark");
        return ApiResponse.ok(purchaseService.pay(id, amount, method, remark));
    }

    /** 付款记录 */
    @GetMapping("/{id}/payments")
    public ApiResponse<List<PurchasePayment>> payments(@PathVariable Long id) {
        return ApiResponse.ok(purchaseService.payments(id));
    }
}
