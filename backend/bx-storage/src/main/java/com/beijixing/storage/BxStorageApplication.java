package com.beijixing.storage;

import org.springframework.context.annotation.Configuration;

/**
 * 北极星AI商机获客系统 - 存储服务配置类
 *
 * <p>功能说明：</p>
 * <ul>
 *   <li>ST-001: 腾讯云COS集成 - 文件上传到COS</li>
 *   <li>ST-002: 分片上传 - 大文件分片断点续传</li>
 *   <li>ST-003: CDN加速 - CDN域名访问</li>
 *   <li>ST-004: 文件访问权限控制 - 签名URL访问</li>
 * </ul>
 *
 * 注意：已从@SpringBootApplication改为@Configuration
 * 原因：单体架构下由beijixing-app统一启动，此模块不再独立运行
 * 最后更新: 2026-05-20 (极简单体版)
 *
 * @author 叶宇（EMP-OPS-001）
 * @version 1.0.0
 * @since 2024-01
 */
@Configuration
public class BxStorageApplication {

    // main方法已移除 - 单体架构下由BeijixingAiApplication统一启动
    // public static void main(String[] args) {
    //     SpringApplication.run(BxStorageApplication.class, args);
    // }
}
