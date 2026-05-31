package com.beijixing.social.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("account_log")
public class AccountLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 账号ID */
    private Long accountId;

    /** 平台代码 */
    private String platformCode;

    /** 操作类型: PUBLISH/LIKE/COMMENT/FOLLOW/DM/VIEW/OTHER */
    private String actionType;

    /** 操作描述 */
    private String actionDesc;

    /** 操作目标ID */
    private String targetId;

    /** 操作目标描述 */
    private String targetDesc;

    /** 内容ID */
    private String contentId;

    /** 内容类型: VIDEO/IMAGE/TEXT */
    private String contentType;

    /** 操作结果: SUCCESS/FAIL */
    private String result;

    /** 失败原因 */
    private String failReason;

    /** 请求参数(JSON) */
    private String requestParams;

    /** 响应结果(JSON) */
    private String responseData;

    /** 操作耗时(ms) */
    private Long duration;

    /** IP地址 */
    private String ipAddress;

    /** 设备ID */
    private String deviceId;

    /** 用户ID */
    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public Long getAccountId() { return accountId; }
    public void setPlatformCode(String platformCode) { this.platformCode = platformCode; }
    public String getPlatformCode() { return platformCode; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getActionType() { return actionType; }
    public void setActionDesc(String actionDesc) { this.actionDesc = actionDesc; }
    public String getActionDesc() { return actionDesc; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public String getTargetId() { return targetId; }
    public void setTargetDesc(String targetDesc) { this.targetDesc = targetDesc; }
    public String getTargetDesc() { return targetDesc; }
    public void setContentId(String contentId) { this.contentId = contentId; }
    public String getContentId() { return contentId; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getContentType() { return contentType; }
    public void setResult(String result) { this.result = result; }
    public String getResult() { return result; }
    public void setFailReason(String failReason) { this.failReason = failReason; }
    public String getFailReason() { return failReason; }
    public void setRequestParams(String requestParams) { this.requestParams = requestParams; }
    public String getRequestParams() { return requestParams; }
    public void setResponseData(String responseData) { this.responseData = responseData; }
    public String getResponseData() { return responseData; }
    public void setDuration(Long duration) { this.duration = duration; }
    public Long getDuration() { return duration; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getIpAddress() { return ipAddress; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getDeviceId() { return deviceId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getUserId() { return userId; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getCreateTime() { return createTime; }
}
