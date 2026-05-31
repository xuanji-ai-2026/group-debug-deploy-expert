package com.beijixing.tenant.job;

import com.beijixing.tenant.service.TenantResourceQuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 配额重置定时任务
 * 每天凌晨0:05检查并重置到期的周期性配额
 *
 * @author bx-tenant
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QuotaResetJob {

    private final TenantResourceQuotaService quotaService;

    /**
     * 每天凌晨0:05执行配额重置
     */
    @Scheduled(cron = "0 5 0 * * ?")
    public void resetDailyQuotas() {
        log.info("===== 配额重置定时任务开始 =====");
        try {
            int count = quotaService.resetExpiredQuotas();
            log.info("===== 配额重置完成，共重置 {} 条 =====", count);
        } catch (Exception e) {
            log.error("配额重置任务执行失败", e);
        }
    }
}
