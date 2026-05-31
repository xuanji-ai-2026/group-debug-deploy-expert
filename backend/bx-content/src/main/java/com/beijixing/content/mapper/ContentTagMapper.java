package com.beijixing.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.content.entity.ContentTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 内容标签Mapper
 * @author 胡云 (EMP-CONTENT-001)
 */
@Mapper
public interface ContentTagMapper extends BaseMapper<ContentTag> {

    /**
     * 根据名称查询标签
     */
    @Select("SELECT * FROM content_tag WHERE name = #{name} AND deleted = 0 LIMIT 1")
    ContentTag selectByName(@Param("name") String name);

    /**
     * 查询内容的所有标签
     */
    @Select("SELECT t.* FROM content_tag t " +
            "INNER JOIN content_tag_relation tr ON t.id = tr.tag_id " +
            "WHERE tr.content_id = #{contentId} AND t.deleted = 0")
    List<ContentTag> selectTagsByContentId(@Param("contentId") Long contentId);

    /**
     * 增加标签使用次数
     */
    @Update("UPDATE content_tag SET usage_count = usage_count + 1 WHERE id = #{id}")
    int incrementUsageCount(@Param("id") Long id);

    /**
     * 减少标签使用次数
     */
    @Update("UPDATE content_tag SET usage_count = GREATEST(0, usage_count - 1) WHERE id = #{id}")
    int decrementUsageCount(@Param("id") Long id);
}
