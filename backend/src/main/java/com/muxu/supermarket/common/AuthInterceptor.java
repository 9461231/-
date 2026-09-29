package com.muxu.supermarket.common;

import com.muxu.supermarket.auth.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 认证与角色校验拦截器：
 * - 除白名单外，所有 /api/** 请求必须携带 Bearer Token
 * - 标注 @RequireRoles 的接口按角色校验
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @org.springframework.beans.factory.annotation.Value("${app.internal-token:agent-internal-2026}")
    private String internalTokenValue;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            writeError(response, 401, "未登录或令牌缺失");
            return false;
        }
        String token = auth.substring(7);

        // Agent 服务内部调用：固定服务令牌，视为 SYSTEM 账号（与业务 API 权限一致）
        String internalToken = internalTokenValue;
        if (internalToken != null && !internalToken.isBlank() && internalToken.equals(token)) {
            CurrentUser.set(new CurrentUser.UserInfo(0L, "AI_AGENT", "SYSTEM", null));
            return true;
        }
        Claims claims = jwtUtil.parse(token);
        if (claims == null) {
            writeError(response, 401, "令牌无效或已过期");
            return false;
        }
        CurrentUser.UserInfo user = new CurrentUser.UserInfo(
                claims.get("uid", Long.class),
                claims.getSubject(),
                claims.get("role", String.class),
                claims.get("storeId", Long.class));
        CurrentUser.set(user);

        RequireRoles required = handlerMethod.getMethodAnnotation(RequireRoles.class);
        if (required == null) {
            required = handlerMethod.getBeanType().getAnnotation(RequireRoles.class);
        }
        if (required != null && !user.hasRole(required.value())) {
            CurrentUser.clear();
            writeError(response, 403, "无权限执行该操作");
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUser.clear();
    }

    private void writeError(HttpServletResponse response, int code, String message) throws Exception {
        response.setStatus(200);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + code + ",\"message\":\"" + message + "\",\"data\":null}");
    }
}
