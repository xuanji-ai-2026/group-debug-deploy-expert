package com.beijixing.risk;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 风控管理服务配置类
 *
 * 注意：已从@SpringBootApplication改为@Configuration
 * 原因：单体架构下由beijixing-app统一启动，此模块不再独立运行
 * 最后更新: 2026-05-20 (极简单体版)
 *
 * @author 林超 (EMP-SEC-001) - 安全总监
 * @since 2024-01-01
 */
@Configuration
@EnableScheduling
public class BxRiskApplication {

    // main方法已移除 - 单体架构下由BeijixingAiApplication统一启动
    // public static void main(String[] args) {
    //     SpringApplication.run(BxRiskApplication.class, args);
    // }
}
