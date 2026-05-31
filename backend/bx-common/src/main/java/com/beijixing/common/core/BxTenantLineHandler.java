package com.beijixing.common.core;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;

import java.util.HashSet;
import java.util.Set;

/**
 * 北极星多租户SQL拦截处理器
 * 自动在所有SQL的WHERE条件中追加 tenant_id = 当前租户ID
 *
 * 忽略规则：
 * 1. TenantContextHolder.isIgnoreTenant() 为true时跳过
 * 2. 表名在忽略列表中的跳过（如系统级表）
 *
 * @author bx-common
 */
public class BxTenantLineHandler implements TenantLineHandler {

    /**
     * 不需要租户隔离的表（系统级公共表）
     */
    private static final Set<String> IGNORE_TABLES = new HashSet<>();

    static {
        // 系统级表不需要租户隔离
        IGNORE_TABLES.add("sys_config");
        IGNORE_TABLES.add("sys_dict");
        IGNORE_TABLES.add("sys_dict_item");
        IGNORE_TABLES.add("sys_job");
        IGNORE_TABLES.add("sys_oper_log");
        IGNORE_TABLES.add("sys_file");
        // 租户表本身不需要隔离
        IGNORE_TABLES.add("tenant");
        IGNORE_TABLES.add("tenant_config");
        IGNORE_TABLES.add("tenant_package");
        IGNORE_TABLES.add("tenant_resource_quota");
        // 套餐是全局配置
        IGNORE_TABLES.add("billing_package");
        // 用户注册时还没有租户上下文
        IGNORE_TABLES.add("user");
        IGNORE_TABLES.add("login_log");
        IGNORE_TABLES.add("refresh_token");
        IGNORE_TABLES.add("data_permission");
        // 社媒平台配置是全局的
        IGNORE_TABLES.add("social_platform");
        // AI模型配置是全局的
        IGNORE_TABLES.add("ai_model_config");
        IGNORE_TABLES.add("ai_prompt_template");
        // 商机相关表暂无tenant_id列
        IGNORE_TABLES.add("bx_lead");
        IGNORE_TABLES.add("bx_lead_follow_up");
        IGNORE_TABLES.add("bx_lead_status_history");
        IGNORE_TABLES.add("bx_lead_stats");
        IGNORE_TABLES.add("bx_intercept_source");
        // 统计表暂无tenant_id列
        IGNORE_TABLES.add("bx_account_stats");
        IGNORE_TABLES.add("bx_operation_stats");
        IGNORE_TABLES.add("bx_billing_stats");
    }

    @Override
    public Expression getTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null || TenantContextHolder.isIgnoreTenant()) {
            return new NullValue();
        }
        return new LongValue(tenantId);
    }

    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }

    @Override
    public boolean ignoreTable(String tableName) {
        if (TenantContextHolder.isIgnoreTenant()) {
            return true;
        }
        if (TenantContextHolder.getTenantId() == null) {
            return true;
        }
        return IGNORE_TABLES.contains(tableName.toLowerCase());
    }

    public static void addIgnoreTable(String tableName) {
        IGNORE_TABLES.add(tableName.toLowerCase());
    }
}
