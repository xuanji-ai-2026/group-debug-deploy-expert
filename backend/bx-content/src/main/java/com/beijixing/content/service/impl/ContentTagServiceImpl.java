package com.beijixing.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.beijixing.content.dto.ContentTagDTO;
import com.beijixing.content.entity.ContentTag;
import com.beijixing.content.exception.ContentException;
import com.beijixing.content.mapper.ContentTagMapper;
import com.beijixing.content.service.ContentTagService;
import com.beijixing.content.vo.ContentTagVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 内容标签服务实现类
 * @author 胡云 (EMP-CONTENT-001)
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("nullness")
public class ContentTagServiceImpl extends ServiceImpl<ContentTagMapper, ContentTag> implements ContentTagService {

    private final ContentTagMapper tagMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContentTagVO createTag(ContentTagDTO dto) {
        // 检查标签名是否已存在
        ContentTag existing = tagMapper.selectByName(dto.getName());
        if (existing != null) {
            throw new ContentException("标签名称已存在");
        }

        ContentTag tag = new ContentTag();
        BeanUtils.copyProperties(dto, tag);
        tag.setUsageCount(0);
        tag.setStatus(1);
        tagMapper.insert(tag);

        return convertToVO(tag);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContentTagVO updateTag(ContentTagDTO dto) {
        if (dto.getId() == null) {
            throw new ContentException("标签ID不能为空");
        }

        ContentTag tag = tagMapper.selectById(dto.getId());
        if (tag == null) {
            throw new ContentException("标签不存在");
        }

        // 检查名称是否冲突
        if (!tag.getName().equals(dto.getName())) {
            ContentTag existing = tagMapper.selectByName(dto.getName());
            if (existing != null) {
                throw new ContentException("标签名称已存在");
            }
        }

        BeanUtils.copyProperties(dto, tag);
        tagMapper.updateById(tag);

        return convertToVO(tag);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTag(Long id) {
        ContentTag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new ContentException("标签不存在");
        }

        // 检查是否有内容使用该标签
        if (tag.getUsageCount() > 0) {
            throw new ContentException("该标签正在被使用，无法删除");
        }

        tagMapper.deleteById(id);
    }

    @Override
    public ContentTagVO getTagById(Long id) {
        ContentTag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new ContentException("标签不存在");
        }
        return convertToVO(tag);
    }

    @Override
    public List<ContentTagVO> listAllTags() {
        List<ContentTag> tags = tagMapper.selectList(
            new LambdaQueryWrapper<ContentTag>()
                .eq(ContentTag::getStatus, 1)
                .orderByAsc(ContentTag::getSortOrder)
        );
        return tags.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public List<ContentTagVO> searchTags(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return listAllTags();
        }

        List<ContentTag> tags = tagMapper.selectList(
            new LambdaQueryWrapper<ContentTag>()
                .like(ContentTag::getName, keyword)
                .or()
                .like(ContentTag::getAlias, keyword)
                .eq(ContentTag::getStatus, 1)
                .orderByDesc(ContentTag::getUsageCount)
        );
        return tags.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public List<ContentTagVO> getHotTags(Integer limit) {
        List<ContentTag> tags = tagMapper.selectList(
            new LambdaQueryWrapper<ContentTag>()
                .eq(ContentTag::getStatus, 1)
                .orderByDesc(ContentTag::getUsageCount)
                .last("LIMIT " + limit)
        );
        return tags.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public List<ContentTagVO> getTagsByContentId(Long contentId) {
        List<ContentTag> tags = tagMapper.selectTagsByContentId(contentId);
        return tags.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @SuppressWarnings("nullness")
    private ContentTagVO convertToVO(ContentTag tag) {
        ContentTagVO vo = new ContentTagVO();
        BeanUtils.copyProperties(tag, vo);
        vo.setStatusName(tag.getStatus() == 1 ? "启用" : "禁用");
        
        // 获取父标签名称
        if (tag.getParentId() != null) {
            ContentTag parent = tagMapper.selectById(tag.getParentId());
            if (parent != null) {
                vo.setParentName(parent.getName());
            }
        }
        
        return vo;
    }
}
