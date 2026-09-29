package com.muxu.supermarket.product.service;

import com.muxu.supermarket.common.BusinessException;
import com.muxu.supermarket.common.PageVO;
import com.muxu.supermarket.product.dto.ProductRequest;
import com.muxu.supermarket.product.dto.ProductVO;
import com.muxu.supermarket.product.entity.PriceChangeRecord;
import com.muxu.supermarket.product.entity.Product;
import com.muxu.supermarket.product.entity.ProductCategory;
import com.muxu.supermarket.product.entity.ProductStatus;
import com.muxu.supermarket.product.repository.PriceChangeRecordRepository;
import com.muxu.supermarket.product.repository.ProductCategoryRepository;
import com.muxu.supermarket.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final PriceChangeRecordRepository priceChangeRecordRepository;

    /**
     * 分页查询：支持关键字搜索（名称/SKU/条码/品牌）、分类筛选、在售/停售筛选
     */
    @Transactional(readOnly = true)
    public PageVO<ProductVO> page(int page, int size, String keyword, Long categoryId, Integer status) {
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 10;

        Specification<Product> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("name"), like),
                        cb.like(root.get("sku"), like),
                        cb.like(root.get("barcode"), like),
                        cb.like(root.get("brand"), like)
                ));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("categoryId"), categoryId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Product> result = productRepository.findAll(spec, pageable);

        Map<Long, String> categoryNameMap = loadCategoryNameMap();
        List<ProductVO> list = result.getContent().stream()
                .map(p -> ProductVO.from(p, categoryNameMap.get(p.getCategoryId())))
                .toList();
        return new PageVO<>(list, result.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public ProductVO detail(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "商品不存在: id=" + id));
        return ProductVO.from(product, findCategoryName(product.getCategoryId()));
    }

    @Transactional
    public ProductVO create(ProductRequest request) {
        validateCategory(request.getCategoryId());
        validateMinMaxStock(request.getMinStock(), request.getMaxStock());
        checkSkuUnique(request.getSku(), null);
        checkBarcodeUnique(request.getBarcode(), null);

        Product product = new Product();
        applyRequest(product, request);
        return ProductVO.from(productRepository.save(product),
                findCategoryName(product.getCategoryId()));
    }

    @Transactional
    public ProductVO update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "商品不存在: id=" + id));
        validateCategory(request.getCategoryId());
        validateMinMaxStock(request.getMinStock(), request.getMaxStock());
        validatePrices(request);
        checkSkuUnique(request.getSku(), id);
        checkBarcodeUnique(request.getBarcode(), id);

        // 价格变更记录（审计）
        recordPriceChange(product, "salePrice", product.getSalePrice(), request.getSalePrice());
        recordPriceChange(product, "purchasePrice", product.getPurchasePrice(), request.getPurchasePrice());
        recordPriceChange(product, "memberPrice", product.getMemberPrice(), request.getMemberPrice());

        applyRequest(product, request);
        return ProductVO.from(productRepository.save(product),
                findCategoryName(product.getCategoryId()));
    }

    private void validatePrices(ProductRequest request) {
        if (request.getMemberPrice() != null
                && request.getSalePrice() != null
                && request.getMemberPrice().compareTo(request.getSalePrice()) > 0) {
            throw new BusinessException("会员价不能高于销售价");
        }
        if (request.getMinSalePrice() != null
                && request.getSalePrice() != null
                && request.getMinSalePrice().compareTo(request.getSalePrice()) > 0) {
            throw new BusinessException("最低销售价不能高于销售价");
        }
    }

    private void recordPriceChange(Product product, String field,
                                   java.math.BigDecimal oldV, java.math.BigDecimal newV) {
        if (oldV == null && newV == null) {
            return;
        }
        if (oldV != null && newV != null && oldV.compareTo(newV) == 0) {
            return;
        }
        PriceChangeRecord record = new PriceChangeRecord();
        record.setProductId(product.getId());
        record.setFieldName(field);
        record.setOldValue(oldV);
        record.setNewValue(newV);
        var user = com.muxu.supermarket.common.CurrentUser.get();
        record.setChangedBy(user == null ? "system" : user.username());
        record.setSource(user != null && "AI_AGENT".equals(user.username()) ? "AGENT" : "WEB");
        priceChangeRecordRepository.save(record);
    }

    @Transactional(readOnly = true)
    public List<PriceChangeRecord> priceHistory(Long productId) {
        return priceChangeRecordRepository.findTop50ByProductIdOrderByIdDesc(productId);
    }

    /** Excel 批量导入：逐行校验，返回成功/跳过数量 */
    @Transactional(readOnly = true)
    public List<Product> exportAll() {
        return productRepository.findAll();
    }

    @Transactional
    public Map<String, Integer> importProducts(List<ProductRequest> requests) {
        int success = 0;
        int skipped = 0;
        for (ProductRequest req : requests) {
            try {
                if (req.getSku() == null || productRepository.existsBySku(req.getSku().trim())) {
                    skipped++;
                    continue;
                }
                if (!categoryRepository.existsById(req.getCategoryId())) {
                    skipped++;
                    continue;
                }
                Product product = new Product();
                applyRequest(product, req);
                productRepository.save(product);
                success++;
            } catch (Exception e) {
                skipped++;
            }
        }
        return Map.of("success", success, "skipped", skipped);
    }

    @Transactional
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new BusinessException(404, "商品不存在: id=" + id);
        }
        // Step 01 阶段无库存/采购/销售数据；后续模块接入后此处需增加引用校验
        productRepository.deleteById(id);
    }

    // ------------------------------------------------------------------

    private void applyRequest(Product product, ProductRequest request) {
        product.setCategoryId(request.getCategoryId());
        product.setSku(request.getSku().trim());
        product.setBarcode(StringUtils.hasText(request.getBarcode()) ? request.getBarcode().trim() : null);
        product.setName(request.getName().trim());
        product.setBrand(StringUtils.hasText(request.getBrand()) ? request.getBrand().trim() : null);
        product.setSpec(StringUtils.hasText(request.getSpec()) ? request.getSpec().trim() : null);
        product.setUnit(request.getUnit().trim());
        product.setPurchasePrice(request.getPurchasePrice());
        product.setSalePrice(request.getSalePrice());
        product.setMemberPrice(request.getMemberPrice());
        product.setMinSalePrice(request.getMinSalePrice());
        product.setMinStock(request.getMinStock());
        product.setMaxStock(request.getMaxStock());
        product.setStatus(ProductStatus.isValid(request.getStatus())
                ? request.getStatus() : ProductStatus.ON_SALE.getCode());
        product.setRemark(request.getRemark());
    }

    private void validateCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new BusinessException("商品分类不存在: id=" + categoryId);
        }
    }

    private void validateMinMaxStock(Integer minStock, Integer maxStock) {
        if (minStock != null && maxStock != null && maxStock < minStock) {
            throw new BusinessException("最高库存不能小于最低库存");
        }
    }

    private void checkSkuUnique(String sku, Long excludeId) {
        String value = sku.trim();
        boolean exists = excludeId == null
                ? productRepository.existsBySku(value)
                : productRepository.existsBySkuAndIdNot(value, excludeId);
        if (exists) {
            throw new BusinessException("SKU 编码已存在: " + value);
        }
    }

    private void checkBarcodeUnique(String barcode, Long excludeId) {
        if (!StringUtils.hasText(barcode)) {
            return;
        }
        String value = barcode.trim();
        boolean exists = excludeId == null
                ? productRepository.existsByBarcode(value)
                : productRepository.existsByBarcodeAndIdNot(value, excludeId);
        if (exists) {
            throw new BusinessException("商品条码已存在: " + value);
        }
    }

    private Map<Long, String> loadCategoryNameMap() {
        return categoryRepository.findAll().stream()
                .collect(Collectors.toMap(ProductCategory::getId,
                        ProductCategory::getName, (a, b) -> a));
    }

    private String findCategoryName(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .map(ProductCategory::getName)
                .orElse("未知分类");
    }
}
