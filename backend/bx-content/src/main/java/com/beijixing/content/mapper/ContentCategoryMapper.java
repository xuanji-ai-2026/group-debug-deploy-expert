package com.beijixing.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.content.entity.ContentCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 内容分类Mapper
 * @author 胡云 (EMP-CONTENT-001)
 */
@Mapper
public interface ContentCategoryMapper extends BaseMapper<ContentCategory> {

    /**
     * 查询所有启用的分类
     */
    @Select("SELECT * FROM content_category WHERE status = 1 AND deleted = 0 ORDER BY sort_order")
    List<ContentCategory> selectAllEnabled();

    /**
     * 查询子分类
     */
    @Select("SELECT * FROM content_category WHERE parent_id = #{parentId} AND deleted = 0 ORDER BY sort_order")
    List<ContentCategory> selectByParentId(@Param("parentId") Long parentId);
}
