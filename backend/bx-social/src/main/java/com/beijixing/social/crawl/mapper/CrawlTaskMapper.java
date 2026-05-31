package com.beijixing.social.crawl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.social.crawl.entity.CrawlTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CrawlTaskMapper extends BaseMapper<CrawlTask> {

    @Select("SELECT * FROM crawl_task WHERE status = #{status} AND deleted = 0 ORDER BY create_time DESC LIMIT #{limit}")
    List<CrawlTask> selectByStatus(@Param("status") int status, @Param("limit") int limit);

    @Update("UPDATE crawl_task SET total_comments_found = #{totalFound}, high_intent_count = #{highIntent}, leads_generated = #{leads}, messages_sent = #{messages}, progress_percent = #{progress}, last_crawl_time = NOW() WHERE id = #{id}")
    int updateProgress(@Param("id") Long id, @Param("totalFound") Integer totalFound, @Param("highIntent") Integer highIntent, @Param("leads") Integer leads, @Param("messages") Integer messages, @Param("progress") Integer progress);

    @Update("UPDATE crawl_task SET status = #{status}, error_msg = #{errorMsg}, end_time = CASE WHEN #{status} IN (2, 3) THEN NOW() ELSE end_time END WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") int status, @Param("errorMsg") String errorMsg);
}
