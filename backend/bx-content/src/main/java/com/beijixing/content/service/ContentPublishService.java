package com.beijixing.content.service;

import com.beijixing.content.entity.Content;
import com.beijixing.content.entity.ContentPublishRecord;

import java.util.List;

/**
 * 内容发布服务接口 - CO-008: 多平台一键发布
 * @author 胡云 (EMP-CONTENT-001)
 */
public interface ContentPublishService {

    /**
     * 发布内容到多个平台
     */
    void publishToPlatforms(Long contentId, List<Integer> platforms);

    /**
     * 发布到指定平台
     */
    boolean publishToPlatform(Content content, Integer platform);

    /**
     * 批量发布
     */
    void batchPublish(List<Long> contentIds, List<Integer> platforms);

    /**
     * 重试失败的发布 - CO-003: 发布重试机制
     */
    void retryFailedPublishes();

    /**
     * 查询发布状态 - CO-004: 发布状态查询
     */
    ContentPublishRecord getPublishStatus(Long recordId);

    /**
     * 撤回平台内容
     */
    boolean withdrawFromPlatform(Long contentId, Integer platform);
}
