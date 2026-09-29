package com.muxu.supermarket.inventory.controller;

import com.muxu.supermarket.common.ApiResponse;
import com.muxu.supermarket.inventory.dto.BatchDTOs.BatchVO;
import com.muxu.supermarket.inventory.dto.BatchDTOs.CreateRequest;
import com.muxu.supermarket.inventory.dto.BatchDTOs.DisposeRequest;
import com.muxu.supermarket.inventory.service.BatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/batches")
@RequiredArgsConstructor
public class BatchController {

    private final BatchService batchService;

    @GetMapping
    public ApiResponse<List<BatchVO>> list(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(batchService.list(productId, status));
    }

    /** 临期/过期预警分组 */
    @GetMapping("/expiry-alerts")
    public ApiResponse<Map<String, List<BatchVO>>> expiryAlerts() {
        return ApiResponse.ok(batchService.expiryAlerts());
    }

    @PostMapping
    public ApiResponse<BatchVO> create(@Valid @RequestBody CreateRequest request) {
        return ApiResponse.ok(batchService.create(request));
    }

    /** 临期/过期处置：扣减批次与总库存 */
    @PostMapping("/{id}/dispose")
    public ApiResponse<Void> dispose(@PathVariable Long id, @Valid @RequestBody DisposeRequest request) {
        batchService.dispose(id, request);
        return ApiResponse.ok();
    }
}
