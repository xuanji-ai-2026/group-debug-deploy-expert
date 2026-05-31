package com.beijixing.content.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.beijixing.content.dto.ContentAuditDTO;
import com.beijixing.content.dto.ContentDTO;
import com.beijixing.content.dto.ContentQueryDTO;
import com.beijixing.content.vo.ContentListVO;
import com.beijixing.content.vo.ContentPublishRecordVO;
import com.beijixing.content.vo.ContentVO;
import com.beijixing.content.vo.ContentVersionVO;
import com.beijixing.content.vo.SensitiveWordCheckVO;

import java.util.List;

/**
 * 内容服务接口 - CO-001: 内容CRUD API
 * @author 胡云 (EMP-CONTENT-001)
 */
public interface ContentService {

    /**
     * 创建内容
     */
    ContentVO createContent(ContentDTO dto);

    /**
     * 更新内容
     */
    ContentVO updateContent(ContentDTO dto);

    /**
     * 删除内容
     */
    void deleteContent(Long id);

    /**
     * 获取内容详情
     */
    ContentVO getContentById(Long id);

    /**
     * 分页查询内容列表
     */
    IPage<ContentListVO> listContents(ContentQueryDTO query);

    /**
     * 保存草稿
     */
    ContentVO saveDraft(ContentDTO dto);

    /**
     * 发布内容 (立即发布)
     */
    ContentVO publishContent(Long id, List<Integer> platforms);

    /**
     * 撤回内容
     */
    ContentVO withdrawContent(Long id);

    /**
     * 置顶/取消置顶
     */
    ContentVO toggleTop(Long id, Boolean isTop);

    /**
     * 增加阅读量
     */
    void incrementViewCount(Long id);

    /**
     * 批量删除内容
     */
    void batchDelete(List<Long> ids);

    /**
     * 批量发布
     */
    void batchPublish(List<Long> ids, List<Integer> platforms);

    /**
     * 获取内容版本历史
     */
    List<ContentVersionVO> getContentVersions(Long contentId);

    /**
     * 回滚到指定版本
     */
    ContentVO rollbackVersion(Long contentId, Integer version);

    /**
     * 检查违禁词 - CO-005: 违禁词过滤API
     */
    SensitiveWordCheckVO checkSensitiveWords(String content);

    /**
     * 提交审核 - CO-007: 内容审核
     */
    ContentVO submitAudit(Long contentId);

    /**
     * 审核内容
     */
    ContentVO auditContent(ContentAuditDTO dto);

    /**
     * 获取发布记录
     */
    List<ContentPublishRecordVO> getPublishRecords(Long contentId);

    /**
     * 定时发布 - CO-002: 定时发布队列
     */
    void schedulePublish(Long contentId, java.time.LocalDateTime scheduledTime, List<Integer> platforms);

    /**
     * 取消定时发布
     */
    void cancelSchedulePublish(Long contentId);
}
