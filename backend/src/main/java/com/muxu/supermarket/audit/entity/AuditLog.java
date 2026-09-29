package com.muxu.supermarket.audit.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作审计日志：谁 / 什么时候 / 对什么 / 做了什么 / 来源
 */
@Data
@Entity
@Table(name = "audit_log", indexes = {
        @Index(name = "idx_audit_created", columnList = "created_at"),
        @Index(name = "idx_audit_user", columnList = "username")
})
public class AuditLog {

    public static final String SOURCE_WEB = "WEB";
    public static final String SOURCE_AGENT = "AGENT";
    public static final String SOURCE_SYSTEM = "SYSTEM";
    public static final String SOURCE_IMPORT = "IMPORT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(length = 50)
    private String username;

    @Column(length = 32)
    private String role;

    /** 操作类型：POST/PUT/DELETE 或业务语义 */
    @Column(name = "operation_type", nullable = false, length = 32)
    private String operationType;

    /** 目标类型，如 PRODUCT / PURCHASE_ORDER */
    @Column(name = "target_type", length = 32)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    /** 操作前数据（关键变更由业务写入，如价格变更） */
    @Column(name = "before_data", columnDefinition = "TEXT")
    private String beforeData;

    /** 操作内容/参数摘要 */
    @Column(name = "after_data", columnDefinition = "TEXT")
    private String afterData;

    /** 来源：WEB/AGENT/SYSTEM/IMPORT */
    @Column(nullable = false, length = 16)
    private String source = SOURCE_WEB;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
