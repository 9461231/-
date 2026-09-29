package com.muxu.supermarket.purchase.controller;

import com.muxu.supermarket.common.ApiResponse;
import com.muxu.supermarket.common.PageVO;
import com.muxu.supermarket.purchase.dto.PurchaseReturnRequest;
import com.muxu.supermarket.purchase.dto.PurchaseReturnVO;
import com.muxu.supermarket.purchase.service.PurchaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/purchase-returns")
@RequiredArgsConstructor
public class PurchaseReturnController {

    private final PurchaseService purchaseService;

    @GetMapping
    public ApiResponse<PageVO<PurchaseReturnVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(purchaseService.pageReturns(page, size, keyword));
    }

    /** 采购退货：库存减少（同一事务） */
    @PostMapping
    public ApiResponse<PurchaseReturnVO> create(@Valid @RequestBody PurchaseReturnRequest request) {
        return ApiResponse.ok(purchaseService.createReturn(request));
    }

    @GetMapping("/stats/month")
    public ApiResponse<Map<String, Object>> monthStats() {
        return ApiResponse.ok(purchaseService.monthStats());
    }
}
