package com.muxu.supermarket.purchase.service;

import com.muxu.supermarket.common.BusinessException;
import com.muxu.supermarket.common.PageVO;
import com.muxu.supermarket.inventory.entity.InventoryTransaction;
import com.muxu.supermarket.inventory.service.InventoryService;
import com.muxu.supermarket.product.entity.Product;
import com.muxu.supermarket.product.repository.ProductRepository;
import com.muxu.supermarket.purchase.dto.*;
import com.muxu.supermarket.purchase.entity.*;
import com.muxu.supermarket.purchase.repository.*;
import com.muxu.supermarket.supplier.entity.Supplier;
import com.muxu.supermarket.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PurchaseOrderRepository orderRepository;
    private final PurchaseOrderItemRepository orderItemRepository;
    private final PurchaseReceiptRepository receiptRepository;
    private final PurchaseReceiptItemRepository receiptItemRepository;
    private final PurchaseReturnRepository returnRepository;
    private final PurchaseReturnItemRepository returnItemRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final com.muxu.supermarket.inventory.service.BatchService batchService;
    private final com.muxu.supermarket.purchase.repository.PurchasePaymentRepository paymentRepository;
    private final com.muxu.supermarket.supplier.repository.SupplierProductRepository supplierProductRepository;

    // ==================== 采购订单 ====================

    @Transactional(readOnly = true)
    public PageVO<PurchaseOrderVO> page(int page, int size, String keyword, Long supplierId, Integer status) {
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 10;

        Specification<PurchaseOrder> spec = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (StringUtils.hasText(keyword)) {
                predicates.add(cb.like(root.get("orderNo"), "%" + keyword.trim() + "%"));
            }
            if (supplierId != null) {
                predicates.add(cb.equal(root.get("supplierId"), supplierId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        Page<PurchaseOrder> result = orderRepository.findAll(spec,
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id")));

        Map<Long, String> supplierNames = loadSupplierNameMap();
        Map<Long, Long> itemCounts = orderItemRepository.findAll().stream()
                .collect(Collectors.groupingBy(PurchaseOrderItem::getOrderId, Collectors.counting()));

        List<PurchaseOrderVO> list = result.getContent().stream()
                .map(o -> PurchaseOrderVO.from(o, supplierNames.get(o.getSupplierId()),
                        itemCounts.getOrDefault(o.getId(), 0L).intValue()))
                .toList();
        return new PageVO<>(list, result.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(Long id) {
        PurchaseOrder order = getOrder(id);
        Map<Long, String> supplierNames = loadSupplierNameMap();
        PurchaseOrderVO orderVO = PurchaseOrderVO.from(order,
                supplierNames.get(order.getSupplierId()), null);

        Map<Long, Product> productMap = loadProductMap();
        List<PurchaseOrderItemVO> items = orderItemRepository.findByOrderId(id).stream()
                .map(item -> {
                    Product p = productMap.get(item.getProductId());
                    return PurchaseOrderItemVO.from(item,
                            p == null ? null : p.getName(),
                            p == null ? null : p.getSku(),
                            p == null ? null : p.getUnit());
                }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("order", orderVO);
        result.put("items", items);
        return result;
    }

    @Transactional
    public PurchaseOrderVO create(PurchaseOrderRequest request) {
        validateSupplier(request.getSupplierId());
        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNo("PO" + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", (int) (Math.random() * 1000)));
        order.setSupplierId(request.getSupplierId());
        order.setStatus(PurchaseOrder.STATUS_DRAFT);
        order.setRemark(request.getRemark());
        order.setTotalAmount(BigDecimal.ZERO);
        order = orderRepository.save(order);

        BigDecimal total = saveItems(order.getId(), request.getItems());
        order.setTotalAmount(total);
        order = orderRepository.save(order);
        return PurchaseOrderVO.from(order, supplierName(order.getSupplierId()), request.getItems().size());
    }

    @Transactional
    public PurchaseOrderVO update(Long id, PurchaseOrderRequest request) {
        PurchaseOrder order = getOrder(id);
        if (order.getStatus() != PurchaseOrder.STATUS_DRAFT) {
            throw new BusinessException("只有草稿状态的采购订单才能修改");
        }
        validateSupplier(request.getSupplierId());
        order.setSupplierId(request.getSupplierId());
        order.setRemark(request.getRemark());
        orderItemRepository.deleteAll(orderItemRepository.findByOrderId(id));
        BigDecimal total = saveItems(order.getId(), request.getItems());
        order.setTotalAmount(total);
        order = orderRepository.save(order);
        return PurchaseOrderVO.from(order, supplierName(order.getSupplierId()), request.getItems().size());
    }

    /** 提交审核：草稿 → 待审核 */
    @Transactional
    public void submit(Long id) {
        PurchaseOrder order = getOrder(id);
        if (order.getStatus() != PurchaseOrder.STATUS_DRAFT) {
            throw new BusinessException("只有草稿状态的采购订单才能提交审核");
        }
        order.setStatus(PurchaseOrder.STATUS_PENDING_AUDIT);
        orderRepository.save(order);
    }

    /** 审核：通过 → 已审核；驳回 → 回到草稿 */
    @Transactional
    public void audit(Long id, boolean approved, String auditRemark) {
        PurchaseOrder order = getOrder(id);
        if (order.getStatus() != PurchaseOrder.STATUS_PENDING_AUDIT) {
            throw new BusinessException("只有待审核状态的采购订单才能审核");
        }
        if (approved) {
            order.setStatus(PurchaseOrder.STATUS_AUDITED);
        } else {
            order.setStatus(PurchaseOrder.STATUS_DRAFT);
        }
        order.setAuditRemark(auditRemark);
        order.setAuditAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    @Transactional
    public void cancel(Long id) {
        PurchaseOrder order = getOrder(id);
        if (order.getStatus() == PurchaseOrder.STATUS_COMPLETED) {
            throw new BusinessException("已完成的采购订单不能取消");
        }
        if (order.getStatus() == PurchaseOrder.STATUS_PARTIAL_RECEIVED) {
            throw new BusinessException("已发生入库的采购订单不能取消");
        }
        order.setStatus(PurchaseOrder.STATUS_CANCELLED);
        orderRepository.save(order);
    }

    // ==================== 采购入库（第一次多表业务事务） ====================

    @Transactional
    public Map<String, Object> receipt(Long orderId, PurchaseReceiptRequest request) {
        PurchaseOrder order = getOrder(orderId);
        if (order.getStatus() != PurchaseOrder.STATUS_AUDITED
                && order.getStatus() != PurchaseOrder.STATUS_PARTIAL_RECEIVED) {
            throw new BusinessException("只有已审核/部分入库状态的采购订单才能入库");
        }

        List<PurchaseOrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        Map<Long, PurchaseOrderItem> itemMap = orderItems.stream()
                .collect(Collectors.toMap(PurchaseOrderItem::getId, Function.identity()));

        PurchaseReceipt receipt = new PurchaseReceipt();
        String receiptNo = "PR" + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", (int) (Math.random() * 1000));
        receipt.setReceiptNo(receiptNo);
        receipt.setOrderId(orderId);
        receipt.setSupplierId(order.getSupplierId());
        receipt.setRemark(request.getRemark());

        BigDecimal total = BigDecimal.ZERO;
        List<PurchaseReceiptItem> receiptItems = new ArrayList<>();
        for (PurchaseReceiptRequest.Item item : request.getItems()) {
            PurchaseOrderItem orderItem = itemMap.get(item.getOrderItemId());
            if (orderItem == null) {
                throw new BusinessException("采购明细不存在: id=" + item.getOrderItemId());
            }
            int remain = orderItem.getQuantity() - orderItem.getReceivedQuantity();
            if (item.getQuantity() > remain) {
                throw new BusinessException(String.format("入库数量超过剩余待入库数量（剩余 %d）", remain));
            }
            orderItem.setReceivedQuantity(orderItem.getReceivedQuantity() + item.getQuantity());
            total = total.add(orderItem.getPurchasePrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity())));

            PurchaseReceiptItem ri = new PurchaseReceiptItem();
            ri.setOrderItemId(orderItem.getId());
            ri.setProductId(orderItem.getProductId());
            ri.setQuantity(item.getQuantity());
            receiptItems.add(ri);

            // 库存增加（同一事务内）
            inventoryService.increase(orderItem.getProductId(), item.getQuantity(),
                    InventoryTransaction.TYPE_PURCHASE_IN, "PURCHASE_RECEIPT",
                    receiptNo, "采购入库");
        }
        orderItemRepository.saveAll(orderItems);

        // 生成批次（提供保质期信息时；库存增加已由 receiptItems 循环完成，批次只记档案）
        if (request.getShelfLifeDays() != null && request.getShelfLifeDays() > 0) {
            LocalDate productionDate = StringUtils.hasText(request.getProductionDate())
                    ? LocalDate.parse(request.getProductionDate()) : null;
            for (PurchaseReceiptItem ri : receiptItems) {
                PurchaseOrderItem oi = itemMap.get(ri.getOrderItemId());
                batchService.createFromReceipt(ri.getProductId(), order.getSupplierId(),
                        ri.getQuantity(), oi.getPurchasePrice(), productionDate, request.getShelfLifeDays());
            }
        }

        receipt.setTotalAmount(total);
        PurchaseReceipt savedReceipt = receiptRepository.save(receipt);
        receiptItems.forEach(ri -> ri.setReceiptId(savedReceipt.getId()));
        receiptItemRepository.saveAll(receiptItems);

        // 更新订单状态
        boolean allReceived = orderItemRepository.findByOrderId(orderId).stream()
                .allMatch(i -> i.getReceivedQuantity() >= i.getQuantity());
        if (allReceived) {
            order.setStatus(PurchaseOrder.STATUS_COMPLETED);
            order.setCompletedAt(LocalDateTime.now());
        } else {
            order.setStatus(PurchaseOrder.STATUS_PARTIAL_RECEIVED);
        }
        orderRepository.save(order);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("receiptNo", receipt.getReceiptNo());
        result.put("orderStatus", PurchaseOrderVO.statusLabel(order.getStatus()));
        return result;
    }

    // ==================== 采购退货 ====================

    @Transactional
    public PurchaseReturnVO createReturn(PurchaseReturnRequest request) {
        validateSupplier(request.getSupplierId());
        if (request.getOrderId() != null) {
            getOrder(request.getOrderId());
        }

        PurchaseReturn ret = new PurchaseReturn();
        String returnNo = "PRN" + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", (int) (Math.random() * 1000));
        ret.setReturnNo(returnNo);
        ret.setSupplierId(request.getSupplierId());
        ret.setOrderId(request.getOrderId());
        ret.setReason(request.getReason());

        BigDecimal total = BigDecimal.ZERO;
        List<PurchaseReturnItem> items = new ArrayList<>();
        for (PurchaseReturnRequest.Item item : request.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new BusinessException("商品不存在: id=" + item.getProductId()));
            total = total.add(item.getPurchasePrice().multiply(BigDecimal.valueOf(item.getQuantity())));

            PurchaseReturnItem ri = new PurchaseReturnItem();
            ri.setProductId(product.getId());
            ri.setQuantity(item.getQuantity());
            ri.setPurchasePrice(item.getPurchasePrice());
            items.add(ri);

            // 库存减少（同一事务内）
            inventoryService.decrease(product.getId(), item.getQuantity(),
                    InventoryTransaction.TYPE_PURCHASE_RETURN_OUT, "PURCHASE_RETURN",
                    returnNo, "采购退货");
        }

        ret.setTotalAmount(total);
        PurchaseReturn savedReturn = returnRepository.save(ret);
        items.forEach(ri -> ri.setReturnId(savedReturn.getId()));
        returnItemRepository.saveAll(items);

        PurchaseReturnVO vo = PurchaseReturnVO.from(ret, supplierName(ret.getSupplierId()));
        Map<Long, Product> productMap = loadProductMap();
        vo.setItems(items.stream().map(ri -> {
            PurchaseReturnVO.PurchaseReturnVOItem vi = new PurchaseReturnVO.PurchaseReturnVOItem();
            vi.setProductId(ri.getProductId());
            Product p = productMap.get(ri.getProductId());
            vi.setProductName(p == null ? null : p.getName());
            vi.setQuantity(ri.getQuantity());
            vi.setPurchasePrice(ri.getPurchasePrice());
            return vi;
        }).toList());
        return vo;
    }

    @Transactional(readOnly = true)
    public PageVO<PurchaseReturnVO> pageReturns(int page, int size, String keyword) {
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 10;

        Specification<PurchaseReturn> spec = (root, query, cb) -> {
            if (StringUtils.hasText(keyword)) {
                return cb.like(root.get("returnNo"), "%" + keyword.trim() + "%");
            }
            return cb.conjunction();
        };
        Page<PurchaseReturn> result = returnRepository.findAll(spec,
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id")));

        Map<Long, String> supplierNames = loadSupplierNameMap();
        Map<Long, List<PurchaseReturnItem>> itemsByReturn = returnItemRepository.findAll().stream()
                .collect(Collectors.groupingBy(PurchaseReturnItem::getReturnId));
        Map<Long, Product> productMap = loadProductMap();

        List<PurchaseReturnVO> list = result.getContent().stream().map(r -> {
            PurchaseReturnVO vo = PurchaseReturnVO.from(r, supplierNames.get(r.getSupplierId()));
            vo.setItems(itemsByReturn.getOrDefault(r.getId(), List.of()).stream().map(ri -> {
                PurchaseReturnVO.PurchaseReturnVOItem vi = new PurchaseReturnVO.PurchaseReturnVOItem();
                vi.setProductId(ri.getProductId());
                Product p = productMap.get(ri.getProductId());
                vi.setProductName(p == null ? null : p.getName());
                vi.setQuantity(ri.getQuantity());
                vi.setPurchasePrice(ri.getPurchasePrice());
                return vi;
            }).toList());
            return vo;
        }).toList();
        return new PageVO<>(list, result.getTotalElements(), page, size);
    }

    // ==================== 采购结算（应付/已付/未付） ====================

    /** 登记付款：同一事务内更新订单已付金额 */
    @Transactional
    public com.muxu.supermarket.purchase.entity.PurchasePayment pay(Long orderId,
                                                                    BigDecimal amount, String method, String remark) {
        PurchaseOrder order = getOrder(orderId);
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException("付款金额必须大于0");
        }
        BigDecimal unpaid = order.getTotalAmount().subtract(order.getPaidAmount());
        if (amount.compareTo(unpaid) > 0) {
            throw new BusinessException(String.format("付款金额超过未付金额（未付 ¥%s）", unpaid));
        }
        order.setPaidAmount(order.getPaidAmount().add(amount));
        orderRepository.save(order);

        com.muxu.supermarket.purchase.entity.PurchasePayment payment = new com.muxu.supermarket.purchase.entity.PurchasePayment();
        payment.setOrderId(orderId);
        payment.setOrderNo(order.getOrderNo());
        payment.setSupplierId(order.getSupplierId());
        payment.setAmount(amount);
        payment.setMethod(method == null ? "CASH" : method);
        payment.setRemark(remark);
        var user = com.muxu.supermarket.common.CurrentUser.get();
        payment.setPaidBy(user == null ? "system" : user.username());
        return paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public List<com.muxu.supermarket.purchase.entity.PurchasePayment> payments(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    /**
     * 供应商履约统计：采购额、已付/未付、准时完成率（审核→完成≤72h）、退货率、综合评分
     */
    @Transactional(readOnly = true)
    public Map<String, Object> supplierStats(Long supplierId) {
        List<PurchaseOrder> orders = orderRepository.findAll().stream()
                .filter(o -> supplierId.equals(o.getSupplierId()))
                .filter(o -> o.getStatus() != PurchaseOrder.STATUS_DRAFT
                        && o.getStatus() != PurchaseOrder.STATUS_CANCELLED)
                .toList();

        BigDecimal totalAmount = orders.stream().map(PurchaseOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paidAmount = orders.stream().map(PurchaseOrder::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long completed = orders.stream()
                .filter(o -> o.getStatus() == PurchaseOrder.STATUS_COMPLETED).count();
        long onTime = orders.stream()
                .filter(o -> o.getStatus() == PurchaseOrder.STATUS_COMPLETED)
                .filter(o -> o.getAuditAt() != null && o.getCompletedAt() != null)
                .filter(o -> java.time.Duration.between(o.getAuditAt(), o.getCompletedAt()).toHours() <= 72)
                .count();
        long returnCount = returnRepository.findAll().stream()
                .filter(r -> supplierId.equals(r.getSupplierId())).count();

        double deliveryRate = completed == 0 ? 100.0 : onTime * 100.0 / completed;
        double returnRate = orders.isEmpty() ? 0.0 : returnCount * 100.0 / orders.size();
        double score = Math.max(0, Math.min(100, 60 + deliveryRate * 0.3 - returnRate * 0.5));

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("orderCount", orders.size());
        stats.put("completedCount", completed);
        stats.put("totalAmount", totalAmount);
        stats.put("paidAmount", paidAmount);
        stats.put("unpaidAmount", totalAmount.subtract(paidAmount));
        stats.put("returnCount", returnCount);
        stats.put("deliveryOnTimeRate", Math.round(deliveryRate * 10) / 10.0);
        stats.put("returnRate", Math.round(returnRate * 10) / 10.0);
        stats.put("score", Math.round(score * 10) / 10.0);
        return stats;
    }

    /**
     * 供应商比较：同一商品不同供应商的供货价与履约数据（价格升序）
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> compareSuppliers(Long productId) {
        List<com.muxu.supermarket.supplier.entity.SupplierProduct> bindings =
                supplierProductRepository.findByProductId(productId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (com.muxu.supermarket.supplier.entity.SupplierProduct sp : bindings) {
            Supplier supplier = supplierRepository.findById(sp.getSupplierId()).orElse(null);
            if (supplier == null) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("supplierId", supplier.getId());
            row.put("supplierName", supplier.getName());
            row.put("supplyPrice", sp.getSupplyPrice());
            row.put("isPrimary", sp.getIsPrimary());
            row.putAll(supplierStats(supplier.getId()));
            result.add(row);
        }
        result.sort((a, b) -> Double.compare(
                ((BigDecimal) a.get("supplyPrice")).doubleValue(),
                ((BigDecimal) b.get("supplyPrice")).doubleValue()));
        return result;
    }

    /** 采购统计：本月订单数与金额 */
    @Transactional(readOnly = true)
    public Map<String, Object> monthStats() {
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();
        List<PurchaseOrder> orders = orderRepository.findAll().stream()
                .filter(o -> o.getCreatedAt().isAfter(monthStart))
                .filter(o -> o.getStatus() != PurchaseOrder.STATUS_CANCELLED)
                .toList();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("monthOrderCount", orders.size());
        stats.put("monthAmount", orders.stream()
                .map(PurchaseOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return stats;
    }

    // ------------------------------------------------------------------

    private BigDecimal saveItems(Long orderId, List<PurchaseOrderRequest.Item> items) {
        BigDecimal total = BigDecimal.ZERO;
        List<PurchaseOrderItem> entities = new ArrayList<>();
        for (PurchaseOrderRequest.Item item : items) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new BusinessException("商品不存在: id=" + item.getProductId()));
            BigDecimal amount = item.getPurchasePrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(amount);
            PurchaseOrderItem entity = new PurchaseOrderItem();
            entity.setOrderId(orderId);
            entity.setProductId(product.getId());
            entity.setQuantity(item.getQuantity());
            entity.setPurchasePrice(item.getPurchasePrice());
            entity.setAmount(amount);
            entities.add(entity);
        }
        orderItemRepository.saveAll(entities);
        return total;
    }

    private void validateSupplier(Long supplierId) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new BusinessException("供应商不存在: id=" + supplierId));
        if (supplier.getStatus() != Supplier.STATUS_ENABLED) {
            throw new BusinessException("供应商「" + supplier.getName() + "」已停止合作，不能用于采购");
        }
    }

    private PurchaseOrder getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "采购订单不存在: id=" + id));
    }

    private Map<Long, String> loadSupplierNameMap() {
        return supplierRepository.findAll().stream()
                .collect(Collectors.toMap(Supplier::getId, Supplier::getName, (a, b) -> a));
    }

    private Map<Long, Product> loadProductMap() {
        return productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, Function.identity(), (a, b) -> a));
    }

    private String supplierName(Long supplierId) {
        return supplierRepository.findById(supplierId).map(Supplier::getName).orElse(null);
    }
}
