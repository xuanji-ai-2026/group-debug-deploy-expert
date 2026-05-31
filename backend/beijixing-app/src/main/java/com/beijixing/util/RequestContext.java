package com.beijixing.util;

import org.slf4j.MDC;

import java.util.Optional;

public class RequestContext {

    private static final ThreadLocal<String> USER_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> TENANT_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> CLIENT_IP_HOLDER = new ThreadLocal<>();

    public static void setUserId(String userId) {
        USER_ID_HOLDER.set(userId);
        MDC.put("userId", userId != null ? userId : "");
    }

    public static String getUserId() {
        String userId = USER_ID_HOLDER.get();
        if (userId == null) {
            userId = MDC.get("userId");
        }
        return userId;
    }

    public static Optional<String> getUserIdOptional() {
        return Optional.ofNullable(getUserId());
    }

    public static void setTenantId(String tenantId) {
        TENANT_ID_HOLDER.set(tenantId);
    }

    public static String getTenantId() {
        return TENANT_ID_HOLDER.get();
    }

    public static Optional<String> getTenantIdOptional() {
        return Optional.ofNullable(getTenantId());
    }

    public static void setClientIp(String clientIp) {
        CLIENT_IP_HOLDER.set(clientIp);
    }

    public static String getClientIp() {
        String ip = CLIENT_IP_HOLDER.get();
        if (ip == null) {
            ip = MDC.get("clientIp");
        }
        return ip;
    }

    public static String getTraceId() {
        return MDC.get("traceId");
    }

    public static boolean isAuthenticated() {
        String userId = getUserId();
        return userId != null && !userId.isEmpty() && !"anonymous".equals(userId);
    }

    public static boolean isSystemUser() {
        String userId = getUserId();
        return "system".equals(userId) || "admin".equals(userId);
    }

    public static void clear() {
        USER_ID_HOLDER.remove();
        TENANT_ID_HOLDER.remove();
        CLIENT_IP_HOLDER.remove();
    }

    public static void initAnonymous(String clientIp) {
        setUserId("anonymous");
        setClientIp(clientIp);
    }
}
