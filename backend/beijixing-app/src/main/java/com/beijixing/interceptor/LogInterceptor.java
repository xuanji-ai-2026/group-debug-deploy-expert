package com.beijixing.interceptor;

import com.beijixing.config.LoggingConfig;
import com.beijixing.util.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.lang.Nullable;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class LogInterceptor implements HandlerInterceptor {

    private static final int MAX_PAYLOAD_LENGTH = 2048;
    private static final long SLOW_REQUEST_THRESHOLD_MS = 3000;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        long startTime = System.currentTimeMillis();
        MDC.put(LoggingConfig.REQUEST_START_TIME, String.valueOf(startTime));

        if (!(request instanceof ContentCachingRequestWrapper)) {
            // 注意：实际使用时需通过Filter包装，此处仅记录元信息
        }

        String queryString = request.getQueryString();
        String fullUrl = request.getRequestURI() + (queryString != null ? "?" + queryString : "");

        log.info("[HTTP请求开始] {} {} | Content-Type: {} | IP: {} | TraceId: {}",
                request.getMethod(),
                fullUrl,
                request.getContentType(),
                LoggingConfig.isSensitiveParam("") ? RequestContext.getClientIp()
                        : maskIp(RequestContext.getClientIp()),
                RequestContext.getTraceId());

        Map<String, String> safeParams = extractSafeParameters(request);
        if (!safeParams.isEmpty()) {
            log.debug("[HTTP请求参数] {} | 参数: {}", fullUrl, safeParams);
        }

        request.setAttribute("requestStartTime", startTime);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) throws Exception {
        Long startTimeAttr = (Long) request.getAttribute("requestStartTime");
        long startTime = startTimeAttr != null ? startTimeAttr : 0L;
        long duration = System.currentTimeMillis() - startTime;

        String queryString = request.getQueryString();
        String path = request.getRequestURI();

        int status = response.getStatus();
        String handlerName = getHandlerName(handler);

        if (duration > SLOW_REQUEST_THRESHOLD_MS) {
            log.warn("[HTTP慢请求] {} {} | 耗时: {}ms | 状态码: {} | 处理器: {} | TraceId: {}",
                    request.getMethod(), path, duration, status, handlerName,
                    RequestContext.getTraceId());
        } else {
            log.info("[HTTP请求结束] {} {} | 耗时: {}ms | 状态码: {} | 处理器: {} | TraceId: {}",
                    request.getMethod(), path, duration, status, handlerName,
                    RequestContext.getTraceId());
        }

        if (ex != null) {
            log.error("[HTTP请求异常] {} {} | 异常: {} | TraceId: {}",
                    request.getMethod(), path, ex.getMessage(), RequestContext.getTraceId(), ex);
        }

        recordMetrics(path, status, duration);

        RequestContext.clear();
    }

    private String getHandlerName(Object handler) {
        if (handler instanceof HandlerMethod handlerMethod) {
            return handlerMethod.getBeanType().getSimpleName()
                    + "." + handlerMethod.getMethod().getName() + "()";
        }
        return handler.getClass().getSimpleName();
    }

    private Map<String, String> extractSafeParameters(HttpServletRequest request) {
        Map<String, String> safeParams = new HashMap<>();
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String name = paramNames.nextElement();
            String[] values = request.getParameterValues(name);
            if (values != null && values.length > 0) {
                if (LoggingConfig.isSensitiveParam(name)) {
                    safeParams.put(name, "******");
                } else {
                    String value = values[0];
                    if (value.length() > MAX_PAYLOAD_LENGTH) {
                        value = value.substring(0, MAX_PAYLOAD_LENGTH) + "...(truncated)";
                    }
                    safeParams.put(name, value);
                }
            }
        }
        return safeParams;
    }

    private void recordMetrics(String path, int status, long duration) {
        // 可选：与MetricsConfig集成记录HTTP请求指标
        // 示例：metricsConfig.recordHttpRequest(path, status, duration);
    }

    private String readPayload(ContentCachingRequestWrapper request) {
        byte[] buf = request.getContentAsByteArray();
        if (buf.length > 0) {
            int length = Math.min(buf.length, MAX_PAYLOAD_LENGTH);
            try {
                return new String(buf, 0, length, request.getCharacterEncoding());
            } catch (UnsupportedEncodingException e) {
                return new String(buf, 0, length, StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    private String readResponse(ContentCachingResponseWrapper response) {
        byte[] buf = response.getContentAsByteArray();
        if (buf.length > 0) {
            int length = Math.min(buf.length, MAX_PAYLOAD_LENGTH);
            try {
                return new String(buf, 0, length, response.getCharacterEncoding());
            } catch (UnsupportedEncodingException e) {
                return new String(buf, 0, length, StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    private static String maskIp(String ip) {
        if (ip == null || ip.isEmpty()) return "unknown";
        String[] parts = ip.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + ".***.***";
        }
        if (ip.contains(":")) {
            return ip.substring(0, 5) + ":***:***:***";
        }
        return "***";
    }
}
