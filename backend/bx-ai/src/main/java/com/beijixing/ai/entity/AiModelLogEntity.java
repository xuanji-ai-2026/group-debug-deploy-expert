package com.beijixing.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_model_log")
public class AiModelLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String requestId;

    private String userId;

    private String provider;

    private String modelName;

    private String requestType;

    private Integer inputTokens;

    private Integer outputTokens;

    private Integer totalTokens;

    private String requestContent;

    private String responseContent;

    private String status;

    private String errorMessage;

    private Long responseTime;

    private Double cost;

    private Boolean isFallback;

    private String clientIp;

    private LocalDateTime createTime;
}