package com.muxu.supermarket.analytics.service;

import com.muxu.supermarket.inventory.repository.InventoryRepository;
import com.muxu.supermarket.inventory.service.InventoryService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 经营分析：汇总销售/采购/库存/会员/损耗数据
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final EntityManager em;

    private final InventoryService inventoryService;
    private final InventoryRepository inventoryRepository;
    private final com.muxu.supermarket.inventory.repository.ProductBatchRepository batchRepository;

    /**
     * 看板 KPI
     */
    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("todaySales", sumSales(todayStart, null));
        result.put("todayOrders", countOrders(todayStart, null));
        result.put("monthSales", sumSales(monthStart, null));
        result.put("monthOrders", countOrders(monthStart, null));
        result.put("monthPurchase", sumPurchase(monthStart));
        result.put("monthProfit", profit(monthStart, LocalDate.now().plusDays(1).atStartOfDay()));
        result.put("inventoryValue", inventoryRepository.sumInventoryValue());
        result.put("alertCount", inventoryService.alerts().size());
        result.put("memberCount", countMembers());
        result.put("memberSalesRatio", memberSalesRatio(monthStart));
        result.put("monthLoss", sumLoss(monthStart));
        // 客单价与毛利率
        long monthOrders = countOrders(monthStart, null);
        BigDecimal monthSales = sumSales(monthStart, null);
        result.put("monthAvgTicket", monthOrders == 0 ? 0
                : monthSales.divide(BigDecimal.valueOf(monthOrders), 2, RoundingMode.HALF_UP));
        BigDecimal monthProfit = profit(monthStart, LocalDate.now().plusDays(1).atStartOfDay());
        result.put("monthMarginRate", monthSales.signum() == 0 ? 0
                : monthProfit.multiply(BigDecimal.valueOf(100))
                        .divide(monthSales, 1, RoundingMode.HALF_UP).doubleValue());
        return result;
    }

    /** 销售趋势：最近 N 天每日销售额与订单数 */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> salesTrend(int days) {
        LocalDateTime start = LocalDate.now().minusDays(days - 1L).atStartOfDay();
        List<Object[]> rows = em.createNativeQuery("""
                SELECT DATE(created_at) AS d, COALESCE(SUM(payable_amount),0) AS amount, COUNT(*) AS orders
                FROM sales_order WHERE created_at >= :start AND status != 3
                GROUP BY DATE(created_at) ORDER BY d
                """).setParameter("start", start).getResultList();

        Map<LocalDate, Object[]> byDate = rows.stream().collect(Collectors.toMap(
                r -> ((java.sql.Date) r[0]).toLocalDate(), r -> r, (a, b) -> b));

        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            Object[] r = byDate.get(date);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", date.toString());
            item.put("amount", r == null ? 0 : ((Number) r[1]).doubleValue());
            item.put("orders", r == null ? 0 : ((Number) r[2]).longValue());
            trend.add(item);
        }
        return trend;
    }

    /** 商品销量排行（热销） */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> topProducts(int days, int limit) {
        LocalDateTime start = LocalDate.now().minusDays(days - 1L).atStartOfDay();
        List<Object[]> rows = em.createNativeQuery("""
                SELECT p.id, p.name, p.sku, SUM(i.quantity) AS qty, SUM(i.amount) AS amount
                FROM sales_order_item i
                JOIN sales_order o ON i.order_id = o.id
                JOIN product p ON i.product_id = p.id
                WHERE o.created_at >= :start AND o.status != 3
                GROUP BY p.id, p.name, p.sku ORDER BY qty DESC LIMIT :limit
                """).setParameter("start", start).setParameter("limit", limit).getResultList();
        return rows.stream().map(this::toRankRow).collect(Collectors.toList());
    }

    /** 滞销商品：近 N 天销量低且有库存 */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> slowProducts(int days, int limit) {
        LocalDateTime start = LocalDate.now().minusDays(days - 1L).atStartOfDay();
        List<Object[]> rows = em.createNativeQuery("""
                SELECT p.id, p.name, p.sku,
                       COALESCE((SELECT SUM(i.quantity) FROM sales_order_item i
                                 JOIN sales_order o ON i.order_id = o.id
                                 WHERE i.product_id = p.id AND o.created_at >= :start AND o.status != 3), 0) AS qty,
                       COALESCE(inv.quantity, 0) AS stock
                FROM product p
                LEFT JOIN inventory inv ON inv.product_id = p.id
                WHERE p.status = 1
                ORDER BY qty ASC, stock DESC LIMIT :limit
                """).setParameter("start", start).setParameter("limit", limit).getResultList();
        return rows.stream().map(r -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("productId", ((Number) r[0]).longValue());
            item.put("productName", (String) r[1]);
            item.put("sku", (String) r[2]);
            item.put("quantity", ((Number) r[3]).longValue());
            item.put("stock", ((Number) r[4]).longValue());
            long qty = ((Number) r[3]).longValue();
            long stock = ((Number) r[4]).longValue();
            item.put("coverDays", qty == 0 ? null : Math.round(stock * (double) days / qty));
            return item;
        }).collect(Collectors.toList());
    }

    /** 分类销售占比 */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> categorySales(int days) {
        LocalDateTime start = LocalDate.now().minusDays(days - 1L).atStartOfDay();
        List<Object[]> rows = em.createNativeQuery("""
                SELECT c.name, COALESCE(SUM(i.amount),0) AS amount
                FROM sales_order_item i
                JOIN sales_order o ON i.order_id = o.id
                JOIN product p ON i.product_id = p.id
                JOIN product_category c ON p.category_id = c.id
                WHERE o.created_at >= :start AND o.status != 3
                GROUP BY c.name ORDER BY amount DESC
                """).setParameter("start", start).getResultList();
        return rows.stream().map(r -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("category", (String) r[0]);
            item.put("amount", ((Number) r[1]).doubleValue());
            return item;
        }).collect(Collectors.toList());
    }

    /** 近 N 月损耗统计 */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> lossStats(int months) {
        LocalDateTime start = LocalDate.now().minusMonths(months - 1L).withDayOfMonth(1).atStartOfDay();
        List<Object[]> rows = em.createNativeQuery("""
                SELECT DATE_FORMAT(created_at, '%Y-%m') AS m, COALESCE(SUM(loss_amount),0) AS amount
                FROM loss_record WHERE created_at >= :start
                GROUP BY DATE_FORMAT(created_at, '%Y-%m') ORDER BY m
                """).setParameter("start", start).getResultList();
        return rows.stream().map(r -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("month", (String) r[0]);
            item.put("amount", ((Number) r[1]).doubleValue());
            return item;
        }).collect(Collectors.toList());
    }

    /** 促销效果分析：按活动汇总销售额与优惠成本 */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> promotionAnalysis(int days) {
        LocalDateTime start = LocalDate.now().minusDays(days - 1L).atStartOfDay();
        List<Object[]> rows = em.createNativeQuery("""
                SELECT COALESCE(pr.name, '未匹配活动'), COUNT(DISTINCT o.id),
                       COALESCE(SUM(i.amount),0), COALESCE(SUM(i.discount_amount),0)
                FROM sales_order_item i
                JOIN sales_order o ON i.order_id = o.id
                LEFT JOIN promotion pr ON i.promotion_id = pr.id
                WHERE o.created_at >= :start AND o.status != 3
                GROUP BY pr.name ORDER BY 4 DESC
                """).setParameter("start", start).getResultList();
        return rows.stream().map(r -> {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("promotion", (String) r[0]);
            item.put("orderCount", ((Number) r[1]).longValue());
            item.put("amount", ((Number) r[2]).doubleValue());
            item.put("discountCost", ((Number) r[3]).doubleValue());
            return item;
        }).collect(Collectors.toList());
    }

    /** 损耗原因排行 */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> lossReasonRanking(int days) {
        LocalDateTime start = LocalDate.now().minusDays(days - 1L).atStartOfDay();
        List<Object[]> rows = em.createNativeQuery("""
                SELECT reason, COUNT(*) AS cnt, COALESCE(SUM(loss_amount),0) AS amount
                FROM loss_record WHERE created_at >= :start
                GROUP BY reason ORDER BY amount DESC LIMIT 10
                """).setParameter("start", start).getResultList();
        return rows.stream().map(r -> {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("reason", (String) r[0]);
            item.put("count", ((Number) r[1]).longValue());
            item.put("amount", ((Number) r[2]).doubleValue());
            return item;
        }).collect(Collectors.toList());
    }

    /** 会员分析：数量/占比/客单价/复购率 */
    @Transactional(readOnly = true)
    public Map<String, Object> memberAnalysis(int days) {
        LocalDateTime start = LocalDate.now().minusDays(days - 1L).atStartOfDay();
        long memberOrders = ((Number) em.createNativeQuery("""
                SELECT COUNT(*) FROM sales_order
                WHERE created_at >= :start AND member_id IS NOT NULL AND status != 3
                """).setParameter("start", start).getSingleResult()).longValue();
        long totalOrders = countOrders(start, null);
        BigDecimal memberAmount = (BigDecimal) em.createNativeQuery("""
                SELECT COALESCE(SUM(payable_amount),0) FROM sales_order
                WHERE created_at >= :start AND member_id IS NOT NULL AND status != 3
                """).setParameter("start", start).getSingleResult();
        long repeatMembers = ((Number) em.createNativeQuery("""
                SELECT COUNT(*) FROM (
                    SELECT member_id FROM sales_order
                    WHERE created_at >= :start AND member_id IS NOT NULL AND status != 3
                    GROUP BY member_id HAVING COUNT(*) >= 2
                ) t
                """).setParameter("start", start).getSingleResult()).longValue();
        long activeMembers = ((Number) em.createNativeQuery("""
                SELECT COUNT(DISTINCT member_id) FROM sales_order
                WHERE created_at >= :start AND member_id IS NOT NULL AND status != 3
                """).setParameter("start", start).getSingleResult()).longValue();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("memberCount", countMembers());
        result.put("memberOrderCount", memberOrders);
        result.put("memberOrderRatio", totalOrders == 0 ? 0
                : Math.round(memberOrders * 1000.0 / totalOrders) / 10.0);
        result.put("memberAvgTicket", memberOrders == 0 ? 0
                : memberAmount.divide(BigDecimal.valueOf(memberOrders), 2, RoundingMode.HALF_UP));
        result.put("activeMembers", activeMembers);
        result.put("repurchaseRate", activeMembers == 0 ? 0
                : Math.round(repeatMembers * 1000.0 / activeMembers) / 10.0);
        return result;
    }

    /** 库存分析：周转天数（按近30天销售成本估算） */
    @Transactional(readOnly = true)
    public Map<String, Object> inventoryAnalysis() {
        LocalDateTime start = LocalDate.now().minusDays(29).atStartOfDay();
        BigDecimal cogs = (BigDecimal) em.createNativeQuery("""
                SELECT COALESCE(SUM(p.purchase_price * i.quantity),0)
                FROM sales_order_item i
                JOIN sales_order o ON i.order_id = o.id
                JOIN product p ON i.product_id = p.id
                WHERE o.created_at >= :start AND o.status != 3
                """).setParameter("start", start).getSingleResult();
        BigDecimal inventoryValue = inventoryRepository.sumInventoryValue();
        double dailyCogs = cogs.doubleValue() / 30.0;
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("inventoryValue", inventoryValue);
        result.put("cogs30d", cogs);
        result.put("turnoverDays", dailyCogs == 0 ? null
                : Math.round(inventoryValue.doubleValue() / dailyCogs));
        return result;
    }

    /** 经营驾驶舱（店主首页） */
    @Transactional(readOnly = true)
    public Map<String, Object> cockpit() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        Map<String, Object> cockpit = new LinkedHashMap<>();
        cockpit.put("todaySales", sumSales(todayStart, null));
        cockpit.put("todayOrders", countOrders(todayStart, null));
        cockpit.put("todayProfit", profit(todayStart, LocalDate.now().plusDays(1).atStartOfDay()));

        List<?> alerts = inventoryService.alerts();
        cockpit.put("inventoryAlerts", alerts.size());

        long expiring = batchRepository.findByExpireDateBeforeAndQuantityGreaterThan(
                LocalDate.now().plusDays(8), 0).size();
        cockpit.put("expiringCount", expiring);

        long pendingReceipt = em.createQuery(
                        "SELECT COUNT(o) FROM com.muxu.supermarket.purchase.entity.PurchaseOrder o " +
                                "WHERE o.status IN (2, 3)", Long.class).getSingleResult();
        cockpit.put("pendingReceiptOrders", pendingReceipt);

        long newMembers = ((Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM member WHERE created_at >= :start")
                .setParameter("start", todayStart).getSingleResult()).longValue();
        cockpit.put("newMembersToday", newMembers);
        return cockpit;
    }

    /** 采购趋势（近 N 月） */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> purchaseTrend(int months) {
        LocalDateTime start = LocalDate.now().minusMonths(months - 1L).withDayOfMonth(1).atStartOfDay();
        List<Object[]> rows = em.createNativeQuery("""
                SELECT DATE_FORMAT(created_at, '%Y-%m') AS m, COALESCE(SUM(total_amount),0) AS amount
                FROM purchase_order
                WHERE created_at >= :start AND status IN (3, 4)
                GROUP BY DATE_FORMAT(created_at, '%Y-%m') ORDER BY m
                """).setParameter("start", start).getResultList();
        return rows.stream().map(r -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("month", (String) r[0]);
            item.put("amount", ((Number) r[1]).doubleValue());
            return item;
        }).collect(Collectors.toList());
    }

    // ------------------------------------------------------------------

    private BigDecimal sumSales(LocalDateTime start, LocalDateTime end) {
        String sql = "SELECT COALESCE(SUM(payable_amount),0) FROM sales_order WHERE created_at >= :start AND status != 3";
        if (end != null) {
            sql += " AND created_at < :end";
        }
        var q = em.createNativeQuery(sql).setParameter("start", start);
        if (end != null) {
            q.setParameter("end", end);
        }
        return (BigDecimal) q.getSingleResult();
    }

    private long countOrders(LocalDateTime start, LocalDateTime end) {
        String sql = "SELECT COUNT(*) FROM sales_order WHERE created_at >= :start AND status != 3";
        if (end != null) {
            sql += " AND created_at < :end";
        }
        var q = em.createNativeQuery(sql).setParameter("start", start);
        if (end != null) {
            q.setParameter("end", end);
        }
        return ((Number) q.getSingleResult()).longValue();
    }

    private BigDecimal sumPurchase(LocalDateTime start) {
        BigDecimal amount = (BigDecimal) em.createNativeQuery("""
                SELECT COALESCE(SUM(total_amount),0) FROM purchase_order
                WHERE created_at >= :start AND status IN (3, 4)
                """).setParameter("start", start).getSingleResult();
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    /** 毛利润 ≈ Σ(实收金额 - 采购价×数量)，采购价取商品当前档案价（近似口径） */
    private BigDecimal profit(LocalDateTime start, LocalDateTime end) {
        BigDecimal profit = (BigDecimal) em.createNativeQuery("""
                SELECT COALESCE(SUM(i.amount - i.purchase_price_snapshot * i.quantity), 0)
                FROM (
                    SELECT i.amount, p.purchase_price AS purchase_price_snapshot, i.quantity
                    FROM sales_order_item i
                    JOIN sales_order o ON i.order_id = o.id
                    JOIN product p ON i.product_id = p.id
                    WHERE o.created_at >= :start AND o.created_at < :end AND o.status != 3
                ) i
                """).setParameter("start", start).setParameter("end", end).getSingleResult();
        return profit == null ? BigDecimal.ZERO : profit.setScale(2, RoundingMode.HALF_UP);
    }

    private long countMembers() {
        return ((Number) em.createNativeQuery("SELECT COUNT(*) FROM member WHERE status = 1")
                .getSingleResult()).longValue();
    }

    /** 会员销售占比（按金额） */
    private double memberSalesRatio(LocalDateTime start) {
        BigDecimal total = sumSales(start, null);
        BigDecimal member = (BigDecimal) em.createNativeQuery("""
                SELECT COALESCE(SUM(payable_amount),0) FROM sales_order
                WHERE created_at >= :start AND member_id IS NOT NULL AND status != 3
                """).setParameter("start", start).getSingleResult();
        if (total.signum() == 0) {
            return 0;
        }
        return member.multiply(BigDecimal.valueOf(100))
                .divide(total, 1, RoundingMode.HALF_UP).doubleValue();
    }

    private BigDecimal sumLoss(LocalDateTime start) {
        BigDecimal amount = (BigDecimal) em.createNativeQuery(
                        "SELECT COALESCE(SUM(loss_amount),0) FROM loss_record WHERE created_at >= :start")
                .setParameter("start", start).getSingleResult();
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, Object> toRankRow(Object[] r) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("productId", ((Number) r[0]).longValue());
        item.put("productName", (String) r[1]);
        item.put("sku", (String) r[2]);
        item.put("quantity", ((Number) r[3]).longValue());
        item.put("amount", ((Number) r[4]).doubleValue());
        return item;
    }
}
