package com.muxu.supermarket.product.controller;

import com.muxu.supermarket.common.ApiResponse;
import com.muxu.supermarket.common.ExcelService;
import com.muxu.supermarket.common.PageVO;
import com.muxu.supermarket.product.dto.ProductRequest;
import com.muxu.supermarket.product.dto.ProductVO;
import com.muxu.supermarket.product.entity.PriceChangeRecord;
import com.muxu.supermarket.product.entity.Product;
import com.muxu.supermarket.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ExcelService excelService;

    /**
     * 商品分页查询
     * 参数：page（从1开始）、size、keyword（名称/SKU/条码/品牌）、categoryId、status（1-在售 0-停售）
     */
    @GetMapping
    public ApiResponse<PageVO<ProductVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.ok(productService.page(page, size, keyword, categoryId, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(productService.detail(id));
    }

    @PostMapping
    public ApiResponse<ProductVO> create(@Valid @RequestBody ProductRequest request) {
        return ApiResponse.ok(productService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductVO> update(@PathVariable Long id,
                                         @Valid @RequestBody ProductRequest request) {
        return ApiResponse.ok(productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ApiResponse.ok();
    }

    /** 价格变更记录 */
    @GetMapping("/{id}/price-changes")
    public ApiResponse<List<PriceChangeRecord>> priceHistory(@PathVariable Long id) {
        return ApiResponse.ok(productService.priceHistory(id));
    }

    // ==================== 数据导入导出 ====================

    @GetMapping("/export")
    public ResponseEntity<byte[]> export() {
        List<Product> products = productService.exportAll();
        byte[] bytes = excelService.exportProducts(products);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=products_" + ExcelService.timestamp() + ".xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template() {
        byte[] bytes = excelService.productTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=product_template.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @PostMapping("/import")
    public ApiResponse<Map<String, Integer>> importProducts(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(productService.importProducts(excelService.parseProducts(file)));
    }
}
