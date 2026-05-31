package com.beijixing.data;

import org.springframework.context.annotation.Configuration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 北极星数据分析服务配置类
 *
 * 注意：已从@SpringBootApplication改为@Configuration + 移除@EnableDiscoveryClient(Nacos已禁用)
 * 原因：单体架构下由beijixing-app统一启动，此模块不再独立运行
 * 最后更新: 2026-05-20 (极简单体版)
 *
 * @author 陈工（EMP-DATA-001）
 */
@Configuration
@EnableCaching
@EnableAsync
public class BxDataApplication {

    // main方法已移除 - 单体架构下由BeijixingAiApplication统一启动
    // public static void main(String[] args) {
    //     SpringApplication.run(BxDataApplication.class, args);
    // }
}
