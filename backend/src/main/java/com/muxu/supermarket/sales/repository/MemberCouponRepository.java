package com.muxu.supermarket.sales.repository;

import com.muxu.supermarket.sales.entity.MemberCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberCouponRepository extends JpaRepository<MemberCoupon, Long> {

    Optional<MemberCoupon> findByCode(String code);

    List<MemberCoupon> findTop50ByMemberIdOrderByIdDesc(Long memberId);

    long countByCouponIdAndStatus(Long couponId, Integer status);
}
