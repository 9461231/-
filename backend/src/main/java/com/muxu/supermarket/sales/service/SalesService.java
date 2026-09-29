package com.muxu.supermarket.sales.service;

import com.muxu.supermarket.common.BusinessException;
import com.muxu.supermarket.common.PageVO;
import com.muxu.supermarket.inventory.entity.InventoryTransaction;
import com.muxu.supermarket.inventory.service.InventoryService;
import com.muxu.supermarket.member.entity.Member;
import com.muxu.supermarket.member.repository.MemberRepository;
import com.muxu.supermarket.member.service.MemberService;
import com.muxu.supermarket.product.entity.Product;
import com.muxu.supermarket.product.repository.ProductRepository;
import com.muxu.supermarket.promotion.service.PromotionService;
import com.muxu.supermarket.sales.dto.*;
import com.muxu.supermarket.sales.entity.*;
import com.muxu.supermarket.sales.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 销售闭环：商品选择 → 购物车 → 识别会员(可选) → 计算促销优惠 → 结算
 *           → 生成销售订单 → 扣减库存 → 会员积分
 */
@Service
@RequiredArgsConstructor
public class SalesService {

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Set<String> PAY_METHODS = Set.of("CASH", "WECHAT", "ALIPAY", "CARD", "BALANCE");

    private final SalesOrderRepository orderRepository;
    private final SalesOrderItemRepository orderItemRepository;
    private final SalesReturnRepository returnRepository;
    private final SalesReturnItemRepository returnItemRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final InventoryService inventoryService;
    private final PromotionService promotionService;
    private final CashShiftRepository shiftRepository;
    private final CouponRepository couponRepository;
    private final MemberCouponRepository memberCouponRepository;

    // ==================== 结算 ====================

    @Transactional
    public SalesOrderVO checkout(SalesOrderRequest request) {
        if (!PAY_METHODS.contains(request.getPayMethod())) {
            throw new BusinessException("不支持的支付方式: " + request.getPayMethod());
        }
        Member member = null;
        if (request.getMemberId() != null) {
            member = memberRepository.findById(request.getMemberId())
                    .orElseThrow(() -> new BusinessException("会员不存在: id=" + request.getMemberId()));
            if (member.getStatus() != 1) {
                throw new BusinessException("会员「" + member.getName() + "」已停用");
            }
        }

        // 加载商品并校验
        List<Long> productIds = request.getItems().stream().map(SalesOrderRequest.Item::getProductId).distinct().toList();
        Map<Long, Product> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        for (SalesOrderRequest.Item item : request.getItems()) {
            Product product = productMap.get(item.getProductId());
            if (product == null) {
                throw new BusinessException("商品不存在: id=" + item.getProductId());
            }
            if (product.getStatus() != 1) {
                throw new BusinessException("商品「" + product.getName() + "」已停售");
            }
        }

        // 促销计算（传统程序保证确定性）
        Map<Long, PromotionService.CartLine> lines = new LinkedHashMap<>();
        for (SalesOrderRequest.Item item : request.getItems()) {
            lines.merge(item.getProductId(),
                    new PromotionService.CartLine(item.getQuantity(), productMap.get(item.getProductId()).getSalePrice()),
                    (a, b) -> new PromotionService.CartLine(a.quantity() + b.quantity(), a.unitPrice()));
        }
        List<PromotionService.SettlementItem> settlements =
                promotionService.calculate(lines, member != null);

        // 生成订单
        SalesOrder order = new SalesOrder();
        order.setOrderNo("SO" + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", (int) (Math.random() * 1000)));
        order.setMemberId(member == null ? null : member.getId());
        order.setPayMethod(request.getPayMethod());
        order.setRemark(request.getRemark());

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        List<SalesOrderItem> orderItems = new ArrayList<>();
        for (PromotionService.SettlementItem s : settlements) {
            Product product = productMap.get(s.productId());
            total = total.add(s.unitPrice().multiply(BigDecimal.valueOf(s.quantity())));
            discount = discount.add(s.discountAmount());

            SalesOrderItem orderItem = new SalesOrderItem();
            orderItem.setProductId(product.getId());
            orderItem.setQuantity(s.quantity());
            orderItem.setUnitPrice(s.unitPrice());
            orderItem.setDiscountAmount(s.discountAmount());
            orderItem.setAmount(s.payableAmount());
            orderItem.setPromotionId(s.promotionId());
            orderItems.add(orderItem);
        }
        BigDecimal payable = total.subtract(discount).setScale(2, RoundingMode.HALF_UP);

        // 优惠券抵扣（促销之后计算；券只能在促销优惠基础上再抵扣）
        BigDecimal couponAmount = BigDecimal.ZERO;
        MemberCoupon usedCoupon = null;
        if (StringUtils.hasText(request.getCouponCode())) {
            MemberCoupon memberCoupon = memberCouponRepository.findByCode(request.getCouponCode().trim())
                    .orElseThrow(() -> new BusinessException("优惠券不存在: " + request.getCouponCode()));
            if (memberCoupon.getStatus() != MemberCoupon.STATUS_UNUSED) {
                throw new BusinessException("优惠券已被使用");
            }
            if (member == null || !memberCoupon.getMemberId().equals(member.getId())) {
                throw new BusinessException("优惠券不属于当前会员，请先识别会员");
            }
            Coupon coupon = couponRepository.findById(memberCoupon.getCouponId())
                    .orElseThrow(() -> new BusinessException("优惠券活动不存在"));
            if (coupon.getStatus() != 1 || coupon.getValidUntil().isBefore(LocalDateTime.now())) {
                throw new BusinessException("优惠券已过期或停用");
            }
            if (total.compareTo(coupon.getMinAmount()) < 0) {
                throw new BusinessException(String.format("未达到优惠券使用门槛（满 ¥%s 可用）", coupon.getMinAmount()));
            }
            couponAmount = coupon.getReduceAmount().min(payable);
            payable = payable.subtract(couponAmount).setScale(2, RoundingMode.HALF_UP);
            memberCoupon.setStatus(MemberCoupon.STATUS_USED);
            memberCoupon.setUsedAt(LocalDateTime.now());
            usedCoupon = memberCoupon;
        }

        // 储值支付
        if ("BALANCE".equals(request.getPayMethod())) {
            if (member == null) {
                throw new BusinessException("储值支付必须先识别会员");
            }
            if (member.getBalance().compareTo(payable) < 0) {
                throw new BusinessException(String.format("储值余额不足（余额 ¥%s，应付 ¥%s）",
                        member.getBalance(), payable));
            }
            member.setBalance(member.getBalance().subtract(payable));
            memberRepository.save(member);
        }

        // 关联当前进行中的交班
        Long currentShiftId = shiftRepository.findFirstByStatusOrderByStartTimeDesc(CashShift.STATUS_OPEN)
                .map(CashShift::getId).orElse(null);
        order.setShiftId(currentShiftId);

        order.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));
        order.setDiscountAmount(discount.setScale(2, RoundingMode.HALF_UP));
        order.setCouponAmount(couponAmount.setScale(2, RoundingMode.HALF_UP));
        order.setPayableAmount(payable);
        // 积分规则：实付金额每 1 元 = 1 积分
        int pointsEarned = payable.intValue();
        order.setPointsEarned(pointsEarned);
        order = orderRepository.save(order);

        // 优惠券核销落库
        if (usedCoupon != null) {
            usedCoupon.setUsedOrderId(order.getId());
            memberCouponRepository.save(usedCoupon);
        }

        for (SalesOrderItem orderItem : orderItems) {
            orderItem.setOrderId(order.getId());
            // 库存扣减（同一事务，库存不足整体回滚）
            inventoryService.decrease(orderItem.getProductId(), orderItem.getQuantity(),
                    InventoryTransaction.TYPE_SALE_OUT, "SALES_ORDER", order.getOrderNo(), "销售出库");
        }
        orderItemRepository.saveAll(orderItems);

        // 会员消费/积分/等级联动（同一事务）
        if (member != null) {
            memberService.recordConsumption(member.getId(), order.getId(), order.getOrderNo(),
                    payable, pointsEarned);
        }
        return SalesOrderVO.from(order, member == null ? null : member.getName());
    }

    // ==================== 查询 ====================

    @Transactional(readOnly = true)
    public PageVO<SalesOrderVO> page(int page, int size, String keyword, Long memberId, Integer status) {
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 10;

        Specification<SalesOrder> spec = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (StringUtils.hasText(keyword)) {
                predicates.add(cb.like(root.get("orderNo"), "%" + keyword.trim() + "%"));
            }
            if (memberId != null) {
                predicates.add(cb.equal(root.get("memberId"), memberId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        Page<SalesOrder> result = orderRepository.findAll(spec,
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id")));

        Map<Long, String> memberNames = memberRepository.findAll().stream()
                .collect(Collectors.toMap(Member::getId, Member::getName, (a, b) -> a));
        List<SalesOrderVO> list = result.getContent().stream()
                .map(o -> SalesOrderVO.from(o, o.getMemberId() == null ? null : memberNames.get(o.getMemberId())))
                .toList();
        return new PageVO<>(list, result.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(Long id) {
        SalesOrder order = getOrder(id);
        SalesOrderVO orderVO = SalesOrderVO.from(order,
                order.getMemberId() == null ? null
                        : memberRepository.findById(order.getMemberId()).map(Member::getName).orElse(null));
        Map<Long, Product> productMap = loadProductMap();
        List<SalesOrderItemVO> items = orderItemRepository.findByOrderId(id).stream()
                .map(item -> {
                    Product p = productMap.get(item.getProductId());
                    return SalesOrderItemVO.from(item,
                            p == null ? null : p.getName(),
                            p == null ? null : p.getSku(),
                            p == null ? null : p.getUnit());
                }).toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("order", orderVO);
        result.put("items", items);
        return result;
    }

    // ==================== 销售退货 ====================

    @Transactional
    public Map<String, Object> createReturn(SalesReturnRequest request) {
        SalesOrder order = getOrder(request.getOrderId());

        List<SalesOrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());
        Map<Long, SalesOrderItem> itemMap = orderItems.stream()
                .collect(Collectors.toMap(SalesOrderItem::getId, Function.identity()));

        SalesReturn ret = new SalesReturn();
        String returnNo = "SR" + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", (int) (Math.random() * 1000));
        ret.setReturnNo(returnNo);
        ret.setOrderId(order.getId());
        ret.setReason(request.getReason());

        BigDecimal totalRefund = BigDecimal.ZERO;
        List<SalesReturnItem> returnItems = new ArrayList<>();
        for (SalesReturnRequest.Item item : request.getItems()) {
            SalesOrderItem orderItem = itemMap.get(item.getOrderItemId());
            if (orderItem == null) {
                throw new BusinessException("订单明细不存在: id=" + item.getOrderItemId());
            }
            int returnable = orderItem.getQuantity() - orderItem.getReturnedQuantity();
            if (item.getQuantity() > returnable) {
                throw new BusinessException(String.format("退货数量超过可退数量（剩余 %d）", returnable));
            }
            // 退款金额 = 实收单价 × 退货数量（实收单价 = 实收金额/数量）
            BigDecimal unitPaid = orderItem.getAmount()
                    .divide(BigDecimal.valueOf(orderItem.getQuantity()), 4, RoundingMode.HALF_UP)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal refund = unitPaid.multiply(BigDecimal.valueOf(item.getQuantity()));
            totalRefund = totalRefund.add(refund);

            orderItem.setReturnedQuantity(orderItem.getReturnedQuantity() + item.getQuantity());

            SalesReturnItem ri = new SalesReturnItem();
            ri.setOrderItemId(orderItem.getId());
            ri.setProductId(orderItem.getProductId());
            ri.setQuantity(item.getQuantity());
            ri.setRefundAmount(refund);
            returnItems.add(ri);

            // 库存回增（同一事务）
            inventoryService.increase(orderItem.getProductId(), item.getQuantity(),
                    InventoryTransaction.TYPE_SALE_RETURN_IN, "SALES_RETURN", returnNo, "销售退货入库");
        }
        orderItemRepository.saveAll(orderItems);

        ret.setTotalAmount(totalRefund.setScale(2, RoundingMode.HALF_UP));
        SalesReturn saved = returnRepository.save(ret);
        returnItems.forEach(ri -> ri.setReturnId(saved.getId()));
        returnItemRepository.saveAll(returnItems);

        // 更新订单状态
        boolean allReturned = orderItemRepository.findByOrderId(order.getId()).stream()
                .allMatch(i -> i.getReturnedQuantity() >= i.getQuantity());
        order.setStatus(allReturned ? SalesOrder.STATUS_RETURNED : SalesOrder.STATUS_PARTIAL_RETURNED);
        orderRepository.save(order);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("returnNo", returnNo);
        result.put("refundAmount", totalRefund.setScale(2, RoundingMode.HALF_UP));
        return result;
    }

    // ------------------------------------------------------------------

    private SalesOrder getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "销售订单不存在: id=" + id));
    }

    private Map<Long, Product> loadProductMap() {
        return productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, Function.identity(), (a, b) -> a));
    }
}
