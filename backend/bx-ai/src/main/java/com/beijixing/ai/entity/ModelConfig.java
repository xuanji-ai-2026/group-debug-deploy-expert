package com.beijixing.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 大模型配置实体
 * 持久化到MySQL数据库
 */
@Data
@TableName("ai_model_config")
public class ModelConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 模型唯一ID
     */
    private String modelId;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 模型厂商：volcano/aliyun/wenxin/xunfei/zhipu/openai/claude
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
     * 支持的场景列表，JSON数组存储
     */
    private String supportScenes;

    /**
     * API密钥
     */
    private String apiKey;

    /**
     * API密钥Secret（部分模型需要）
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

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
