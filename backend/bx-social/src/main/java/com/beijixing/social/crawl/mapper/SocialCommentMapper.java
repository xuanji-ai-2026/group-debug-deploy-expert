package com.beijixing.social.crawl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.social.crawl.entity.SocialComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SocialCommentMapper extends BaseMapper<SocialComment> {

    @Select("SELECT COUNT(*) FROM social_comment WHERE crawl_task_id = #{taskId} AND deleted = 0")
    int countByTaskId(@Param("taskId") Long taskId);

    @Select("SELECT COUNT(*) FROM social_comment WHERE crawl_task_id = #{taskId} AND is_high_intent = 1 AND deleted = 0")
    int countHighIntentByTaskId(@Param("taskId") Long taskId);

    @Select("SELECT * FROM social_comment WHERE crawl_task_id = #{taskId} AND is_high_intent = 1 AND lead_generated = 0 AND deleted = 0 ORDER BY ai_intent_score DESC LIMIT #{limit}")
    List<SocialComment> selectUnprocessedHighIntentComments(@Param("taskId") Long taskId, @Param("limit") int limit);

    @Select("SELECT * FROM social_comment WHERE platform_code = #{platformCode} AND content_id = #{contentId} AND comment_id = #{commentId} AND deleted = 0")
    SocialComment selectByUniqueKey(@Param("platformCode") String platformCode, @Param("contentId") String contentId, @Param("commentId") String commentId);

    @Select("SELECT * FROM social_comment WHERE platform_code = #{platformCode} AND author_id = #{authorId} AND publish_time >= #{since} AND deleted = 0 ORDER BY publish_time DESC")
    List<SocialComment> selectRecentByAuthor(@Param("platformCode") String platformCode, @Param("authorId") String authorId, @Param("since") LocalDateTime since);
}
