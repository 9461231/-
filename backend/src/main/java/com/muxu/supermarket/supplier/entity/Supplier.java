package com.muxu.supermarket.supplier.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 供应商
 */
@Data
@Entity
@Table(name = "supplier")
public class Supplier {

    public static final int STATUS_ENABLED = 1;
    public static final int STATUS_DISABLED = 0;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @Column(name = "contact_person", length = 50)
    private String contactPerson;

    @Column(length = 32)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(length = 200)
    private String address;

    /** 状态：1-合作中 0-已停止合作 */
    @Column(nullable = false)
    private Integer status = STATUS_ENABLED;

    @Column(length = 255)
    private String remark;

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
