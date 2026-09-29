package com.muxu.supermarket.product.service;

import com.muxu.supermarket.common.BusinessException;
import com.muxu.supermarket.product.dto.CategoryRequest;
import com.muxu.supermarket.product.dto.CategoryVO;
import com.muxu.supermarket.product.entity.ProductCategory;
import com.muxu.supermarket.product.repository.ProductCategoryRepository;
import com.muxu.supermarket.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final ProductCategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<CategoryVO> list() {
        return categoryRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(c -> CategoryVO.from(c, productRepository.countByCategoryId(c.getId())))
                .toList();
    }

    @Transactional
    public CategoryVO create(CategoryRequest request) {
        String name = request.getName().trim();
        if (categoryRepository.existsByName(name)) {
            throw new BusinessException("分类名称已存在: " + name);
        }
        ProductCategory category = new ProductCategory();
        category.setName(name);
        category.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        category.setStatus(request.getStatus() == null ? ProductCategory.STATUS_ENABLED : request.getStatus());
        return CategoryVO.from(categoryRepository.save(category), 0);
    }

    @Transactional
    public CategoryVO update(Long id, CategoryRequest request) {
        ProductCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "分类不存在: id=" + id));
        String name = request.getName().trim();
        if (categoryRepository.existsByNameAndIdNot(name, id)) {
            throw new BusinessException("分类名称已存在: " + name);
        }
        category.setName(name);
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }
        if (request.getStatus() != null) {
            category.setStatus(request.getStatus());
        }
        return CategoryVO.from(categoryRepository.save(category),
                productRepository.countByCategoryId(id));
    }

    @Transactional
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new BusinessException(404, "分类不存在: id=" + id);
        }
        long productCount = productRepository.countByCategoryId(id);
        if (productCount > 0) {
            throw new BusinessException("该分类下存在 " + productCount + " 个商品，无法删除");
        }
        categoryRepository.deleteById(id);
    }
}
