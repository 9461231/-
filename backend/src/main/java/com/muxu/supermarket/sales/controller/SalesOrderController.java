package com.muxu.supermarket.sales.controller;

import com.muxu.supermarket.common.ApiResponse;
import com.muxu.supermarket.common.PageVO;
import com.muxu.supermarket.sales.dto.SalesOrderRequest;
import com.muxu.supermarket.sales.dto.SalesOrderVO;
import com.muxu.supermarket.sales.dto.SalesReturnRequest;
import com.muxu.supermarket.sales.service.SalesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sales-orders")
@RequiredArgsConstructor
public class SalesOrderController {

    private final SalesService salesService;

    /** 结算：生成订单 + 扣库存 + 会员积分（同一事务） */
    @PostMapping
    public ApiResponse<SalesOrderVO> checkout(@Valid @RequestBody SalesOrderRequest request) {
        return ApiResponse.ok(salesService.checkout(request));
    }

    @GetMapping
    public ApiResponse<PageVO<SalesOrderVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.ok(salesService.page(page, size, keyword, memberId, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.ok(salesService.detail(id));
    }

    /** 销售退货：库存回增（同一事务） */
    @PostMapping("/returns")
    public ApiResponse<Map<String, Object>> createReturn(@Valid @RequestBody SalesReturnRequest request) {
        return ApiResponse.ok(salesService.createReturn(request));
    }
}
