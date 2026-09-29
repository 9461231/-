package com.muxu.supermarket.auth.controller;

import com.muxu.supermarket.auth.dto.AuthDTOs.StoreRequest;
import com.muxu.supermarket.auth.dto.AuthDTOs.UserRequest;
import com.muxu.supermarket.auth.dto.AuthDTOs.UserVO;
import com.muxu.supermarket.auth.entity.SysUser;
import com.muxu.supermarket.auth.service.UserService;
import com.muxu.supermarket.common.ApiResponse;
import com.muxu.supermarket.common.PageVO;
import com.muxu.supermarket.common.RequireRoles;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ==================== 店内员工（店家/店长管理） ====================

    @RequireRoles({SysUser.ROLE_SUPER_ADMIN, SysUser.ROLE_STORE_OWNER, SysUser.ROLE_STORE_MANAGER})
    @GetMapping("/users")
    public ApiResponse<PageVO<UserVO>> pageUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role) {
        return ApiResponse.ok(userService.page(page, size, keyword, role));
    }

    @RequireRoles({SysUser.ROLE_SUPER_ADMIN, SysUser.ROLE_STORE_OWNER})
    @PostMapping("/users")
    public ApiResponse<UserVO> createUser(@RequestBody UserRequest request) {
        return ApiResponse.ok(userService.create(request));
    }

    @RequireRoles({SysUser.ROLE_SUPER_ADMIN, SysUser.ROLE_STORE_OWNER})
    @PutMapping("/users/{id}")
    public ApiResponse<UserVO> updateUser(@PathVariable Long id, @RequestBody UserRequest request) {
        return ApiResponse.ok(userService.update(id, request));
    }

    @RequireRoles({SysUser.ROLE_SUPER_ADMIN, SysUser.ROLE_STORE_OWNER})
    @PutMapping("/users/{id}/password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        userService.resetPassword(id, body.get("password"));
        return ApiResponse.ok();
    }

    @RequireRoles({SysUser.ROLE_SUPER_ADMIN, SysUser.ROLE_STORE_OWNER})
    @PutMapping("/users/{id}/status")
    public ApiResponse<Void> updateUserStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return ApiResponse.ok();
    }

    // ==================== 店家 / 租户（超级管理员） ====================

    @RequireRoles(SysUser.ROLE_SUPER_ADMIN)
    @GetMapping("/stores")
    public ApiResponse<PageVO<Map<String, Object>>> pageStores(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(userService.pageStores(page, size, keyword));
    }

    @RequireRoles(SysUser.ROLE_SUPER_ADMIN)
    @PostMapping("/stores")
    public ApiResponse<Map<String, Object>> createStore(@RequestBody StoreRequest request) {
        return ApiResponse.ok(userService.createStore(request));
    }

    @RequireRoles(SysUser.ROLE_SUPER_ADMIN)
    @PutMapping("/stores/{id}")
    public ApiResponse<Void> updateStore(@PathVariable Long id, @RequestBody StoreRequest request) {
        userService.updateStore(id, request);
        return ApiResponse.ok();
    }

    @RequireRoles(SysUser.ROLE_SUPER_ADMIN)
    @GetMapping("/stores/platform-stats")
    public ApiResponse<Map<String, Object>> platformStats() {
        return ApiResponse.ok(userService.platformStats());
    }
}
