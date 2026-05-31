package com.beijixing.common.core;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;

/**
 * 多租户感知实体基类（MyBatis-Plus版本）
 * 继承此类的实体自动启用租户ID字段，配合TenantLineInnerInterceptor实现自动租户过滤
 *
 * 使用方式：
 * @TableName("your_table")
 * public class YourEntity extends TenantAwareEntity { ... }
 *
 * 需要在MybatisPlusConfig中配置 TenantLineInnerInterceptor
 *
 * @author bx-common
 * @version 2.0.0 (从JPA迁移到MyBatis-Plus)
 */
public abstract class TenantAwareEntity {

    @TableField("tenant_id")
    private Long tenantId;

    @TableLogic
    @TableField(value = "deleted", fill = FieldFill.INSERT)
    private Integer deleted;

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}