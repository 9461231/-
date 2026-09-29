package com.muxu.supermarket.auth;

import com.muxu.supermarket.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 首次启动初始化平台账号（admin / store_demo / 员工演示账号）
 */
@Component
@RequiredArgsConstructor
public class AuthInitializer implements ApplicationRunner {

    private final AuthService authService;

    @Override
    public void run(ApplicationArguments args) {
        authService.initIfNeeded();
    }
}
