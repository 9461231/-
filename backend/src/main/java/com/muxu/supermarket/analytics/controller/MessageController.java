package com.muxu.supermarket.analytics.controller;

import com.muxu.supermarket.common.ApiResponse;
import com.muxu.supermarket.inventory.dto.InventoryAlertVO;
import com.muxu.supermarket.inventory.dto.InventoryTransactionVO;
import com.muxu.supermarket.inventory.service.BatchService;
import com.muxu.supermarket.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一消息与预警中心：汇总各类业务提醒
 */
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final InventoryService inventoryService;
    private final BatchService batchService;

    /** 分类汇总（顶部铃铛徽标） */
    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        List<InventoryAlertVO> alerts = inventoryService.alerts();
        summary.put("inventoryAlerts", alerts.size());
        summary.put("outOfStock", alerts.stream()
                .filter(a -> "OUT_OF_STOCK".equals(a.getAlertType())).count());

        var expiry = batchService.expiryAlerts();
        summary.put("expiringUrgent", expiry.get("urgent").size());
        summary.put("expired", expiry.get("expired").size());
        return ApiResponse.ok(summary);
    }

    /** 库存预警明细 */
    @GetMapping("/inventory-alerts")
    public ApiResponse<List<InventoryAlertVO>> inventoryAlerts() {
        return ApiResponse.ok(inventoryService.alerts());
    }

    /** 临期/过期明细（group: expired / urgent / near） */
    @GetMapping("/expiring")
    public ApiResponse<Map<String, List<com.muxu.supermarket.inventory.dto.BatchDTOs.BatchVO>>> expiring() {
        return ApiResponse.ok(batchService.expiryAlerts());
    }

    /** 库存流水预警（例如最近损耗记录，供消息中心列表） */
    @GetMapping("/recent-transactions")
    public ApiResponse<List<InventoryTransactionVO>> recentTransactions(
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(inventoryService
                .transactions(1, Math.min(Math.max(size, 1), 50), null, null).getList());
    }
}
