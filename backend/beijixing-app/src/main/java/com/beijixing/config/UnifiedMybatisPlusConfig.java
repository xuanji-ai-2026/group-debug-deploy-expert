package com.beijixing.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.beijixing.common.core.BxTenantLineHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.LocalDateTime;

@Configuration
@MapperScan({
    "com.beijixing.ai.mapper",
    "com.beijixing.bxlead.mapper",
    "com.beijixing.bxuser.mapper",
    "com.beijixing.tenant.mapper",
    "com.beijixing.tenant.repository.mapper",
    "com.beijixing.tenant.repository",
    "com.beijixing.schedule.mapper",
    "com.beijixing.message.mapper",
    "com.beijixing.social.mapper",
    "com.beijixing.social.crawl.mapper",
    "com.beijixing.social.crawl.collection.mapper",
    "com.beijixing.social.message.mapper",
    "com.beijixing.billing.mapper",
    "com.beijixing.content.mapper",
    "com.beijixing.risk.repository.mapper",
    "com.beijixing.risk.repository",
    "com.beijixing.system.mapper",
    "com.beijixing.data.repository"
})
public class UnifiedMybatisPlusConfig {

    @Bean("unifiedMybatisPlusInterceptor")
    @Primary
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new BxTenantLineHandler()));
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean("unifiedMetaObjectHandler")
    @Primary
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}