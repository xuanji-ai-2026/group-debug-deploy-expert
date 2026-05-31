package com.beijixing.common.core;

/**
 * 多租户上下文持有器
 * 基于ThreadLocal实现租户ID的自动传递，配合MyBatis拦截器实现字段级数据隔离
 *
 * 使用流程：
 * 1. Gateway/AuthFilter解析JWT中的tenantId，放入请求Header X-Tenant-Id
 * 2. 各服务Filter/Interceptor调用setTenantId()写入ThreadLocal
 * 3. MyBatis TenantLineInnerInterceptor自动在SQL中追加 tenant_id = ?
 * 4. 请求结束调用clear()避免内存泄漏
 *
 * @author bx-common
 */
public class TenantContextHolder {

    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> IGNORE_TENANT = new ThreadLocal<>();

    /**
     * 设置当前租户ID
     */
    public static void setTenantId(Long tenantId) {
        TENANT_ID.set(tenantId);
    }

    /**
     * 获取当前租户ID
     */
    public static Long getTenantId() {
        return TENANT_ID.get();
    }

    /**
     * 清除当前租户ID（请求结束时必须调用，防止内存泄漏）
     */
    public static void clear() {
        TENANT_ID.remove();
        IGNORE_TENANT.remove();
    }

    /**
     * 标记忽略租户隔离（超级管理员操作、系统级任务等场景）
     */
    public static void setIgnoreTenant(boolean ignore) {
        IGNORE_TENANT.set(ignore);
    }

    /**
     * 是否忽略租户隔离
     */
    public static boolean isIgnoreTenant() {
        Boolean ignore = IGNORE_TENANT.get();
        return ignore != null && ignore;
    }
}
