package com.muxu.supermarket.stocktake.service;

import com.muxu.supermarket.common.BusinessException;
import com.muxu.supermarket.common.PageVO;
import com.muxu.supermarket.inventory.entity.Inventory;
import com.muxu.supermarket.inventory.entity.InventoryTransaction;
import com.muxu.supermarket.inventory.repository.InventoryRepository;
import com.muxu.supermarket.inventory.service.InventoryService;
import com.muxu.supermarket.product.entity.Product;
import com.muxu.supermarket.product.repository.ProductRepository;
import com.muxu.supermarket.stocktake.dto.*;
import com.muxu.supermarket.stocktake.entity.LossRecord;
import com.muxu.supermarket.stocktake.entity.Stocktake;
import com.muxu.supermarket.stocktake.entity.StocktakeItem;
import com.muxu.supermarket.stocktake.repository.LossRecordRepository;
import com.muxu.supermarket.stocktake.repository.StocktakeItemRepository;
import com.muxu.supermarket.stocktake.repository.StocktakeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 盘点：系统库存 + 实际盘点 → 差异 → 盘盈/盘亏 → 自动生成库存调整
 */
@Service
@RequiredArgsConstructor
public class StocktakeService {

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final StocktakeRepository stocktakeRepository;
    private final StocktakeItemRepository itemRepository;
    private final LossRecordRepository lossRecordRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;

    // ==================== 盘点 ====================

    @Transactional
    public StocktakeVO create(StocktakeCreateRequest request) {
        List<Long> productIds = request.getProductIds().stream().distinct().toList();
        Map<Long, Product> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        for (Long pid : productIds) {
            if (!productMap.containsKey(pid)) {
                throw new BusinessException("商品不存在: id=" + pid);
            }
        }
        Map<Long, Inventory> inventoryMap = inventoryRepository.findAll().stream()
                .collect(Collectors.toMap(Inventory::getProductId, i -> i));

        Stocktake stocktake = new Stocktake();
        stocktake.setTaskNo("ST" + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", (int) (Math.random() * 1000)));
        stocktake.setRemark(request.getRemark());
        final Stocktake savedStocktake = stocktakeRepository.save(stocktake);

        List<StocktakeItem> items = productIds.stream().map(pid -> {
            StocktakeItem item = new StocktakeItem();
            item.setStocktakeId(savedStocktake.getId());
            item.setProductId(pid);
            Inventory inv = inventoryMap.get(pid);
            item.setSystemQty(inv == null ? 0 : inv.getQuantity());
            return item;
        }).toList();
        itemRepository.saveAll(items);

        return toVO(savedStocktake, items, productMap);
    }

    @Transactional(readOnly = true)
    public PageVO<StocktakeVO> page(int page, int size) {
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 10;
        Page<Stocktake> result = stocktakeRepository.findAll(
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id")));
        List<StocktakeVO> list = result.getContent().stream()
                .map(t -> toVO(t, itemRepository.findByStocktakeId(t.getId()), loadProductMap()))
                .toList();
        return new PageVO<>(list, result.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public StocktakeVO detail(Long id) {
        Stocktake stocktake = getStocktake(id);
        return toVO(stocktake, itemRepository.findByStocktakeId(id), loadProductMap());
    }

    /** 录入实际数量 */
    @Transactional
    public void recordActual(Long stocktakeId, Long itemId, Integer actualQty) {
        Stocktake stocktake = getStocktake(stocktakeId);
        if (stocktake.getStatus() != Stocktake.STATUS_IN_PROGRESS) {
            throw new BusinessException("只有进行中的盘点任务才能录入实盘数量");
        }
        if (actualQty == null || actualQty < 0) {
            throw new BusinessException("实盘数量不能为负数");
        }
        StocktakeItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(404, "盘点明细不存在"));
        if (!item.getStocktakeId().equals(stocktakeId)) {
            throw new BusinessException("盘点明细不属于该任务");
        }
        item.setActualQty(actualQty);
        item.setDiff(actualQty - item.getSystemQty());
        itemRepository.save(item);
    }

    /** 完成盘点：差异自动生成库存调整（同一事务） */
    @Transactional
    public StocktakeVO complete(Long id) {
        Stocktake stocktake = getStocktake(id);
        if (stocktake.getStatus() != Stocktake.STATUS_IN_PROGRESS) {
            throw new BusinessException("盘点任务状态不允许完成操作");
        }
        List<StocktakeItem> items = itemRepository.findByStocktakeId(id);
        boolean anyRecorded = items.stream().anyMatch(i -> i.getActualQty() != null);
        if (!anyRecorded) {
            throw new BusinessException("尚未录入任何实盘数量，无法完成盘点");
        }

        for (StocktakeItem item : items) {
            if (item.getActualQty() == null) {
                continue;
            }
            if (item.getDiff() != null && item.getDiff() != 0) {
                // 盘点调整：正=盘盈，负=盘亏
                inventoryService.adjust(item.getProductId(), item.getDiff(),
                        InventoryTransaction.TYPE_STOCKTAKE_ADJUST, "STOCKTAKE",
                        stocktake.getTaskNo(), "盘点调整（系统 " + item.getSystemQty()
                                + " → 实盘 " + item.getActualQty() + "）");
            }
        }
        stocktake.setStatus(Stocktake.STATUS_COMPLETED);
        stocktake.setCompletedAt(LocalDateTime.now());
        stocktakeRepository.save(stocktake);

        return toVO(stocktake, items, loadProductMap());
    }

    @Transactional
    public void cancel(Long id) {
        Stocktake stocktake = getStocktake(id);
        if (stocktake.getStatus() != Stocktake.STATUS_IN_PROGRESS) {
            throw new BusinessException("只有进行中的盘点任务才能取消");
        }
        stocktake.setStatus(Stocktake.STATUS_CANCELLED);
        stocktakeRepository.save(stocktake);
    }

    // ==================== 损耗 ====================

    @Transactional
    public LossRecord createLoss(LossRecordRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new BusinessException("商品不存在: id=" + request.getProductId()));
        BigDecimal unitCost = request.getUnitCost() != null
                ? request.getUnitCost() : product.getPurchasePrice();

        // 库存减少（同一事务，库存不足抛异常）
        inventoryService.decrease(product.getId(), request.getQuantity(),
                InventoryTransaction.TYPE_LOSS_OUT, "LOSS_RECORD", null, "损耗：" + request.getReason());

        LossRecord record = new LossRecord();
        record.setProductId(product.getId());
        record.setQuantity(request.getQuantity());
        record.setLossAmount(unitCost.multiply(BigDecimal.valueOf(request.getQuantity()))
                .setScale(2, java.math.RoundingMode.HALF_UP));
        record.setReason(request.getReason());
        return lossRecordRepository.save(record);
    }

    @Transactional(readOnly = true)
    public PageVO<Map<String, Object>> pageLoss(int page, int size) {
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 10;
        Page<LossRecord> result = lossRecordRepository.findAll(
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id")));
        Map<Long, Product> productMap = loadProductMap();
        List<Map<String, Object>> list = result.getContent().stream().map(r -> {
            Map<String, Object> vo = new java.util.LinkedHashMap<>();
            vo.put("id", r.getId());
            vo.put("productId", r.getProductId());
            Product p = productMap.get(r.getProductId());
            vo.put("productName", p == null ? null : p.getName());
            vo.put("sku", p == null ? null : p.getSku());
            vo.put("quantity", r.getQuantity());
            vo.put("lossAmount", r.getLossAmount());
            vo.put("reason", r.getReason());
            vo.put("createdAt", r.getCreatedAt());
            return vo;
        }).toList();
        return new PageVO<>(list, result.getTotalElements(), page, size);
    }

    // ------------------------------------------------------------------

    private Stocktake getStocktake(Long id) {
        return stocktakeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "盘点任务不存在: id=" + id));
    }

    private StocktakeVO toVO(Stocktake t, List<StocktakeItem> items, Map<Long, Product> productMap) {
        return StocktakeVO.from(t, items.stream().map(i -> {
            Product p = productMap.get(i.getProductId());
            return StocktakeVO.toItem(i,
                    p == null ? null : p.getName(),
                    p == null ? null : p.getSku(),
                    p == null ? null : p.getUnit());
        }).toList());
    }

    private Map<Long, Product> loadProductMap() {
        return productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, Function.identity(), (a, b) -> a));
    }
}
