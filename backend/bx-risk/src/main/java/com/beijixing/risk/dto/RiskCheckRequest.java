package com.beijixing.risk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 风控检查请求DTO
 *
 * @author 林超 (EMP-SEC-001)
 */
@Data
public class RiskCheckRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 租户ID
     */
    @NotNull(message = "租户ID不能为空")
    private Long tenantId;

    /**
     * 账号ID（可为null）
     */
    private Long accountId;

    /**
     * 操作类型：publish/message/follow/comment/login/access
     */
    @NotBlank(message = "操作类型不能为空")
    private String operationType;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 设备指纹
     */
    private String deviceFingerprint;

    /**
     * 风控类型（如 CONTENT, OPERATION, ACCOUNT, CRAWLER）
     */
    private String riskType;

    /**
     * 待检测内容
     */
    private String content;

    /**
     * 请求参数（内容、关键词等）
     */
    private Map<String, Object> requestParams;

    /**
     * 平台编码
     */
    private String platformCode;

    /**
     * 关联业务ID（如内容ID、商机ID等）
     */
    private String businessId;

    /**
     * 是否仅检查不记录（用于预检）
     */
    private Boolean dryRun;

    /**
     * 备注
     */
    private String remark;
}
