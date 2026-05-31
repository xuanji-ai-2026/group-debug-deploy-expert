package com.beijixing.monitor.controller;

import com.beijixing.monitor.entity.TraceSpan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/trace")
@SuppressWarnings("nullness")
public class TraceController {

    private final StringRedisTemplate redisTemplate;
    private static final String TRACE_PREFIX = "bx:trace:";
    private static final String TRACE_SPANS_KEY = "bx:trace:spans:";

    public TraceController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 根据traceId查询链路
     */
    @GetMapping("/{traceId}")
    public Map<String, Object> getTrace(@PathVariable String traceId) {
        try {
            List<TraceSpan> spans = getTraceSpans(traceId);
            if (spans.isEmpty()) {
                return Map.of("code", 404, "message", "链路不存在或已过期");
            }

            // 构建树形结构
            List<Map<String, Object>> tree = buildTraceTree(spans);

            // 统计信息
            Map<String, Object> stats = calculateTraceStats(spans);

            return Map.of(
                    "code", 0,
                    "traceId", traceId,
                    "spans", tree,
                    "stats", stats,
                    "spanCount", spans.size()
            );
        } catch (Exception e) {
            log.error("Failed to get trace {}: {}", traceId, e.getMessage());
            return Map.of("code", 500, "message", "查询失败: " + e.getMessage());
        }
    }

    /**
     * 上报链路数据
     */
    @PostMapping("/report")
    public Map<String, Object> reportTrace(@RequestBody List<TraceSpan> spans) {
        try {
            if (spans == null || spans.isEmpty()) {
                return Map.of("code", 400, "message", "空的链路数据");
            }

            String traceId = spans.get(0).getTraceId();
            if (traceId == null) {
                traceId = UUID.randomUUID().toString();
            }

            for (TraceSpan span : spans) {
                if (span.getCreateTime() == null) {
                    span.setCreateTime(LocalDateTime.now());
                }
                saveSpan(traceId, span);
            }

            return Map.of("code", 0, "message", "链路数据已上报", "traceId", traceId, "spanCount", spans.size());
        } catch (Exception e) {
            log.error("Failed to report trace: {}", e.getMessage());
            return Map.of("code", 500, "message", "上报失败: " + e.getMessage());
        }
    }

    /**
     * 搜索链路
     */
    @GetMapping("/search")
    @SuppressWarnings("nullness")
    public Map<String, Object> searchTraces(
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String operationName,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            // 从Redis中搜索（简化实现）
            Set<String> keys = redisTemplate.keys(TRACE_SPANS_KEY + "*");
            List<Map<String, Object>> results = new ArrayList<>();

            if (keys != null) {
                int skip = page * size;
                int count = 0;
                for (String key : keys) {
                    if (count >= skip + size) break;

                    Object spanData = redisTemplate.opsForHash().get(key, "data");
                    if (spanData != null) {
                        // 实际应反序列化并过滤
                        if (count >= skip) {
                            results.add(Map.of("traceId", key.replace(TRACE_SPANS_KEY, "")));
                        }
                        count++;
                    }
                }
            }

            return Map.of("code", 0, "data", results, "page", page, "size", size);
        } catch (Exception e) {
            log.error("Failed to search traces: {}", e.getMessage());
            return Map.of("code", 500, "message", "搜索失败: " + e.getMessage());
        }
    }

    private List<TraceSpan> getTraceSpans(String traceId) {
        List<TraceSpan> spans = new ArrayList<>();
        try {
            String key = TRACE_PREFIX + "spans:" + traceId;
            Set<String> members = redisTemplate.opsForSet().members(key);
            if (members != null) {
                for (String m : members) {
                    // 简化：实际应反序列化
                    TraceSpan span = TraceSpan.builder()
                            .traceId(traceId)
                            .spanId(m.toString())
                            .status("OK")
                            .duration(0L)
                            .build();
                    spans.add(span);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get trace spans for {}: {}", traceId, e.getMessage());
        }
        return spans;
    }

    private void saveSpan(String traceId, TraceSpan span) {
        try {
            String spanKey = TRACE_PREFIX + "span:" + span.getSpanId();
            String setKey = TRACE_PREFIX + "spans:" + traceId;

            Map<String, String> data = new HashMap<>();
            data.put("traceId", span.getTraceId());
            data.put("spanId", span.getSpanId());
            data.put("parentSpanId", span.getParentSpanId() != null ? span.getParentSpanId() : "");
            data.put("serviceName", span.getServiceName() != null ? span.getServiceName() : "");
            data.put("operationName", span.getOperationName() != null ? span.getOperationName() : "");
            data.put("spanKind", span.getSpanKind() != null ? span.getSpanKind() : "INTERNAL");
            data.put("duration", String.valueOf(span.getDuration() != null ? span.getDuration() : 0));
            data.put("status", span.getStatus() != null ? span.getStatus() : "OK");
            data.put("startTime", String.valueOf(span.getStartTime() != null ? span.getStartTime() : System.currentTimeMillis()));
            data.put("createTime", span.getCreateTime().toString());

            redisTemplate.opsForHash().putAll(spanKey, data);
            redisTemplate.expire(spanKey, 1, TimeUnit.HOURS);

            redisTemplate.opsForSet().add(setKey, span.getSpanId());
            redisTemplate.expire(setKey, 1, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("Failed to save span: {}", e.getMessage());
        }
    }

    private List<Map<String, Object>> buildTraceTree(List<TraceSpan> spans) {
        List<Map<String, Object>> tree = new ArrayList<>();
        for (TraceSpan span : spans) {
            Map<String, Object> node = new HashMap<>();
            node.put("spanId", span.getSpanId());
            node.put("parentSpanId", span.getParentSpanId());
            node.put("serviceName", span.getServiceName());
            node.put("operationName", span.getOperationName());
            node.put("spanKind", span.getSpanKind());
            node.put("duration", span.getDuration());
            node.put("status", span.getStatus());
            tree.add(node);
        }
        return tree;
    }

    private Map<String, Object> calculateTraceStats(List<TraceSpan> spans) {
        long totalDuration = 0;
        int errorCount = 0;
        Set<String> services = new HashSet<>();

        for (TraceSpan span : spans) {
            totalDuration += span.getDuration() != null ? span.getDuration() : 0;
            if ("ERROR".equals(span.getStatus())) errorCount++;
            if (span.getServiceName() != null) services.add(span.getServiceName());
        }

        return Map.of(
                "totalDuration", totalDuration,
                "spanCount", spans.size(),
                "errorCount", errorCount,
                "serviceCount", services.size()
        );
    }
}
