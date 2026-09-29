package com.muxu.supermarket.common;

import lombok.Data;

import java.util.List;

/**
 * 分页结果统一封装
 */
@Data
public class PageVO<T> {

    private List<T> list;
    private long total;
    private int page;
    private int size;

    public PageVO(List<T> list, long total, int page, int size) {
        this.list = list;
        this.total = total;
        this.page = page;
        this.size = size;
    }
}
