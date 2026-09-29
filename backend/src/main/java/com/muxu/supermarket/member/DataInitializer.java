package com.muxu.supermarket.member;

import com.muxu.supermarket.member.entity.MemberLevel;
import com.muxu.supermarket.member.repository.MemberLevelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 基础数据初始化：会员等级（仅当为空时初始化一次）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final MemberLevelRepository levelRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (levelRepository.count() > 0) {
            return;
        }
        levelRepository.save(level("普通会员", "0", 1));
        levelRepository.save(level("银卡会员", "1000", 2));
        levelRepository.save(level("金卡会员", "5000", 3));
        levelRepository.save(level("钻石会员", "20000", 4));
        log.info("会员等级基础数据初始化完成");
    }

    private MemberLevel level(String name, String threshold, int sort) {
        MemberLevel level = new MemberLevel();
        level.setName(name);
        level.setPointsThreshold(new BigDecimal(threshold));
        level.setDiscount(BigDecimal.ONE);
        level.setSortOrder(sort);
        return level;
    }
}
