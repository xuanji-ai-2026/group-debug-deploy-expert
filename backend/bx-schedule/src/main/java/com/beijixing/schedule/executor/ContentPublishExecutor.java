package com.beijixing.schedule.executor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beijixing.content.entity.Content;
import com.beijixing.content.entity.ContentPublishRecord;
import com.beijixing.content.enums.PublishStatus;
import com.beijixing.content.mapper.ContentMapper;
import com.beijixing.content.mapper.ContentPublishRecordMapper;
import com.beijixing.content.service.ContentPublishService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 内容发布任务执行器
 * 负责定时将待发布内容发布到各平台
 */
@Slf4j
@Component
public class ContentPublishExecutor extends BaseExecutor {

    @Autowired(required = false)
    private ContentMapper contentMapper;

    @Autowired(required = false)
    private ContentPublishRecordMapper publishRecordMapper;

    @Autowired(required = false)
    private ContentPublishService contentPublishService;

    @XxlJob("contentPublishJob")
    public void xxlExecute() {
        String params = XxlJobHelper.getJobParam();
        execute(params);
    }

    @Override
    protected String doExecute(String params) {
        log.info("开始执行内容发布任务");
        XxlJobHelper.log("开始执行内容发布任务");

        List<Map<String, Object>> pendingContents = queryPendingContents();

        int successCount = 0;
        int failCount = 0;

        for (Map<String, Object> content : pendingContents) {
            try {
                Long contentId = (Long) content.get("id");
                Integer platform = (Integer) content.get("platform");

                boolean success = publishToPlatform(contentId, platform);

                if (success) {
                    updatePublishStatus(contentId, "success");
                    successCount++;
                    log.info("内容 {} 发布到平台{} 成功", contentId, platform);
                } else {
                    updatePublishStatus(contentId, "failed");
                    failCount++;
                    log.warn("内容 {} 发布到平台{} 失败", contentId, platform);
                }
            } catch (Exception e) {
                failCount++;
                log.error("内容发布异常", e);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", pendingContents.size());
        result.put("success", successCount);
        result.put("failed", failCount);

        return toJson(result);
    }

    @Override
    protected String getJobName() {
        return "内容发布任务";
    }

    @Override
    protected String getJobType() {
        return "content_publish";
    }

    /**
     * 查询待发布内容
     * 通过ContentPublishRecord查询状态为PENDING(待发布)的记录，关联Content信息
     */
    private List<Map<String, Object>> queryPendingContents() {
        List<Map<String, Object>> contents = new ArrayList<>();

        if (publishRecordMapper == null) {
            log.warn("ContentPublishRecordMapper未注入，跳过待发布内容查询");
            return contents;
        }

        LambdaQueryWrapper<ContentPublishRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContentPublishRecord::getStatus, 0)
               .eq(ContentPublishRecord::getDeleted, 0)
               .orderByAsc(ContentPublishRecord::getCreateTime)
               .last("LIMIT 50");

        List<ContentPublishRecord> records = publishRecordMapper.selectList(wrapper);
        for (ContentPublishRecord record : records) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", record.getContentId());
            item.put("platform", record.getPlatform());
            item.put("recordId", record.getId());
            contents.add(item);
        }

        if (contents.isEmpty()) {
            log.info("当前无待发布内容");
        } else {
            log.info("查询到 {} 条待发布内容", contents.size());
        }

        return contents;
    }

    /**
     * 发布内容到指定平台
     * 调用ContentPublishService执行实际发布逻辑
     */
    private boolean publishToPlatform(Long contentId, Integer platform) {
        try {
            if (contentMapper == null || contentPublishService == null) {
                log.warn("ContentMapper或ContentPublishService未注入，跳过内容发布: contentId={}, platform={}", contentId, platform);
                return false;
            }

            Content content = contentMapper.selectById(contentId);
            if (content == null) {
                log.warn("内容不存在: contentId={}", contentId);
                return false;
            }
            return contentPublishService.publishToPlatform(content, platform);
        } catch (Exception e) {
            log.error("发布内容失败: contentId={}, platform={}", contentId, platform, e);
            return false;
        }
    }

    /**
     * 更新内容发布状态
     * 同时更新Content表的publishStatus和ContentPublishRecord的状态
     */
    private void updatePublishStatus(Long contentId, String status) {
        try {
            if (contentMapper == null || publishRecordMapper == null) {
                log.warn("ContentMapper或ContentPublishRecordMapper未注入，跳过发布状态更新: contentId={}, status={}", contentId, status);
                return;
            }

            int targetStatus = "success".equals(status) ? PublishStatus.PUBLISHED.getCode() : PublishStatus.FAILED.getCode();

            Content contentUpdate = new Content();
            contentUpdate.setId(contentId);
            contentUpdate.setPublishStatus(targetStatus);
            contentMapper.updateById(contentUpdate);

            LambdaQueryWrapper<ContentPublishRecord> recordWrapper = new LambdaQueryWrapper<>();
            recordWrapper.eq(ContentPublishRecord::getContentId, contentId)
                        .eq(ContentPublishRecord::getStatus, 0)
                        .eq(ContentPublishRecord::getDeleted, 0)
                        .orderByDesc(ContentPublishRecord::getCreateTime)
                .last("LIMIT 1");

            ContentPublishRecord record = publishRecordMapper.selectOne(recordWrapper);
            if (record != null) {
                int recordStatus = "success".equals(status) ? 2 : 3;
                record.setStatus(recordStatus);
                publishRecordMapper.updateById(record);
            }

            log.info("更新内容发布状态: contentId={}, status={}", contentId, status);
        } catch (Exception e) {
            log.error("更新发布状态异常: contentId={}", contentId, e);
        }
    }

    private String toJson(Map<String, Object> data) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }
}
