package com.muxu.supermarket.inventory.controller;

import com.muxu.supermarket.common.ApiResponse;
import com.muxu.supermarket.common.PageVO;
import com.muxu.supermarket.inventory.dto.*;
import com.muxu.supermarket.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ApiResponse<PageVO<InventoryVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String alertType) {
        return ApiResponse.ok(inventoryService.page(page, size, keyword, categoryId, alertType));
    }

    @GetMapping("/alerts")
    public ApiResponse<List<InventoryAlertVO>> alerts() {
        return ApiResponse.ok(inventoryService.alerts());
    }

    @GetMapping("/transactions")
    public ApiResponse<PageVO<InventoryTransactionVO>> transactions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String type) {
        return ApiResponse.ok(inventoryService.transactions(page, size, productId, type));
    }

    @PostMapping("/adjust")
    public ApiResponse<Void> adjust(@Valid @RequestBody InventoryAdjustRequest request) {
        inventoryService.manualAdjust(request);
        return ApiResponse.ok();
    }
}
