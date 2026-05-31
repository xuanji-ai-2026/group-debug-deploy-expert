package com.beijixing.tenant;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 北极星AI商机获客系统 - 租户管理服务配置类
 *
 * 注意：已从@SpringBootApplication改为@Configuration
 * 原因：单体架构下由beijixing-app统一启动，此模块不再独立运行
 * 最后更新: 2026-05-20 (极简单体版)
 *
 * @author bx-tenant
 * @version 1.0.0
 */
@Configuration
@EnableScheduling
@MapperScan("com.beijixing.tenant.repository.mapper")
public class BxTenantApplication {

    // main方法已移除 - 单体架构下由BeijixingAiApplication统一启动
    // public static void main(String[] args) {
    //     SpringApplication.run(BxTenantApplication.class, args);
    // }
}
