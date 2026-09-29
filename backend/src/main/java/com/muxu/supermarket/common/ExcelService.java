package com.muxu.supermarket.common;

import com.muxu.supermarket.product.dto.ProductRequest;
import com.muxu.supermarket.product.entity.Product;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel 导入 / 导出（基于 Apache POI）
 */
@Slf4j
@Service
public class ExcelService {

    private static final String[] PRODUCT_HEADERS = {
            "商品名称", "SKU", "条码", "分类ID", "品牌", "规格", "单位",
            "采购价", "销售价", "会员价", "最低销售价", "最低库存", "最高库存", "备注"
    };

    /** 导出商品 */
    public byte[] exportProducts(List<Product> products) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("商品数据");
            CellStyle headerStyle = wb.createCellStyle();
            Font bold = wb.createFont();
            bold.setBold(true);
            headerStyle.setFont(bold);

            Row header = sheet.createRow(0);
            for (int i = 0; i < PRODUCT_HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(PRODUCT_HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 14 * 256);
            }
            int r = 1;
            for (Product p : products) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(p.getName());
                row.createCell(1).setCellValue(p.getSku());
                row.createCell(2).setCellValue(p.getBarcode() == null ? "" : p.getBarcode());
                row.createCell(3).setCellValue(p.getCategoryId());
                row.createCell(4).setCellValue(p.getBrand() == null ? "" : p.getBrand());
                row.createCell(5).setCellValue(p.getSpec() == null ? "" : p.getSpec());
                row.createCell(6).setCellValue(p.getUnit());
                row.createCell(7).setCellValue(p.getPurchasePrice() == null ? 0 : p.getPurchasePrice().doubleValue());
                row.createCell(8).setCellValue(p.getSalePrice() == null ? 0 : p.getSalePrice().doubleValue());
                row.createCell(9).setCellValue(p.getMemberPrice() == null ? 0 : p.getMemberPrice().doubleValue());
                row.createCell(10).setCellValue(p.getMinSalePrice() == null ? 0 : p.getMinSalePrice().doubleValue());
                row.createCell(11).setCellValue(p.getMinStock());
                row.createCell(12).setCellValue(p.getMaxStock() == null ? 0 : p.getMaxStock());
                row.createCell(13).setCellValue(p.getRemark() == null ? "" : p.getRemark());
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("导出失败: " + e.getMessage());
        }
    }

    /** 导入商品：返回解析出的请求数据（由调用方校验保存） */
    public List<ProductRequest> parseProducts(MultipartFile file) {
        List<ProductRequest> list = new ArrayList<>();
        try (InputStream is = file.getInputStream(); Workbook wb = WorkbookFactory.create(is)) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String name = getString(row, 0);
                if (name == null || name.isBlank()) {
                    continue;
                }
                ProductRequest req = new ProductRequest();
                req.setName(name);
                req.setSku(getString(row, 1));
                req.setBarcode(getString(row, 2));
                req.setCategoryId((long) getInt(row, 3, 1));
                req.setBrand(getString(row, 4));
                req.setSpec(getString(row, 5));
                req.setUnit(getString(row, 6) == null ? "个" : getString(row, 6));
                req.setPurchasePrice(getDecimal(row, 7, BigDecimal.ZERO));
                req.setSalePrice(getDecimal(row, 8, BigDecimal.ZERO));
                req.setMemberPrice(getDecimal(row, 9, null) == null ? null : getDecimal(row, 9, null));
                req.setMinSalePrice(getDecimal(row, 10, null));
                req.setMinStock(getInt(row, 11, 0));
                req.setMaxStock(getInt(row, 12, 0) == 0 ? null : getInt(row, 12, 0));
                req.setRemark(getString(row, 13));
                list.add(req);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Excel 解析失败", e);
            throw new BusinessException("Excel 解析失败，请使用导出的模板格式");
        }
        return list;
    }

    /** 生成导入模板 */
    public byte[] productTemplate() {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("商品导入模板");
            Row header = sheet.createRow(0);
            for (int i = 0; i < PRODUCT_HEADERS.length; i++) {
                header.createCell(i).setCellValue(PRODUCT_HEADERS[i]);
                sheet.setColumnWidth(i, 16 * 256);
            }
            Row example = sheet.createRow(1);
            example.createCell(0).setCellValue("示例可乐 330ml");
            example.createCell(1).setCellValue("YL-0001");
            example.createCell(2).setCellValue("6901234500011");
            example.createCell(3).setCellValue(1);
            example.createCell(4).setCellValue("可口可乐");
            example.createCell(5).setCellValue("330ml");
            example.createCell(6).setCellValue("罐");
            example.createCell(7).setCellValue(2.5);
            example.createCell(8).setCellValue(3.5);
            example.createCell(11).setCellValue(10);
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("模板生成失败: " + e.getMessage());
        }
    }

    public static String timestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private String getString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((long) cell.getNumericCellValue());
        }
        return cell.getStringCellValue().trim();
    }

    private int getInt(Row row, int col, int def) {
        Cell cell = row.getCell(col);
        if (cell == null || cell.getCellType() != CellType.NUMERIC) {
            return def;
        }
        return (int) cell.getNumericCellValue();
    }

    private BigDecimal getDecimal(Row row, int col, BigDecimal def) {
        Cell cell = row.getCell(col);
        if (cell == null) {
            return def;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        try {
            return new BigDecimal(cell.getStringCellValue().trim());
        } catch (Exception e) {
            return def;
        }
    }
}
