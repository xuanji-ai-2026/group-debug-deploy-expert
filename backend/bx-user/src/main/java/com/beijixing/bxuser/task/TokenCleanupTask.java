package com.beijixing.bxuser.task;

import com.beijixing.bxuser.mapper.RefreshTokenMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupTask {

    private final RefreshTokenMapper refreshTokenMapper;

    @Scheduled(cron = "0 0 * * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("开始清理过期Token...");
        try {
            LocalDateTime now = LocalDateTime.now();
            refreshTokenMapper.deleteExpiredTokens(now);
            log.info("过期Token清理完成");
        } catch (Exception e) {
            log.error("清理过期Token任务执行失败", e);
        }
    }
}