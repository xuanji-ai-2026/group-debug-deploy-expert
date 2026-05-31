package com.beijixing.content;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 北极星AI商机获客系统 - 内容运营模块配置类
 *
 * 注意：已从@SpringBootApplication改为@Configuration
 * 原因：单体架构下由beijixing-app统一启动，此模块不再独立运行
 * 最后更新: 2026-05-20 (极简单体版)
 *
 * @author 胡云 (EMP-CONTENT-001)
 */
@Configuration
@EnableScheduling
public class BxContentApplication {

    // main方法已移除 - 单体架构下由BeijixingAiApplication统一启动
    // public static void main(String[] args) {
    //     SpringApplication.run(BxContentApplication.class, args);
    // }
}
