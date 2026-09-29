package com.muxu.supermarket.sales.service;

import com.muxu.supermarket.common.BusinessException;
import com.muxu.supermarket.member.repository.MemberRepository;
import com.muxu.supermarket.sales.entity.Coupon;
import com.muxu.supermarket.sales.entity.MemberCoupon;
import com.muxu.supermarket.sales.repository.CouponRepository;
import com.muxu.supermarket.sales.repository.MemberCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final MemberCouponRepository memberCouponRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public List<Coupon> list() {
        return couponRepository.findAll();
    }

    @Transactional
    public Coupon create(String name, BigDecimal minAmount, BigDecimal reduceAmount,
                         LocalDateTime validUntil, Integer totalCount) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("优惠券名称不能为空");
        }
        if (minAmount == null || reduceAmount == null || reduceAmount.signum() <= 0
                || minAmount.compareTo(reduceAmount) < 0) {
            throw new BusinessException("门槛金额必须 ≥ 抵扣金额，且抵扣金额大于0");
        }
        if (validUntil == null || validUntil.isBefore(LocalDateTime.now())) {
            throw new BusinessException("有效期必须晚于当前时间");
        }
        if (totalCount == null || totalCount < 1) {
            throw new BusinessException("发放总量必须大于0");
        }
        Coupon coupon = new Coupon();
        coupon.setName(name.trim());
        coupon.setMinAmount(minAmount);
        coupon.setReduceAmount(reduceAmount);
        coupon.setValidUntil(validUntil);
        coupon.setTotalCount(totalCount);
        coupon.setStatus(1);
        return couponRepository.save(coupon);
    }

    @Transactional
    public void updateStatus(Long id, Integer status) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "优惠券不存在"));
        coupon.setStatus(status != null && status == 1 ? 1 : 0);
        couponRepository.save(coupon);
    }

    /** 向会员发放一张券码 */
    @Transactional
    public MemberCoupon issue(Long couponId, Long memberId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(404, "优惠券不存在"));
        if (coupon.getStatus() != 1) {
            throw new BusinessException("优惠券已停用");
        }
        if (coupon.getIssuedCount() >= coupon.getTotalCount()) {
            throw new BusinessException("优惠券已发放完毕");
        }
        memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException("会员不存在: id=" + memberId));

        coupon.setIssuedCount(coupon.getIssuedCount() + 1);
        couponRepository.save(coupon);

        MemberCoupon mc = new MemberCoupon();
        mc.setCouponId(couponId);
        mc.setMemberId(memberId);
        mc.setCode("C" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        return memberCouponRepository.save(mc);
    }

    @Transactional(readOnly = true)
    public List<MemberCoupon> memberCoupons(Long memberId) {
        return memberCouponRepository.findTop50ByMemberIdOrderByIdDesc(memberId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> couponUsage(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(404, "优惠券不存在"));
        return Map.of(
                "name", coupon.getName(),
                "issued", coupon.getIssuedCount(),
                "total", coupon.getTotalCount(),
                "used", memberCouponRepository.countByCouponIdAndStatus(couponId, MemberCoupon.STATUS_USED));
    }
}
