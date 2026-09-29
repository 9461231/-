package com.muxu.supermarket.auth.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户（超级管理员 / 店家 / 店内员工）
 */
@Data
@Entity
@Table(name = "sys_user", uniqueConstraints = @UniqueConstraint(name = "uk_user_username", columnNames = "username"))
public class SysUser {

    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ROLE_STORE_OWNER = "STORE_OWNER";
    public static final String ROLE_STORE_MANAGER = "STORE_MANAGER";
    public static final String ROLE_CASHIER = "CASHIER";
    public static final String ROLE_PURCHASER = "PURCHASER";
    public static final String ROLE_WAREHOUSE = "WAREHOUSE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String username;

    /** BCrypt 散列 */
    @Column(nullable = false, length = 100)
    private String password;

    @Column(name = "real_name", length = 50)
    private String realName;

    /** SUPER_ADMIN/STORE_OWNER/STORE_MANAGER/CASHIER/PURCHASER/WAREHOUSE */
    @Column(nullable = false, length = 32)
    private String role;

    /** 超级管理员为 NULL；店家与员工归属某店家 */
    @Column(name = "store_id")
    private Long storeId;

    /** 1-正常 0-禁用 */
    @Column(nullable = false)
    private Integer status = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
