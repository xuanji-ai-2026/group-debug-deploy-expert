package com.beijixing.ai;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI服务配置类
 *
 * 注意：已从@SpringBootApplication改为@Configuration
 * 原因：单体架构下由beijixing-app统一启动，此模块不再独立运行
 * 类名改为AiModuleConfig避免与主应用BeijixingAiApplication冲突
 * 最后更新: 2026-05-20 (极简单体版)
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AiModuleConfig {

    // main方法已移除 - 单体架构下由BeijixingAiApplication统一启动
    // public static void main(String[] args) {
    //     SpringApplication.run(BeijixingAiApplication.class, args);
    // }
}