package com.beijixing.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.content.entity.ContentVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 内容版本Mapper
 * @author 胡云 (EMP-CONTENT-001)
 */
@Mapper
public interface ContentVersionMapper extends BaseMapper<ContentVersion> {

    /**
     * 查询内容的版本历史
     */
    @Select("SELECT * FROM content_version WHERE content_id = #{contentId} AND deleted = 0 ORDER BY version DESC")
    List<ContentVersion> selectByContentId(@Param("contentId") Long contentId);

    /**
     * 获取内容最大版本号
     */
    @Select("SELECT MAX(version) FROM content_version WHERE content_id = #{contentId} AND deleted = 0")
    Integer selectMaxVersionByContentId(@Param("contentId") Long contentId);
}
