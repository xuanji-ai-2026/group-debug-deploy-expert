package com.beijixing.content.service;

import com.beijixing.content.dto.ContentTagDTO;
import com.beijixing.content.vo.ContentTagVO;

import java.util.List;

/**
 * 内容标签服务接口 - CO-006: 内容标签管理
 * @author 胡云 (EMP-CONTENT-001)
 */
public interface ContentTagService {

    /**
     * 创建标签
     */
    ContentTagVO createTag(ContentTagDTO dto);

    /**
     * 更新标签
     */
    ContentTagVO updateTag(ContentTagDTO dto);

    /**
     * 删除标签
     */
    void deleteTag(Long id);

    /**
     * 获取标签详情
     */
    ContentTagVO getTagById(Long id);

    /**
     * 获取所有标签
     */
    List<ContentTagVO> listAllTags();

    /**
     * 根据名称搜索标签
     */
    List<ContentTagVO> searchTags(String keyword);

    /**
     * 获取热门标签
     */
    List<ContentTagVO> getHotTags(Integer limit);

    /**
     * 获取内容的标签列表
     */
    List<ContentTagVO> getTagsByContentId(Long contentId);
}
