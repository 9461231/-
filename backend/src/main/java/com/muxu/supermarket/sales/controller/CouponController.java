package com.muxu.supermarket.sales.controller;

import com.muxu.supermarket.common.ApiResponse;
import com.muxu.supermarket.sales.entity.Coupon;
import com.muxu.supermarket.sales.entity.MemberCoupon;
import com.muxu.supermarket.sales.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping
    public ApiResponse<List<Coupon>> list() {
        return ApiResponse.ok(couponService.list());
    }

    @PostMapping
    public ApiResponse<Coupon> create(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(couponService.create(
                (String) body.get("name"),
                new BigDecimal(String.valueOf(body.get("minAmount"))),
                new BigDecimal(String.valueOf(body.get("reduceAmount"))),
                body.get("validUntil") == null ? null : LocalDateTime.parse((String) body.get("validUntil")),
                body.get("totalCount") == null ? null : Integer.parseInt(String.valueOf(body.get("totalCount")))));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        couponService.updateStatus(id, status);
        return ApiResponse.ok();
    }

    /** 发放给会员，返回券码 */
    @PostMapping("/{id}/issue")
    public ApiResponse<MemberCoupon> issue(@PathVariable Long id, @RequestParam Long memberId) {
        return ApiResponse.ok(couponService.issue(id, memberId));
    }

    @GetMapping("/member/{memberId}")
    public ApiResponse<List<MemberCoupon>> memberCoupons(@PathVariable Long memberId) {
        return ApiResponse.ok(couponService.memberCoupons(memberId));
    }

    @GetMapping("/{id}/usage")
    public ApiResponse<Map<String, Object>> usage(@PathVariable Long id) {
        return ApiResponse.ok(couponService.couponUsage(id));
    }
}
