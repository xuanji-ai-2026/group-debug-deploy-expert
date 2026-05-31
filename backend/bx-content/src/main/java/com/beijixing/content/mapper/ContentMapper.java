package com.beijixing.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.beijixing.content.dto.ContentQueryDTO;
import com.beijixing.content.entity.Content;
import com.beijixing.content.vo.ContentListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 内容Mapper
 * @author 胡云 (EMP-CONTENT-001)
 */
@Mapper
public interface ContentMapper extends BaseMapper<Content> {

    /**
     * 分页查询内容列表
     */
    IPage<ContentListVO> selectContentPage(Page<ContentListVO> page, @Param("query") ContentQueryDTO query);

    /**
     * 查询待发布的定时内容
     */
    List<Content> selectScheduledContents(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 更新发布状态
     */
    @Update("UPDATE content SET publish_status = #{publishStatus}, publish_time = #{publishTime}, update_time = NOW() WHERE id = #{id}")
    int updatePublishStatus(@Param("id") Long id, @Param("publishStatus") Integer publishStatus, 
                           @Param("publishTime") LocalDateTime publishTime);

    /**
     * 增加阅读量
     */
    @Update("UPDATE content SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(@Param("id") Long id);

    /**
     * 查询需要定时发布的内容
     */
    @Select("SELECT * FROM content WHERE status = 0 AND scheduled_time <= #{now} AND scheduled_time IS NOT NULL AND deleted = 0")
    List<Content> selectContentsToPublish(@Param("now") LocalDateTime now);
}
