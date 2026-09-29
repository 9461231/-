package com.muxu.supermarket.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

public class AuthDTOs {

    @Data
    public static class LoginRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Data
    public static class LoginVO {
        private String token;
        private Long userId;
        private String username;
        private String realName;
        private String role;
        private String roleLabel;
        private Long storeId;
        private String storeName;
    }

    @Data
    public static class UserRequest {
        private String username;
        private String password;
        private String realName;
        private String role;
        private Long storeId;
        private Integer status;
    }

    @Data
    public static class UserVO {
        private Long id;
        private String username;
        private String realName;
        private String role;
        private String roleLabel;
        private Long storeId;
        private String storeName;
        private Integer status;
        private String createdAt;
    }

    @Data
    public static class StoreRequest {
        private String name;
        private String contactPerson;
        private String phone;
        private String address;
        private Integer status;
    }
}
