package com.muxu.supermarket.product.entity;

import lombok.Getter;

/**
 * 商品生命周期：草稿 → 在售 → 停售 → 归档
 */
@Getter
public enum ProductStatus {

    OFF_SALE(0, "停售"),
    ON_SALE(1, "在售"),
    DRAFT(2, "草稿"),
    ARCHIVED(3, "归档");

    private final int code;
    private final String label;

    ProductStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static boolean isValid(Integer code) {
        if (code == null) {
            return false;
        }
        for (ProductStatus s : values()) {
            if (s.code == code) {
                return true;
            }
        }
        return false;
    }

    public static String label(Integer code) {
        for (ProductStatus s : values()) {
            if (s.code == (code == null ? -1 : code)) {
                return s.label;
            }
        }
        return "未知";
    }
}
