package com.muxu.supermarket.audit.controller;

import com.muxu.supermarket.audit.entity.AuditLog;
import com.muxu.supermarket.audit.repository.AuditLogRepository;
import com.muxu.supermarket.auth.entity.SysUser;
import com.muxu.supermarket.common.ApiResponse;
import com.muxu.supermarket.common.PageVO;
import com.muxu.supermarket.common.RequireRoles;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@RequireRoles({SysUser.ROLE_SUPER_ADMIN, SysUser.ROLE_STORE_OWNER, SysUser.ROLE_STORE_MANAGER})
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    public ApiResponse<PageVO<AuditLog>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String source) {
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 15;

        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (username != null && !username.isBlank()) {
                predicates.add(cb.like(root.get("username"), "%" + username.trim() + "%"));
            }
            if (targetType != null && !targetType.isBlank()) {
                predicates.add(cb.equal(root.get("targetType"), targetType.trim().toUpperCase()));
            }
            if (source != null && !source.isBlank()) {
                predicates.add(cb.equal(root.get("source"), source));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<AuditLog> result = auditLogRepository.findAll(spec,
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id")));
        return ApiResponse.ok(new PageVO<>(result.getContent(), result.getTotalElements(), page, size));
    }
}
