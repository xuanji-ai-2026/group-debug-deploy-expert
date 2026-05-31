package com.beijixing.tenant.aspect;

import com.beijixing.common.core.TenantContextHolder;
import com.beijixing.tenant.annotation.QuotaCheck;
import com.beijixing.tenant.service.TenantResourceQuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 配额检查AOP切面
 * 拦截标注了@QuotaCheck的方法，自动检查配额并扣减
 *
 * @author bx-tenant
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class QuotaCheckAspect {

    private final TenantResourceQuotaService quotaService;

    @Around("@annotation(quotaCheck)")
    public Object checkQuota(ProceedingJoinPoint joinPoint, QuotaCheck quotaCheck) throws Throwable {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            // 无租户信息，放行（如系统内部调用）
            return joinPoint.proceed();
        }

        String resource = quotaCheck.resource();
        long amount = quotaCheck.amount();

        // 检查配额
        if (!quotaService.checkQuota(tenantId, resource)) {
            log.warn("配额不足拒绝: tenantId={}, resource={}, amount={}", tenantId, resource, amount);
            throw new RuntimeException("配额不足，资源[" + resource + "]已达上限");
        }

        // 扣减配额
        if (!quotaService.tryConsume(tenantId, resource, amount)) {
            log.warn("配额扣减失败: tenantId={}, resource={}, amount={}", tenantId, resource, amount);
            throw new RuntimeException("配额扣减失败，资源[" + resource + "]不足");
        }

        try {
            Object result = joinPoint.proceed();
            return result;
        } catch (Throwable ex) {
            // 业务执行失败，返还配额
            quotaService.refund(tenantId, resource, amount);
            log.info("业务失败配额返还: tenantId={}, resource={}, amount={}", tenantId, resource, amount);
            throw ex;
        }
    }
}
