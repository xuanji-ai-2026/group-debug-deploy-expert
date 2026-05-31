package com.beijixing.content.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.beijixing.content.dto.ContentAuditDTO;
import com.beijixing.content.dto.ContentDTO;
import com.beijixing.content.dto.ContentQueryDTO;
import com.beijixing.content.entity.*;
import com.beijixing.content.enums.*;
import com.beijixing.content.exception.ContentException;
import com.beijixing.content.mapper.*;
import com.beijixing.content.service.ContentPublishService;
import com.beijixing.content.service.ContentService;
import com.beijixing.content.vo.*;
import com.beijixing.social.compliance.service.SensitiveWordFilterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 内容服务实现类
 * @author 胡云 (EMP-CONTENT-001)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("nullness")
public class ContentServiceImpl extends ServiceImpl<ContentMapper, Content> implements ContentService {

    private final ContentMapper contentMapper;
    private final ContentVersionMapper versionMapper;
    private final ContentTagMapper tagMapper;
    private final ContentTagRelationMapper tagRelationMapper;
    private final ContentAuditRecordMapper auditRecordMapper;
    private final ContentPublishRecordMapper publishRecordMapper;
    private final ContentPublishService publishService;
    private final RedissonClient redissonClient;
    private final SensitiveWordFilterService sensitiveWordFilterService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContentVO createContent(ContentDTO dto) {
        // 检查违禁词
        SensitiveWordCheckVO checkResult = checkSensitiveWords(dto.getContent());
        if (Boolean.TRUE.equals(checkResult.getHasSensitive()) && checkResult.getRiskLevel() >= 3) {
            throw new ContentException("内容包含高风险违禁词，请修改后再提交");
        }

        Content content = new Content();
        BeanUtils.copyProperties(dto, content);
        
        // 设置默认值
        content.setStatus(ContentStatus.DRAFT.getCode());
        content.setPublishStatus(PublishStatus.UNPUBLISHED.getCode());
        content.setViewCount(0);
        content.setLikeCount(0);
        content.setCommentCount(0);
        content.setShareCount(0);
        content.setVersion(1);
        content.setAuthorId(getCurrentUserId());
        content.setAuthorName(getCurrentUserName());
        
        // 处理标签
        if (!CollectionUtils.isEmpty(dto.getTags())) {
            content.setTags(JSON.toJSONString(dto.getTags()));
        }

        contentMapper.insert(content);

        // 保存标签关联
        saveContentTags(content.getId(), dto.getTags());

        // 创建版本记录
        createVersionRecord(content, VersionOperationType.CREATE);

        return convertToVO(content);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContentVO updateContent(ContentDTO dto) {
        if (dto.getId() == null) {
            throw new ContentException("内容ID不能为空");
        }

        Content content = contentMapper.selectById(dto.getId());
        if (content == null || content.getDeleted() == 1) {
            throw new ContentException("内容不存在");
        }

        // 检查违禁词
        SensitiveWordCheckVO checkResult = checkSensitiveWords(dto.getContent());
        if (Boolean.TRUE.equals(checkResult.getHasSensitive()) && checkResult.getRiskLevel() >= 3) {
            throw new ContentException("内容包含高风险违禁词，请修改后再提交");
        }

        BeanUtils.copyProperties(dto, content);
        
        // 更新标签
        if (!CollectionUtils.isEmpty(dto.getTags())) {
            content.setTags(JSON.toJSONString(dto.getTags()));
        }
        
        content.setVersion(content.getVersion() + 1);
        contentMapper.updateById(content);

        // 更新标签关联
        tagRelationMapper.deleteByContentId(content.getId());
        saveContentTags(content.getId(), dto.getTags());

        // 创建版本记录
        createVersionRecord(content, VersionOperationType.EDIT);

        return convertToVO(content);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteContent(Long id) {
        Content content = contentMapper.selectById(id);
        if (content == null) {
            throw new ContentException("内容不存在");
        }
        
        contentMapper.deleteById(id);
        
        // 删除标签关联
        tagRelationMapper.deleteByContentId(id);
        
        log.info("删除内容: id={}", id);
    }

    @Override
    public ContentVO getContentById(Long id) {
        Content content = contentMapper.selectById(id);
        if (content == null || content.getDeleted() == 1) {
            throw new ContentException("内容不存在");
        }
        return convertToVO(content);
    }

    @Override
    public IPage<ContentListVO> listContents(ContentQueryDTO query) {
        Page<ContentListVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        return contentMapper.selectContentPage(page, query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContentVO saveDraft(ContentDTO dto) {
        if (dto.getId() == null) {
            Content content = new Content();
            BeanUtils.copyProperties(dto, content);
            content.setStatus(ContentStatus.DRAFT.getCode());
            content.setPublishStatus(PublishStatus.UNPUBLISHED.getCode());
            content.setAuthorId(getCurrentUserId());
            content.setAuthorName(getCurrentUserName());
            content.setVersion(1);
            if (!CollectionUtils.isEmpty(dto.getTags())) {
                content.setTags(JSON.toJSONString(dto.getTags()));
            }
            contentMapper.insert(content);
            saveContentTags(content.getId(), dto.getTags());
            return convertToVO(content);
        } else {
            return updateContent(dto);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContentVO publishContent(Long id, List<Integer> platforms) {
        Content content = contentMapper.selectById(id);
        if (content == null) {
            throw new ContentException("内容不存在");
        }

        // 提交审核或发布
        if (content.getStatus() == ContentStatus.DRAFT.getCode()) {
            content.setStatus(ContentStatus.PUBLISHED.getCode());
            content.setPublishStatus(PublishStatus.PUBLISHING.getCode());
            content.setPublishTime(LocalDateTime.now());
            contentMapper.updateById(content);
            
            // 创建版本记录
            createVersionRecord(content, VersionOperationType.PUBLISH);
        }

        // 多平台发布
        if (!CollectionUtils.isEmpty(platforms)) {
            publishService.publishToPlatforms(id, platforms);
        }

        return convertToVO(content);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContentVO withdrawContent(Long id) {
        Content content = contentMapper.selectById(id);
        if (content == null) {
            throw new ContentException("内容不存在");
        }

        content.setStatus(ContentStatus.WITHDRAWN.getCode());
        content.setPublishStatus(PublishStatus.UNPUBLISHED.getCode());
        contentMapper.updateById(content);

        // 创建版本记录
        createVersionRecord(content, VersionOperationType.WITHDRAW);

        return convertToVO(content);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContentVO toggleTop(Long id, Boolean isTop) {
        Content content = contentMapper.selectById(id);
        if (content == null) {
            throw new ContentException("内容不存在");
        }

        content.setIsTop(isTop);
        if (Boolean.TRUE.equals(isTop)) {
            content.setSortOrder(9999);
        } else {
            content.setSortOrder(0);
        }
        contentMapper.updateById(content);

        return convertToVO(content);
    }

    @Override
    public void incrementViewCount(Long id) {
        contentMapper.incrementViewCount(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        if (!CollectionUtils.isEmpty(ids)) {
            for (Long id : ids) {
                deleteContent(id);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchPublish(List<Long> ids, List<Integer> platforms) {
        if (!CollectionUtils.isEmpty(ids)) {
            for (Long id : ids) {
                publishContent(id, platforms);
            }
        }
    }

    @Override
    public List<ContentVersionVO> getContentVersions(Long contentId) {
        List<ContentVersion> versions = versionMapper.selectByContentId(contentId);
        return versions.stream().map(this::convertVersionToVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContentVO rollbackVersion(Long contentId, Integer version) {
        ContentVersion contentVersion = versionMapper.selectOne(
            new LambdaQueryWrapper<ContentVersion>()
                .eq(ContentVersion::getContentId, contentId)
                .eq(ContentVersion::getVersion, version)
        );
        
        if (contentVersion == null) {
            throw new ContentException("版本不存在");
        }

        Content content = contentMapper.selectById(contentId);
        if (content == null) {
            throw new ContentException("内容不存在");
        }

        // 回滚内容
        content.setTitle(contentVersion.getTitle());
        content.setContent(contentVersion.getContent());
        content.setSummary(contentVersion.getSummary());
        content.setCoverImage(contentVersion.getCoverImage());
        content.setTags(contentVersion.getTags());
        content.setVersion(content.getVersion() + 1);
        contentMapper.updateById(content);

        // 创建新版本记录
        createVersionRecord(content, VersionOperationType.EDIT);

        return convertToVO(content);
    }

    @Override
    public SensitiveWordCheckVO checkSensitiveWords(String content) {
        SensitiveWordCheckVO result = new SensitiveWordCheckVO();
        result.setHasSensitive(false);
        result.setRiskLevel(0);
        result.setSensitiveWords(new ArrayList<>());
        
        long start = System.currentTimeMillis();
        
        SensitiveWordFilterService.FilterResult filterResult = sensitiveWordFilterService.detect(content);
        
        if (!filterResult.isSafe() && filterResult.getHitWords() != null) {
            result.setHasSensitive(true);
            result.setSensitiveWords(filterResult.getHitWords());
            
            int hitCount = filterResult.getHitWords().size();
            double safetyScore = filterResult.getSafetyScore();
            
            if (hitCount > 5 || safetyScore < 30) {
                result.setRiskLevel(3);
                result.setRiskLevelName("高风险");
                result.setSuggestion("内容包含大量敏感词或安全评分过低，建议重新编辑");
            } else if (hitCount > 2 || safetyScore < 60) {
                result.setRiskLevel(2);
                result.setRiskLevelName("中风险");
                result.setSuggestion("内容存在较多敏感词，建议修改后重新提交");
            } else if (hitCount > 0) {
                result.setRiskLevel(1);
                result.setRiskLevelName("低风险");
                result.setSuggestion("建议修改敏感词后再发布");
            }
        } else {
            result.setRiskLevel(0);
            result.setRiskLevelName("无风险");
            result.setSuggestion("内容合规");
        }
        
        result.setCheckTime(System.currentTimeMillis() - start);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContentVO submitAudit(Long contentId) {
        Content content = contentMapper.selectById(contentId);
        if (content == null) {
            throw new ContentException("内容不存在");
        }

        content.setStatus(ContentStatus.AUDITING.getCode());
        contentMapper.updateById(content);

        // 创建AI审核记录
        ContentAuditRecord auditRecord = new ContentAuditRecord();
        auditRecord.setContentId(contentId);
        auditRecord.setAuditType(AuditType.AI.getCode());
        auditRecord.setAuditResult(AuditResult.PENDING.getCode());
        auditRecordMapper.insert(auditRecord);

        SensitiveWordFilterService.FilterResult filterResult = sensitiveWordFilterService.detect(content.getContent());
        
        if (!filterResult.isSafe()) {
            auditRecord.setAuditResult(AuditResult.REJECTED.getCode());
            auditRecord.setAuditOpinion("AI审核未通过: 检测到" + filterResult.getHitWords().size() + "个敏感词，安全评分" + filterResult.getSafetyScore());
            content.setStatus(ContentStatus.DRAFT.getCode());
        } else {
            auditRecord.setAuditResult(AuditResult.PASSED.getCode());
            auditRecord.setAuditOpinion("AI审核通过: 安全评分" + filterResult.getSafetyScore());
            content.setStatus(ContentStatus.PUBLISHED.getCode());
        }
        auditRecordMapper.updateById(auditRecord);
        contentMapper.updateById(content);
        
        log.info("AI审核完成: contentId={}, result={}, score={}", 
                contentId, auditRecord.getAuditResult(), filterResult.getSafetyScore());

        return convertToVO(content);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContentVO auditContent(ContentAuditDTO dto) {
        Content content = contentMapper.selectById(dto.getContentId());
        if (content == null) {
            throw new ContentException("内容不存在");
        }

        // 创建人工审核记录
        ContentAuditRecord auditRecord = new ContentAuditRecord();
        auditRecord.setContentId(dto.getContentId());
        auditRecord.setAuditType(AuditType.MANUAL.getCode());
        auditRecord.setAuditResult(dto.getAuditResult());
        auditRecord.setAuditOpinion(dto.getAuditOpinion());
        auditRecord.setAuditorId(getCurrentUserId());
        auditRecord.setAuditorName(getCurrentUserName());
        auditRecord.setAuditTime(LocalDateTime.now());
        auditRecordMapper.insert(auditRecord);

        // 更新内容状态
        if (dto.getAuditResult() == AuditResult.PASSED.getCode()) {
            content.setStatus(ContentStatus.PUBLISHED.getCode());
        } else if (dto.getAuditResult() == AuditResult.REJECTED.getCode()) {
            content.setStatus(ContentStatus.DRAFT.getCode());
        }
        contentMapper.updateById(content);

        return convertToVO(content);
    }

    @Override
    public List<ContentPublishRecordVO> getPublishRecords(Long contentId) {
        List<ContentPublishRecord> records = publishRecordMapper.selectByContentId(contentId);
        return records.stream().map(this::convertPublishRecordToVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void schedulePublish(Long contentId, LocalDateTime scheduledTime, List<Integer> platforms) {
        Content content = contentMapper.selectById(contentId);
        if (content == null) {
            throw new ContentException("内容不存在");
        }

        content.setScheduledTime(scheduledTime);
        contentMapper.updateById(content);

        // 使用Redisson延迟队列实现时间轮盘 CO-002
        long delay = scheduledTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() 
                    - System.currentTimeMillis();
        
        if (delay > 0) {
            RDelayedQueue<SchedulePublishTask> delayedQueue = redissonClient.getDelayedQueue(
                redissonClient.getQueue("content:schedule:queue")
            );
            
            SchedulePublishTask task = new SchedulePublishTask();
            task.setContentId(contentId);
            task.setPlatforms(platforms);
            task.setScheduledTime(scheduledTime);
            
            delayedQueue.offer(task, delay, TimeUnit.MILLISECONDS);
            log.info("添加定时发布任务: contentId={}, scheduledTime={}", contentId, scheduledTime);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelSchedulePublish(Long contentId) {
        Content content = contentMapper.selectById(contentId);
        if (content == null) {
            throw new ContentException("内容不存在");
        }

        content.setScheduledTime(null);
        contentMapper.updateById(content);

        try {
            org.redisson.api.RQueue<SchedulePublishTask> queue = redissonClient.getQueue("content:schedule:queue");
            java.util.Iterator<SchedulePublishTask> iterator = queue.iterator();
            while (iterator.hasNext()) {
                SchedulePublishTask task = iterator.next();
                if (task.getContentId().equals(contentId)) {
                    iterator.remove();
                    log.info("已从延迟队列中移除定时发布任务: contentId={}", contentId);
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("从延迟队列移除任务失败(任务可能已被消费): contentId={}, error={}", contentId, e.getMessage());
        }

        log.info("取消定时发布: contentId={}", contentId);
    }

    // ============ 私有方法 ============

    private void saveContentTags(Long contentId, List<String> tagNames) {
        if (CollectionUtils.isEmpty(tagNames)) {
            return;
        }

        for (String tagName : tagNames) {
            ContentTag tag = tagMapper.selectByName(tagName);
            if (tag == null) {
                tag = new ContentTag();
                tag.setName(tagName);
                tag.setStatus(1);
                tag.setUsageCount(0);
                tagMapper.insert(tag);
            }

            // 创建关联
            ContentTagRelation relation = new ContentTagRelation();
            relation.setContentId(contentId);
            relation.setTagId(tag.getId());
            tagRelationMapper.insert(relation);

            // 增加使用次数
            tagMapper.incrementUsageCount(tag.getId());
        }
    }

    private void createVersionRecord(Content content, VersionOperationType operationType) {
        ContentVersion version = new ContentVersion();
        version.setContentId(content.getId());
        version.setVersion(content.getVersion());
        version.setTitle(content.getTitle());
        version.setContent(content.getContent());
        version.setSummary(content.getSummary());
        version.setCoverImage(content.getCoverImage());
        version.setTags(content.getTags());
        version.setOperatorId(getCurrentUserId());
        version.setOperatorName(getCurrentUserName());
        version.setOperationType(operationType.getCode());
        versionMapper.insert(version);
    }

    @SuppressWarnings("nullness")
    private ContentVO convertToVO(Content content) {
        ContentVO vo = new ContentVO();
        BeanUtils.copyProperties(content, vo);
        
        // 设置枚举名称
        vo.setContentTypeName(ContentType.getNameByCode(content.getContentType()));
        vo.setStatusName(ContentStatus.getNameByCode(content.getStatus()));
        vo.setPublishStatusName(PublishStatus.getNameByCode(content.getPublishStatus()));
        
        // 解析标签
        if (StringUtils.hasText(content.getTags())) {
            vo.setTags(JSON.parseArray(content.getTags(), String.class));
        }
        
        return vo;
    }

    private ContentVersionVO convertVersionToVO(ContentVersion version) {
        ContentVersionVO vo = new ContentVersionVO();
        BeanUtils.copyProperties(version, vo);
        vo.setOperationTypeName(VersionOperationType.getNameByCode(version.getOperationType()));
        return vo;
    }

    @SuppressWarnings("nullness")
    private ContentPublishRecordVO convertPublishRecordToVO(ContentPublishRecord record) {
        ContentPublishRecordVO vo = new ContentPublishRecordVO();
        BeanUtils.copyProperties(record, vo);
        vo.setPlatformName(PublishPlatform.getNameByCode(record.getPlatform()));
        vo.setStatusName(PublishRecordStatus.getNameByCode(record.getStatus()));
        return vo;
    }

    private Long getCurrentUserId() {
        return 1L;
    }

    private String getCurrentUserName() {
        return "系统用户";
    }
}
