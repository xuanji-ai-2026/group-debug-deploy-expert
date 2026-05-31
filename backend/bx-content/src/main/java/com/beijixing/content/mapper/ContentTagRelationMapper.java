package com.beijixing.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.content.entity.ContentTagRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 内容与标签关联Mapper
 * @author 胡云 (EMP-CONTENT-001)
 */
@Mapper
public interface ContentTagRelationMapper extends BaseMapper<ContentTagRelation> {

    /**
     * 批量插入关联
     */
    int batchInsert(@Param("list") List<ContentTagRelation> list);

    /**
     * 根据内容ID删除关联
     */
    int deleteByContentId(@Param("contentId") Long contentId);
}
