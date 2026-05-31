package com.beijixing.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.content.entity.ContentPublishRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 内容发布记录Mapper
 * @author 胡云 (EMP-CONTENT-001)
 */
@Mapper
public interface ContentPublishRecordMapper extends BaseMapper<ContentPublishRecord> {

    /**
     * 查询内容的发布记录
     */
    @Select("SELECT * FROM content_publish_record WHERE content_id = #{contentId} AND deleted = 0 ORDER BY create_time DESC")
    List<ContentPublishRecord> selectByContentId(@Param("contentId") Long contentId);

    /**
     * 查询待重试的发布记录
     */
    @Select("SELECT * FROM content_publish_record WHERE status = 3 AND retry_count < max_retry_count AND deleted = 0")
    List<ContentPublishRecord> selectRetryRecords();

    /**
     * 更新重试次数和状态
     */
    @Update("UPDATE content_publish_record SET retry_count = retry_count + 1, status = #{status}, " +
            "error_msg = #{errorMsg}, update_time = NOW() WHERE id = #{id}")
    int updateRetryStatus(@Param("id") Long id, @Param("status") Integer status, @Param("errorMsg") String errorMsg);

    /**
     * 查询需要发布的内容记录
     */
    List<ContentPublishRecord> selectPendingRecords(@Param("limit") Integer limit);

    /**
     * 根据内容ID和平台查询最新的发布记录
     */
    @Select("SELECT * FROM content_publish_record WHERE content_id = #{contentId} AND platform = #{platform} AND deleted = 0 ORDER BY create_time DESC LIMIT 1")
    ContentPublishRecord selectLatestByContentAndPlatform(@Param("contentId") Long contentId, @Param("platform") Integer platform);
}
