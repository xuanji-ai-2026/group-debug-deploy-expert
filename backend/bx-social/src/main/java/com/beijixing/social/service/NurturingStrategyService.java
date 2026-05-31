package com.beijixing.social.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.beijixing.social.entity.NurturingStrategy;
import com.beijixing.social.entity.SocialAccount;
import com.beijixing.social.mapper.NurturingStrategyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 养号策略服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NurturingStrategyService extends ServiceImpl<NurturingStrategyMapper, NurturingStrategy> {

    private final AccountService accountService;

    @Autowired
    private NurturingExecutionEngine nurturingExecutionEngine;

    /** 获取账号的养号策略 */
    public NurturingStrategy getByAccountId(Long accountId) {
        LambdaQueryWrapper<NurturingStrategy> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NurturingStrategy::getAccountId, accountId);
        return getOne(wrapper);
    }

    /** 创建/更新养号策略 */
    public NurturingStrategy saveStrategy(NurturingStrategy strategy) {
        if (strategy.getId() != null) {
            updateById(strategy);
        } else {
            strategy.setCreateTime(LocalDateTime.now());
            strategy.setUpdateTime(LocalDateTime.now());
            strategy.setEnabled(1);
            save(strategy);
            // 更新账号养号状态
            SocialAccount account = accountService.getById(strategy.getAccountId());
            if (account != null) {
                account.setNurturingStatus(1);
                accountService.updateById(account);
            }
        }
        return strategy;
    }

    /** 启用策略 */
    public boolean enableStrategy(Long id) {
        NurturingStrategy strategy = getById(id);
        if (strategy == null) return false;
        strategy.setEnabled(1);
        return updateById(strategy);
    }

    /** 禁用策略 */
    public boolean disableStrategy(Long id) {
        NurturingStrategy strategy = getById(id);
        if (strategy == null) return false;
        strategy.setEnabled(0);
        return updateById(strategy);
    }

    /** 完成养号 */
    public void completeNurturing(Long accountId) {
        SocialAccount account = accountService.getById(accountId);
        if (account != null) {
            account.setNurturingStatus(2);
            accountService.updateById(account);
        }
        LambdaQueryWrapper<NurturingStrategy> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NurturingStrategy::getAccountId, accountId);
        NurturingStrategy strategy = getOne(wrapper);
        if (strategy != null) {
            strategy.setEnabled(0);
            updateById(strategy);
        }
        log.info("账号养号完成: accountId={}", accountId);
    }

    /** 定时检查养号策略执行情况 */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void checkNurturingStatus() {
        log.info("开始检查养号策略执行情况...");
        try {
            LambdaQueryWrapper<NurturingStrategy> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(NurturingStrategy::getEnabled, 1);
            List<NurturingStrategy> strategies = list(wrapper);

            for (NurturingStrategy strategy : strategies) {
                try {
                    log.info("检查养号策略: accountId={}, strategyId={}",
                            strategy.getAccountId(), strategy.getId());

                    // 获取执行进度
                    NurturingExecutionEngine.NurturingProgress progress =
                            nurturingExecutionEngine.getProgress(strategy.getId());

                    if (progress != null) {
                        SocialAccount account = accountService.getById(strategy.getAccountId());
                        if (account != null) {
                            double progressPercent = progress.getProgressPercentage() != null ?
                                    progress.getProgressPercentage() : 0.0;

                            log.info("养号进度: accountId={}, progress={}%, status={}",
                                    account.getId(), progressPercent, progress.getStatus());

                            if (progressPercent >= 100.0 &&
                                    "COMPLETED".equals(progress.getStatus())) {
                                completeNurturing(strategy.getAccountId());
                            }
                        }
                    } else {
                        // 如果没有正在执行的实例，启动执行
                        boolean started = nurturingExecutionEngine.startExecution(strategy.getId());
                        if (started) {
                            log.info("已启动养号策略执行: strategyId={}", strategy.getId());
                        }
                    }

                } catch (Exception e) {
                    log.error("检查养号策略失败: strategyId={}, error={}",
                            strategy.getId(), e.getMessage(), e);
                }
            }

            log.info("养号策略检查完成，共检查 {} 个策略", strategies.size());

        } catch (Exception e) {
            log.error("检查养号策略任务执行失败", e);
        }
    }

    /**
     * 启动养号策略执行
     */
    public boolean startNurturing(Long strategyId) {
        return nurturingExecutionEngine.startExecution(strategyId);
    }

    /**
     * 停止养号策略执行
     */
    public void stopNurturing(Long strategyId) {
        nurturingExecutionEngine.stopExecution(strategyId);
    }

    /**
     * 获取养号进度
     */
    public NurturingExecutionEngine.NurturingProgress getNurturingProgress(Long strategyId) {
        return nurturingExecutionEngine.getProgress(strategyId);
    }
}
