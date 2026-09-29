package com.muxu.supermarket.stocktake.controller;

import com.muxu.supermarket.common.ApiResponse;
import com.muxu.supermarket.common.PageVO;
import com.muxu.supermarket.stocktake.dto.*;
import com.muxu.supermarket.stocktake.entity.LossRecord;
import com.muxu.supermarket.stocktake.service.StocktakeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/stocktakes")
@RequiredArgsConstructor
public class StocktakeController {

    private final StocktakeService stocktakeService;

    @GetMapping
    public ApiResponse<PageVO<StocktakeVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(stocktakeService.page(page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<StocktakeVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(stocktakeService.detail(id));
    }

    @PostMapping
    public ApiResponse<StocktakeVO> create(@Valid @RequestBody StocktakeCreateRequest request) {
        return ApiResponse.ok(stocktakeService.create(request));
    }

    @PutMapping("/{id}/items/{itemId}")
    public ApiResponse<Void> recordActual(@PathVariable Long id, @PathVariable Long itemId,
                                          @Valid @RequestBody StocktakeItemRequest request) {
        stocktakeService.recordActual(id, itemId, request.getActualQty());
        return ApiResponse.ok();
    }

    /** 完成盘点：差异自动生成库存调整 */
    @PostMapping("/{id}/complete")
    public ApiResponse<StocktakeVO> complete(@PathVariable Long id) {
        return ApiResponse.ok(stocktakeService.complete(id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        stocktakeService.cancel(id);
        return ApiResponse.ok();
    }
}
