package com.muxu.supermarket.supplier.service;

import com.muxu.supermarket.common.BusinessException;
import com.muxu.supermarket.common.PageVO;
import com.muxu.supermarket.product.entity.Product;
import com.muxu.supermarket.product.repository.ProductRepository;
import com.muxu.supermarket.supplier.dto.*;
import com.muxu.supermarket.supplier.entity.Supplier;
import com.muxu.supermarket.supplier.entity.SupplierProduct;
import com.muxu.supermarket.supplier.repository.SupplierProductRepository;
import com.muxu.supermarket.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierProductRepository supplierProductRepository;
    private final ProductRepository productRepository;

    /** 分页查询：关键字（名称/联系人/手机号）+ 状态筛选 */
    @Transactional(readOnly = true)
    public PageVO<SupplierVO> page(int page, int size, String keyword, Integer status) {
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 10;

        Specification<Supplier> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("name"), like),
                        cb.like(root.get("contactPerson"), like),
                        cb.like(root.get("phone"), like)
                ));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<Supplier> result = supplierRepository.findAll(spec,
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "id")));

        Map<Long, Long> countMap = supplierProductRepository.findAll().stream()
                .collect(Collectors.groupingBy(SupplierProduct::getSupplierId, Collectors.counting()));

        List<SupplierVO> list = result.getContent().stream()
                .map(s -> SupplierVO.from(s, countMap.getOrDefault(s.getId(), 0L)))
                .toList();
        return new PageVO<>(list, result.getTotalElements(), page, size);
    }

    /** 下拉选项（全部合作中的供应商） */
    @Transactional(readOnly = true)
    public List<SupplierVO> options() {
        return supplierRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(s -> SupplierVO.from(s, 0))
                .toList();
    }

    @Transactional(readOnly = true)
    public SupplierVO detail(Long id) {
        Supplier supplier = getSupplier(id);
        return SupplierVO.from(supplier, supplierProductRepository.countBySupplierId(id));
    }

    @Transactional
    public SupplierVO create(SupplierRequest request) {
        String name = request.getName().trim();
        if (supplierRepository.existsByName(name)) {
            throw new BusinessException("供应商名称已存在: " + name);
        }
        Supplier supplier = new Supplier();
        applyRequest(supplier, request);
        return SupplierVO.from(supplierRepository.save(supplier), 0);
    }

    @Transactional
    public SupplierVO update(Long id, SupplierRequest request) {
        Supplier supplier = getSupplier(id);
        String name = request.getName().trim();
        if (supplierRepository.existsByNameAndIdNot(name, id)) {
            throw new BusinessException("供应商名称已存在: " + name);
        }
        applyRequest(supplier, request);
        return SupplierVO.from(supplierRepository.save(supplier),
                supplierProductRepository.countBySupplierId(id));
    }

    @Transactional
    public void delete(Long id) {
        if (!supplierRepository.existsById(id)) {
            throw new BusinessException(404, "供应商不存在: id=" + id);
        }
        if (supplierProductRepository.existsBySupplierId(id)) {
            throw new BusinessException("该供应商仍存在合作商品，请先解除绑定后再删除");
        }
        // Step 03 接入后此处需增加采购订单引用校验
        supplierRepository.deleteById(id);
    }

    // ---------------- 合作商品 ----------------

    @Transactional(readOnly = true)
    public List<SupplierProductVO> listProducts(Long supplierId) {
        getSupplier(supplierId);
        Map<Long, Product> productMap = productRepository.findAllById(
                        supplierProductRepository.findBySupplierId(supplierId).stream()
                                .map(SupplierProduct::getProductId).toList()).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        return supplierProductRepository.findBySupplierId(supplierId).stream()
                .map(sp -> {
                    SupplierProductVO vo = new SupplierProductVO();
                    vo.setProductId(sp.getProductId());
                    vo.setSupplyPrice(sp.getSupplyPrice());
                    vo.setIsPrimary(sp.getIsPrimary());
                    vo.setBoundAt(sp.getCreatedAt());
                    Product p = productMap.get(sp.getProductId());
                    if (p != null) {
                        vo.setProductName(p.getName());
                        vo.setSku(p.getSku());
                        vo.setUnit(p.getUnit());
                        vo.setPurchasePrice(p.getPurchasePrice());
                    }
                    return vo;
                }).toList();
    }

    @Transactional
    public SupplierProductVO bindProduct(Long supplierId, SupplierProductBindRequest request) {
        getSupplier(supplierId);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new BusinessException("商品不存在: id=" + request.getProductId()));
        if (supplierProductRepository.findBySupplierIdAndProductId(supplierId, request.getProductId()).isPresent()) {
            throw new BusinessException("该商品已与此供应商绑定");
        }
        SupplierProduct sp = new SupplierProduct();
        sp.setSupplierId(supplierId);
        sp.setProductId(product.getId());
        sp.setSupplyPrice(request.getSupplyPrice());
        sp.setIsPrimary(Boolean.TRUE.equals(request.getIsPrimary()));
        supplierProductRepository.save(sp);

        SupplierProductVO vo = new SupplierProductVO();
        vo.setProductId(product.getId());
        vo.setProductName(product.getName());
        vo.setSku(product.getSku());
        vo.setUnit(product.getUnit());
        vo.setPurchasePrice(product.getPurchasePrice());
        vo.setSupplyPrice(sp.getSupplyPrice());
        vo.setIsPrimary(sp.getIsPrimary());
        vo.setBoundAt(sp.getCreatedAt());
        return vo;
    }

    @Transactional
    public void updateSupplyPrice(Long supplierId, Long productId, java.math.BigDecimal supplyPrice) {
        SupplierProduct sp = supplierProductRepository
                .findBySupplierIdAndProductId(supplierId, productId)
                .orElseThrow(() -> new BusinessException(404, "绑定关系不存在"));
        if (supplyPrice == null || supplyPrice.signum() < 0) {
            throw new BusinessException("供货价不能为负数");
        }
        sp.setSupplyPrice(supplyPrice);
        supplierProductRepository.save(sp);
    }

    @Transactional
    public void unbindProduct(Long supplierId, Long productId) {
        SupplierProduct sp = supplierProductRepository
                .findBySupplierIdAndProductId(supplierId, productId)
                .orElseThrow(() -> new BusinessException(404, "绑定关系不存在"));
        supplierProductRepository.delete(sp);
    }

    // ------------------------------------------------------------------

    private Supplier getSupplier(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "供应商不存在: id=" + id));
    }

    private void applyRequest(Supplier supplier, SupplierRequest request) {
        supplier.setName(request.getName().trim());
        supplier.setContactPerson(request.getContactPerson());
        supplier.setPhone(request.getPhone());
        supplier.setEmail(request.getEmail());
        supplier.setAddress(request.getAddress());
        supplier.setStatus(request.getStatus() == null ? Supplier.STATUS_ENABLED : request.getStatus());
        supplier.setRemark(request.getRemark());
    }
}
