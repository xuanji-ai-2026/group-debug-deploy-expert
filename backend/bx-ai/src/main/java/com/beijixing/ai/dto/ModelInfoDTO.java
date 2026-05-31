package com.beijixing.ai.dto;

import lombok.Data;
import java.util.List;

/**
 * 大模型配置信息DTO
 * 所有字段支持热更新，修改后实时生效
 */
@Data
public class ModelInfoDTO {

    /**
     * 模型唯一ID
     */
    private String modelId;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 模型厂商
     */
    private String provider;

    /**
     * 模型类型：text=文本生成, image=图片生成, speech=语音, multi=多模态
     */
    private String type;

    /**
     * 状态：0=禁用, 1=启用
     */
    private Integer status;

    /**
     * 优先级：数字越小优先级越高
     */
    private Integer priority;

    /**
     * 支持的场景列表
     */
    private List<String> supportScenes;

    /**
     * API密钥
     */
    private String apiKey;

    /**
     * API密钥Secret（部分模型需要，比如文心一言）
     */
    private String apiSecret;

    /**
     * API endpoint
     */
    private String endpoint;

    /**
     * 最大tokens
     */
    private Integer maxTokens;

    /**
     * 温度系数
     */
    private Double temperature;

    /**
     * 限流阈值（每分钟调用次数）
     */
    private Integer rateLimit;

    /**
     * 超时时间（毫秒）
     */
    private Integer timeout;
}
