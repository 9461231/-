package com.muxu.supermarket.auth.controller;

import com.muxu.supermarket.auth.dto.AuthDTOs.LoginRequest;
import com.muxu.supermarket.auth.dto.AuthDTOs.LoginVO;
import com.muxu.supermarket.auth.service.AuthService;
import com.muxu.supermarket.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        // 无状态 JWT：前端删除令牌即可
        return ApiResponse.ok();
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserHolder> me() {
        var user = com.muxu.supermarket.common.CurrentUser.get();
        CurrentUserHolder holder = new CurrentUserHolder();
        if (user != null) {
            holder.setUserId(user.userId());
            holder.setUsername(user.username());
            holder.setRole(user.role());
            holder.setStoreId(user.storeId());
        }
        return ApiResponse.ok(holder);
    }

    @lombok.Data
    public static class CurrentUserHolder {
        private Long userId;
        private String username;
        private String role;
        private Long storeId;
    }

    @GetMapping("/roles")
    public ApiResponse<List<String>> roles() {
        return ApiResponse.ok(authService.allRoles());
    }

    @GetMapping("/role-labels")
    public ApiResponse<Map<String, String>> roleLabels() {
        return ApiResponse.ok(java.util.Map.of(
                "SUPER_ADMIN", "超级管理员",
                "STORE_OWNER", "店主",
                "STORE_MANAGER", "店长",
                "CASHIER", "收银员",
                "PURCHASER", "采购员",
                "WAREHOUSE", "库管员"
        ));
    }
}
