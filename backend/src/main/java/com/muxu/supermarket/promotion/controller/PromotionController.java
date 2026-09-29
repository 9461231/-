package com.muxu.supermarket.promotion.controller;

import com.muxu.supermarket.common.ApiResponse;
import com.muxu.supermarket.common.PageVO;
import com.muxu.supermarket.promotion.dto.PromotionRequest;
import com.muxu.supermarket.promotion.dto.PromotionVO;
import com.muxu.supermarket.promotion.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping
    public ApiResponse<PageVO<PromotionVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.ok(promotionService.page(page, size, type, status));
    }

    @PostMapping
    public ApiResponse<PromotionVO> create(@Valid @RequestBody PromotionRequest request) {
        return ApiResponse.ok(promotionService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<PromotionVO> update(@PathVariable Long id,
                                           @Valid @RequestBody PromotionRequest request) {
        return ApiResponse.ok(promotionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        promotionService.delete(id);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        promotionService.updateStatus(id, status);
        return ApiResponse.ok();
    }
}
