package com.muxu.supermarket.sales.service;

import com.muxu.supermarket.common.BusinessException;
import com.muxu.supermarket.common.CurrentUser;
import com.muxu.supermarket.sales.entity.CashShift;
import com.muxu.supermarket.sales.entity.SalesOrder;
import com.muxu.supermarket.sales.repository.CashShiftRepository;
import com.muxu.supermarket.sales.repository.SalesOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 收银交班与营业日结
 */
@Service
@RequiredArgsConstructor
public class ShiftService {

    private final CashShiftRepository shiftRepository;
    private final SalesOrderRepository orderRepository;

    /** 开班 */
    @Transactional
    public CashShift open(String remark) {
        shiftRepository.findFirstByStatusOrderByStartTimeDesc(CashShift.STATUS_OPEN)
                .ifPresent(s -> {
                    throw new BusinessException("当前已有进行中的班次，请先交班");
                });
        CurrentUser.UserInfo user = CurrentUser.get();
        if (user == null) {
            throw new BusinessException("未登录");
        }
        CashShift shift = new CashShift();
        shift.setCashierId(user.userId());
        shift.setCashierName(user.username());
        shift.setStartTime(LocalDateTime.now());
        shift.setRemark(remark);
        return shiftRepository.save(shift);
    }

    @Transactional(readOnly = true)
    public Optional<CashShift> current() {
        return shiftRepository.findFirstByStatusOrderByStartTimeDesc(CashShift.STATUS_OPEN);
    }

    @Transactional(readOnly = true)
    public List<CashShift> history() {
        return shiftRepository.findTop50ByOrderByIdDesc();
    }

    /** 交班：汇总本班次销售数据 */
    @Transactional
    public CashShift close(Long shiftId, String remark) {
        CashShift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new BusinessException(404, "班次不存在"));
        if (shift.getStatus() != CashShift.STATUS_OPEN) {
            throw new BusinessException("该班次已交班");
        }
        LocalDateTime end = LocalDateTime.now();
        shift.setEndTime(end);
        shift.setRemark(remark != null ? remark : shift.getRemark());

        List<SalesOrder> orders = orderRepository.findAll().stream()
                .filter(o -> shiftId.equals(o.getShiftId()))
                .filter(o -> o.getStatus() != SalesOrder.STATUS_RETURNED)
                .toList();
        shift.setOrderCount(orders.size());
        for (SalesOrder o : orders) {
            shift.setTotalAmount(shift.getTotalAmount().add(o.getPayableAmount()));
            shift.setDiscountAmount(shift.getDiscountAmount()
                    .add(o.getDiscountAmount()).add(o.getCouponAmount()));
            switch (o.getPayMethod()) {
                case "CASH" -> shift.setCashAmount(shift.getCashAmount().add(o.getPayableAmount()));
                case "WECHAT" -> shift.setWechatAmount(shift.getWechatAmount().add(o.getPayableAmount()));
                case "ALIPAY" -> shift.setAlipayAmount(shift.getAlipayAmount().add(o.getPayableAmount()));
                case "CARD" -> shift.setCardAmount(shift.getCardAmount().add(o.getPayableAmount()));
                case "BALANCE" -> shift.setBalanceAmount(shift.getBalanceAmount().add(o.getPayableAmount()));
                default -> { }
            }
        }
        shift.setStatus(CashShift.STATUS_CLOSED);
        return shiftRepository.save(shift);
    }

    /** 营业日结单：按日期汇总各支付方式 */
    @Transactional(readOnly = true)
    public Map<String, Object> dailySettlement(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        List<SalesOrder> orders = orderRepository.findAll().stream()
                .filter(o -> !o.getCreatedAt().isBefore(start) && o.getCreatedAt().isBefore(end))
                .filter(o -> o.getStatus() != SalesOrder.STATUS_RETURNED)
                .toList();

        Map<String, BigDecimal> byMethod = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        for (SalesOrder o : orders) {
            total = total.add(o.getPayableAmount());
            discount = discount.add(o.getDiscountAmount()).add(o.getCouponAmount());
            byMethod.merge(o.getPayMethod(), o.getPayableAmount(), BigDecimal::add);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", date.toString());
        result.put("orderCount", orders.size());
        result.put("totalAmount", total);
        result.put("discountAmount", discount);
        result.put("byMethod", byMethod);
        return result;
    }
}
