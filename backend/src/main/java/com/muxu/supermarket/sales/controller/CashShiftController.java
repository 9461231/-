package com.muxu.supermarket.sales.controller;

import com.muxu.supermarket.auth.entity.SysUser;
import com.muxu.supermarket.common.ApiResponse;
import com.muxu.supermarket.common.RequireRoles;
import com.muxu.supermarket.sales.entity.CashShift;
import com.muxu.supermarket.sales.service.ShiftService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cash-shifts")
@RequiredArgsConstructor
public class CashShiftController {

    private final ShiftService shiftService;

    /** 开班（收银员/店长/店主） */
    @RequireRoles({SysUser.ROLE_SUPER_ADMIN, SysUser.ROLE_STORE_OWNER,
            SysUser.ROLE_STORE_MANAGER, SysUser.ROLE_CASHIER})
    @PostMapping("/open")
    public ApiResponse<CashShift> open(@RequestBody(required = false) Map<String, String> body) {
        return ApiResponse.ok(shiftService.open(body == null ? null : body.get("remark")));
    }

    /** 当前进行中的班次 */
    @GetMapping("/current")
    public ApiResponse<CashShift> current() {
        return ApiResponse.ok(shiftService.current().orElse(null));
    }

    /** 交班（生成本班次汇总） */
    @RequireRoles({SysUser.ROLE_SUPER_ADMIN, SysUser.ROLE_STORE_OWNER,
            SysUser.ROLE_STORE_MANAGER, SysUser.ROLE_CASHIER})
    @PostMapping("/{id}/close")
    public ApiResponse<CashShift> close(@PathVariable Long id,
                                        @RequestBody(required = false) Map<String, String> body) {
        return ApiResponse.ok(shiftService.close(id, body == null ? null : body.get("remark")));
    }

    @GetMapping
    public ApiResponse<List<CashShift>> history() {
        return ApiResponse.ok(shiftService.history());
    }

    /** 营业日结单 */
    @GetMapping("/daily-settlement")
    public ApiResponse<Map<String, Object>> dailySettlement(
            @RequestParam(required = false) String date) {
        LocalDate day = date == null || date.isBlank()
                ? LocalDate.now() : LocalDate.parse(date);
        return ApiResponse.ok(shiftService.dailySettlement(day));
    }
}
