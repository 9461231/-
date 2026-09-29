package com.muxu.supermarket.promotion.service;

import com.muxu.supermarket.common.BusinessException;
import com.muxu.supermarket.common.PageVO;
import com.muxu.supermarket.promotion.dto.PromotionRequest;
import com.muxu.supermarket.promotion.dto.PromotionVO;
import com.muxu.supermarket.promotion.entity.Promotion;
import com.muxu.supermarket.promotion.entity.PromotionProduct;
import com.muxu.supermarket.promotion.repository.PromotionProductRepository;
import com.muxu.supermarket.promotion.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final PromotionProductRepository promotionProductRepository;

    // ==================== 活动管理 ====================

    @Transactional(readOnly = true)
    public PageVO<PromotionVO> page(int page, int size, Integer type, Integer status) {
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 10;

        Page<Promotion> result = promotionRepository.findAll(
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id")));
        Map<Long, List<Long>> productIdsMap = promotionProductRepository.findAll().stream()
                .collect(Collectors.groupingBy(PromotionProduct::getPromotionId,
                        Collectors.mapping(PromotionProduct::getProductId, Collectors.toList())));

        List<PromotionVO> list = result.getContent().stream()
                .map(p -> PromotionVO.from(p, productIdsMap.getOrDefault(p.getId(), List.of())))
                .toList();
        return new PageVO<>(list, result.getTotalElements(), page, size);
    }

    @Transactional
    public PromotionVO create(PromotionRequest request) {
        validate(request);
        Promotion promotion = new Promotion();
        applyRequest(promotion, request);
        promotion = promotionRepository.save(promotion);
        saveProducts(promotion.getId(), request.getProductIds());
        return PromotionVO.from(promotion, request.getProductIds());
    }

    @Transactional
    public PromotionVO update(Long id, PromotionRequest request) {
        Promotion promotion = getPromotion(id);
        validate(request);
        applyRequest(promotion, request);
        promotion = promotionRepository.save(promotion);
        promotionProductRepository.deleteAll(promotionProductRepository.findByPromotionId(id));
        saveProducts(id, request.getProductIds());
        return PromotionVO.from(promotion, request.getProductIds());
    }

    @Transactional
    public void delete(Long id) {
        getPromotion(id);
        promotionRepository.deleteById(id);
        promotionProductRepository.deleteAll(promotionProductRepository.findByPromotionId(id));
    }

    @Transactional
    public void updateStatus(Long id, Integer status) {
        Promotion promotion = getPromotion(id);
        promotion.setStatus(status != null && status == 1 ? Promotion.STATUS_ENABLED : Promotion.STATUS_DISABLED);
        promotionRepository.save(promotion);
    }

    // ==================== 结算计算（销售模块调用） ====================

    /**
     * 结算明细项
     */
    public record SettlementItem(Long productId, Integer quantity, BigDecimal unitPrice,
                                 Long promotionId, BigDecimal discountAmount, BigDecimal payableAmount) {
    }

    /**
     * 购物车行：数量 + 单价
     */
    public record CartLine(Integer quantity, BigDecimal unitPrice) {
    }

    /**
     * 对购物车中每个商品行计算促销优惠（传统程序保证确定性）。
     * 每个商品在同一时刻取"优惠金额最大"的生效活动。
     *
     * @param items   productId -> 购物车行
     * @param isMember 是否会员结算（会员专享价只对会员生效）
     */
    @Transactional(readOnly = true)
    public List<SettlementItem> calculate(Map<Long, CartLine> items, boolean isMember) {
        LocalDateTime now = LocalDateTime.now();

        // 查询当前时间生效的启用活动
        List<Promotion> activePromotions = promotionRepository.findAll().stream()
                .filter(p -> p.getStatus() == Promotion.STATUS_ENABLED)
                .filter(p -> !now.isBefore(p.getStartTime()) && !now.isAfter(p.getEndTime()))
                .toList();

        Map<Long, List<Promotion>> promotionsByProduct = promotionProductRepository
                .findByProductIdIn(items.keySet().stream().toList()).stream()
                .collect(Collectors.groupingBy(PromotionProduct::getProductId,
                        Collectors.mapping(pp -> pp.getPromotionId(), Collectors.toList())))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .map(pid -> activePromotions.stream()
                                        .filter(p -> p.getId().equals(pid)).findFirst().orElse(null))
                                .filter(p -> p != null)
                                .toList()));

        return items.entrySet().stream().map(e -> {
            Long productId = e.getKey();
            int quantity = e.getValue().quantity();
            BigDecimal unitPrice = e.getValue().unitPrice();
            BigDecimal origin = unitPrice.multiply(BigDecimal.valueOf(quantity));

            Promotion best = null;
            BigDecimal bestDiscount = BigDecimal.ZERO;
            for (Promotion p : promotionsByProduct.getOrDefault(productId, List.of())) {
                BigDecimal discount = calcDiscount(p, quantity, unitPrice, origin, isMember);
                if (discount.compareTo(bestDiscount) > 0) {
                    bestDiscount = discount;
                    best = p;
                }
            }
            return new SettlementItem(productId, quantity, unitPrice,
                    best == null ? null : best.getId(),
                    bestDiscount.setScale(2, RoundingMode.HALF_UP),
                    origin.subtract(bestDiscount).setScale(2, RoundingMode.HALF_UP));
        }).toList();
    }

    private BigDecimal calcDiscount(Promotion p, int quantity, BigDecimal unitPrice, BigDecimal origin, boolean isMember) {
        return switch (p.getType()) {
            case Promotion.TYPE_DISCOUNT -> {
                if (p.getDiscountRate() == null) yield BigDecimal.ZERO;
                yield origin.multiply(BigDecimal.ONE.subtract(p.getDiscountRate()))
                        .setScale(2, RoundingMode.HALF_UP);
            }
            case Promotion.TYPE_FULL_REDUCTION -> {
                if (p.getMinAmount() == null || p.getReduceAmount() == null
                        || origin.compareTo(p.getMinAmount()) < 0) {
                    yield BigDecimal.ZERO;
                }
                yield p.getReduceAmount().min(origin);
            }
            case Promotion.TYPE_SECOND_HALF -> {
                if (quantity < 2 || p.getSecondRate() == null) yield BigDecimal.ZERO;
                int pairs = quantity / 2;
                yield unitPrice.multiply(p.getSecondRate())
                        .multiply(BigDecimal.valueOf(pairs))
                        .setScale(2, RoundingMode.HALF_UP);
            }
            case Promotion.TYPE_MEMBER_PRICE -> {
                if (!isMember || p.getMemberPrice() == null || p.getMemberPrice().compareTo(unitPrice) >= 0) {
                    yield BigDecimal.ZERO;
                }
                yield unitPrice.subtract(p.getMemberPrice())
                        .multiply(BigDecimal.valueOf(quantity))
                        .setScale(2, RoundingMode.HALF_UP);
            }
            default -> BigDecimal.ZERO;
        };
    }

    // ------------------------------------------------------------------

    private void validate(PromotionRequest request) {
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new BusinessException("活动时间不能为空");
        }
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }
        switch (request.getType()) {
            case Promotion.TYPE_DISCOUNT -> {
                if (request.getDiscountRate() == null) {
                    throw new BusinessException("直接折扣活动必须设置折扣率");
                }
            }
            case Promotion.TYPE_FULL_REDUCTION -> {
                if (request.getMinAmount() == null || request.getReduceAmount() == null) {
                    throw new BusinessException("满减活动必须设置门槛金额与减免金额");
                }
            }
            case Promotion.TYPE_SECOND_HALF -> {
                if (request.getSecondRate() == null) {
                    throw new BusinessException("第二件优惠必须设置第二件折扣率");
                }
            }
            case Promotion.TYPE_MEMBER_PRICE -> {
                if (request.getMemberPrice() == null) {
                    throw new BusinessException("会员专享价活动必须设置会员价");
                }
            }
            default -> throw new BusinessException("促销类型不正确");
        }
    }

    private void applyRequest(Promotion promotion, PromotionRequest request) {
        promotion.setName(request.getName().trim());
        promotion.setType(request.getType());
        promotion.setStartTime(request.getStartTime());
        promotion.setEndTime(request.getEndTime());
        promotion.setDiscountRate(request.getDiscountRate());
        promotion.setMinAmount(request.getMinAmount());
        promotion.setReduceAmount(request.getReduceAmount());
        promotion.setSecondRate(request.getSecondRate());
        promotion.setMemberPrice(request.getMemberPrice());
        promotion.setRemark(request.getRemark());
    }

    private void saveProducts(Long promotionId, List<Long> productIds) {
        List<PromotionProduct> list = productIds.stream().distinct().map(pid -> {
            PromotionProduct pp = new PromotionProduct();
            pp.setPromotionId(promotionId);
            pp.setProductId(pid);
            return pp;
        }).toList();
        promotionProductRepository.saveAll(list);
    }

    private Promotion getPromotion(Long id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "促销活动不存在: id=" + id));
    }
}
