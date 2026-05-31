package com.beijixing.tenant.annotation;

import java.lang.annotation.*;

/**
 * 配额检查注解
 * 标注在需要消耗配额的业务方法上，自动检查并扣减配额
 *
 * 使用方式：
 * @QuotaCheck(resource = "daily_intercept_task", amount = 1)
 * public void interceptLead() { ... }
 *
 * @author bx-tenant
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface QuotaCheck {

    /**
     * 资源类型（对应tenant_resource_quota.resource_type）
     */
    String resource();

    /**
     * 消耗数量，默认1
     */
    long amount() default 1;
}
