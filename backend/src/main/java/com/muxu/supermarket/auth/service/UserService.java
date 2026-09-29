package com.muxu.supermarket.auth.service;

import com.muxu.supermarket.auth.dto.AuthDTOs.StoreRequest;
import com.muxu.supermarket.auth.dto.AuthDTOs.UserRequest;
import com.muxu.supermarket.auth.dto.AuthDTOs.UserVO;
import com.muxu.supermarket.auth.entity.Store;
import com.muxu.supermarket.auth.entity.SysUser;
import com.muxu.supermarket.auth.repository.StoreRepository;
import com.muxu.supermarket.auth.repository.SysUserRepository;
import com.muxu.supermarket.common.BusinessException;
import com.muxu.supermarket.common.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserRepository userRepository;
    private final StoreRepository storeRepository;

    // ==================== 员工 / 用户管理 ====================

    /**
     * 分页查询用户。店家只能看本店员工；超级管理员可看全部。
     */
    @Transactional(readOnly = true)
    public com.muxu.supermarket.common.PageVO<UserVO> page(int page, int size, String keyword, String role) {
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 10;
        CurrentUser.UserInfo current = CurrentUser.get();

        Specification<SysUser> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (current != null && SysUser.ROLE_SUPER_ADMIN.equals(current.role())) {
                if (StringUtils.hasText(keyword)) {
                    String like = "%" + keyword.trim() + "%";
                    predicates.add(cb.or(
                            cb.like(root.get("username"), like),
                            cb.like(root.get("realName"), like)));
                }
                if (StringUtils.hasText(role)) {
                    predicates.add(cb.equal(root.get("role"), role));
                }
            } else {
                predicates.add(cb.equal(root.get("storeId"), current == null ? null : current.storeId()));
                predicates.add(cb.notEqual(root.get("role"), SysUser.ROLE_SUPER_ADMIN));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        Page<SysUser> result = userRepository.findAll(spec,
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "id")));
        return toPageVO(result);
    }

    @Transactional
    public UserVO create(UserRequest request) {
        CurrentUser.UserInfo current = CurrentUser.get();
        if (!StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
            throw new BusinessException("用户名与密码不能为空");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("用户名已存在: " + request.getUsername());
        }
        validateRole(request.getRole());
        SysUser user = new SysUser();
        user.setUsername(request.getUsername().trim());
        user.setPassword(AuthService.ENCODER.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setRole(request.getRole());
        // 店家只能创建本店员工
        user.setStoreId(SysUser.ROLE_SUPER_ADMIN.equals(request.getRole()) ? null
                : (current != null && current.storeId() != null ? current.storeId() : request.getStoreId()));
        user.setStatus(1);
        return toVO(userRepository.save(user));
    }

    @Transactional
    public UserVO update(Long id, UserRequest request) {
        SysUser user = getUser(id);
        if (StringUtils.hasText(request.getRealName())) {
            user.setRealName(request.getRealName());
        }
        if (StringUtils.hasText(request.getRole())) {
            validateRole(request.getRole());
            user.setRole(request.getRole());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        return toVO(userRepository.save(user));
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 8) {
            throw new BusinessException("新密码长度至少 8 位");
        }
        SysUser user = getUser(id);
        user.setPassword(AuthService.ENCODER.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void updateStatus(Long id, Integer status) {
        SysUser user = getUser(id);
        if (SysUser.ROLE_SUPER_ADMIN.equals(user.getRole())) {
            throw new BusinessException("不能禁用超级管理员");
        }
        user.setStatus(status != null && status == 1 ? 1 : 0);
        userRepository.save(user);
    }

    // ==================== 店家 / 租户管理（超级管理员） ====================

    @Transactional(readOnly = true)
    public com.muxu.supermarket.common.PageVO<Map<String, Object>> pageStores(int page, int size, String keyword) {
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 10;
        Specification<Store> spec = (root, query, cb) -> {
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim() + "%";
                return cb.or(cb.like(root.get("name"), like), cb.like(root.get("contactPerson"), like));
            }
            return cb.conjunction();
        };
        Page<Store> result = storeRepository.findAll(spec,
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id")));
        List<Map<String, Object>> list = result.getContent().stream().map(s -> {
            Map<String, Object> vo = new java.util.LinkedHashMap<String, Object>();
            vo.put("id", s.getId());
            vo.put("name", s.getName());
            vo.put("contactPerson", s.getContactPerson());
            vo.put("phone", s.getPhone());
            vo.put("address", s.getAddress());
            vo.put("status", s.getStatus());
            vo.put("userCount", userRepository.countByStoreId(s.getId()));
            vo.put("createdAt", s.getCreatedAt().toString());
            return vo;
        }).toList();
        return new com.muxu.supermarket.common.PageVO<>(list, result.getTotalElements(), page, size);
    }

    @Transactional
    public Map<String, Object> createStore(StoreRequest request) {
        Store store = new Store();
        applyStore(store, request);
        store = storeRepository.save(store);
        Map<String, Object> vo = new java.util.LinkedHashMap<String, Object>();
        vo.put("id", store.getId());
        vo.put("name", store.getName());
        vo.put("status", store.getStatus());
        return vo;
    }

    @Transactional
    public void updateStore(Long id, StoreRequest request) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "店家不存在"));
        applyStore(store, request);
        storeRepository.save(store);
    }

    /** 店家注册（自助）：创建店家 + 店主账号，状态待审核 */
    @Transactional
    public Map<String, Object> registerStore(StoreRequest request, String ownerUsername, String ownerPassword) {
        if (userRepository.existsByUsername(ownerUsername)) {
            throw new BusinessException("用户名已存在: " + ownerUsername);
        }
        Store store = new Store();
        applyStore(store, request);
        store.setStatus(Store.STATUS_PENDING);
        store = storeRepository.save(store);

        SysUser owner = new SysUser();
        owner.setUsername(ownerUsername);
        owner.setPassword(AuthService.ENCODER.encode(ownerPassword));
        owner.setRealName(request.getContactPerson());
        owner.setRole(SysUser.ROLE_STORE_OWNER);
        owner.setStoreId(store.getId());
        owner.setStatus(1);
        userRepository.save(owner);

        Map<String, Object> vo = new java.util.LinkedHashMap<String, Object>();
        vo.put("storeId", store.getId());
        vo.put("status", "待平台管理员审核");
        return vo;
    }

    /** 平台统计（超级管理员） */
    @Transactional(readOnly = true)
    public Map<String, Object> platformStats() {
        Map<String, Object> stats = new java.util.LinkedHashMap<String, Object>();
        stats.put("storeTotal", storeRepository.count());
        stats.put("storeActive", storeRepository.findAll().stream()
                .filter(s -> s.getStatus() == Store.STATUS_ACTIVE).count());
        stats.put("userTotal", userRepository.count());
        return stats;
    }

    // ------------------------------------------------------------------

    private void applyStore(Store store, StoreRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new BusinessException("店家名称不能为空");
        }
        store.setName(request.getName().trim());
        store.setContactPerson(request.getContactPerson());
        store.setPhone(request.getPhone());
        store.setAddress(request.getAddress());
        if (request.getStatus() != null) {
            store.setStatus(request.getStatus());
        }
    }

    private void validateRole(String role) {
        if (SysUser.ROLE_SUPER_ADMIN.equals(role)) {
            throw new BusinessException("不允许创建超级管理员账号");
        }
        if (!List.of(SysUser.ROLE_STORE_OWNER, SysUser.ROLE_STORE_MANAGER, SysUser.ROLE_CASHIER,
                SysUser.ROLE_PURCHASER, SysUser.ROLE_WAREHOUSE).contains(role)) {
            throw new BusinessException("角色不正确: " + role);
        }
    }

    private SysUser getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "用户不存在: id=" + id));
    }

    private com.muxu.supermarket.common.PageVO<UserVO> toPageVO(Page<SysUser> result) {
        Map<Long, String> storeNames = storeRepository.findAll().stream()
                .collect(Collectors.toMap(Store::getId, Store::getName, (a, b) -> a));
        List<UserVO> list = result.getContent().stream()
                .map(u -> {
                    UserVO vo = toVO(u);
                    vo.setStoreName(u.getStoreId() == null ? "平台" : storeNames.get(u.getStoreId()));
                    return vo;
                }).toList();
        return new com.muxu.supermarket.common.PageVO<>(list, result.getTotalElements(),
                result.getNumber() + 1, result.getSize());
    }

    private UserVO toVO(SysUser u) {
        UserVO vo = new UserVO();
        vo.setId(u.getId());
        vo.setUsername(u.getUsername());
        vo.setRealName(u.getRealName());
        vo.setRole(u.getRole());
        vo.setRoleLabel(AuthService.roleLabel(u.getRole()));
        vo.setStoreId(u.getStoreId());
        vo.setStatus(u.getStatus());
        vo.setCreatedAt(u.getCreatedAt() == null ? null : u.getCreatedAt().toString());
        return vo;
    }
}
