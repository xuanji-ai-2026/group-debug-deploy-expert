package com.beijixing.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 北极星AI商机获客系统 - 系统管理模块配置类
 *
 * 功能模块：
 * - SM-001 参数配置（系统参数、动态配置）
 * - SM-002 字典管理（数据字典、字典项）
 * - SM-003 定时任务（任务配置、执行日志）
 * - SM-004 文件管理（文件上传、预览、删除）
 * - SM-005 系统监控（服务状态、资源监控）
 * - SM-006 日志管理（操作日志、审计日志）
 *
 * 注意：已从@SpringBootApplication改为@Configuration + 移除@EnableDiscoveryClient(Nacos已禁用)
 * 原因：单体架构下由beijixing-app统一启动，此模块不再独立运行
 * 最后更新: 2026-05-20 (极简单体版)
 *
 * @author bx-system
 */
@Configuration
@EnableAsync
@EnableScheduling
@MapperScan("com.beijixing.system.mapper")
public class BxSystemApplication {

    // main方法已移除 - 单体架构下由BeijixingAiApplication统一启动
    // public static void main(String[] args) {
    //     SpringApplication.run(BxSystemApplication.class, args);
    // }
}
