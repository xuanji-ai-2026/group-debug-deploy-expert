package com.beijixing.social;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 社交运营服务配置类
 *
 * 注意：已从@SpringBootApplication改为@Configuration + 移除@EnableDiscoveryClient(Nacos已禁用)
 * 原因：单体架构下由beijixing-app统一启动，此模块不再独立运行
 * 最后更新: 2026-05-20 (极简单体版)
 */
@Configuration
@MapperScan({"com.beijixing.social.mapper", "com.beijixing.social.crawl.mapper", "com.beijixing.social.crawl.collection.mapper", "com.beijixing.social.message.mapper"})
@EnableScheduling
public class BxSocialApplication {

    // main方法已移除 - 单体架构下由BeijixingAiApplication统一启动
    // public static void main(String[] args) {
    //     SpringApplication.run(BxSocialApplication.class, args);
    // }
}
