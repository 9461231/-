package com.muxu.supermarket.member.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 积分明细
 */
@Data
@Entity
@Table(name = "member_points_record", indexes = @Index(name = "idx_mpr_member", columnList = "member_id"))
public class MemberPointsRecord {

    public static final String TYPE_EARN = "EARN";
    public static final String TYPE_DEDUCT = "DEDUCT";
    public static final String TYPE_ADJUST = "ADJUST";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** 变动积分：正数累计，负数扣减 */
    @Column(nullable = false)
    private Integer change;

    @Column(nullable = false, length = 16)
    private String type;

    @Column(name = "ref_type", length = 32)
    private String refType;

    @Column(name = "ref_no", length = 64)
    private String refNo;

    @Column(length = 255)
    private String remark;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
