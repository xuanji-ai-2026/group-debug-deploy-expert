package com.beijixing.billing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 发票申请实体
 * BL-008: 发票申请
 */
@Data
@TableName("bx_invoice_request")
public class InvoiceRequest {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 租户ID
     */
    private Long tenantId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 申请单号
     */
    private String requestNo;
    
    /**
     * 发票类型: 1-电子普票, 2-电子专票
     */
    private Integer invoiceType;
    
    /**
     * 发票金额（分）
     */
    private Long amount;
    
    /**
     * 发票抬头
     */
    private String title;
    
    /**
     * 纳税人识别号
     */
    private String taxNumber;
    
    /**
     * 注册地址
     */
    private String address;
    
    /**
     * 注册电话
     */
    private String phone;
    
    /**
     * 开户银行
     */
    private String bankName;
    
    /**
     * 银行账号
     */
    private String bankAccount;
    
    /**
     * 接收邮箱
     */
    private String receiveEmail;
    
    /**
     * 关联订单ID列表（JSON数组）
     */
    private String orderIds;
    
    /**
     * 状态: 0-待处理, 1-处理中, 2-已完成, 3-已驳回
     */
    private Integer status;
    
    /**
     * 驳回原因
     */
    private String rejectReason;
    
    /**
     * 发票文件URL
     */
    private String invoiceUrl;
    
    /**
     * 发票代码
     */
    private String invoiceCode;
    
    /**
     * 发票号码
     */
    private String invoiceNumber;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
