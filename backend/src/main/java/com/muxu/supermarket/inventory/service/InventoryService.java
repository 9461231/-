package com.muxu.supermarket.inventory.service;

import com.muxu.supermarket.common.BusinessException;
import com.muxu.supermarket.common.PageVO;
import com.muxu.supermarket.inventory.dto.*;
import com.muxu.supermarket.inventory.entity.Inventory;
import com.muxu.supermarket.inventory.entity.InventoryTransaction;
import com.muxu.supermarket.inventory.repository.InventoryRepository;
import com.muxu.supermarket.inventory.repository.InventoryTransactionRepository;
import com.muxu.supermarket.product.entity.Product;
import com.muxu.supermarket.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 统一库存中心：
 * 采购入库 → 库存+；销售出库 → 库存-；销售退货 → 库存+；
 * 采购退货 → 库存-；盘点调整 → 库存±
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final ProductRepository productRepository;

    // ==================== 核心库存变动（供其他模块事务内调用） ====================

    /**
     * 库存增加（采购入库、销售退货等）。必须在调用方事务内执行。
     */
    @Transactional
    public void increase(Long productId, int qty, String type, String refType, String refNo, String remark) {
        if (qty <= 0) {
            throw new BusinessException("库存增加数量必须大于0");
        }
        changeStock(productId, qty, type, refType, refNo, remark);
    }

    /**
     * 库存减少（销售出库、采购退货、损耗等），库存不足时抛出异常。
     */
    @Transactional
    public void decrease(Long productId, int qty, String type, String refType, String refNo, String remark) {
        if (qty <= 0) {
            throw new BusinessException("库存减少数量必须大于0");
        }
        Inventory inventory = inventoryRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new BusinessException("商品尚未建立库存记录，无法出库: productId=" + productId));
        if (inventory.getQuantity() < qty) {
            Product product = productRepository.findById(productId).orElse(null);
            throw new BusinessException(String.format("库存不足：商品「%s」当前库存 %d，需要 %d",
                    product == null ? productId : product.getName(), inventory.getQuantity(), qty));
        }
        changeStock(productId, -qty, type, refType, refNo, remark);
    }

    /**
     * 库存调整（可正可负，用于盘点/手工调整）
     */
    @Transactional
    public void adjust(Long productId, int changeQty, String type, String refType, String refNo, String remark) {
        if (changeQty == 0) {
            return;
        }
        if (changeQty < 0) {
            Inventory inventory = inventoryRepository.findByProductIdForUpdate(productId)
                    .orElseThrow(() -> new BusinessException("商品尚未建立库存记录: productId=" + productId));
            if (inventory.getQuantity() + changeQty < 0) {
                throw new BusinessException("调整后库存不能为负数");
            }
        }
        changeStock(productId, changeQty, type, refType, refNo, remark);
    }

    private void changeStock(Long productId, int changeQty, String type, String refType, String refNo, String remark) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException("商品不存在: id=" + productId));

        Inventory inventory = inventoryRepository.findByProductIdForUpdate(productId)
                .orElseGet(() -> {
                    Inventory created = new Inventory();
                    created.setProductId(productId);
                    created.setQuantity(0);
                    return inventoryRepository.save(created);
                });

        int before = inventory.getQuantity();
        int after = before + changeQty;
        if (after < 0) {
            throw new BusinessException(String.format("库存不足：商品「%s」当前库存 %d，需要 %d",
                    product.getName(), before, -changeQty));
        }
        inventory.setQuantity(after);
        inventoryRepository.save(inventory);

        InventoryTransaction tx = new InventoryTransaction();
        tx.setProductId(productId);
        tx.setType(type);
        tx.setChangeQty(changeQty);
        tx.setBeforeQty(before);
        tx.setAfterQty(after);
        tx.setRefType(refType);
        tx.setRefNo(refNo);
        tx.setRemark(remark);
        transactionRepository.save(tx);
        log.debug("库存变动: product={} {} {} {}", productId, type, changeQty, after);
    }

    // ==================== 查询接口 ====================

    @Transactional(readOnly = true)
    public PageVO<InventoryVO> page(int page, int size, String keyword, Long categoryId, String alertType) {
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 10;

        List<Product> products;
        if (StringUtils.hasText(keyword)) {
            products = productRepository.findAll((root, query, cb) -> {
                String like = "%" + keyword.trim() + "%";
                return cb.or(
                        cb.like(root.get("name"), like),
                        cb.like(root.get("sku"), like),
                        cb.like(root.get("barcode"), like));
            });
        } else if (categoryId != null) {
            products = productRepository.findAll((root, query, cb) ->
                    cb.equal(root.get("categoryId"), categoryId));
        } else {
            products = productRepository.findAll();
        }

        Map<Long, Inventory> inventoryMap = inventoryRepository.findAll().stream()
                .collect(Collectors.toMap(Inventory::getProductId, i -> i));

        List<InventoryVO> all = products.stream()
                .map(p -> toVO(p, inventoryMap.get(p.getId())))
                .filter(vo -> filterAlert(vo, alertType))
                .toList();

        int total = all.size();
        int from = Math.min((page - 1) * size, total);
        int to = Math.min(from + size, total);
        return new PageVO<>(all.subList(from, to), total, page, size);
    }

    @Transactional(readOnly = true)
    public PageVO<InventoryTransactionVO> transactions(int page, int size, Long productId, String type) {
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 10;

        Specification<InventoryTransaction> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (productId != null) {
                predicates.add(cb.equal(root.get("productId"), productId));
            }
            if (StringUtils.hasText(type)) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        Page<InventoryTransaction> result = transactionRepository.findAll(spec,
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id")));

        Map<Long, Product> productMap = loadProductMap();
        List<InventoryTransactionVO> list = result.getContent().stream()
                .map(tx -> {
                    InventoryTransactionVO vo = new InventoryTransactionVO();
                    vo.setId(tx.getId());
                    vo.setProductId(tx.getProductId());
                    vo.setType(tx.getType());
                    vo.setChangeQty(tx.getChangeQty());
                    vo.setBeforeQty(tx.getBeforeQty());
                    vo.setAfterQty(tx.getAfterQty());
                    vo.setRefType(tx.getRefType());
                    vo.setRefNo(tx.getRefNo());
                    vo.setRemark(tx.getRemark());
                    vo.setCreatedAt(tx.getCreatedAt());
                    Product p = productMap.get(tx.getProductId());
                    if (p != null) {
                        vo.setProductName(p.getName());
                        vo.setSku(p.getSku());
                    }
                    return vo;
                }).toList();
        return new PageVO<>(list, result.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public List<InventoryAlertVO> alerts() {
        Map<Long, Inventory> inventoryMap = inventoryRepository.findAll().stream()
                .collect(Collectors.toMap(Inventory::getProductId, i -> i));

        return productRepository.findAll().stream()
                .filter(p -> p.getStatus() == 1)
                .map(p -> {
                    Inventory inv = inventoryMap.get(p.getId());
                    int qty = inv == null ? 0 : inv.getQuantity();
                    String alertType = null;
                    if (qty <= 0) {
                        alertType = "OUT_OF_STOCK";
                    } else if (qty <= p.getMinStock()) {
                        alertType = "LOW";
                    } else if (p.getMaxStock() != null && qty >= p.getMaxStock()) {
                        alertType = "HIGH";
                    }
                    if (alertType == null) {
                        return null;
                    }
                    InventoryAlertVO vo = new InventoryAlertVO();
                    vo.setProductId(p.getId());
                    vo.setProductName(p.getName());
                    vo.setSku(p.getSku());
                    vo.setQuantity(qty);
                    vo.setMinStock(p.getMinStock());
                    vo.setMaxStock(p.getMaxStock());
                    vo.setAlertType(alertType);
                    if (!"HIGH".equals(alertType)) {
                        int target = Math.max(p.getMinStock() * 2, p.getMinStock());
                        vo.setSuggestQty(Math.max(target - qty, p.getMinStock() - qty));
                    }
                    return vo;
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /** 手工调整（前端入口） */
    @Transactional
    public void manualAdjust(InventoryAdjustRequest request) {
        adjust(request.getProductId(), request.getChangeQty(),
                InventoryTransaction.TYPE_MANUAL_ADJUST, "MANUAL", null, request.getReason());
    }

    // ------------------------------------------------------------------

    private InventoryVO toVO(Product p, Inventory inv) {
        InventoryVO vo = new InventoryVO();
        vo.setProductId(p.getId());
        vo.setProductName(p.getName());
        vo.setSku(p.getSku());
        vo.setUnit(p.getUnit());
        vo.setMinStock(p.getMinStock());
        vo.setMaxStock(p.getMaxStock());
        int qty = inv == null ? 0 : inv.getQuantity();
        vo.setQuantity(qty);
        vo.setInventoryValue(p.getPurchasePrice() == null ? null
                : p.getPurchasePrice().multiply(java.math.BigDecimal.valueOf(qty)));
        vo.setUpdatedAt(inv == null ? null : inv.getUpdatedAt());
        if (qty <= 0) {
            vo.setAlertType("OUT_OF_STOCK");
        } else if (qty <= p.getMinStock()) {
            vo.setAlertType("LOW");
        } else if (p.getMaxStock() != null && qty >= p.getMaxStock()) {
            vo.setAlertType("HIGH");
        }
        return vo;
    }

    private boolean filterAlert(InventoryVO vo, String alertType) {
        if (!StringUtils.hasText(alertType)) {
            return true;
        }
        return alertType.equals(vo.getAlertType());
    }

    private Map<Long, Product> loadProductMap() {
        return productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
    }
}
