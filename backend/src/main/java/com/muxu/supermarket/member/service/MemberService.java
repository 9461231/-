package com.muxu.supermarket.member.service;

import com.muxu.supermarket.common.BusinessException;
import com.muxu.supermarket.common.PageVO;
import com.muxu.supermarket.member.dto.MemberRequest;
import com.muxu.supermarket.member.dto.MemberVO;
import com.muxu.supermarket.member.dto.PointsAdjustRequest;
import com.muxu.supermarket.member.entity.*;
import com.muxu.supermarket.member.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberService {

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final MemberRepository memberRepository;
    private final MemberLevelRepository levelRepository;
    private final MemberPointsRecordRepository pointsRecordRepository;
    private final MemberConsumptionRepository consumptionRepository;

    // ==================== 会员 ====================

    @Transactional(readOnly = true)
    public PageVO<MemberVO> page(int page, int size, String keyword, Long levelId, Integer status) {
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 10;

        Specification<Member> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("name"), like),
                        cb.like(root.get("phone"), like),
                        cb.like(root.get("memberNo"), like)));
            }
            if (levelId != null) {
                predicates.add(cb.equal(root.get("levelId"), levelId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        Page<Member> result = memberRepository.findAll(spec,
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id")));

        Map<Long, String> levelNames = levelRepository.findAll().stream()
                .collect(Collectors.toMap(MemberLevel::getId, MemberLevel::getName));
        List<MemberVO> list = result.getContent().stream()
                .map(m -> MemberVO.from(m, levelNames.get(m.getLevelId())))
                .toList();
        return new PageVO<>(list, result.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public MemberVO detail(Long id) {
        Member member = getMember(id);
        return MemberVO.from(member, levelName(member.getLevelId()));
    }

    @Transactional
    public MemberVO create(MemberRequest request) {
        if (memberRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException("手机号已注册: " + request.getPhone());
        }
        MemberLevel defaultLevel = levelRepository.findFirstByOrderByPointsThresholdAsc()
                .orElseThrow(() -> new BusinessException("缺少会员等级数据，请先初始化等级"));
        Member member = new Member();
        member.setMemberNo("M" + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", (int) (Math.random() * 1000)));
        member.setName(request.getName().trim());
        member.setPhone(request.getPhone());
        member.setGender(request.getGender() == null ? 0 : request.getGender());
        member.setBirthday(request.getBirthday());
        member.setLevelId(defaultLevel.getId());
        member.setStatus(1);
        return MemberVO.from(memberRepository.save(member), defaultLevel.getName());
    }

    @Transactional
    public MemberVO update(Long id, MemberRequest request) {
        Member member = getMember(id);
        if (memberRepository.existsByPhoneAndIdNot(request.getPhone(), id)) {
            throw new BusinessException("手机号已被其他会员使用: " + request.getPhone());
        }
        member.setName(request.getName().trim());
        member.setPhone(request.getPhone());
        member.setGender(request.getGender() == null ? 0 : request.getGender());
        member.setBirthday(request.getBirthday());
        return MemberVO.from(memberRepository.save(member), levelName(member.getLevelId()));
    }

    @Transactional
    public void updateStatus(Long id, Integer status) {
        Member member = getMember(id);
        member.setStatus(status == null || status == 1 ? 1 : 0);
        memberRepository.save(member);
    }

    // ==================== 等级 ====================

    @Transactional(readOnly = true)
    public List<MemberLevel> levels() {
        return levelRepository.findAllByOrderBySortOrderAsc();
    }

    // ==================== 积分 ====================

    @Transactional(readOnly = true)
    public List<MemberPointsRecord> pointsRecords(Long memberId) {
        return pointsRecordRepository.findTop50ByMemberIdOrderByIdDesc(memberId);
    }

    @Transactional(readOnly = true)
    public List<MemberConsumption> consumptionRecords(Long memberId) {
        return consumptionRepository.findTop50ByMemberIdOrderByIdDesc(memberId);
    }

    /** 手工调整积分 */
    @Transactional
    public void adjustPoints(Long memberId, PointsAdjustRequest request) {
        Member member = getMember(memberId);
        changePoints(member, request.getChange(), request.getChange() > 0
                ? MemberPointsRecord.TYPE_EARN : MemberPointsRecord.TYPE_DEDUCT,
                "MANUAL", null, request.getReason());
    }

    /** 会员储值充值 */
    @Transactional
    public Member recharge(Long memberId, java.math.BigDecimal amount, String remark) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException("充值金额必须大于0");
        }
        Member member = getMember(memberId);
        member.setBalance(member.getBalance().add(amount));
        return memberRepository.save(member);
    }

    // ==================== 销售结算联动（供销售模块调用） ====================

    /**
     * 会员消费：累计消费金额 + 积分 + 等级重算（在销售事务内调用）
     */
    @Transactional
    public void recordConsumption(Long memberId, Long salesOrderId, String orderNo,
                                  BigDecimal amount, int pointsEarned) {
        Member member = getMember(memberId);
        member.setTotalSpent(member.getTotalSpent().add(amount));
        if (pointsEarned != 0) {
            changePoints(member, pointsEarned, MemberPointsRecord.TYPE_EARN,
                    "SALES_ORDER", orderNo, "消费积分");
        }
        // 等级重算：累计消费达到的最高阈值等级
        levelRepository.findAllByOrderBySortOrderAsc().stream()
                .filter(l -> member.getTotalSpent().compareTo(l.getPointsThreshold()) >= 0)
                .max(Comparator.comparing(MemberLevel::getPointsThreshold))
                .ifPresent(l -> member.setLevelId(l.getId()));
        memberRepository.save(member);

        MemberConsumption consumption = new MemberConsumption();
        consumption.setMemberId(memberId);
        consumption.setSalesOrderId(salesOrderId);
        consumption.setOrderNo(orderNo);
        consumption.setAmount(amount);
        consumption.setPointsEarned(pointsEarned);
        consumptionRepository.save(consumption);
    }

    // ------------------------------------------------------------------

    private void changePoints(Member member, int change, String type, String refType, String refNo, String remark) {
        int after = member.getPoints() + change;
        if (after < 0) {
            throw new BusinessException("积分不足：当前 " + member.getPoints() + "，需要扣减 " + (-change));
        }
        member.setPoints(after);
        memberRepository.save(member);

        MemberPointsRecord record = new MemberPointsRecord();
        record.setMemberId(member.getId());
        record.setChange(change);
        record.setType(type);
        record.setRefType(refType);
        record.setRefNo(refNo);
        record.setRemark(remark);
        pointsRecordRepository.save(record);
    }

    private Member getMember(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "会员不存在: id=" + id));
    }

    private String levelName(Long levelId) {
        return levelRepository.findById(levelId).map(MemberLevel::getName).orElse(null);
    }
}
