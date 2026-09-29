package com.muxu.supermarket.stocktake.controller;

import com.muxu.supermarket.common.ApiResponse;
import com.muxu.supermarket.common.PageVO;
import com.muxu.supermarket.stocktake.dto.LossRecordRequest;
import com.muxu.supermarket.stocktake.entity.LossRecord;
import com.muxu.supermarket.stocktake.service.StocktakeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/loss-records")
@RequiredArgsConstructor
public class LossRecordController {

    private final StocktakeService stocktakeService;

    @GetMapping
    public ApiResponse<PageVO<Map<String, Object>>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(stocktakeService.pageLoss(page, size));
    }

    /** 损耗登记：库存减少（同一事务） */
    @PostMapping
    public ApiResponse<LossRecord> create(@Valid @RequestBody LossRecordRequest request) {
        return ApiResponse.ok(stocktakeService.createLoss(request));
    }
}
