package com.muxu.supermarket.common;

import lombok.Getter;

/**
 * 业务异常：由全局异常处理器统一转换为统一响应结构
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        this(400, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
