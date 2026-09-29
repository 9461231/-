package com.muxu.supermarket.audit;

import com.muxu.supermarket.audit.entity.AuditLog;
import com.muxu.supermarket.audit.repository.AuditLogRepository;
import com.muxu.supermarket.common.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 审计切面：自动记录所有业务 Controller 的写操作（POST/PUT/DELETE）
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;

    @Around("execution(* com.muxu.supermarket..controller..*(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attrs == null ? null : attrs.getRequest();
        String method = request == null ? "" : request.getMethod();
        boolean writeOp = "POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method);

        Object result = joinPoint.proceed();

        if (writeOp && request != null) {
            try {
                save(request, joinPoint);
            } catch (Exception e) {
                log.warn("审计记录失败: {}", e.getMessage());
            }
        }
        return result;
    }

    private void save(HttpServletRequest request, ProceedingJoinPoint joinPoint) {
        CurrentUser.UserInfo user = CurrentUser.get();
        AuditLog auditLog = new AuditLog();
        if (user != null) {
            auditLog.setUserId(user.userId());
            auditLog.setUsername(user.username());
            auditLog.setRole(user.role());
        }
        auditLog.setOperationType(request.getMethod());
        // 路径：/api/products/12 → 目标类型 PRODUCTS，ID 12
        String path = request.getRequestURI().replace("/api/", "");
        String[] parts = path.split("/");
        auditLog.setTargetType(parts.length > 0 ? parts[0].toUpperCase() : path);
        try {
            auditLog.setTargetId(parts.length > 1 ? Long.parseLong(parts[1]) : null);
        } catch (NumberFormatException ignore) {
            // 路径第二段不是 ID（如 /receipts、/audit 等）
        }
        Object[] args = joinPoint.getArgs();
        String summary = args.length > 0 ? String.valueOf(args[args.length - 1]) : "";
        auditLog.setAfterData(abbreviate(summary, 1000));
        String source = request.getHeader("X-Source");
        auditLog.setSource("AGENT".equalsIgnoreCase(source) ? AuditLog.SOURCE_AGENT
                : "IMPORT".equalsIgnoreCase(source) ? AuditLog.SOURCE_IMPORT : AuditLog.SOURCE_WEB);
        auditLogRepository.save(auditLog);
    }

    private String abbreviate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
