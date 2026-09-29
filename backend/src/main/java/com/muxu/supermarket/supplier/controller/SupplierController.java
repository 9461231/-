package com.muxu.supermarket.supplier.controller;

import com.muxu.supermarket.common.ApiResponse;
import com.muxu.supermarket.common.PageVO;
import com.muxu.supermarket.supplier.dto.*;
import com.muxu.supermarket.supplier.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;
    private final com.muxu.supermarket.purchase.service.PurchaseService purchaseService;

    /** 供应商履约统计（采购额/已付未付/准时率/退货率/评分） */
    @GetMapping("/{id}/stats")
    public ApiResponse<java.util.Map<String, Object>> stats(@PathVariable Long id) {
        return ApiResponse.ok(purchaseService.supplierStats(id));
    }

    /** 供应商比较：同一商品不同供应商价格与履约对比 */
    @GetMapping("/compare")
    public ApiResponse<java.util.List<java.util.Map<String, Object>>> compare(@RequestParam Long productId) {
        return ApiResponse.ok(purchaseService.compareSuppliers(productId));
    }

    @GetMapping
    public ApiResponse<PageVO<SupplierVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.ok(supplierService.page(page, size, keyword, status));
    }

    /** 下拉选项 */
    @GetMapping("/options")
    public ApiResponse<List<SupplierVO>> options() {
        return ApiResponse.ok(supplierService.options());
    }

    @GetMapping("/{id}")
    public ApiResponse<SupplierVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(supplierService.detail(id));
    }

    @PostMapping
    public ApiResponse<SupplierVO> create(@Valid @RequestBody SupplierRequest request) {
        return ApiResponse.ok(supplierService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<SupplierVO> update(@PathVariable Long id,
                                          @Valid @RequestBody SupplierRequest request) {
        return ApiResponse.ok(supplierService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        supplierService.delete(id);
        return ApiResponse.ok();
    }

    // ---------------- 合作商品 ----------------

    @GetMapping("/{id}/products")
    public ApiResponse<List<SupplierProductVO>> listProducts(@PathVariable Long id) {
        return ApiResponse.ok(supplierService.listProducts(id));
    }

    @PostMapping("/{id}/products")
    public ApiResponse<SupplierProductVO> bindProduct(@PathVariable Long id,
                                                      @Valid @RequestBody SupplierProductBindRequest request) {
        return ApiResponse.ok(supplierService.bindProduct(id, request));
    }

    @PutMapping("/{id}/products/{productId}")
    public ApiResponse<Void> updateSupplyPrice(@PathVariable Long id, @PathVariable Long productId,
                                               @RequestBody SupplierProductBindRequest request) {
        supplierService.updateSupplyPrice(id, productId, request.getSupplyPrice());
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}/products/{productId}")
    public ApiResponse<Void> unbindProduct(@PathVariable Long id, @PathVariable Long productId) {
        supplierService.unbindProduct(id, productId);
        return ApiResponse.ok();
    }
}
