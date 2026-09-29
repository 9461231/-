package com.muxu.supermarket.auth.service;

import com.muxu.supermarket.auth.JwtUtil;
import com.muxu.supermarket.auth.dto.AuthDTOs.*;
import com.muxu.supermarket.auth.entity.Store;
import com.muxu.supermarket.auth.entity.SysUser;
import com.muxu.supermarket.auth.repository.StoreRepository;
import com.muxu.supermarket.auth.repository.SysUserRepository;
import com.muxu.supermarket.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    public static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private final SysUserRepository userRepository;
    private final StoreRepository storeRepository;
    private final JwtUtil jwtUtil;

    /** 初始化：超级管理员 + 演示店家/店家账号（仅首次） */
    @Transactional
    public void initIfNeeded() {
        if (userRepository.count() > 0) {
            return;
        }
        Store store = new Store();
        store.setName("演示超市（阳光店）");
        store.setContactPerson("王老板");
        store.setPhone("13800000001");
        store.setAddress("某市某区某街道 88 号");
        store.setStatus(Store.STATUS_ACTIVE);
        store = storeRepository.save(store);

        createUser("admin", "Admin@123456", "超级管理员", SysUser.ROLE_SUPER_ADMIN, null);
        SysUser owner = createUser("store_demo", "Store@123456", "王老板", SysUser.ROLE_STORE_OWNER, store.getId());
        createUser("cashier01", "Cashier@123456", "收银员小李", SysUser.ROLE_CASHIER, store.getId());
        createUser("purchaser01", "Purchase@123456", "采购员老张", SysUser.ROLE_PURCHASER, store.getId());
        createUser("warehouse01", "Warehouse@123456", "库管员小刘", SysUser.ROLE_WAREHOUSE, store.getId());
        org.slf4j.LoggerFactory.getLogger(AuthService.class)
                .info("初始化账号完成：admin / store_demo（店家 {}）", owner.getStoreId());
    }

    private SysUser createUser(String username, String rawPassword, String realName, String role, Long storeId) {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(ENCODER.encode(rawPassword));
        user.setRealName(realName);
        user.setRole(role);
        user.setStoreId(storeId);
        user.setStatus(1);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public LoginVO login(LoginRequest request) {
        SysUser user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(401, "用户名或密码错误"));
        if (user.getStatus() != 1) {
            throw new BusinessException(403, "账号已被禁用");
        }
        if (!ENCODER.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        Store store = user.getStoreId() == null ? null
                : storeRepository.findById(user.getStoreId()).orElse(null);
        if (store != null && store.getStatus() == Store.STATUS_DISABLED) {
            throw new BusinessException(403, "店家已被停用，请联系平台管理员");
        }
        LoginVO vo = new LoginVO();
        vo.setToken(jwtUtil.generate(user.getId(), user.getUsername(), user.getRole(), user.getStoreId()));
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getRole());
        vo.setRoleLabel(roleLabel(user.getRole()));
        vo.setStoreId(user.getStoreId());
        vo.setStoreName(store == null ? "平台管理" : store.getName());
        return vo;
    }

    public static String roleLabel(String role) {
        return switch (role) {
            case SysUser.ROLE_SUPER_ADMIN -> "超级管理员";
            case SysUser.ROLE_STORE_OWNER -> "店主";
            case SysUser.ROLE_STORE_MANAGER -> "店长";
            case SysUser.ROLE_CASHIER -> "收银员";
            case SysUser.ROLE_PURCHASER -> "采购员";
            case SysUser.ROLE_WAREHOUSE -> "库管员";
            default -> role;
        };
    }

    @Transactional(readOnly = true)
    public List<String> allRoles() {
        return List.of(SysUser.ROLE_STORE_OWNER, SysUser.ROLE_STORE_MANAGER, SysUser.ROLE_CASHIER,
                SysUser.ROLE_PURCHASER, SysUser.ROLE_WAREHOUSE);
    }
}
