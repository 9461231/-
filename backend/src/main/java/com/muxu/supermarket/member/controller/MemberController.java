package com.muxu.supermarket.member.controller;

import com.muxu.supermarket.common.ApiResponse;
import com.muxu.supermarket.common.PageVO;
import com.muxu.supermarket.member.dto.MemberRequest;
import com.muxu.supermarket.member.dto.MemberVO;
import com.muxu.supermarket.member.dto.PointsAdjustRequest;
import com.muxu.supermarket.member.entity.MemberConsumption;
import com.muxu.supermarket.member.entity.MemberLevel;
import com.muxu.supermarket.member.entity.MemberPointsRecord;
import com.muxu.supermarket.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping
    public ApiResponse<PageVO<MemberVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long levelId,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.ok(memberService.page(page, size, keyword, levelId, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<MemberVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(memberService.detail(id));
    }

    @PostMapping
    public ApiResponse<MemberVO> create(@Valid @RequestBody MemberRequest request) {
        return ApiResponse.ok(memberService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<MemberVO> update(@PathVariable Long id,
                                        @Valid @RequestBody MemberRequest request) {
        return ApiResponse.ok(memberService.update(id, request));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        memberService.updateStatus(id, status);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/points")
    public ApiResponse<Void> adjustPoints(@PathVariable Long id,
                                          @Valid @RequestBody PointsAdjustRequest request) {
        memberService.adjustPoints(id, request);
        return ApiResponse.ok();
    }

    /** 储值充值 */
    @PostMapping("/{id}/balance")
    public ApiResponse<MemberVO> recharge(@PathVariable Long id,
                                          @RequestBody java.util.Map<String, Object> body) {
        java.math.BigDecimal amount = new java.math.BigDecimal(String.valueOf(body.get("amount")));
        return ApiResponse.ok(com.muxu.supermarket.member.dto.MemberVO.from(
                memberService.recharge(id, amount, (String) body.get("remark")), null));
    }

    @GetMapping("/{id}/points-records")
    public ApiResponse<List<MemberPointsRecord>> pointsRecords(@PathVariable Long id) {
        return ApiResponse.ok(memberService.pointsRecords(id));
    }

    @GetMapping("/{id}/consumptions")
    public ApiResponse<List<MemberConsumption>> consumptions(@PathVariable Long id) {
        return ApiResponse.ok(memberService.consumptionRecords(id));
    }
}
