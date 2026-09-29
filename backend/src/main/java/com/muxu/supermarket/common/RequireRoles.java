package com.muxu.supermarket.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口角色限制：标注在 Controller 类或方法上，由 AuthInterceptor 校验
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRoles {

    /** 允许访问的角色，如 {"STORE_OWNER", "SUPER_ADMIN"} */
    String[] value();
}
