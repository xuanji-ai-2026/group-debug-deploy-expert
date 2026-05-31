package com.beijixing.config;

import com.beijixing.interceptor.LogInterceptor;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

@Slf4j
@Configuration
public class LoggingConfig implements WebMvcConfigurer {

    public static final String TRACE_ID = "traceId";
    public static final String USER_ID = "userId";
    public static final String CLIENT_IP = "clientIp";
    public static final String REQUEST_START_TIME = "requestStartTime";

    private static final String[] SENSITIVE_PARAMS = {"password", "passwd", "secret",
            "token", "apiKey", "api_key", "cardNo", "idCard", "phone", "mobile"};

    @Bean
    public LogInterceptor logInterceptor() {
        return new LogInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(logInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/health/**", "/api/actuator/**",
                        "/api/swagger-ui/**", "/api/v3/api-docs/**");
        log.info("[LoggingConfig] 日志拦截器已注册 - 拦截路径: /api/**");
    }

    @Bean
    public FilterRegistrationBean<Filter> traceIdFilter() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TraceIdFilter());
        registration.setName("traceIdFilter");
        registration.setOrder(Integer.MIN_VALUE);
        registration.setUrlPatterns(Collections.singletonList("/*"));
        log.info("[LoggingConfig] TraceId过滤器已注册 - 优先级: 最高");
        return registration;
    }

    public static class TraceIdFilter implements Filter {

        @Override
        public void doFilter(ServletRequest request, ServletResponse response,
                             FilterChain chain) throws IOException, ServletException {
            try {
                HttpServletRequest httpRequest = (HttpServletRequest) request;
                String traceId = httpRequest.getHeader("X-Request-Id");
                if (traceId == null || traceId.isEmpty()) {
                    traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
                }
                MDC.put(TRACE_ID, traceId);
                String clientIp = getClientIp(httpRequest);
                MDC.put(CLIENT_IP, clientIp);
                MDC.put(REQUEST_START_TIME, String.valueOf(System.currentTimeMillis()));
                ((HttpServletResponse) response).setHeader("X-Trace-Id", traceId);
                chain.doFilter(request, response);
            } finally {
                MDC.clear();
            }
        }

        private String getClientIp(HttpServletRequest request) {
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip;
        }
    }

    public static boolean isSensitiveParam(String paramName) {
        String lowerName = paramName.toLowerCase();
        for (String sensitive : SENSITIVE_PARAMS) {
            if (lowerName.contains(sensitive)) {
                return true;
            }
        }
        return false;
    }

    public static String maskSensitiveValue(String paramName, String value) {
        if (value == null || value.isEmpty()) {
            return "******";
        }
        String lowerName = paramName.toLowerCase();
        if (lowerName.contains("password") || lowerName.contains("passwd")
                || lowerName.contains("secret")) {
            return "******";
        }
        if (lowerName.contains("cardno") || lowerName.contains("idcard")) {
            if (value.length() <= 6) return "******";
            return value.substring(0, 3) + "****" + value.substring(value.length() - 3);
        }
        if (lowerName.contains("phone") || lowerName.contains("mobile")) {
            if (value.length() >= 7) {
                return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
            }
            return "******";
        }
        if (lowerName.contains("token") || lowerName.contains("apikey")
                || lowerName.contains("api_key")) {
            if (value.length() > 8) {
                return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
            }
            return "******";
        }
        return "******";
    }
}
