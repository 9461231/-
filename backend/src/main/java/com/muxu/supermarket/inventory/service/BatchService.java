package com.muxu.supermarket.inventory.service;

import com.muxu.supermarket.common.BusinessException;
import com.muxu.supermarket.inventory.dto.BatchDTOs.BatchVO;
import com.muxu.supermarket.inventory.dto.BatchDTOs.CreateRequest;
import com.muxu.supermarket.inventory.dto.BatchDTOs.DisposeRequest;
import com.muxu.supermarket.inventory.entity.InventoryTransaction;
import com.muxu.supermarket.inventory.entity.ProductBatch;
import com.muxu.supermarket.inventory.repository.ProductBatchRepository;
import com.muxu.supermarket.product.entity.Product;
import com.muxu.supermarket.product.repository.ProductRepository;
import com.muxu.supermarket.supplier.entity.Supplier;
import com.muxu.supermarket.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 批次与临期管理：批次建档 → 临期预警 → 过期/临期处置（扣减库存）
 */
@Service
@RequiredArgsConstructor
public class BatchService {

    private final ProductBatchRepository batchRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final InventoryService inventoryService;

    /** 手工创建批次（入库新批次） */
    @Transactional
    public BatchVO create(CreateRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new BusinessException("商品不存在: id=" + request.getProductId()));

        ProductBatch batch = new ProductBatch();
        batch.setBatchNo("B" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%04d", (int) (Math.random() * 10000)));
        batch.setProductId(product.getId());
        batch.setSupplierId(request.getSupplierId());
        batch.setQuantity(request.getQuantity());
        batch.setPurchasePrice(request.getPurchasePrice() != null
                ? request.getPurchasePrice() : product.getPurchasePrice());
        if (request.getExpireDate() != null && !request.getExpireDate().isBlank()) {
            batch.setExpireDate(LocalDate.parse(request.getExpireDate()));
        } else if (request.getShelfLifeDays() != null && request.getShelfLifeDays() > 0) {
            batch.setShelfLifeDays(request.getShelfLifeDays());
            batch.setExpireDate(request.getProductionDate() != null && !request.getProductionDate().isBlank()
                    ? LocalDate.parse(request.getProductionDate()).plusDays(request.getShelfLifeDays())
                    : LocalDate.now().plusDays(request.getShelfLifeDays()));
        }
        batch.setRemark(request.getRemark());
        batch = batchRepository.save(batch);

        // 批次入库：同步增加总库存
        inventoryService.increase(product.getId(), request.getQuantity(),
                InventoryTransaction.TYPE_PURCHASE_IN, "BATCH_IN", batch.getBatchNo(), "批次入库");

        return toVO(batch);
    }

    /** 采购入库时自动生成批次（同一事务，由采购模块调用） */
    @Transactional
    public void createFromReceipt(Long productId, Long supplierId, int quantity,
                                  java.math.BigDecimal purchasePrice,
                                  LocalDate productionDate, Integer shelfLifeDays) {
        if (shelfLifeDays == null || shelfLifeDays <= 0) {
            return;
        }
        ProductBatch batch = new ProductBatch();
        batch.setBatchNo("B" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%04d", (int) (Math.random() * 10000)));
        batch.setProductId(productId);
        batch.setSupplierId(supplierId);
        batch.setQuantity(quantity);
        batch.setPurchasePrice(purchasePrice);
        batch.setShelfLifeDays(shelfLifeDays);
        batch.setExpireDate((productionDate != null ? productionDate : LocalDate.now()).plusDays(shelfLifeDays));
        batchRepository.save(batch);
    }

    /** 全部批次（按到期日升序） */
    @Transactional(readOnly = true)
    public List<BatchVO> list(Long productId, String status) {
        List<ProductBatch> batches = productId == null
                ? batchRepository.findByQuantityGreaterThanOrderByExpireDateAsc(0)
                : batchRepository.findByProductIdOrderByExpireDateAsc(productId);
        return batches.stream().filter(b -> status == null || status.isBlank() || status.equals(b.expiryStatus()))
                .map(this::toVO).toList();
    }

    /** 临期预警：已过期 / ≤7天 / ≤30天 */
    @Transactional(readOnly = true)
    public Map<String, List<BatchVO>> expiryAlerts() {
        LocalDate today = LocalDate.now();
        List<ProductBatch> all = batchRepository.findByQuantityGreaterThanOrderByExpireDateAsc(0);
        Map<String, List<BatchVO>> result = new LinkedHashMap<>();
        result.put("expired", all.stream().filter(b -> b.expiryStatus().equals("EXPIRED")).map(this::toVO).toList());
        result.put("urgent", all.stream().filter(b -> b.expiryStatus().equals("URGENT")).map(this::toVO).toList());
        result.put("near", all.stream().filter(b -> b.expiryStatus().equals("NEAR")).map(this::toVO).toList());
        return result;
    }

    /** 临期/过期处置：扣减批次与总库存 */
    @Transactional
    public void dispose(Long batchId, DisposeRequest request) {
        ProductBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new BusinessException(404, "批次不存在"));
        if (request.getQuantity() > batch.getQuantity()) {
            throw new BusinessException("处置数量超过批次剩余数量（剩余 " + batch.getQuantity() + "）");
        }
        batch.setQuantity(batch.getQuantity() - request.getQuantity());
        batchRepository.save(batch);

        inventoryService.adjust(batch.getProductId(), -request.getQuantity(),
                InventoryTransaction.TYPE_LOSS_OUT, "BATCH", batch.getBatchNo(),
                "临期/过期处置：" + request.getReason());
    }

    private BatchVO toVO(ProductBatch b) {
        BatchVO vo = new BatchVO();
        vo.setId(b.getId());
        vo.setBatchNo(b.getBatchNo());
        vo.setProductId(b.getProductId());
        vo.setSupplierId(b.getSupplierId());
        vo.setQuantity(b.getQuantity());
        vo.setProductionDate(b.getProductionDate() == null ? null : b.getProductionDate().toString());
        vo.setShelfLifeDays(b.getShelfLifeDays());
        vo.setExpireDate(b.getExpireDate() == null ? null : b.getExpireDate().toString());
        vo.setDaysToExpire(b.daysToExpire());
        vo.setExpiryStatus(b.expiryStatus());
        vo.setPurchasePrice(b.getPurchasePrice());
        vo.setRemark(b.getRemark());
        vo.setCreatedAt(b.getCreatedAt() == null ? null : b.getCreatedAt().toString());
        productRepository.findById(b.getProductId()).ifPresent(p -> {
            vo.setProductName(p.getName());
            vo.setSku(p.getSku());
            vo.setUnit(p.getUnit());
        });
        if (b.getSupplierId() != null) {
            vo.setSupplierName(supplierRepository.findById(b.getSupplierId())
                    .map(Supplier::getName).orElse(null));
        }
        return vo;
    }
}
