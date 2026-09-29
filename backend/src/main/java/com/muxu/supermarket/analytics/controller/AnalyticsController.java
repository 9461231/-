package com.muxu.supermarket.analytics.controller;

import com.muxu.supermarket.analytics.service.AnalyticsService;
import com.muxu.supermarket.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.ok(analyticsService.dashboard());
    }

    @GetMapping("/sales-trend")
    public ApiResponse<List<Map<String, Object>>> salesTrend(@RequestParam(defaultValue = "14") int days) {
        return ApiResponse.ok(analyticsService.salesTrend(Math.min(Math.max(days, 1), 90)));
    }

    @GetMapping("/top-products")
    public ApiResponse<List<Map<String, Object>>> topProducts(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(analyticsService.topProducts(Math.min(Math.max(days, 1), 365),
                Math.min(Math.max(limit, 1), 50)));
    }

    @GetMapping("/slow-products")
    public ApiResponse<List<Map<String, Object>>> slowProducts(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(analyticsService.slowProducts(Math.min(Math.max(days, 1), 365),
                Math.min(Math.max(limit, 1), 50)));
    }

    @GetMapping("/category-sales")
    public ApiResponse<List<Map<String, Object>>> categorySales(@RequestParam(defaultValue = "30") int days) {
        return ApiResponse.ok(analyticsService.categorySales(Math.min(Math.max(days, 1), 365)));
    }

    @GetMapping("/loss-stats")
    public ApiResponse<List<Map<String, Object>>> lossStats(@RequestParam(defaultValue = "6") int months) {
        return ApiResponse.ok(analyticsService.lossStats(Math.min(Math.max(months, 1), 24)));
    }

    @GetMapping("/purchase-trend")
    public ApiResponse<List<Map<String, Object>>> purchaseTrend(@RequestParam(defaultValue = "6") int months) {
        return ApiResponse.ok(analyticsService.purchaseTrend(Math.min(Math.max(months, 1), 24)));
    }

    @GetMapping("/promotion-analysis")
    public ApiResponse<List<Map<String, Object>>> promotionAnalysis(@RequestParam(defaultValue = "30") int days) {
        return ApiResponse.ok(analyticsService.promotionAnalysis(Math.min(Math.max(days, 1), 365)));
    }

    @GetMapping("/loss-reasons")
    public ApiResponse<List<Map<String, Object>>> lossReasons(@RequestParam(defaultValue = "30") int days) {
        return ApiResponse.ok(analyticsService.lossReasonRanking(Math.min(Math.max(days, 1), 365)));
    }

    @GetMapping("/member-analysis")
    public ApiResponse<Map<String, Object>> memberAnalysis(@RequestParam(defaultValue = "30") int days) {
        return ApiResponse.ok(analyticsService.memberAnalysis(Math.min(Math.max(days, 1), 365)));
    }

    @GetMapping("/inventory-analysis")
    public ApiResponse<Map<String, Object>> inventoryAnalysis() {
        return ApiResponse.ok(analyticsService.inventoryAnalysis());
    }

    /** 经营驾驶舱（店主首页） */
    @GetMapping("/cockpit")
    public ApiResponse<Map<String, Object>> cockpit() {
        return ApiResponse.ok(analyticsService.cockpit());
    }
}
