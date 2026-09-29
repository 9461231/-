package com.muxu.supermarket.sales.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会员持有的优惠券实例
 */
@Data
@Entity
@Table(name = "member_coupon", uniqueConstraints = @UniqueConstraint(name = "uk_coupon_code", columnNames = "code"))
public class MemberCoupon {

    /** 未使用 */
    public static final int STATUS_UNUSED = 0;
    /** 已使用 */
    public static final int STATUS_USED = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** 券码（收银时报文核销） */
    @Column(nullable = false, length = 32)
    private String code;

    @Column(nullable = false)
    private Integer status = STATUS_UNUSED;

    @Column(name = "used_order_id")
    private Long usedOrderId;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
