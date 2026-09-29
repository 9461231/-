package com.muxu.supermarket.sales.repository;

import com.muxu.supermarket.sales.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
}
