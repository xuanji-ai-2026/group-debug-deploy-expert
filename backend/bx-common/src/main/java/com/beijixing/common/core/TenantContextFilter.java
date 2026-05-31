package com.beijixing.common.core;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 多租户上下文过滤器
 * 从请求Header中提取X-Tenant-Id，写入TenantContextHolder
 * 必须在所有业务Filter之前执行
 *
 * @author bx-common
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@SuppressWarnings("nullness")
public class TenantContextFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String IGNORE_TENANT_HEADER = "X-Ignore-Tenant";

    @Override
    @SuppressWarnings("nullness")
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 从Header获取租户ID
            String tenantIdStr = request.getHeader(TENANT_HEADER);
            if (tenantIdStr != null && !tenantIdStr.isEmpty()) {
                try {
                    Long tenantId = Long.parseLong(tenantIdStr);
                    TenantContextHolder.setTenantId(tenantId);
                } catch (NumberFormatException e) {
                    // 租户ID格式错误，不设置
                }
            }

            // 超级管理员标记（仅限内部服务调用使用）
            String ignoreTenant = request.getHeader(IGNORE_TENANT_HEADER);
            if ("true".equalsIgnoreCase(ignoreTenant)) {
                TenantContextHolder.setIgnoreTenant(true);
            }

            filterChain.doFilter(request, response);
        } finally {
            // 请求结束必须清理，防止线程池复用导致租户ID泄漏
            TenantContextHolder.clear();
        }
    }
}
