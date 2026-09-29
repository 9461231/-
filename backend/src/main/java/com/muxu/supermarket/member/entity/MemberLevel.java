package com.muxu.supermarket.member.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 会员等级（按累计消费金额阈值）
 */
@Data
@Entity
@Table(name = "member_level")
public class MemberLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30, unique = true)
    private String name;

    /** 累计消费达到该阈值升级到此等级 */
    @Column(name = "points_threshold", nullable = false)
    private BigDecimal pointsThreshold = BigDecimal.ZERO;

    /** 折扣（如 0.95），仅作展示，结算优惠由促销模块计算 */
    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal discount = BigDecimal.ONE;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
