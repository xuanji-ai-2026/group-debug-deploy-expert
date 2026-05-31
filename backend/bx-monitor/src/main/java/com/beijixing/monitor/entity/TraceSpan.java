package com.beijixing.monitor.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceSpan {

    private String traceId;
    private String spanId;
    private String parentSpanId;
    private String serviceName;
    private String operationName;
    private String spanKind;          // SERVER, CLIENT, INTERNAL
    private Long startTime;            // 毫秒时间戳
    private Long duration;            // 耗时(毫秒)
    private String status;            // OK, ERROR
    private String errorMessage;
    private List<TraceTag> tags;
    private List<TraceLog> logs;
    private LocalDateTime createTime;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TraceTag {
        private String key;
        private String value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TraceLog {
        private Long timestamp;
        private String fields;
    }
}
